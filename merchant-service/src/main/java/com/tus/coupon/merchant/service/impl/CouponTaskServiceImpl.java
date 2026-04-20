package com.tus.coupon.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tus.coupon.common.context.UserContext;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.mapper.CouponTaskMapper;
import com.tus.coupon.common.enums.CouponTaskStatusEnum;
import com.tus.coupon.common.exception.ClientException;
import com.tus.coupon.common.mq.event.CouponTaskExecuteEvent;
import com.tus.coupon.merchant.common.constant.enums.CouponTaskSendTypeEnum;
import com.tus.coupon.merchant.dto.req.CouponTaskCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTaskPageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskPageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTemplateQueryRespDTO;
import com.tus.coupon.merchant.mq.producer.CouponTaskActualExecuteProducer;
import com.tus.coupon.merchant.service.CouponTaskService;
import com.tus.coupon.merchant.service.CouponTemplateService;
import com.tus.coupon.merchant.service.handler.RowCountListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {
    private final CouponTemplateService couponTemplateService;
    private final CouponTaskMapper couponTaskMapper;
    private final RedissonClient redissonClient;
    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;

    // thread pool here
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() << 1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCouponDeliverTask(CouponTaskCreateReqDTO requestParam) {
        CouponTemplateQueryRespDTO couponTemplateQueryRespDTO =
                couponTemplateService.findCouponTemplateById(requestParam.getCouponTemplateId());
        if (couponTemplateQueryRespDTO == null) {
            throw new ClientException("Request coupon associated coupon template not exist, " +
                    "please validate request template id");
        }

        CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);
        couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        couponTaskDO.setOperatorId(UserContext.getShopNumber());
        couponTaskDO.setStatus(
                Objects.equals(requestParam.getSendType(),
                        CouponTaskSendTypeEnum.IMMEDIATE.getType())
                        ? CouponTaskStatusEnum.IN_PROGRESS.getStatus()
                        : CouponTaskStatusEnum.PENDING.getStatus()
        );

        // sync coupon deliver task record to db
        couponTaskMapper.insert(couponTaskDO);

        JSONObject delayJsonObject = JSONObject
                .of("fileAddress", requestParam.getFileAddress(), "couponTaskId",
                        couponTaskDO.getId());
        executorService.execute(() -> refreshCouponTaskSendNum(delayJsonObject));

        // suppose just now we commit our message to thread pool, and the service sudden
        // crash down, we can use the delay queue to refresh
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(
                "COUPON_TASK_SEND_NUM_DELAY_QUEUE");
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);

        // here we set the delay period to 20s, because we determined that the above thread
        // pool can complete its tasks in 20s, when it comes to the prod env,
        // this time period needs to adjust based on monitored metric value
        if (Objects.equals(requestParam.getSendType(),
                CouponTaskSendTypeEnum.IMMEDIATE.getType())) {
            // task type is send immediately , then wrap the entity into event and send via
            // producer to mq
            CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder()
                    .couponTaskId(couponTaskDO.getId())
                    .build();
            couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
        }
    }

    private void refreshCouponTaskSendNum(JSONObject delayJsonObject) {
        // fetch all excel row numbers via EasyExcel
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(delayJsonObject.getString("fileAddress"), listener).sheet().doRead();
        int totalRows = listener.getRowCount();

        CouponTaskDO updateCouponTaskDO = CouponTaskDO.builder()
                .id(delayJsonObject.getLong("couponTaskId"))
                .sendNum(totalRows)
                .build();
        couponTaskMapper.updateById(updateCouponTaskDO);
    }

    @Override
    public IPage<CouponTaskPageQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<CouponTaskDO> queryWrapper = Wrappers.lambdaQuery(CouponTaskDO.class)
                .eq(CouponTaskDO::getShopNumber, UserContext.getShopNumber())
                .eq(StrUtil.isNotBlank(requestParam.getBatchId()), CouponTaskDO::getBatchId, requestParam.getBatchId())
                .like(StrUtil.isNotBlank(requestParam.getTaskName()), CouponTaskDO::getTaskName, requestParam.getTaskName())
                .eq(StrUtil.isNotBlank(requestParam.getCouponTemplateId()), CouponTaskDO::getCouponTemplateId, requestParam.getCouponTemplateId())
                .eq(Objects.nonNull(requestParam.getStatus()), CouponTaskDO::getStatus, requestParam.getStatus());
        IPage<CouponTaskDO> selectPage = couponTaskMapper.selectPage(requestParam, queryWrapper);

        return selectPage.convert(each -> BeanUtil.toBean(each, CouponTaskPageQueryRespDTO.class));
    }

    @Override
    public CouponTaskQueryRespDTO findCouponTaskById(String taskId) {
        CouponTaskDO couponTaskDO = couponTaskMapper.selectById(taskId);
        return BeanUtil.toBean(couponTaskDO, CouponTaskQueryRespDTO.class);
    }


    @PostConstruct
    public void init() {
        new RefreshCouponTaskDelayQueueRunner(this, couponTaskMapper, redissonClient).run();
    }

    @RequiredArgsConstructor
    static class RefreshCouponTaskDelayQueueRunner {
        private final CouponTaskServiceImpl couponTaskService;
        private final CouponTaskMapper couponTaskMapper;
        private final RedissonClient redissonClient;

        public void run() {
            Executors.newSingleThreadExecutor(
                            runnable -> {
                                Thread thread = new Thread(runnable);
                                thread.setName("delay_coupon-task_send-num_consumer");
                                thread.setDaemon(Boolean.TRUE);
                                return thread;
                            })
                    .execute(() -> {
                        RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
                        for (; ; ) {
                            try {
                                JSONObject delayJsonObject = blockingDeque.take();
                                if (delayJsonObject != null) {
                                    CouponTaskDO couponTaskDO = couponTaskMapper.selectById(delayJsonObject.getLong("couponTaskId"));
                                    if (couponTaskDO.getSendNum() == null) {
                                        couponTaskService.refreshCouponTaskSendNum(delayJsonObject);
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        }
    }
}
