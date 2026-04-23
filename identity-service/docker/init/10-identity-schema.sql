USE coupon_identity;

CREATE TABLE IF NOT EXISTS t_user
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    identity_type VARCHAR(32)     NOT NULL COMMENT 'identity type: MERCHANT / CONSUMER',
    identity_no   VARCHAR(64)     NOT NULL COMMENT 'identity number: shop number / consumer number',
    shop_number   VARCHAR(32)     NULL COMMENT 'legacy merchant alias, optional',
    username      VARCHAR(64)     NOT NULL COMMENT 'username',
    password      VARCHAR(128)    NULL COMMENT 'password hash or placeholder',
    phone         VARCHAR(32)     NULL COMMENT 'phone number',
    mail          VARCHAR(128)    NULL COMMENT 'mail address',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    del_flag      TINYINT         NOT NULL DEFAULT 0 COMMENT 'delete flag: 0 active, 1 deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_identity_username (identity_type, identity_no, username),
    KEY idx_identity_del_id (identity_type, identity_no, del_flag, id),
    KEY idx_shop_del_id (shop_number, del_flag, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
COMMENT ='identity user table, supports merchant and consumer';
