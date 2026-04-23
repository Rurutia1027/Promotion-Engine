# Design: Consul Service Discovery + Spring Cloud Gateway

## 1. Goals and Decision

These two components solve different concerns:

- **Consul**: service registry, discovery, and health checks.
- **Spring Cloud Gateway**: unified ingress traffic governance (routing, auth, rate limiting, observability).

Decision:

- If you want a real API gateway layer, use **both**.
- If you only need discovery, Consul alone is possible but insufficient for gateway governance.

This document adopts: **Spring Cloud Gateway + Consul**.

## 2. Context

The project already has multiple services (`coupon-user`, `coupon-distribution`, `coupon-merchant`,
`coupon-identity`).  
As service count and call paths grow, we need:

- A unified API ingress.
- Dynamic service discovery (avoid static host:port coupling).
- Consistent observability (trace, metrics, structured logs).

## 3. Target Architecture

```text
Client
  -> Spring Cloud Gateway
      -> lb://service-name routing
          -> Consul (registry + health)
          -> coupon-user / coupon-distribution / coupon-merchant / coupon-identity
```

Principles:

- Gateway handles cross-cutting concerns only (no business logic).
- Services register themselves in Consul.
- Gateway resolves upstream targets by service name.

## 4. Responsibilities

### 4.1 Consul

Responsible for:

- Service registration/deregistration.
- Health checks.
- Service discovery.

Not responsible for:

- API route governance.
- Gateway filter chains (auth/rate-limit/logging).

### 4.2 Spring Cloud Gateway

Responsible for:

- Route matching and forwarding.
- Global/per-route filters (auth, context propagation, rate limiting).
- Observability on ingress traffic.

Not responsible for:

- Acting as the service registry itself.

## 5. Suggested Module Layout

- New module: `gateway-service` (Spring Cloud Gateway app).
- Existing services keep business responsibilities unchanged.
- Gateway + all services integrate with Consul client.

## 6. Dependencies

### 6.1 `gateway-service`

Required:

- `spring-cloud-starter-gateway`
- `spring-cloud-starter-consul-discovery`
- `spring-cloud-starter-loadbalancer`
- `spring-boot-starter-actuator`

Optional:

- Resilience4j (circuit-breaker, bulkhead)
- Micrometer + Prometheus registry

### 6.2 Business services

Recommended baseline:

- `spring-cloud-starter-consul-discovery`
- `spring-boot-starter-actuator`

## 7. Configuration Model

### 7.1 Gateway

Key properties:

- `spring.application.name=gateway-service`
- `spring.cloud.consul.host/port`
- `spring.cloud.consul.discovery.register=true`
- `spring.cloud.gateway.discovery.locator.enabled=true` (bootstrap/dev convenience)
- Prefer explicit static routes in production.

Route mapping examples:

- `/api/users/**` -> `lb://coupon-user`
- `/api/distribution/**` -> `lb://coupon-distribution`
- `/api/merchant/**` -> `lb://coupon-merchant`
- `/api/identity/**` -> `lb://coupon-identity`

### 7.2 Services

Key properties:

- `spring.application.name=<service-name>`
- `spring.cloud.consul.discovery.service-name=<service-name>`
- Health endpoint: `/actuator/health`
- Stable `instance-id` recommendation: host + port + startup timestamp.

## 8. Gateway Capability Rollout

### Phase 1: Minimum viable ingress

- Basic routes
- Consul discovery integration
- Health checks

### Phase 2: Governance

- Global auth filter
- Unified error mapping
- Rate limiting (IP/user/route)

### Phase 3: Reliability and observability

- Trace propagation
- Metrics and alerts
- Circuit-breaker/fallback

## 8.1 Global Filter Order (Recommended)

1. `TraceContextFilter`
2. `AccessLogStartFilter`
3. `AuthnAuthzFilter`
4. `UserContextExtractFilter`
5. `RateLimitFilter`
6. `RouteForwardFilter`
7. `MetricsRecordFilter`
8. `AccessLogEndFilter`

Rationale:

- Tracing first ensures all downstream logs/metrics carry correlation.
- User context before rate limiting enables user/shop-based policies.
- Metrics and final access log should run on response completion.

## 8.2 User Context Header Contract

Gateway-internal trusted headers:

- `X-Trace-Id`
- `X-Request-Id`
- `X-User-Id`
- `X-User-Type`
- `X-Shop-Number`
- `X-Roles`
- `X-Auth-Source`
- `X-User-Context-Signature` (optional)

Security rules:

- Strip incoming external headers with same names.
- Re-inject gateway-trusted headers only.
- For sensitive flows, sign context headers and verify downstream.

## 8.3 Rate Limit Key Design

Recommended layered keys:

- Anonymous: `rl:route:{routeId}:ip:{clientIp}`
- Logged-in user: `rl:route:{routeId}:user:{userId}`
- Merchant scope: `rl:route:{routeId}:shop:{shopNumber}`
- Global fallback: `rl:global`

Algorithm:

- Redis token bucket by default.
- Support `burstCapacity` and `replenishRate` separately.

Response contract:

- Return `429` when limited.
- Include:
    - `X-RateLimit-Limit`
    - `X-RateLimit-Remaining`
    - `X-RateLimit-Reset`

## 9. Observability

### 9.1 Tracing

Requirements:

- Generate/propagate `trace-id` and `span-id`.
- Forward trace headers to upstream services.
- Add attributes: `routeId`, `serviceId`, status, latency.

Span suggestions:

- `gateway.request`
- `gateway.route.match`
- `gateway.forward`

### 9.2 Metrics

Core metrics:

- `gateway_requests_total{routeId,status}`
- `gateway_request_latency_ms{routeId}`
- `gateway_4xx_total{routeId}`
- `gateway_5xx_total{routeId}`
- `gateway_discovery_lookup_fail_total`

Alert examples:

- Route-level 5xx ratio above threshold.
- Route-level P95 latency above threshold.
- Consul lookup failures elevated.

### 9.3 Logging

Structured fields:

- `trace_id`, `request_id`
- `route_id`, `service_id`
- `method`, `path`, `status`
- `latency_ms`, `client_ip`
- `user_id`, `shop_number`, `user_type`
- `rate_limit_key`, `rate_limit_result`
- `upstream_host`, `upstream_status`
- `error_code`, `error_message`

Sampling:

- Keep all `ERROR`.
- Sample high-volume `INFO` logs per route.
- Keep all 4xx/5xx access logs.

## 10. Security Baseline

- Expose only gateway publicly; keep services internal.
- Centralize auth at gateway, but keep service-side authorization guardrails.
- Add route-level throttling and allow/deny lists for sensitive paths.

## 11. Risks and Mitigations

- **Risk: relying only on discovery locator causes uncontrolled routing**
    - Mitigation: explicit routes for production.
- **Risk: Consul instability impacts routing**
    - Mitigation: Consul HA setup and robust health checks.
- **Risk: gateway bottleneck / single point**
    - Mitigation: multi-replica deployment + autoscaling + SLO-driven alerts.

## 12. Kubernetes Deployment Design (Gateway + Consul)

## 12.1 Deployment Topology

- `gateway-service`: Kubernetes `Deployment` + `Service` + `HPA`.
- `consul`: recommended via official Helm chart in server HA mode (`StatefulSet`).
- Optional ingress:
    - Cloud LoadBalancer -> Ingress Controller -> `gateway-service`
    - or directly expose `gateway-service` with `LoadBalancer`.

## 12.2 Gateway on K8s

Recommended resources:

- `replicas >= 2` (HA baseline)
- `readinessProbe`: `/actuator/health/readiness`
- `livenessProbe`: `/actuator/health/liveness`
- `PodDisruptionBudget`: `minAvailable: 1`
- `HPA`: scale by CPU + request latency/custom metric
- `topologySpreadConstraints` or anti-affinity across nodes/zones

Recommended env/config:

- `SPRING_PROFILES_ACTIVE=k8s`
- Consul address via DNS (e.g. `consul-server.consul.svc.cluster.local:8500`)
- Route config via ConfigMap/Secret (sensitive keys in Secret)

## 12.3 Consul on K8s

Recommended mode:

- Server HA cluster (3 or 5 servers).
- Persistent volumes for server state.
- ACL enabled for production.
- Gossip encryption enabled.
- mTLS between Consul agents (recommended).

Health and operations:

- Enable server/client health checks.
- Backup snapshots regularly.
- Define failure-domain-aware anti-affinity.

## 12.4 Service Registration Pattern

Two viable patterns:

1. Spring apps register directly to Consul from inside pods.
2. Consul K8s sync/controller style integration.

For this project, start with pattern 1 for lower migration cost.

## 12.5 Traffic and Release Strategy on K8s

- Blue/green or canary deployment for `gateway-service`.
- Route-level gradual rollout for new filters (auth/rate-limit).
- Keep fallback route groups to isolate risky changes quickly.

## 12.6 K8s Observability Integration

- Prometheus scrape on `/actuator/prometheus`.
- Log shipping via Fluent Bit / Vector to centralized storage.
- Trace export via OpenTelemetry Collector.
- Dashboards:
    - Gateway traffic by route
    - 4xx/5xx by route
    - Rate-limit hit ratio
    - Consul health and leader status

## 13. Project-Specific Adoption Path

1. Add `gateway-service` module.
2. Enable Consul registration in `user-service` and `distribution-service` first.
3. Route critical `/api/users/**` and `/api/distribution/**` paths through gateway.
4. Roll out auth + user context + rate limit filters incrementally.
5. Move all client ingress traffic to gateway after validation.

## 14. Final Answer to the Original Question

- Yes, **Consul is for service discovery/health**.
- Yes, **Gateway should host log/metrics/tracing/user-context/rate-limit**.
- For production ingress governance, **use both together**.

## 15. Revision History

| Version | Date       | Notes                                                                                              |
|---------|------------|----------------------------------------------------------------------------------------------------|
| 0.1     | 2026-04-23 | Initial design for Consul + Spring Cloud Gateway                                                   |
| 0.2     | 2026-04-23 | Added filter order, user context header contract, rate-limit key design, enhanced logging guidance |
| 0.3     | 2026-04-23 | Added Kubernetes deployment design for gateway and Consul (HA, scaling, rollout, observability)    |
