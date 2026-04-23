package com.tus.coupon.distribution.service.handler;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Deprecated
// entity of coupon excel record
@Data
public class CouponTaskExcelObject {
    @ExcelProperty("User ID")
    private String userId;

    @ExcelProperty("phone")
    private String phone;

    @ExcelProperty("mail")
    private String mail;
}
