USE coupon_merchant;

-- Seed one merchant admin account.
-- Password here is a placeholder for local-only debugging.
INSERT INTO t_user (shop_number, username, password, phone, mail, create_time, update_time, del_flag)
SELECT '10000001', 'merchant_admin', '123456', '13800000000', 'merchant_admin@onecoupon.local', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM t_user WHERE shop_number = '10000001' AND username = 'merchant_admin'
);

-- Seed one active coupon template for API smoke tests.
INSERT INTO t_coupon_template (
    name,
    shop_number,
    source,
    target,
    goods,
    type,
    valid_start_time,
    valid_end_time,
    stock,
    receive_rule,
    consume_rule,
    status,
    create_time,
    update_time,
    del_flag
)
SELECT
    'Demo New User Coupon',
    10000001,
    0,
    1,
    NULL,
    1,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    1000,
    JSON_OBJECT('limitPerUser', 1, 'channels', JSON_ARRAY('app', 'h5')),
    JSON_OBJECT('thresholdAmount', 100, 'discountAmount', 20),
    0,
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM t_coupon_template WHERE shop_number = 10000001 AND name = 'Demo New User Coupon'
);

-- Seed one pending task linked to the first template.
INSERT INTO t_coupon_task (
    shop_number,
    batch_id,
    task_name,
    file_address,
    send_num,
    fail_file_address,
    notify_type,
    coupon_template_id,
    send_type,
    send_time,
    status,
    completion_time,
    create_time,
    operator_id,
    update_time,
    del_flag
)
SELECT
    10000001,
    202604160001,
    'Demo Immediate Send Task',
    '/tmp/demo-users.xlsx',
    10,
    NULL,
    '0,2',
    tpl.id,
    0,
    NOW(),
    0,
    NULL,
    NOW(),
    1,
    NOW(),
    0
FROM (
         SELECT id
         FROM t_coupon_template
         WHERE shop_number = 10000001 AND name = 'Demo New User Coupon'
         ORDER BY id ASC
         LIMIT 1
     ) AS tpl
WHERE NOT EXISTS (
    SELECT 1 FROM t_coupon_task WHERE batch_id = 202604160001
);
