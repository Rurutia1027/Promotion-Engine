package com.tus.coupon.merchant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.mapper.CouponTaskMapper;
import com.tus.coupon.merchant.dto.req.CouponTaskCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTaskPageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskPageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskQueryRespDTO;
import com.tus.coupon.merchant.service.CouponTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {
    @Override
    public void createCouponDeliverTask(CouponTaskCreateReqDTO requestParam) {

    }

    @Override
    public IPage<CouponTaskPageQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam) {
        return null;
    }

    @Override
    public CouponTaskQueryRespDTO findCouponTaskById(String taskId) {
        return null;
    }
}
