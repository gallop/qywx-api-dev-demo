package com.gallop.wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author gallop
 * date 2022-03-24 10:49
 * Description:
 * Modified By:
 */
@Data
@Component
@ConfigurationProperties(prefix = "qywx-dev")
public class QywxDevProperties {
    private String corpId; //服务商企业id
    private String token; // 企业微信后台，开发者设置的模板token
    private String encodingAESKey;//企业微信后台，开发者设置的模板EncodingAESKey
    private String clientCorpId;//客户企业微信id
    private String suiteId;
    private String suiteSecret;
    //private String clientToken;
    //private String clientEncodingAESKey;
    private String redirectUri;//授权后重定向的回调链接地址

    private String baseUrl = "https://qyapi.weixin.qq.com/cgi-bin/";
    // https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=ID&corpsecret=SECRET

    //服务商相关
    private String serviceUrl = baseUrl+"service/";
    private String suiteTokenUrl = serviceUrl+"get_suite_token";
    private String permanentCodeUrl = serviceUrl+"get_permanent_code?suite_access_token=%s";
    private String corpAccessTokenUrl = baseUrl + "gettoken?corpid=%s&corpsecret=%s";
    // 需要的参数苏：CORPID REDIRECT_URI AGENTID STATE
    private String userAuthorizeUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&agentid=%s&state=%s#wechat_redirect";
    // 需要的参数：SUITE_ACCESS_TOKEN  CODE
    private String userIdUrl = baseUrl+ "user/getuserinfo?access_token=%s&code=%s";
    //获取通讯录员工信息
    //https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&userid=USERID
    // 参数：ACCESS_TOKEN USERID
    private String userInfoUrl = baseUrl + "user/get?access_token=%s&userid=%s";

}
