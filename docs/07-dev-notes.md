# Project DTOs

## Distribution Module

- Request DTO: `MessageSendReqDTO`
- Response DTO: `MessageSendRespDTO`
- Common Extension DTO: `BaseSendExtendDTO`

## Search Module

- Request DTO: `CouponTemplatePageQueryReqDTO`
- Response DTO: `CouponTemplatePageQueryRespDTO`
- Common Extension DTO: `BaseSendExtendDTO`

## Settlement Module

- Request Coupon Goods DTO: `QueryCouponGoodsReqDTO`
- Request Coupon DTO: `QueryCouponReqDTO`

- Response Coupon DTO: `QueryCouponRespDTO`
- Response Coupon Detail DTO: `QueryCouponDetailRespDTO`
- Response Coupon Template DTO: `CouponTemplateQueryRespDTO`

- UserContext Info DTO: `UserInfoDTO`

## Engine Module

### Request DTO (core flow)

- `CouponProcessPaymentReqDTO`
- `CouponProcessRefundReqDTO`
- `CouponTemplateQueryReqDTO`
- `CouponTemplateRedeemReqDTO`
- `CouponTemplateRemindQueryReqDTO`
- `CouponTemplateRemindCancelReqDTO`
- `CouponCreatePaymentReqDTO`
- `CouponCreatePaymentGoodsReqDTO`
- `CouponTemplateReminCreateReqDTO`

### Response DTO

- `CouponTemplateQueryRespDTO`
- `CouponTemplateRemindQueryRespDTO`

### Remind DTO

- `CouponTemplateRemindDTO`

### User Context Info DTO

- `UserInfoDTO`

### Common Extension DTO

- `BaseSendExtendDTO`

## Merchant-Admin

### Request DTO

- `CouponTaskCreateReqDTO`
- `CouponTaskPageQueryReqDTO`
- `CouponTemplateNumberReqDTO`
- `CouponTemplatePageQueryReqDTO`
- `CouponTemplateSaveReqDTO`

### Response DTO

- `CouponTemplatePageQueryRespDTO`
- `CouponTaskPageQueryRespDTO`
- `CouponTemplateQueryRespDTO`
- `CouponTaskQueryRespDTO`

### User Context Info DTO

- `UserInfoDTO`

---

# Independent Services in Current Architecture

## gateway

**Role**: Unified entry point responsible for routing, logging, rate limiting, etc.
**External interface**: Exposes a single HTTP entry for the frontend/callers and forwards requests to backend business
services.

## merchant-admin

**Role**: Merchant management backend, responsible for coupon templates and task configuration.
**External interface**: Exposes REST APIs to the operations/admin frontend (`/api/merchant-admin/**`) and also sends MQ
messages to services such as distribution.

## engine

**Role**: Core user coupon engine (viewing, reserving/locking coupons, redemption, consumption after payment, refund
release, reminders).
**External interface**: Exposes REST APIs to the BFF/gateway (`/api/engine/**`) and also consumes/sends MQ events.

## settlement

**Role**: Coupon template search and exposure (based on Elasticsearch)
**External interface**: Expose search APIs (`/api/search/**`) and synchronizes indexes from MQ or other services.

## distribution

**Role**: Batch coupon distribution (task-driven, scheduled/batch processing).
**External interface**: Usually not directly exposed to the frontend via HTTP. Instead it mainly:

- Consumes "coupon distribution task created" events via MQ
- Or is invoked via HTTP/RPC by the backend or scheduling systems (e.g., XXL-Job)
  From a deployment perspective, it is still an independent process/service.

## framework

Only a shared base library (Result, exceptions, idempotency utilities, helper tools). It is not a deployed as a
standalone service.

## search

## distribution

## framework 