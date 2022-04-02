package com.gallop.wechat.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.gallop.wechat.entity.QywxThirdCompany;
import com.gallop.wechat.repository.CompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;

/**
 * author gallop
 * date 2022-03-31 9:17
 * Description: 由于微信后台重新获取authCode 需要1秒内进行响应，所以消息先扔redis队列里，
 *              此类是处理消费redis队列信息的。
 * Modified By:
 */
@Component
@Slf4j
public class RedisReceiver {
    @Autowired
    private WxAssistDevServiceImpl wxAssistDevService;


    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance , ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

    public void receiveMessage(String message) throws Exception {
        System.err.println("authCode-message:"+message);
        String authCode = objectMapper.readValue(message, String.class);
        log.info("insertOrUpdate-company-authCode:"+authCode);
        wxAssistDevService.getPermentCodeFromApi(authCode);
    }


}
