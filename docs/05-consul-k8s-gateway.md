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

