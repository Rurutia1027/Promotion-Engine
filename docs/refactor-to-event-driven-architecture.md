# Promotion Refactoring Design: EDA, DDD, and Clean Architecture

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Reference Architecture (food-ordering-system)](#2-reference-architecture-food-ordering-system)
3. [Current State Assessment](#3-current-state-assessment)
4. [Target Bounded Contexts](#4-target-bounded-contexts)
5. [Target Module Structure](#5-target-module-structure)
6. [Clean Architecture Layers and Dependency Rules](#6-clean-architecture-layers-and-dependency-rules)
7. [DDD Tactical Design](#7-ddd-tactical-design)
8. [EDA Design: Events, Outbox, and Sagas](#8-eda-design-events-outbox-and-saga)
9. [Current-to-Target Mapping](#9-current-to-target-mapping)
10. [Integration Event Catalog](#10-integration-event-catalog)
11. [Data Ownership and Database-per-Service](#11-data-ownership-and-database-per-service)
12. [Shared Infrastructure Modules](#12-shared-infrastructure-modules)
13. [Phased Migration Roadmap](#13-phased-migration-roadmap)
14. [Testing Strategy](#14-testing-strategy)
15. [Non-Functional Requirements](#15-non-functional-requirements)
16. [Risks and Mitigations](#16-risks-and-mitigations)
17. [Success Criteria](#17-success-criteria)

---

## 1. Executive Summary

### 1.1 Goal

Refactor Promotion from a **vertical-slice Spring MVC microservice layout** (Controller --> Service --> Mapper -->
shared MySQL) into an **event-driven, domain-centric platform** that follows:
| Pillar | Intent |
|--------|--------|
| **Clean Architecture** | Domain at the center; infrastructure (JPA, RocketMQ, Redis, ES) as adapters behind ports |
| **DDD** | Explicit bounded contexts, aggregates, domain events, ubiquitous language |
| **EDA** | Cross-service collaboration via versioned integration events, transactional outbox, idempotent consumers,
and sagas |

The structural blueprint mirrors the **food-ordering-system** Maven layout: one aggregator per bounded context, split
into `domain-core`, `application-service`, `dataaccess`, `messaging`, `application` (REST) and `container`.

### 1.2 What changes

| Today                                         | Target                                                      |
|-----------------------------------------------|-------------------------------------------------------------|
| Duplicated `*DO` entities across 4–5 services | One aggregate model per context in `*-domain-core`          |
| Shared sharded MySQL + Canal binlog sync      | Database-per-service; projection via events                 |
| RocketMQ templates duplicated per module      | Shared `infrastructure/messaging` + schema registry         |
| Sync HTTP for order payment/refund on engine  | Choreographed saga driven by `order-event-topic`            |
| XXL-Job polling for coupon tasks              | Temporal workflow (prototype) + outbox for MQ publish       |
| `promotion-api` unused                        | Evolves into `infrastructure/kafka-model` / event contracts |

### 1.3 What stays (pragmatic)


