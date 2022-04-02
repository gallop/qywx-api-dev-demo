package com.gallop.wechat.vo;

import lombok.Data;

import java.security.PrivateKey;
import java.util.List;

/**
 * author gallop
 * date 2022-03-27 13:21
 * Description:
 * Modified By:
 */
@Data
public class User {
    private String id;
    private String account;
    private String username;
    private List<Integer> departments;
    private String code;
}
