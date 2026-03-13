# Promotion-Engine

`Promotion-Engine` is a high-throughput promotion and coupon engine designed for real-world marketing scenarios. It is
currently based on the original oneCoupon implementation, with an explicit roadmap to evolve into a fully cloud-native,
event-driven promotion platform.

Our long-term goals:

- **Business**: Provide a complete set of capabilities for coupon template management, user coupon acquisition and batch
  distribution, order settlement, search, and op analytics.
- **Technology**: Evolve into a **cloud-native, event-driven, horizontal scalable** promotion engine with strong
  observability, clearing, and reconciliation features.

The detailed technical design and evolution plans are documented under the `docs/` folder.

---

## High-Level Architecture & Expectations

We expect `Promotion-Engine` to coverage on these key directions:

**Clear module boundaries**

- `engine`: user coupon lifecycle (view, lock, redeem, consume, expire)
- `distribution`: batch coupon tasks and distribution execution
- `settlement`: order amount calculation and coupon deducations
- `merchant-admin`: merchant-facing coupon templates and distribution tasks.
- `search`: coupon template search and exposure.

**Truly cloud-native target**

- Data layer evolving from MySQL + ShardingSphere to **TiDB** or similar cloud-native distributed databases.
- Service discovery & config managed by **Consul** (No Nacos / Spring Cloud Alibaba / Sentinel in the target state)
- Gateway provided by **Kubernetes-native Gateway / Ingress**, **NGINX**, or **Consul API Gateway**, depending on
  deployment needs.

**Event-driven with CQRS & Saga**

- RocketMQ carries key business events: redeem, batch distribution, payment, refund, settlement, etc.
- Command side (write) via dedicated command services (`redeem/lock/consume/release`); query side (read) via query
  services backed by caches/search.
- Cross-service consistency (order, payment, coupons) managed via **Saga** patterns (choreography with minimal
  orchestration).

**Observability, clearing, and reconciliation**

- Unified logging and metrics for critical flows (redeem, distribution, settlement)
- Standardized business events written into **ClickHouse** to support online reports and formal reconciliation across
  ledgers.

---

## What's in `docs/` (Architecture & Evolution)

The `docs/` folder contains the design documents summarizing our current understanding and target direction:

### 01 Architecture & Module Analysis

- Current module boundaries and core APIs
- Gaps versus an ideal cloud-native microservice architecture
- Where and how MQ is used today, and decoupling feasibility.

### 02 Migration Execution Plan

- Common DTO/API contracts extraction
- Database refactor toward TiDB and clearer domain boundaries
- Event-driven architecture with RocketMQ and command/event services.
- Cloud-native hardening (observability, rate limiting, autoscaling)

### Target Architecture (CQRS + Saga)

- Target service landscape, domain data layout, CQRS design, and Saga flows for orders, payments, and coupons

### Logging Strategy

- Separation of **business audit** (e.g., template operation history) from **observability logs**
- Guidance to move generic logs to Loki (or similar), while keeping only necessary audit data in DB/analytic stores.

### Consul + K8s Gateway Strategy

- Clear principle: remove Nacos and Sentinel from the target architecture.
- Use Consul for service discovery / config
- Gateway options: K8s Gateway / Ingress, NGINX, or Consul API Gateway, and how they fit into the overall design.

### Event Clearing & ClickHouse Reporting

- End-to-end pipeline: RocketMQ events -> Clearing ETL Service -> ClickHouse fact/agg tables -> reporting &
  reconciliation APIs.
- Main flow diagrams, ClickHouse table and computation mode design, and recommended RocketMQ -> CK ingestion pattern.

---

## How We Plan to Evolve Promotion-Engine

**Short-term**

- Keep all existing business features working.
- Gradually encode the documented constraints and targets (Consul instead of Nacos, standardized events, DTO extraction,
  etc.) into the codebases.

**Medium term**

- Implement CORS + Saga, migrate key data to TiDB, introduce Consul-based discovery and K8s gateway, and stand up
  ClickHouse as the event warehouse.
- Make `Promotion-Engine` not just a learning / demo project, but a credible blueprint for a production-ready promotion
  engine.

**Long-term**

- Layer on multi-tenancy, multi-region deployments, and richer ops analytics and promotion scenarios on top of the solid
  cloud-native foundation.