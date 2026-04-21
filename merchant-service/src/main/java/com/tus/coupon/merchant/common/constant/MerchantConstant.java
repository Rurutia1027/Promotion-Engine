package com.tus.coupon.merchant.common.constant;

public final class MerchantConstant {
    /**
     * template coupon promotion close log content
     */
    public static final String TERMINATE_COUPON_TEMPLATE_LOG_CONTENT = "{CURRENT_USER{''}} " +
            "promotion close";

    /**
     * increase the number of coupon template issuance log content
     */
    public static final String INCREASE_NUMBER_COUPON_TEMPLATE_LOG_CONTENT = "{CURRENT_USER" +
            "{''}} increase issuance number：{{#requestParam.number}}";

    /**
     * Create coupon template log content
     */
    public static final String CREATE_COUPON_TEMPLATE_LOG_CONTENT = "{CURRENT_USER{''}} " +
            "User create coupon name: {{#requestParam.name}}，" +
            "Coupon promotion object: {COMMON_ENUM_PARSE{'DiscountTargetEnum' + '_' + " +
            "#requestParam.target}}，" +
            "Promotion type: {COMMON_ENUM_PARSE{'DiscountTypeEnum' + '_' + #requestParam" +
            ".type}}，" +
            "Stock:{{#requestParam.stock}}，" +
            "Promotion product number: {{#requestParam.goods}}，" +
            "Promotion start time: {{#requestParam.validStartTime}}，" +
            "Promotion end time: {{#requestParam.validEndTime}}，" +
            "Coupon claim rule: {{#requestParam.receiveRule}}，" +
            "Coupon consume rule: {{#requestParam.consumeRule}};";

}
