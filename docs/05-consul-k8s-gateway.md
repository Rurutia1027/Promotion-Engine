# 05 Consul-Based Service Discovery & K8s Gateway Strategy 
> This document describes the **target solution** for replacing Nacos / Spring Cloud Alibaba with a **Consul-based** stack and expose services through **Kubernetes-native gateways(Ingress/Gateway API), NGINX, or Consul API Gateway**.
> Key point: **Nacos and Sentinel are removed from the target architecture; Consul is the single source of truth for discovery and config.**


---

### 1. Design Principles
- **Fully cloud-native**: No Nacos/ Spring Cloud Alibaba dependencies in the target state; all discovery and config go through Consul (via Spring Cloud Consul or sidecars). 
- **Discovery / config abstractions**: Applications depend on an abstract "Service Discovery / Config" layer, not on a particular implementation; Consul is the conrete choice. 
- **Gateway flexibility**: North-south traffic can go through: 
    - K8s-native Ingress or Gateway API controllers (e.g., NGINX Ingress, Envoy Gateway), or 
    - A traditional NGINX API gateway + Consul-based discovery, or
    - Consul API Gateway (Gateway API implementation)
- **Environment agnostic**: Consul supports multi-cluster and hybrid (K8s + VMs); future multi-region / multi-cluster is a first-class scenario. 

---

### 2. Service Discovery & Config: Consul Replaces Nacos
#### 2.1 Target State 
**Service registry**
- All Java services register with **Consul** via Spring Cloud Consul (or Consul agent sidecars), **not** Nacos.
- Non-Java services register using Consul's native HTTP API or via sidecar/mesh integration 

**Configuration**
- Centralized configuration in Consul KV/ config entries.
- Spring apps use `spring-cloud-starter-consul-config` instead of Nacos Config.


#### 2.2 High-level Implementation Path 
**Code level**
- Remove Nacos / Spring Cloud Alibaba dependencies and configuration. 
- Add `spring-cloud-starter-consul-discovery` and `spring-cloud-starter-consul-config`.
- Use `DiscoveryClient`, `@LoadBalanced RestTemplate`/ `WebClient` and friends to resolve services from Consul.

**Ops level**

**TODO (implementation checklist)**
- [ ] Inventory all Nacos usages (discovery  + config) in engine, distribution, settlement, merchant-admin, gateway.
- [ ] Define Consul datacenter/namespace/service naming conventions.
- [ ] Provide baseline Spring Cloud Consul configs for local/test/prod. 

---

### 3. K8s Gateway Layer: Ingress / NGINX / Consul API Gateway 
#### 3.1 Option A: K8s-Native Ingress / Gateway  API + Consul 
**Traffic flow**
- External traffic -> Ingress Controller / Gateway API implementation -> K8s Service -> Pod (App container + optional sidecar).
**Service discovery**
- Ingress/Gateway targets K8s Services; services themselves use Consul for internal service discovery (K8s Service <-> Consul Service mapping)
**Use when**
- You prefer to lean on Kubernetes-native Gateway API as the standard
- You don't yet need full Consul API Gateway or service mesh everywhere


#### 3.2 Option B: Standalone NGINX API Gateway + Consul 
**Traffic flow**
- External traffic -> NGINX cluster (outside or inside K8s) -> Upstream instances discovered via Consul -> Pods

**Service discovery**
- NGINX uses Consul DNS / Consul Template / Lua / dyanmic reloading to resolve upstreams
- Internal services still use Consul-based discovery

**Use when**
- You alreay have a strong NGINX-based gateway operational model.
- You want complex routing/canary/A/B logic centralized in NGINX 


#### 3.3 Option C: Consul API Gateway (Gateway API + CRDs)

**Traffic flow**
- External traffic -> Consul API Gateway (Gateway API implementation) -> mesh-attached Services -> Pods

**Resource model (simplified)**
- `GatewayClass`: owned by platform team, with `controllerName: hashicorp.com/consul-api-gateway-controller`.
- `Gateway`: concrete gateway instance (listeners, addresses)
- `HTTPRoute`: app team-owned routes attached via `parentRefs` to a `Gateway`

**Use when**
- You are (or will be) using Consul Service Mesh 
- You want to standardize on Gateway API CRDs managed along with mesh configuration. 

**TODO (gateway selection & rollout)**
- [ ] Decide which option (A/B/C) to use first for north-south traffic, and document an evolution path to others if needed.
- [ ] Define standard hostname/routing/TLS management across the chosen gateway model.
- [ ] Gradually reduce custom logic in the Spring `gatewway` module to BFF / auth / headers only, moving traffic control into the gateway/mesh.



---

### 4. Replacing Sentinel: Rate Limiting & Resilience 
Since the target architecture removes Sentinel, rate limiting and circuit breaking are handled by a **combination of gateway and application-level resilience**:
- **Gateway-level rate limiting**
    -  Use capabilities from the chosen gateway (Ingress/Gateway API controller, NGINX, or Consul API Gateway) to rate-limit and throttle high-risk endpoints (seckill, redeem, settlement, queries)
    - Apply coarse-grained protection at the edge (per IP, per user, per path) - seems istio cricuit break , rate limiting looks fine 
- **Application-level resilience**
    - Use Resilience4j / Spring Cloud CircuitBreaker for retries, circuit breaking, and fallbacks on outbound calls (order, payment, etc.). 
    - Keep a clean abstraction for "resilience policies" inside the app, so you can swap implementations without touching business logic. 

**TODO**
- [ ] Identify all Sentinel usages (dependencies and annotations) and design replacements (gateway rate limiting + Resilience4j)
- [ ] Define standard SLO-driven limits and fallback strategies for critical flows (redeem, consume, settlement query). 

---

### 5. Alignment With Existing Docs
- In **02 · Migration Execution Plan**  
  - Any references to “Nacos / Sentinel” in **target phases** should be read as the abstract “Discovery/Config + RateLimit/Resilience” layer, **implemented as Consul + K8s gateway + Resilience4j/mesh**.  
- In **03 · Target Architecture (CQRS + Saga)**  
  - The logical `Gateway Service` may be realized as:
    - A Spring Boot gateway module sitting behind K8s Ingress / NGINX / Consul API Gateway, or  
    - In later phases, almost all north–south concerns handled by the gateway/mesh, with the `gateway` module acting mostly as BFF / façade.

---

### 6. Summary 

- **Discovery & config**: Consul (with Spring Cloud Consul / sidecars) fully replaces Nacos in the target state.  
- **Gateway**: North–south gateway is provided by K8s-native Ingress/Gateway API, NGINX, or Consul API Gateway, depending on your chosen operational model.  
- **Rate limiting & resilience**: Sentinel is removed; responsibilities move to gateway components plus Resilience4j-style application-level resilience.  
- Together with the CQRS + Saga + TiDB + RocketMQ blueprint from docs 02/03, this forms a coherent, fully cloud-native target architecture.
