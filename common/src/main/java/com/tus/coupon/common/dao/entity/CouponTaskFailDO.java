package com.tus.coupon.common.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_coupon_task_fail")
public class CouponTaskFailDO {
    // id
    private Long id;

    // batch id
    private Long batchId;

    // json object , task deliver failure issue
    @TableField(value = "`json_object`")
    private String jsonObject;
}
