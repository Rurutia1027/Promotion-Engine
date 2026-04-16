package com.tus.coupon.merchant.dao.entity;

// TODO: coupon template log should be wrapped and encapsulate into events
// TODO: let  fluent bit/logstash push to the ELK log center rather than holding in
//  Relational DB

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
@TableName("t_coupon_template_log")
public class CouponTemplateLogDO {
    // id
    private Long id;

    // shop number
    private Long shopNumber;

    // coupon template id
    private String couponTemplateId;

    // operator
    private String operatorId;

    // operator log
    private String operatorLog;

    // raw data
    private String originalData;

    // modified data
    private String modifiedData;

    // creation time
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
