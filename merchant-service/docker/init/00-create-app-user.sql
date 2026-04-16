-- Create a dedicated local development account.
-- MySQL official image executes these scripts only on first initialization.

CREATE USER IF NOT EXISTS 'coupon'@'%' IDENTIFIED BY 'coupon123';
GRANT ALL PRIVILEGES ON coupon_merchant.* TO 'coupon'@'%';
FLUSH PRIVILEGES;
