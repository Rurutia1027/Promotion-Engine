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
@TableName("t_user_coupon")
public class UserCouponDO {
    private Long id;

    private Long userId;

    private Long couponTemplateId;

    private Date receiveTime;

    private Integer receiveCount;

    private Date validStartTime;

    private Date validEndTime;

    private Date useTime;

    private Integer source;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    // distribute excel record row number
    @TableField(exist = false)
    private Integer rowNum;
}
