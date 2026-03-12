# Migration Execution Plan 
> Phased plan to evolve the coupon system toward cloud-native microservices while preserving existing behavior: common DTO exraction, DB refactor (TiDB/sharding) , event-driven MQ decoupling, and cloud-native capabilities. 


---

## 1. Objectives and Scope 
### 1.1 Goals 
- Unify API contracts (DTO), refactor the data layer (TiDB/sharding), and adopt event-driven flows (MQ).
- Reduce coupling and duplicaion; improve stability and scalability under high load; prepare for multi-region and multi-tenant deployment. 

### 1.2 Scope and Prerequisites
- **Modules**: framework, merchant-admin, engine, distribution, settlement, saerch gateway.
- **Middleware**: RocketMQ, Redis, Consul, ShardingSphere in place; TiDB to be introduced later. 


---

## 2. Phase 1 Unify DTO Contracts (Common API Module)
### 2.1 Goal 
Remove duplicated DTOs across services and establish a single API contract layer. 

### 2.2 Steps 
**Identify shared DTOs and enums**
- Merchant admin -> engine/distribution/search: template create/page DTOs.
- Order system -> engine/settlement: settlement create, consume, refund DTOs.
- Search/admin: template search VOs.

**Create API contract module**
New Maven module, e.g., `onecoupon-api`:
- `api.coupon.template` - template query/create DTOs, VOs, enums
- `api.coupon.user` - user coupon lifecycle DTOs.
- `api.coupon.settlement` - settlement and order-amount DTOs.

**Switch services to shared contract**
- In merchant-admin, engine, settlement, search: remove local duplicate DTOs and depend on `oncoupon-api`.

**Build and regression**
- Ensure all modules compile and all APIs behave as before. 


---

## 3. Phase 2 - Database Refactor (TiDB / Sharding)
### 3.1 Goal 
Optimize the DB layer around high-traffic areas (user coupons, settlements) and make it cloud-friendly .

### 3.2 Steps 
**Define database domains (bounded contexts)**
- User coupon domain: holdings and state changes (engine + distribution).
- Template domain: templates, quota, lifecycle (merchant admin + template service).
- Settlement domain: settlement orders and usage (settlement + order system).


**Design TiDB / sharding strategy**
- User coupon DB: shard/place by `user_id` (and optionally tenant)
- Template DB: partition by `merchant_id` or business category.
- Settlement DB: partition by order or time, with archival support. 
- 

**Introduce HQL / query layer**
- Add query services per domain (e.g., UserCouponQueryService, TemplateQueryService, SettlementQueryService).
- Expose domain-level operations only; hide SQL/Mappers and storage (TiDB/ES/cache).

**Gradual migration**
- Deploy TiDB in non-prod; dual-write or shadow tables for key write paths; switch read path to TiDB under a feature flag. 

**Cut-over**
- After validation, move hotspot tables to TiDB; keep or phase out ShardingSphere as needed. 


--- 

## 4. Phase 3 - Event Driven and MQ Decoupling

### 4.1 Goal 
Replace tight synchronous coupling with event-driven flows using RocketMQ. 

### 4.2 Steps 
**Standardize topics event models**
- Define evnets: CouponRedeemRequested, CouponRedeemed, CouponLocked, CouponConsumed, CouponReleased; OrderCreated, PaymentSucceeded, RefundSucceeded; TemplateCreated/Updated/Terminated; CouponTaskCreated, CouponRemindCreated, etc. 
- Define DTOs for each in `onecoupon-api`


**Introduce Coupon Command / Event Service**
- New (or logical) service: expose REST/gRPC (/command/redeem, lock, consume, release); own user-coupon DB writes and emit domain events (MQ). 
- Other services (orders, campagins, ops) do not touch coupon DB directly


**Refactor synchronous flows**
- UserCouponController: normalize / redeem-mq as command entry and publish CouponRedeemRequested; processPayment/processRefund triggered by consuming PaymentSucceeded/RefundSucceeded.
- Settlement keeps sync query APIs; state maintained by events. 


**DLQ and retry**
- Configure retry and dead-letter queues for critical topics; add compensation where needed. 


---

## 5. Phase 4 -- Cloud-Native Capabilities 
### 5.1 GOal 
Enable self-healing and autoscaling on cloud platform. 


### 5.2 Steps 
**Unified metrics and logging**
- Add business metrics on key flows (redeem, consume, settlement query, template create): QPS, success rate, latency
- Ensure logs carry TraceId, UserId, TemplateId, OrderId consistently.

**Rate limiting and degration**
- Use Senetinel/Gateway to limit high-QPS endpoints (redeem, settlement query); graceful degration for non-core features (search, remineds). 


**Autoscaling and health checks**
