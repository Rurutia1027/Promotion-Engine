package com.tus.coupon.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.merchant.dto.req.CouponTaskCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTaskPageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskPageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskQueryRespDTO;

public interface CouponTaskService extends IService<CouponTaskDO> {
    /**
     * Merchant create coupon deliver task
     *
     * @param requestParam request parameter
     */
    void createCouponDeliverTask(CouponTaskCreateReqDTO requestParam);

    /**
     * Pagination query merchant coupon delivery tasks
     *
     * @param requestParam request parameter
     * @return coupon delivery tasks organized in page
     */
    IPage<CouponTaskPageQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam);

    /**
     * Query coupon delivery task detail
     *
     * @param taskId coupon delivery task id
     * @return details of coupon delivery task info
     */
    CouponTaskQueryRespDTO findCouponTaskById(String taskId);
}
