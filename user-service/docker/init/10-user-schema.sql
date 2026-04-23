USE coupon_user;

CREATE TABLE IF NOT EXISTS t_user
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_type   VARCHAR(32)     NOT NULL COMMENT 'user type: MERCHANT / CONSUMER',
    shop_number VARCHAR(32)     NULL COMMENT 'merchant shop number, nullable for consumer',
    username    VARCHAR(64)     NOT NULL COMMENT 'username',
    password    VARCHAR(128)    NULL COMMENT 'password hash or placeholder',
    phone       VARCHAR(32)     NULL COMMENT 'phone number',
    mail        VARCHAR(128)    NULL COMMENT 'mail address',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    del_flag    TINYINT         NOT NULL DEFAULT 0 COMMENT 'delete flag: 0 active, 1 deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_type_shop_username (user_type, shop_number, username),
    KEY idx_user_type_del_id (user_type, del_flag, id),
    KEY idx_shop_del_id (shop_number, del_flag, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
COMMENT ='user service user table';
