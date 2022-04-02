package com.gallop.wechat.controller;

import com.gallop.wechat.base.BaseResult;
import com.gallop.wechat.base.BaseResultUtil;
import com.gallop.wechat.config.QywxDevProperties;
import com.gallop.wechat.entity.QywxThirdCompany;
import com.gallop.wechat.service.WxAssistDevServiceImpl;
import com.gallop.wechat.sso.SsoService;
import com.gallop.wechat.sso.SsoServiceRoute;
import com.gallop.wechat.sso.SsoTypeEnum;
import com.gallop.wechat.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * author gallop
 * date 2022-03-27 13:16
 * Description:
 * Modified By:
 */
@Slf4j
@RestController
//@RequestMapping(value = "/qywx")
public class QywxLoginController {

    @Autowired
    private QywxDevProperties qywxDevProperties;
    @Resource
    private WxAssistDevServiceImpl wxAssistDevService;

    @Autowired
    private SsoServiceRoute ssoServiceRoute;

    @Resource
    private RestTemplate restTemplate;

    @RequestMapping(value = "/redirectTest", method = RequestMethod.GET)
    public void redirectAuthorizeTest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        String url = "http://localhost:8088/callback?code=abc123&state="+state;

        String urlEncode = URLEncoder.encode(url,"utf-8");

        //restTemplate.getForEntity(url,String.class);
        log.info("redirect-url:" + urlEncode);
        response.sendRedirect(url);
    }

    @RequestMapping(value = "/redirectAuth", method = RequestMethod.GET)
    public void redirectAuthorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        QywxThirdCompany company = wxAssistDevService.getCompany(qywxDevProperties.getClientCorpId());
        String urlEncode = URLEncoder.encode(qywxDevProperties.getRedirectUri(),"utf-8");
        // 参数顺序： CORPID REDIRECT_URI AGENTID STATE
        String url = String.format(qywxDevProperties.getUserAuthorizeUrl(),qywxDevProperties.getClientCorpId(),urlEncode,company.getAgentId(),state);
        log.info("redirect-url:" + url);
        response.sendRedirect(url);
    }

    @RequestMapping(value = "/getTokenTest", method = RequestMethod.POST)
    public BaseResult getTokenTest(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        if (ObjectUtils.isEmpty(user.getCode())) {
            return BaseResultUtil.failure("code 不能为空！");
        }
        log.info("user信息：{}",user);
        Map returnMap = new HashMap();
        String uuid = UUID.randomUUID().toString();
        returnMap.put("loginToken", uuid);

        return BaseResultUtil.success(returnMap);
    }

    @RequestMapping(value = "/getWxToken", method = RequestMethod.POST)
    public BaseResult login(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        if (ObjectUtils.isEmpty(user.getCode())) {
            return BaseResultUtil.failure("code 不能为空！");
        }
        BaseResult baseResult = null;
        try {
            SsoService ssoService = ssoServiceRoute.route(SsoTypeEnum.QYWX);
            baseResult = ssoService.login(user,request,response);
        } catch (Exception e) {
            e.printStackTrace();
            baseResult = BaseResultUtil.failure("没有可以的sso service！");
        }

        return baseResult;
    }
}
