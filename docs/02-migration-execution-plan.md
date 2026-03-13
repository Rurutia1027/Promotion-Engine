# Migration Execution Plan

> Phased plan to evolve the coupon system toward cloud-native microservices while preserving existing behavior: common DTO extraction, DB refactor (TiDB/sharding), event-driven MQ decoupling, and cloud-native capabilities.

---

## 1. Objectives and Scope

### 1.1 Goals

- Unify API contracts (DTO), refactor the data layer (TiDB/sharding), and adopt event-driven flows (MQ).
- Reduce coupling and duplication; improve stability and scalability under high load; prepare for multi-region and multi-tenant deployment.

### 1.2 Scope and Prerequisites

- **Modules**: framework, merchant-admin, engine, distribution, settlement, search, gateway.
- **Middleware**: RocketMQ, Redis, Nacos, ShardingSphere in place; TiDB to be introduced later.

---

## 2. Phase 1 — Unify DTO Contracts (Common API Module)

### 2.1 Goal

Remove duplicated DTOs across services and establish a single API contract layer.

### 2.2 Steps

1. **Identify shared DTOs and enums**
   - Merchant admin → engine/distribution/search: template create/page DTOs.
   - Order system → engine/settlement: settlement create, consume, refund DTOs.
   - Search/admin: template search VOs.

2. **Create API contract module**
   - New Maven module, e.g. `onecoupon-api`:
     - `api.coupon.template` — template query/create DTOs, VOs, enums.
     - `api.coupon.user` — user coupon lifecycle DTOs.
     - `api.coupon.settlement` — settlement and order-amount DTOs.
   - Move only truly cross-service DTOs here; keep DOs (entities) inside each service.

3. **Switch services to shared contracts**
   - In merchant-admin, engine, settlement, search: remove local duplicate DTOs and depend on `onecoupon-api`.

4. **Build and regression**
   - Ensure all modules compile and all APIs behave as before.

---

## 3. Phase 2 — Database Refactor (TiDB / Sharding)

### 3.1 Goal

Optimize the DB layer around high-traffic areas (user coupons, settlements) and make it cloud-friendly.

### 3.2 Steps

1. **Define database domains (bounded contexts)**
   - User coupon domain: holdings and state changes (engine + distribution).
   - Template domain: templates, quota, lifecycle (merchant admin + template service).
   - Settlement domain: settlement orders and usage (settlement + order system).

2. **Design TiDB / sharding strategy**
   - User coupon DB: shard/place by `user_id` (and optionally tenant).
   - Template DB: partition by `merchant_id` or business category.
   - Settlement DB: partition by order or time, with archival support.

3. **Introduce HQL / query layer**
   - Add query services per domain (e.g. UserCouponQueryService, TemplateQueryService, SettlementQueryService).
   - Expose domain-level operations only; hide SQL/Mappers and storage (TiDB/ES/cache).

4. **Gradual migration**
   - Deploy TiDB in non-prod; dual-write or shadow tables for key write paths; switch read paths to TiDB under a feature flag.

5. **Cut-over**
   - After validation, move hotspot tables to TiDB; keep or phase out ShardingSphere as needed.

---

## 4. Phase 3 — Event-Driven and MQ Decoupling

### 4.1 Goal

Replace tight synchronous coupling with event-driven flows using RocketMQ.

### 4.2 Steps

1. **Standardize topics and event models**
   - Define events: CouponRedeemRequested, CouponRedeemed, CouponLocked, CouponConsumed, CouponReleased; OrderCreated, PaymentSucceeded, RefundSucceeded; TemplateCreated/Updated/Terminated; CouponTaskCreated, CouponRemindCreated, etc.
   - Define DTOs for each in `onecoupon-api`.

2. **Introduce Coupon Command / Event Service**
   - New (or logical) service: expose REST/gRPC (/command/redeem, lock, consume, release); own user-coupon DB writes and emit domain events (MQ).
   - Other services (orders, campaigns, ops) do not touch coupon DB directly.

3. **Refactor synchronous flows**
   - UserCouponController: normalize /redeem-mq as command entry and publish CouponRedeemRequested; processPayment/processRefund triggered by consuming PaymentSucceeded/RefundSucceeded.
   - Settlement keeps sync query APIs; state maintained by events.

4. **DLQ and retry**
   - Configure retry and dead-letter queues for critical topics; add compensation where needed.

---

## 5. Phase 4 — Cloud-Native Capabilities

### 5.1 Goal

Enable self-healing and autoscaling on cloud platforms.

### 5.2 Steps

1. **Unified metrics and logging**
   - Add business metrics on key flows (redeem, consume, settlement query, template create): QPS, success rate, latency.
   - Ensure logs carry TraceId, UserId, TemplateId, OrderId consistently.

2. **Rate limiting and degradation**
   - Use Sentinel/Gateway to limit high-QPS endpoints (redeem, settlement query); graceful degradation for non-core features (search, reminds).

3. **Autoscaling and health checks**
   - Configure HPA (CPU/QPS/custom metrics); health checks for DB, MQ, and cache connectivity.

---

## 6. Current State Summary

- The system is already a multi-module Spring Cloud microservice; controller APIs are documented.
- Gaps: DB still MySQL+ShardingSphere; shared DTOs/models create coupling; event-driven patterns not fully applied; MQ used for seckill, delayed close, batch distribution—payments/refunds, index sync, notifications can be eventized further.
- The four-phase path (DTO unification → DB domain split + TiDB → event-driven refactor → cloud-native hardening) offers a controlled, high-value migration roadmap.
