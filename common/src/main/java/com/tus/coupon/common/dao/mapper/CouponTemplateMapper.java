package com.tus.coupon.common.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
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
                                     @Param("couponTemplateId") Long couponTemplateId,
                                     @Param("number") Integer number);

    /**
     * decrease coupon distribution number
     *
     * @param shopNumber
     * @param couponTemplateId coupon template id
     * @param decrementStock decrement stock value
     */
    int decrementCouponTemplateStock(@Param("shopNumber") Long shopNumber,
                                     @Param("couponTemplateId") Long couponTemplateId,
                                     @Param("decrementStock") Integer decrementStock);
}
