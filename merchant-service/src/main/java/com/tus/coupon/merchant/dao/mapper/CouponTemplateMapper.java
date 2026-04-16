package com.tus.coupon.merchant.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tus.coupon.merchant.dao.entity.CouponTemplateDO;
import org.springframework.data.repository.query.Param;

public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    /**
     * increase coupon distribution number
     *
     * @param shopNumber       shop number
     * @param couponTemplateId coupon template id
     * @param number           coupon distribution number
     */
    int increaseNumberCouponTemplate(@Param("shopNumber") Long shopNumber,
                                     @Param("couponTemplateId") String couponTemplateId,
                                     @Param("number") Integer number);
}
