package com.tus.coupon.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.tus.coupon.common.context.UserContext;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.mapper.CouponTemplateMapper;
import com.tus.coupon.common.enums.CouponTemplateStatusEnum;
import com.tus.coupon.common.exception.ClientException;
import com.tus.coupon.common.exception.ServiceException;
import com.tus.coupon.common.mq.event.CouponTemplateDelayEvent;
import com.tus.coupon.merchant.dto.req.CouponTemplateCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTemplateNumberReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTemplatePageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplatePageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplateQueryRespDTO;
import com.tus.coupon.merchant.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tus.coupon.common.constants.MerchantRedisConstant.COUPON_TEMPLATE_KEY;
import static com.tus.coupon.merchant.common.constant.MerchantConstant.CREATE_COUPON_TEMPLATE_LOG_CONTENT;
import static com.tus.coupon.merchant.common.constant.MerchantConstant.INCREASE_NUMBER_COUPON_TEMPLATE_LOG_CONTENT;
import static com.tus.coupon.merchant.common.constant.MerchantConstant.TERMINATE_COUPON_TEMPLATE_LOG_CONTENT;

@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper,
        CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    // private final
    private final StringRedisTemplate stringRedisTemplate;
    private final RBloomFilter<String> couponTemplateQueryBloomFilter;

    @LogRecord(
            success = CREATE_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#bixNo}}",
            extra = "{{#requestParam.toString()}}"
    )

    @Override
    public void createCouponTemplate(CouponTemplateCreateReqDTO requestParam) {
        // validate received params valid or not via responsibility chain
        // todo

        // parse received param & construct coupon template and save to db
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam,
                CouponTemplateDO.class);
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        couponTemplateDO.setShopNumber(UserContext.getShopNumber());
        couponTemplateMapper.insert(couponTemplateDO);

        // because template id is generated after it is sync/received by db layer
        // which could be missing to the log collector
        LogRecordContext.putVariable("bizNo", couponTemplateDO.getId());

        // cache warm up: load db sync records into JSON to redis
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO,
                CouponTemplateQueryRespDTO.class);
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
        String couponTemplateCacheKey = String.format(COUPON_TEMPLATE_KEY,
                couponTemplateDO.getId());

        // here we add lua scripts to set Hash data and coupon template's expiry
        String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV -1)) " +
                "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";
        List<String> keys = Collections.singletonList(couponTemplateCacheKey);
        List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
        actualCacheTargetMap.forEach((key, value) -> {
            args.add(key);
            args.add(value);
        });

        // convert coupon template exirty date into second unit in Unix
        args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime() / 1000));

        // here we invoke Lua script
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                keys,
                args.toArray()
        );

        // delivery delay event
        CouponTemplateDelayEvent templateDelayEvent = CouponTemplateDelayEvent.builder()
                .shopNumber(UserContext.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .delayTime(couponTemplateDO.getValidEndTime().getTime())
                .build();

        // couponTemplateDelayExecuteStatusProducer.sendMessage(templateDelayEvent);

        // add coupon template id to bloom filter
        couponTemplateQueryBloomFilter.add(String.valueOf(couponTemplateDO.getId()));
    }

    @Override
    public IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        // Construct pagination query
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .like(StrUtil.isNotBlank(requestParam.getName()), CouponTemplateDO::getName, requestParam.getName())
                .like(StrUtil.isNotBlank(requestParam.getGoodsNumber()), CouponTemplateDO::getGoods,
                        requestParam.getGoodsNumber())
                .eq(Objects.nonNull(requestParam.getType()), CouponTemplateDO::getType, requestParam.getType())
                .eq(Objects.nonNull(requestParam.getTarget()), CouponTemplateDO::getTarget, requestParam.getTarget());

        IPage<CouponTemplateDO> selectPage = couponTemplateMapper.selectPage(requestParam, queryWrapper);

        return selectPage.convert(each -> BeanUtil.toBean(each, CouponTemplatePageQueryRespDTO.class));
    }

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId) {
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, couponTemplateId);

        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        return BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
    }

    @LogRecord(
            success = TERMINATE_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#couponTemplateId}}"
    )
    @Override
    public void terminateCouponTemplate(String couponTemplateId) {
        // validate whether there are permission leak
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, couponTemplateId);
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (couponTemplateDO == null) {
            // if coupon template cannot be found in db, then there is a permission leak
            throw new ClientException("coupon template invalid, please " +
                    "check operation illegal");
        }

        // coupon template valid
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("Coupon Template Expiry");
        }

        // record coupon template original content
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        // modify coupon template to expire
        CouponTemplateDO updateCouponTemplateDO = CouponTemplateDO.builder()
                .status(CouponTemplateStatusEnum.ENDED.getStatus())
                .build();
        Wrapper<CouponTemplateDO> updateWrapper = Wrappers.lambdaUpdate(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getId, couponTemplateDO.getId())
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber());
        couponTemplateMapper.update(updateCouponTemplateDO, updateWrapper);

        // sync expiry status of the coupon template to cache
        String couponTemplateCacheKey = String.format(COUPON_TEMPLATE_KEY, couponTemplateId);
        stringRedisTemplate.opsForHash().put(couponTemplateCacheKey, "status",
                String.valueOf(CouponTemplateStatusEnum.ENDED.getStatus()));
    }

    @LogRecord(
            success = INCREASE_NUMBER_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#requestParam.couponTemplateId}}"
    )
    @Override
    public void increaseCouponIssuanceNumber(CouponTemplateNumberReqDTO requestParam) {
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, requestParam.getCouponTemplateId());
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (couponTemplateDO == null) {
            // once coupon template cannot be found under current context user account
            // we can decide there is a leak permission
            throw new ClientException("coupon template not exist under " +
                    "current user account, please double check if there is " +
                    "a permission leak");
        }

        // validate whether coupon still in validity
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("coupon template already got expired");
        }

        // record increase the number of coupon issuance number on log layer
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        // set db layer coupon issuance number via updating coupon template db record
        int increased = couponTemplateMapper.increaseNumberCouponTemplate(UserContext.getShopNumber(),
                Long.valueOf(requestParam.getCouponTemplateId()), requestParam.getNumber());
        if (!SqlHelper.retBool(increased)) {
            throw new ServiceException("Increase coupon issuance number failed");
        }

        // sync db layer successfully increased coupon issuance number to cache layer
        String couponTemplateCacheKey = String.format(COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        stringRedisTemplate.opsForHash().increment(couponTemplateCacheKey, "stock", requestParam.getNumber());
    }
}
