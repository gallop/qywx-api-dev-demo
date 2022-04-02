package com.gallop.wechat.sso;

/**
 * author gallop
 * date 2021-06-17 16:29
 * Description:
 * Modified By:
 */
public enum SsoTypeEnum {
    OAUTH2("oauth2"), // oauth2 协议的sso
    QYWX("qywechat");// 企业微信sso

    private final String value;

    SsoTypeEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
