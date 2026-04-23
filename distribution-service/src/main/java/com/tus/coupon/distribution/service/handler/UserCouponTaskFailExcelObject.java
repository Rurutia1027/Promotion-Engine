package com.tus.coupon.distribution.service.handler;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCouponTaskFailExcelObject {

    @ColumnWidth(20)
    @ExcelProperty("row number")
    private Integer rowNum;

    @ColumnWidth(30)
    @ExcelProperty("failure reason")
    private String cause;
}
