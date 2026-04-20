package com.tus.coupon.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.merchant.dto.req.CouponTemplateCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTemplateNumberReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTemplatePageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplatePageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplateQueryRespDTO;

public interface CouponTemplateService extends IService<CouponTemplateDO> {
    /**
     * Create merchant coupon template
     *
     * @param requestParam create merchant coupon template request
     */
    void createCouponTemplate(CouponTemplateCreateReqDTO requestParam);

    /**
     * Pagination query merchant coupon template DOs
     *
     * @param requestParam pagination request parameter
     * @return paged merchant template items
     */
    IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam);

    /**
     * Query coupon template detail
     *
     * @param couponTemplateId coupon template id
     * @return detail of queried coupon template id
     */
    CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId);

    /**
     * Coupon template termination request
     *
     * @param couponTemplateId id of to be terminated coupon template
     */
    void terminateCouponTemplate(String couponTemplateId);

    /**
     * Increase the coupon template's issuance number
     *
     * @param requestParam
     */
    void increaseCouponIssuanceNumber(CouponTemplateNumberReqDTO requestParam);
}
