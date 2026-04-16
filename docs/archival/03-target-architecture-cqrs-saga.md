# Cloud-Native Coupon System - Target Architecture (CQRS + Saga)

> This document describes the **target architecture** (blueprint), not the current codebase as-is.
> This project aims to evolve into a cloud-native, event-driven, horizontally scalable coupon platform with CQRS and
> Saga.


---

## 1. Overview

**Promotion-Engine** target state supports:

- High-concurrency seckill/redeem (10+ QPS read/write)
- Clear module/domain boundaries: template, user coupon, settlement, search, ops tasks
- **CQRS** and **Saga** for consistency across services
- **TiDB + sharding** as the cloud-native data layer
- **RocketMQ** for service decoupling and async processing

---

## 2. Service Landscape

### 2.1 Core Services

**Gateway Service**: Single entry: routing, auth, rate limiting, request logging/tracing.

**Merchant Admin Service**:

- Manages coupon templates and distribution tasks; create/update/terminate templates, create batch-issue tasks.
- Example APIs:
    - `POST /api/merchant-admin/coupon-template/create`
    - `GET /api/merchant-admin/coupon-template/page`
    - `POST /api/merchant-admin/coupon-template/terminate`
    - `POST /api/merchant-admin/coupon-task/create`

**Template Lifecycle Service**:

- Template state machine; publishes `TemplateCreated`, `TemplateUpdated`, `TemplateTerminated`; uses delayed
  messages to close templates at expiry.

**Coupon Command Service**

- User-coupon **writes** (CQRS command side): redeem/seckill, lock, consume, release; coordinates with
  order/payment/settlement via Saga.
- Example APIs:
    - `POST /command/coupon/redeem`
    - `/command/lock`
    - `/command/consume`
    - `/command/release`

**Coupon Query Service**

- User-coupon **read model** (CQRS query side): list usable/unusable coupons; read store updated from event stream.
- Example: `POST /api/settlement/coupon-query`, `POST /api/settlement/coupon-query-sync`

**Search Service**

- Template search; ES/TiDB; consumes `Template` events to update index
- Example: `GET / api/search/coupon-template/page`

**Distribution Service**

- Batch coupon issue by task/audience; consumes `CouponTaskCreated`; shared execution; retry/DLQ on failure.

**Notification Service**

- Reminds, SMS/in-app; consumes `CouponRemindCreated` etc.; unified channels and rate limits.

---

## 3. Data Architecture and TiDB

### 3.1 Domain Databases

- **Template DB**: Template master data; partition by `merchant_id` or business type.
- **User Coupon DB**: User holdings and lifecycle; high write; shared by `user_id`.
- **Settlement DB**: Settlement orders, consume records, refunds; partition by order or time; archival support.

Each domain service owns its database (database per service); cross-domain access only via API or MQ events.


---

## 4. CQRS Design

### 4.1 Command Side

- Handles state changes; redeem, lock, consume, release
- Example (redeem): Gateway -> Coupon Command Service receives RedeemCouponCommand -> validate , idempotency, TiDB
  transaction/distributed lock -> write User Coupon DB -> publish `CouponRedeem` to MQ.

### 4.2 Query Side

- Efficient read APIs for frontend and downstream: usable/unusable coupon list, consume history.
- Read model: Coupon Query Service consumes `CouponRedeemd`,`CouponLocked`, `CouponConsumed`, `CouponReleased` and
  updates read store (TiDB replica/ES/cache).
- Consistency: normal queries eventually consistent; settlement path can use `/coupon-query-sync` for stronger
  consistency when needed.

---

## 5. Saga Pattern (Cross-Service Consistency)

### 5.1 Why Saga

Order, payment, and coupon consume span multiple services and steps; traditional 2PC is not suitable. Saga manages the
flow with local transactions and compensations.

### 5.2 Example: Order + Lock + Pay + Consume

**Forward**: OrderCreated -> Coupon Command locks and publishes CouponLocked -> Payment succeeds and publishes
PaymentSucceeded -> Coupon Command consumes and publishes CouponCommand -> Order updated to paid + consumed.

**Compensation**: Payment timeout/failure -> PaymentFailed or timeout event -> Coupon Command runs "release lock"
compensation and publishes Coupon Released -> Order marks order as failed/closed.

Can use **Orchestrator** or **Choreography**; Promotion-Engine leans toward choreography with minimal orchestration.


---

## 6. Messaging and Event Model 
### 6.1 Key Topics 
- `coupon-command-topic` -- Commands (e.g., RedeemCouponCommand)
- `coupon-event-topic` -- Domain events: CouponRedeemed, CouponLocked, CouponConsumed, CouponReleased
- `template-event-topic` -- TemplateCreated/Updated/Terminated 
- `order-event-topic` -- OrderCreated, PaymentSucceeded, RefundSucceeded
- `task-event-topic` -- CouponTaskCreated, CouponTaskCompleted
- `notification-event-topic` -- Remind/notification events 

### 6.2 DLQ and Retry 

Critical topics: retry then DLQ; ops or compensation jobs handle replay or manual intervention. 


---

## 7. Non-Functional Goals 
- **Scalability**: Stateless services scale horizontally; seckill/redeem scales via MQ + TiDB. 
- **Observability**: End-to-end TraceId; QPS, error rate, P95/P99, MQ lag on key paths.
- **Resilience**: Sentinel/Gateway rate limiting; degradation for non-core features. 

---

## 8. High-Level Roadmap 
- Extract common DTO module
- Database domain split + TiDB introduction 
- Implement CQRS (command/query separation; query from events)
- Saga and event-driven flows (order-payment-consume)
- Cloud-native hardening (monitoring, rate limit, degradation, autoscaling)