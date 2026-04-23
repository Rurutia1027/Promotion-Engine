package com.tus.coupon.distribution.remote;

import com.tus.coupon.user.api.UserApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "coupon-user",
        url = "${coupon.distribution.feign.remote-url.user:http://127.0.0.1:10031}"
)
public interface UserRemoteClient extends UserApi {
}
