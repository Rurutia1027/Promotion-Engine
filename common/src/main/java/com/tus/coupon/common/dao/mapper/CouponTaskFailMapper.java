package com.tus.coupon.common.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tus.coupon.common.dao.entity.CouponTaskFailDO;
import com.tus.coupon.common.dao.entity.UserCouponDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponTaskFailMapper extends BaseMapper<CouponTaskFailDO> {
    List<UserCouponDO> selectUserCourseMaxReceiveCount(
            @Param("couponTemplateId") Long couponTemplateId,
            @Param("userIds") List<Long> userIds);
}