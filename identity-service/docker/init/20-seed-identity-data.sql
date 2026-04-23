USE coupon_identity;

-- Seed demo merchant users for API smoke testing.
INSERT INTO t_user (identity_type, identity_no, shop_number, username, password, phone, mail, create_time, update_time,
                    del_flag)
SELECT 'MERCHANT',
       '10000001',
       '10000001',
       'merchant_user_a',
       '123456',
       '13800000001',
       'merchant_user_a@coupon.local',
       NOW(),
       NOW(),
       0
WHERE NOT EXISTS (SELECT 1
                  FROM t_user
                  WHERE identity_type = 'MERCHANT' AND identity_no = '10000001' AND username = 'merchant_user_a');

INSERT INTO t_user (identity_type, identity_no, shop_number, username, password, phone, mail, create_time, update_time,
                    del_flag)
SELECT 'MERCHANT',
       '10000001',
       '10000001',
       'merchant_user_b',
       '123456',
       '13800000002',
       'merchant_user_b@coupon.local',
       NOW(),
       NOW(),
       0
WHERE NOT EXISTS (SELECT 1
                  FROM t_user
                  WHERE identity_type = 'MERCHANT' AND identity_no = '10000001' AND username = 'merchant_user_b');

-- Seed demo consumer users.
INSERT INTO t_user (identity_type, identity_no, shop_number, username, password, phone, mail, create_time, update_time,
                    del_flag)
SELECT 'CONSUMER',
       'C-20260001',
       NULL,
       'consumer_user_a',
       '123456',
       '13900000001',
       'consumer_user_a@coupon.local',
       NOW(),
       NOW(),
       0
WHERE NOT EXISTS (SELECT 1
                  FROM t_user
                  WHERE identity_type = 'CONSUMER' AND identity_no = 'C-20260001' AND username = 'consumer_user_a');

INSERT INTO t_user (identity_type, identity_no, shop_number, username, password, phone, mail, create_time, update_time,
                    del_flag)
SELECT 'CONSUMER',
       'C-20260002',
       NULL,
       'consumer_user_b',
       '123456',
       '13900000002',
       'consumer_user_b@coupon.local',
       NOW(),
       NOW(),
       0
WHERE NOT EXISTS (SELECT 1
                  FROM t_user
                  WHERE identity_type = 'CONSUMER' AND identity_no = 'C-20260002' AND username = 'consumer_user_b');
