package com.tus.coupon.merchant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.mapper.CouponTemplateMapper;
import com.tus.coupon.merchant.dto.req.CouponTemplateCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTemplateNumberReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTemplatePageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplatePageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplateQueryRespDTO;
import com.tus.coupon.merchant.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper,
        CouponTemplateDO> implements CouponTemplateService {
    @Override
    public void createCouponTemplate(CouponTemplateCreateReqDTO requestParam) {

    }

    @Override
    public IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        return null;
    }

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId) {
        return null;
    }

    @Override
    public void terminateCouponTemplate(String couponTemplateId) {

    }

    @Override
    public void increaseCouponIssuanceNumber(CouponTemplateNumberReqDTO requestParam) {

    }
}
