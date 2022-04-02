package com.gallop.wechat.sso;

import com.alibaba.fastjson.JSONObject;
import com.gallop.wechat.base.BaseResult;
import com.gallop.wechat.base.BaseResultUtil;
import com.gallop.wechat.config.QywxDevProperties;
import com.gallop.wechat.service.WxAssistDevServiceImpl;
import com.gallop.wechat.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * author gallop
 * date 2022-03-27 13:45
 * Description:
 * Modified By:
 */
@Service
@Slf4j
public class QywxSsoServiceOauth2Impl extends SsoServiceBasic{
    @Autowired
    private QywxDevProperties qywxDevProperties;

    @Resource
    private WxAssistDevServiceImpl wxAssistDevService;

    @Resource
    private RestTemplate restTemplate;

    @Override
    public SsoTypeEnum ssoType() {
        return SsoTypeEnum.QYWX;
    }

    @Override
    public BaseResult login(User user, HttpServletRequest request, HttpServletResponse response) {
        if (ObjectUtils.isEmpty(user.getCode())) {
            return BaseResultUtil.failure("code 不能为空！");
        }
        log.info("user-code:"+user.getCode());
        String userId = getUserIdFromApi(user.getCode());
        if (ObjectUtils.isEmpty(userId)) {
            return BaseResultUtil.failure("获取userId失败！");
        }
        log.info("wx_userId:"+userId);
        User userInfoFromApi = getUserInfoFromApi(userId);
        if (ObjectUtils.isEmpty(userInfoFromApi)) {
            return BaseResultUtil.failure("获取用户信息失败！");
        }

        BaseResult baseResult = returnspot(userInfoFromApi, request, response);
        return baseResult;
    }
     /**
      * date 2022-03-27 13:53
      * Description:
      * Param:
      * return:
      * {
      * 	"errcode": 0,
      * 	"errmsg": "ok",
      * 	"CorpId": "CORPID",
      * 	"UserId": "USERID",
      * 	"DeviceId": "DEVICEID",
      * 	"user_ticket": "USER_TICKET",
      * 	"expires_in": 7200,
      * 	"open_userid": "wwxxxx"
      * }
      **/
    private String getUserIdFromApi(String code){
        //String suiteToken = wxAssistDevService.getSuiteToken();
        String corpAccessToken = wxAssistDevService.getCorpAccessToken(qywxDevProperties.getClientCorpId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> entity = new HttpEntity(headers);
        String url = String.format(qywxDevProperties.getUserIdUrl(),corpAccessToken,code);
        log.info("get-user-id-apiurl:"+url);
        ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity,JSONObject.class);
        if(responseEntity.getStatusCode()== HttpStatus.OK){
            Map responseBody = responseEntity.getBody();
            log.info("get-userId-return:"+responseBody.toString());
            if(responseBody.containsKey("errcode") && (Integer) responseBody.get("errcode") == 0){
                String userId= (String) responseBody.get("UserId");
                return userId;
            }
        }

        return null;
    }

    private User getUserInfoFromApi(String userId){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> entity = new HttpEntity(headers);
        String corpAccessToken = wxAssistDevService.getCorpAccessToken(qywxDevProperties.getClientCorpId());
        //String suiteToken = wxAssistDevService.getSuiteToken();
        /*String openUserId = transformToOpenUserId(userId);
        if(openUserId==null){
            return null;
        }*/
        String url = String.format(qywxDevProperties.getUserInfoUrl(),corpAccessToken,userId);
        log.info("call getUserInfo api url:"+url);
        ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity,JSONObject.class);
        if(responseEntity.getStatusCode()== HttpStatus.OK){
            Map responseBody = responseEntity.getBody();
            log.info("user-info:"+responseBody.toString());
            if(responseBody.containsKey("errcode") && (Integer) responseBody.get("errcode") == 0){
                String userAccount= (String) responseBody.get("UserId");
                String userName = (String) responseBody.get("name");
                List<Integer> departmentList = (List) responseBody.get("department");
                //Integer [] dep = (Integer[]) responseBody.get("department");
                System.err.println(">>>>dep:"+ departmentList);
                User user = new User();
                user.setAccount(userAccount);
                user.setUsername(userName);
                user.setDepartments(departmentList);
                return user;
            }
        }

        return null;
    }


}
