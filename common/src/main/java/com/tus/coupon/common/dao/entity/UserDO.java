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
@TableName("t_user")
public class UserDO {
    // id
    private Long id;

    // shop number
    private String shopNumber;

    // username
    private String username;

    // password
    private String password;

    // mobile phone
    private String phone;

    // mail
    private String mail;

    // creation time
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    // modification time
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // delete flag: 0: non-deleted, 1: deleted
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
