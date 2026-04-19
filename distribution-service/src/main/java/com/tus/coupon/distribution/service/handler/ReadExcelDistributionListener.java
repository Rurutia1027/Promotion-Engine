package com.tus.coupon.distribution.service.handler;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.entity.CouponTaskFailDO;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.distribution.common.DistributionRedisConstant;
import com.tus.coupon.distribution.common.EngineRedisConstant;
import com.tus.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.tus.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import com.tus.coupon.distribution.tookit.StockDecrementReturnCombinedUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Map;

import static com.tus.coupon.distribution.common.DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_PROGRESS_KEY;

// coupon excel record read and task convert listener
@RequiredArgsConstructor
public class ReadExcelDistributionListener extends AnalysisEventListener<CouponTaskExcelObject> {
    private final CouponTaskDO couponTaskDO;
    private final CouponTemplateDO couponTemplateDO;
    private final CouponTaskFailMapper couponTaskFailMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponExecuteDistributionProducer couponExecuteDistributionProducer;

    private int rowCount = 1;
    private final static String STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH = "lua" +
            "/stock_decrement_and_batch_save_user_record.lua";
    private final static int BATCH_USER_COUPON_SIZE = 5000;

    @Override
    public void invoke(CouponTaskExcelObject data, AnalysisContext context) {
        Long couponTaskId = couponTaskDO.getId();

        // fetch current release progress
        String templateTaskExecuteProgressKey =
                String.format(TEMPLATE_TASK_EXECUTE_PROGRESS_KEY, couponTaskId);
        String progress =
                stringRedisTemplate.opsForValue().get(templateTaskExecuteProgressKey);
        if (StrUtil.isNotBlank(progress) && Integer.parseInt(progress) >= rowCount) {
            ++rowCount;
            return;
        }

        // fetch lua script, and let Hutool's Singleton holds the lua script in memory to avoid
        // secondary loading from disk
        DefaultRedisScript<Long> buildLuaScript =
                Singleton.get(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH, () -> {
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH)));
                    redisScript.setResultType(Long.class);
                    return redisScript;
                });

        // here we combine the parameters that gonna passing to lua script and commit to
        // redis server side
        // lua script key1: coupon template key (prefix + coupon template id)
        String couponTemplateKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY,
                couponTemplateDO.getId());

        // lua script key2: batch user set key , set : Excel file : task  => 1:1:1
        // key = (prefix + couponTaskId)
        String batchUserSetKey =
                String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY,
                        couponTaskId);

        // lua script args1 and args2
        Map<Object, Object> userRowNumMap = MapUtil.builder()
                .put("userId", data.getUserId())
                .put("rowNum", rowCount + 1)
                .build();

        Long combinedField = stringRedisTemplate.execute(buildLuaScript,
                ListUtil.of(couponTemplateKey, batchUserSetKey),
                JSON.toJSONString(userRowNumMap));

        // return value combined field contains two field, field one reflects committed
        // script + params executes success on server side,
        // field two reflects the size of user set (how many user in total pre-allocated
        // coupon success, only when field one status is true)
        boolean firstField =
                StockDecrementReturnCombinedUtil.extractFirstField(combinedField);

        if (!firstField) {
            // sync current process to redis cache, then move rowCount(pointer that iterate
            // each records in Excel file)
            stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey,
                    String.valueOf(rowCount));
            ++rowCount;

            // also wrap the coupon distribution failure info to db table
            Map<Object, Object> objectMap = MapUtil.builder()
                    .put("rowNum", rowCount)
                    .put("cause", "coupon run out of usage")
                    .build();
            CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                    .batchId(couponTaskDO.getBatchId()) // which batch the coupon
                    // distribution got failed
                    .jsonObject(JSON.toJSONString(objectMap, SerializerFeature.WriteMapNullValue))
                    .build();
            couponTaskFailMapper.insert(couponTaskFailDO);
            return;
        }

        // every thing goes right, it means current Excel record corresponding user
        // successfully pre-allocated the coupon
        // here we got batch user set size on redis side (only user allocated coupon success
        // will the user key append to the set, so the set size means how many user in total
        // have been successfully assigned the coupon)
        int batchUserSetSize =
                StockDecrementReturnCombinedUtil.extractSecondField(combinedField.intValue());

        // downstream notify message only be sent when total number attach to the batch user
        // size threshold and current task notify type is not null
        if (batchUserSetSize < BATCH_USER_COUPON_SIZE && StrUtil.isBlank(couponTaskDO.getNotifyType())) {
            // sync current Excel file record iterate progress to cache
            stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey,
                    String.valueOf(rowCount));
            ++rowCount;
            return;
        }

        // here we encapsulate current context into message and deliver to the downstream MQ
        // when downstream MQ receive that event it will retrieve user id set from redis by
        // the redis user set key , and send notification via previous configured type to
        CouponTemplateDistributionEvent couponTemplateDistributionEvent =
                CouponTemplateDistributionEvent.builder()
                        .userId(data.getUserId())
                        .mail(data.getMail())
                        .phone(data.getPhone())
                        .couponTaskId(couponTaskId)
                        .notifyType(couponTaskDO.getNotifyType())
                        .shopNumber(couponTaskDO.getShopNumber())
                        .couponTemplateId(couponTemplateDO.getId())
                        .validEndTime(couponTemplateDO.getValidEndTime())
                        .couponTaskBatchId(couponTaskDO.getBatchId())
                        .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                        .batchUserSetSize(batchUserSetSize)
                        .distributionEndFlag(Boolean.FALSE)
                        .build();

        // couponExecuteDistributionProducer.sendMessage(couponTemplateExecuteEvent);

        stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
        ++rowCount;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // suppose total number of user who be allocated coupon is 15001, and batch threshold
        // is 5000, then complete event send times = 15001 / 5000 + 15001 % 5000
        // the previous 3 times message delivery with distribution end flag = false,
        // the last one will be triggered here, when record iterator comes to the end of the
        // Excel file, so the last 1 record context will be sync to the last message and
        // then delivery ; without this function doAfterAllAnalysed, the remaining records
        // that total number < batch threshold will be missed
        CouponTemplateDistributionEvent couponTemplateExecuteEvent = CouponTemplateDistributionEvent.builder()
                .distributionEndFlag(Boolean.TRUE) // here we set the end flag to true, this
                // is the last batch of records to sync via message to downstream MQ
                .shopNumber(couponTaskDO.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .validEndTime(couponTemplateDO.getValidEndTime())
                .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                .couponTaskBatchId(couponTaskDO.getBatchId())
                .couponTaskId(couponTaskDO.getId())
                .build();
        // couponExecuteDistributionProducer.sendMessage(couponTemplateExecuteEvent);
    }
}
