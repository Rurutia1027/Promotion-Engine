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
@TableName("t_coupon_template")
public class CouponTemplateDO {
    // id
    private Long id;

    // shop number
    private Long shopNumber;

    // coupon name;
    private String name;

    // coupon source: 0-Store Coupon; 1: Platform Coupon
    private Integer source;

    // Eligible Items: 0 Product Specific; 1 Storewide
    private Integer target;

    // coupon goods number
    private String goods;

    // discount type: 0: instant deduction coupon
    // 1: threshold-based deduction coupon
    // 2: discount coupon
    private Integer type;

    // validity start time
    private Date validStartTime;

    // validity end time
    private Date validEndTime;

    // stock
    private Integer stock;

    // claiming rules
    private String receiveRule;

    // consuming rules
    private String consumeRule;

    // coupon status: 0: valid, 1: non-valid
    private Integer status;

    // creation time
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    // modification time
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // delete flag: 0: non-delete, 1: deleted
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
