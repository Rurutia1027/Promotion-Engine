# Architecture and Module Analysis

> Module boundaries, cloud-native gaps, core APIs, message-queue usage, and decoupling feasibility for the current oneCoupon codebase.

---

## 1. Module Boundaries and Core APIs

The project is a Maven-based multi-module Spring Boot + Spring Cloud Alibaba setup. Main outward-facing modules:

### 1.1 Gateway

- **Role**: Single entry point; routing, logging (e.g. `RequestLoggingFilter`), and optional rate-limiting/auth.
- **APIs**: No business controllers; routing and filters only.

### 1.2 Merchant-Admin (Back-office)

- **CouponTemplateController**
  - `POST /api/merchant-admin/coupon-template/create` — Create coupon template
  - `GET  /api/merchant-admin/coupon-template/page` — Page query templates
  - `GET  /api/merchant-admin/coupon-template/find` — Get template detail
  - `POST /api/merchant-admin/coupon-template/increase-number` — Increase issuance
  - `POST /api/merchant-admin/coupon-template/terminate` — Terminate template
- **CouponTaskController**
  - `POST /api/merchant-admin/coupon-task/create` — Create push task
  - `GET  /api/merchant-admin/coupon-task/page` — Page query tasks
  - `GET  /api/merchant-admin/coupon-task/find` — Get task detail

### 1.3 Engine (View, Lock, Consume)

- **CouponTemplateController**
  - `GET /api/engine/coupon-template/query` — Query single template (detail/list)
- **UserCouponController**
  - `POST /api/engine/user-coupon/redeem` — Direct redeem (high concurrency / seckill-like)
  - `POST /api/engine/user-coupon/redeem-mq` — Redeem via MQ (async peak-shaving)
  - `POST /api/engine/user-coupon/create-payment-record` — Create settlement (order lock)
  - `POST /api/engine/user-coupon/process-payment` — Consume after payment (ideally MQ-driven)
  - `POST /api/engine/user-coupon/process-refund` — Release on refund (ideally MQ-driven)
- **CouponTemplateRemindController**
  - `POST /api/engine/coupon-template-remind/create` — Create remind
  - `GET  /api/engine/coupon-template-remind/list` — List user reminds
  - `POST /api/engine/coupon-template-remind/cancel` — Cancel remind

### 1.4 Settlement

- **CouponQueryController**
  - `POST /api/settlement/coupon-query` — List usable/unusable coupons
  - `POST /api/settlement/coupon-query-sync` — Sync query (stronger consistency)

### 1.5 Search

- **CouponTemplateController**
  - `GET /api/search/coupon-template/page` — Page search templates (ES/pagination)

### 1.6 Distribution

- No HTTP controllers; handles batch coupon distribution triggered by admin or schedulers (e.g. XXL-Job).

### 1.7 Framework

- Shared infra: `Result/Results`, idempotency `NoDuplicateSubmit`, ID generation, exception handling.

---

## 2. Gaps vs “Fully Cloud-Native Microservices”

### 2.1 Data Layer and Multi-Tenant / Multi-Region

- **Current**: MySQL + ShardingSphere embedded in apps (engine, distribution, settlement, merchant-admin); shared relational model.
- **Gaps**: No strict separation of “template DB”, “user-coupon DB”, “settlement DB” by bounded context or TiDB placement; sharding is app-bound, so elasticity is at app-instance level rather than DB-cluster level.
- **TiDB + sharding**: Enables placement by user_id/shop_id/region, cold/hot separation, and a clear HQL/query layer for future storage changes.

### 2.2 Service Boundaries and Coupling

- Modules are clear but “vertical services + shared model”: similar DTO/DO names across services, and engine ↔ order/settlement still rely on sync APIs and shared data instead of event-driven flows (order/payment/refund events → coupon state).
- **Ideal**: Each service owns its DB and model; communication via events (MQ) + API contracts.

### 2.3 Elasticity and Statelessness

- Business logic is largely stateless. Nacos/Sentinel exist but there is no explicit tuning for cloud HPA/Pod-level scaling or adaptive rate-limiting; lock and hotspot strategies would need review when moving to TiDB.

### 2.4 Observability

- SkyWalking, logging, Sentinel are mentioned; without consistent business metrics and SLOs (e.g. consume success rate, seckill drop rate, MQ lag), observability remains short of cloud-native expectations.

---

## 3. Message-Queue Usage (Current and Classic)

### 3.1 Seckill / Redeem Peak-Shaving

- `/api/engine/user-coupon/redeem-mq`: request → engine publishes to MQ → async stock decrement and user-coupon write. Reduces DB load and allows queuing/feedback.

### 3.2 Template Lifecycle (Delayed Messages)

- RocketMQ delayed messages to close/terminate templates at expiry; avoids single-instance cron for lifecycle.

### 3.3 Distribution Tasks (Batch Issue)

- Admin creates coupon-task → distribution consumes and issues in batch (EasyExcel, XXL-Job); MQ enables sharding, retry, and DLQ for failures.

### 3.4 Order Payment / Refund → Coupon State

- `processPayment` / `processRefund` are intended to be driven by payment/refund success events; order service publishes `PaymentSucceeded` / `RefundSucceeded`, engine consumes and updates coupon state.

### 3.5 Search Index Sync

- Template create/update/terminate can be published to MQ; search service consumes and updates ES index instead of direct MySQL reads.

---

## 4. Feasibility of Extracting MQ-Based Services

### 4.1 Coupon-Command / Coupon-Event Service

- Centralize write operations (redeem, lock, consume, release) behind a command service; expose `/command/redeem`, `/command/lock`, etc. Other domains (order, campaign, task) send commands or events only.

### 4.2 Coupon-Query Service (CQRS)

- Dedicated read side for “user’s coupons” and template search; read models updated from events; enables HQL and storage flexibility.

### 4.3 Template-Lifecycle Service

- Owns template create/update/terminate and delayed close; publishes template events; engine, search, distribution subscribe and update their views.

### 4.4 Notification / Remind Service

- Remind and push/sms/in-app messages in one service; remind creation → MQ → notification service sends at the right time; centralizes channels and rate limits.

### 4.5 Summary

- Architecturally feasible: boundaries are clear; moving “send/consume MQ” into dedicated command/event services and having existing services call them or publish events is straightforward.
- Data: If introducing TiDB + HQL, migrate write-heavy services (user coupon, settlement) first; move read side to event-subscribed read models gradually to limit risk.
