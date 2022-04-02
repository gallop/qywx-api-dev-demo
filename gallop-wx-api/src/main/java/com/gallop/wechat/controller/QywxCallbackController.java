package com.gallop.wechat.controller;

import com.gallop.wechat.service.WxAssistDevServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * author gallop
 * date 2022-03-24 10:57
 * Description:企业微信回调controller
 * Modified By:
 */
@Controller
@RequestMapping("/callback")
@Slf4j
public class QywxCallbackController {
    @Resource
    private WxAssistDevServiceImpl wxAssistDevService;

    @ResponseBody
    @GetMapping("/redirect")
    public String getVerify(@RequestParam(value = "code") String code,
                            @RequestParam(value = "state") String state){
        log.info("user-code="+code);
        log.info("user-state="+state);

        return "success";
    }

    /**
      * @date 2022-03-25 9:19
      * Description: 代开发模板回调url验证
      * Param:
      * return:
      **/
    @ResponseBody
    @GetMapping("/templateVerify")
    public String getVerify(@RequestParam(value = "msg_signature") String sVerifyMsgSig,
                       @RequestParam(value = "timestamp") String sVerifyTimeStamp,
                       @RequestParam(value = "nonce") String sVerifyNonce,
                       @RequestParam(value = "echostr") String sVerifyEchoStr){
        log.info("get回调验证开始");
        log.info("msg_signature:"+sVerifyMsgSig);
        log.info("timestamp:"+sVerifyTimeStamp);
        log.info("nonce:"+sVerifyNonce);
        log.info("echostr:"+sVerifyEchoStr);
        log.info("get回调验证");

        String result = wxAssistDevService.verifyGetByTemplate(sVerifyMsgSig,sVerifyTimeStamp, sVerifyNonce,sVerifyEchoStr);

        return  result;

    }
    @ResponseBody
    @PostMapping("/templateVerify")
    public String postVerify(@RequestParam(value = "msg_signature") String sVerifyMsgSig,
                              @RequestParam(value = "timestamp") String sVerifyTimeStamp,
                              @RequestParam(value = "nonce") String sVerifyNonce,
                             @RequestBody String xmlBody){
        log.info("post回调验证开始:/templateVerify");
        log.info("msg_signature:"+sVerifyMsgSig);
        log.info("timestamp:"+sVerifyTimeStamp);
        log.info("nonce:"+sVerifyNonce);
        log.info("xmlBody:"+xmlBody);
        log.info("post回调验证结束");

        String result = wxAssistDevService.verifyPostByTemplate(sVerifyMsgSig,sVerifyTimeStamp,sVerifyNonce,xmlBody);

        return  result;

    }
    @ResponseBody
    @GetMapping("/suite/receive")
    public String instructGet(@RequestParam(value = "msg_signature") String sVerifyMsgSig,
                               @RequestParam(value = "timestamp") String sVerifyTimeStamp,
                               @RequestParam(value = "nonce") String sVerifyNonce,
                               @RequestParam(value = "echostr") String sVerifyEchoStr){
        log.info("客户授权应用：get回调验证开始");
        log.info("客户授权应用：msg_signature:"+sVerifyMsgSig);
        log.info("客户授权应用：timestamp:"+sVerifyTimeStamp);
        log.info("客户授权应用：nonce:"+sVerifyNonce);
        log.info("客户授权应用：echostr:"+sVerifyEchoStr);
        log.info("客户授权应用：get回调验证");
        String result = wxAssistDevService.verifyGetByClientAuth(sVerifyMsgSig,sVerifyTimeStamp,sVerifyNonce,sVerifyEchoStr);
        return result;
    }
    @ResponseBody
    @PostMapping("/suite/receive")
    public String instructPost(@RequestParam(value = "msg_signature") String sVerifyMsgSig,
                        @RequestParam(value = "timestamp") String sVerifyTimeStamp,
                        @RequestParam(value = "nonce") String sVerifyNonce,
                        @RequestBody String xmlBody){
        String result = "success";
        log.info("sVerifyMsgSig:"+sVerifyMsgSig);
        log.info("sVerifyTimeStamp:"+sVerifyTimeStamp);
        log.info("nonce:"+sVerifyNonce);
        log.info("xmlBody:"+xmlBody);
        result = wxAssistDevService.verifyPostByClientAuth(sVerifyMsgSig,sVerifyTimeStamp,sVerifyNonce,xmlBody);
        return result;
    }
}
