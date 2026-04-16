-- Merchant-only schema (single-table style)
-- Scope:
-- - coupon template management
-- - coupon task management
-- Excludes distribution/engine/settlement tables.

CREATE DATABASE IF NOT EXISTS coupon_merchant DEFAULT CHARACTER SET utf8mb4;
USE coupon_merchant;

CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `shop_number` VARCHAR(64)          DEFAULT NULL COMMENT 'shop number',
    `username`    VARCHAR(64)          DEFAULT NULL COMMENT 'username',
    `password`    VARCHAR(512)         DEFAULT NULL COMMENT 'password',
    `phone`       VARCHAR(128)         DEFAULT NULL COMMENT 'mobile phone number',
    `mail`        VARCHAR(512)         DEFAULT NULL COMMENT 'email',
    `create_time` DATETIME             DEFAULT NULL COMMENT 'create time',
    `update_time` DATETIME             DEFAULT NULL COMMENT 'update time',
    `del_flag`    TINYINT              DEFAULT 0 COMMENT 'delete flag 0：not delete 1：already deleted',
    PRIMARY KEY (`id`),
    KEY `idx_shop_number` (`shop_number`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='merchant user table';

CREATE TABLE IF NOT EXISTS `t_coupon_template`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name`             VARCHAR(256)          DEFAULT NULL COMMENT 'coupon template name',
    `shop_number`      BIGINT                DEFAULT NULL COMMENT 'shop number',
    `source`           TINYINT               DEFAULT NULL COMMENT 'coupon source 0: shop coupon 1: platform coupon',
    `target`           TINYINT               DEFAULT NULL COMMENT 'Eligible Items 0: Product-Specific, 1: Storewide',
    `goods`            VARCHAR(64)           DEFAULT NULL COMMENT 'Promotional Item Code',
    `type`             TINYINT               DEFAULT NULL COMMENT 'Promotional Type: 0: Fixed-Amount Coupon, 1: Threshold Coupon, 2: Percentage-Off Coupon',
    `valid_start_time` DATETIME              DEFAULT NULL COMMENT 'Validity Start Date',
    `valid_end_time`   DATETIME              DEFAULT NULL COMMENT 'Validity End Date',
    `stock`            INT                   DEFAULT NULL COMMENT 'Stock',
    `receive_rule`     JSON                  DEFAULT NULL COMMENT 'Claiming Rules',
    `consume_rule`     JSON                  DEFAULT NULL COMMENT 'Consumption Rules',
    `status`           TINYINT               DEFAULT NULL COMMENT 'Coupon Status: 0-Active, 1-Ended',
    `create_time`      DATETIME              DEFAULT NULL COMMENT 'Creation Time',
    `update_time`      DATETIME              DEFAULT NULL COMMENT 'Modification Time',
    `del_flag`         TINYINT               DEFAULT 0 COMMENT 'Deletion Flag: 0=Not Deleted, 1=Deleted',
    PRIMARY KEY (`id`),
    KEY `idx_shop_number` (`shop_number`),
    KEY `idx_status_valid_end_time` (`status`, `valid_end_time`),
    KEY `idx_source_status` (`source`, `status`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Coupon Template';

CREATE TABLE IF NOT EXISTS `t_coupon_template_log`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `shop_number`        BIGINT                DEFAULT NULL COMMENT 'Show Number',
    `coupon_template_id` BIGINT                DEFAULT NULL COMMENT 'Coupon Template ID',
    `operator_id`        BIGINT                DEFAULT NULL COMMENT 'Operator',
    `operation_log`      TEXT COMMENT 'Operation log',
    `original_data`      VARCHAR(1024)         DEFAULT NULL COMMENT 'Raw data',
    `modified_data`      VARCHAR(1024)         DEFAULT NULL COMMENT 'Modified data',
    `create_time`        DATETIME              DEFAULT NULL COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    KEY `idx_shop_number` (`shop_number`),
    KEY `idx_coupon_template_id` (`coupon_template_id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Coupon Template operation log table';

CREATE TABLE IF NOT EXISTS `t_coupon_task`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `shop_number`        BIGINT                DEFAULT NULL COMMENT 'Shop Number',
    `batch_id`           BIGINT                DEFAULT NULL COMMENT 'Batch ID',
    `task_name`          VARCHAR(128)          DEFAULT NULL COMMENT 'Coupon Batch Task Name',
    `file_address`       VARCHAR(512)          DEFAULT NULL COMMENT 'File address',
    `send_num`           INT                   DEFAULT NULL COMMENT 'Issued Coupons Number',
    `fail_file_address`  VARCHAR(512)          DEFAULT NULL COMMENT 'File Path for Failed Distributions',
    `notify_type`        VARCHAR(32)           DEFAULT NULL COMMENT 'Notification methods(combinable): 0: In-site Message; 1: Pop-up Push; 2: Email; 3: SMS',
    `coupon_template_id` BIGINT                DEFAULT NULL COMMENT 'Coupon Template ID',
    `send_type`          TINYINT               DEFAULT NULL COMMENT 'Send Type 0: Send Immediately, 1: Scheduled Send',
    `send_time`          DATETIME              DEFAULT NULL COMMENT 'Send Time',
    `status`             TINYINT               DEFAULT NULL COMMENT 'Status 0: Pending | 1: In Progress | 2: Failed | 3: Succeeded | 4: Cancelled',
    `completion_time`    DATETIME              DEFAULT NULL COMMENT 'Complete time',
    `create_time`        DATETIME              DEFAULT NULL COMMENT 'Operation time',
    `operator_id`        BIGINT                DEFAULT NULL COMMENT 'Operator',
    `update_time`        DATETIME              DEFAULT NULL COMMENT 'Modification time',
    `del_flag`           TINYINT               DEFAULT 0 COMMENT 'Deletion Flag 0: Deleted, 1: Non-Deleted',
    PRIMARY KEY (`id`),
    KEY `idx_batch_id` (`batch_id`),
    KEY `idx_coupon_template_id` (`coupon_template_id`),
    KEY `idx_shop_status_send_time` (`shop_number`, `status`, `send_time`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Coupon Template Distribution Task Sheet';

CREATE TABLE IF NOT EXISTS `t_coupon_task_fail`
(
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `batch_id`    BIGINT   DEFAULT NULL COMMENT 'Batch ID',
    `json_object` TEXT COMMENT 'Failed Content',
    PRIMARY KEY (`id`),
    KEY `idx_batch_id` (`batch_id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Coupon Task Failure Log';
