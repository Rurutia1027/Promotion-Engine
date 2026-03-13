package org.tus.promotion.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User login context info DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    /**
     * User id
     */
    private String userId;

    /**
     * Username
     */
    private String username;

    /**
     * Shop number
     */
    private Long shopNumber;
}

