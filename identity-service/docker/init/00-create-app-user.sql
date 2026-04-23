-- Create dedicated local user for identity service.
-- This script is executed only on first MySQL initialization.

CREATE DATABASE IF NOT EXISTS coupon_identity
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'coupon'@'%' IDENTIFIED BY 'coupon123';
GRANT ALL PRIVILEGES ON coupon_identity.* TO 'coupon'@'%';
FLUSH PRIVILEGES;
