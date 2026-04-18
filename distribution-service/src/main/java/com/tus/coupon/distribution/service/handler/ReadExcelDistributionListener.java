package com.tus.coupon.distribution.service.handler;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

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

        // fetch lua script, and let Hutool tool hold the lua script in memory to avoid
        // secondary loading from disk
        DefaultRedisScript<Long> buildLuaScript =
                Singleton.get(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH, () -> {
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH)));
                    redisScript.setResultType(Long.class);
                    return redisScript;
                });

        // todo:
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // send excel finish parse flag, even though it does not satisfy batch saved
        // threshold, we need to sync the records to db

        // TODO
    }
}
