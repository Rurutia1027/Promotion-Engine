package com.tus.coupon.common.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_coupon_task")
public class CouponTaskDO {
    // id
    private Long id;

    // shop number
    private Long shopNumber;

    // batch id
    private Long batchId;

    // coupon batch task name
    private String taskName;

    // file address
    private String fileAddress;

    // distribution failed file address
    private String failFileAddress;

    // coupon distribution count
    private Integer sendNum;

    // notification type(combinable): 0: in site message;
    // 1: pop-up push 2: email, 3: sms
    private String notifyType;

    // coupon template id
    private Long couponTemplateId;

    // send type: 0: send immediately, 1: send scheduled
    private Integer sendType;

    // send type
    private Date sendTime;

    // send status:
    // 0: in-progress, 1: execution/processing, 2: execute failed, 3: execute success, 4:
    // cancel
    private Integer status;

    // completion time
    private Date completionTime;

    // operator id
    private Long operatorId;

    // creation time
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    // modification time
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // delete flag: 1: non-deleted, 2: deleted
    private Integer delFlag;
}
