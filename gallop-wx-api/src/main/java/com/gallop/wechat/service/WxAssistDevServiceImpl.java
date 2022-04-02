package com.gallop.wechat.service;

import com.alibaba.fastjson.JSONObject;
import com.gallop.wechat.aes.AesException;
import com.gallop.wechat.aes.WXBizMsgCrypt;
import com.gallop.wechat.config.QywxDevProperties;
import com.gallop.wechat.config.RedisConfig;
import com.gallop.wechat.entity.QywxThirdCompany;
import com.gallop.wechat.repository.CompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * author gallop
 * date 2022-03-24 11:38
 * Description:企业微信自建代开发service
 * Modified By:
 */
@Service
@Slf4j
public class WxAssistDevServiceImpl {
    public static final String SUITTICKET_KEY_PREFIX = "wechat-suitTicket:";
    public static final String SUITE_ACCESS_TOKEN_KEY_PREFIX = "suite-access-token:";
    //private static String suiteTicket = "";
    //private static String suiteAccessToken = ""; //临时存于此，有效期2个小时，生成环境应存于redis中

    @Autowired
    private QywxDevProperties qywxDevProperties;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private CompanyRepository companyRepository;

     /**
      * @date 2022-03-25 9:16
      * Description: 创建代开发模板时，回调url验证 get请求
      * Param:
      * return:
      **/
    public String verifyGetByTemplate(String sVerifyMsgSig, String sVerifyTimeStamp,
                                      String sVerifyNonce, String sVerifyEchoStr){
        String sToken = qywxDevProperties.getToken();
        String sCorpID =  qywxDevProperties.getCorpId();
        String sEncodingAESKey = qywxDevProperties.getEncodingAESKey();

        System.err.println("sToken:"+sToken);
        System.err.println("sCorpID:"+sCorpID);
        System.err.println("sEncodingAESKey:"+sEncodingAESKey);

        WXBizMsgCrypt wxcpt = null;
        try {
            wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, sCorpID);
        }catch (AesException E){
            return "error";
        }

        String sEchoStr; //需要返回的明文
        try {
            sEchoStr = wxcpt.VerifyURL(sVerifyMsgSig, sVerifyTimeStamp,
                    sVerifyNonce, sVerifyEchoStr);
            System.err.println("verifyurl echostr: " + sEchoStr);
            // 验证URL成功，将sEchoStr返回
        } catch (Exception e) {
            //验证URL失败，错误原因请查看异常
            e.printStackTrace();
            return "error";
        }
        return  sEchoStr;
    }
     /**
      * @date 2022-03-25 16:29
      * Description: 企业授权代开发模板应用后的回调地址，获取的信息处理（处理post的请求）
      *        1、当授权成功后，微信后台会向回调地址推送授权成功的信息（可得到临时授权码-->永久授权码）
      *        2、微信后台每10分钟会向回调地址推送一次suite_ticket 的信息；
      * Param:
      * return:
      **/
    public String verifyPostByTemplate(String sVerifyMsgSig, String sVerifyTimeStamp,
                                       String sVerifyNonce, String xmlData){
        String result = "error";

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            StringReader xmlBodyReader = new StringReader(xmlData);
            InputSource xmlBodySource = new InputSource(xmlBodyReader);
            Document documentBody = db.parse(xmlBodySource);
            Element rootBody = documentBody.getDocumentElement();
            NodeList toUserNameNode = rootBody.getElementsByTagName("ToUserName");
            String toUserName = toUserNameNode.item(0).getTextContent();

            String sToken = qywxDevProperties.getToken();
            String receiveid =  toUserName;
            String sEncodingAESKey = qywxDevProperties.getEncodingAESKey();

            System.err.println("sToken:"+sToken);
            System.err.println("receiveid:"+receiveid);
            System.err.println("sEncodingAESKey:"+sEncodingAESKey);

            WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, receiveid);

            String sMsg = wxcpt.DecryptMsg(sVerifyMsgSig, sVerifyTimeStamp, sVerifyNonce, xmlData);
            log.info("after template-encrypt sMsg: " + sMsg);
            // TODO: 解析出明文xml标签的内容进行处理

            StringReader msgReader = new StringReader(sMsg);
            InputSource msgSource = new InputSource(msgReader);
            Document document = db.parse(msgSource);

            Element root = document.getDocumentElement();
            NodeList infoTypeNode = root.getElementsByTagName("InfoType");
            String infoType = infoTypeNode.item(0).getTextContent();
            log.info(">>>infoType:"+infoType);
            switch (infoType){
                case "create_auth":
                case "reset_permanent_code":
                    //获取auth_code
                    NodeList authcodeNode = root.getElementsByTagName("AuthCode");
                    String authCode = authcodeNode.item(0).getTextContent();
                    log.info("auth code:"+authCode);
                    //因为微信后端需要在1000毫秒内返回，这里先把信息扔到redis队列里
                    redisTemplate.convertAndSend(RedisConfig.AUTH_CODE_TOPIC,authCode);
                    //getPermentCodeFromApi(authCode);
                    break;
                case "suite_ticket":
                    setSuitTicket(root);
                    break;
            }
            result = "success";
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AesException e){
            e.printStackTrace();
        }

        return result;
    }

     /**
      * @date 2022-03-25 11:40
      * Description: 客户企业微信授权模板的应用get验证
      * Param:
      * return:
      **/
    public String verifyGetByClientAuth(String sVerifyMsgSig, String sVerifyTimeStamp,
                                        String sVerifyNonce, String sVerifyEchoStr){
        String sToken = qywxDevProperties.getToken();
        String receiveId =  qywxDevProperties.getClientCorpId();
        String sEncodingAESKey = qywxDevProperties.getEncodingAESKey();

        System.err.println("sToken:"+sToken);
        System.err.println("receiveid:"+receiveId);
        System.err.println("sEncodingAESKey:"+sEncodingAESKey);

        WXBizMsgCrypt wxcpt = null;
        try {
            wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, receiveId);
        }catch (AesException E){
            return "error";
        }

        String sEchoStr; //需要返回的明文
        try {
            sEchoStr = wxcpt.VerifyURL(sVerifyMsgSig, sVerifyTimeStamp,
                    sVerifyNonce, sVerifyEchoStr);
            System.err.println("verifyurl echostr: " + sEchoStr);
            // 验证URL成功，将sEchoStr返回
            // HttpUtils.SetResponse(sEchoStr);
        } catch (Exception e) {
            //验证URL失败，错误原因请查看异常
            e.printStackTrace();
            return "error";
        }
        return  sEchoStr;
    }

    public String verifyPostByClientAuth(String sVerifyMsgSig, String sVerifyTimeStamp,
                                       String sVerifyNonce, String xmlData){
        String result = "error";

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            StringReader xmlBodyReader = new StringReader(xmlData);
            InputSource xmlBodySource = new InputSource(xmlBodyReader);
            Document documentBody = db.parse(xmlBodySource);
            Element rootBody = documentBody.getDocumentElement();
            NodeList toUserNameNode = rootBody.getElementsByTagName("ToUserName");
            String toUserName = toUserNameNode.item(0).getTextContent();

            String sToken = qywxDevProperties.getToken();
            String receiveid =  toUserName;
            String sEncodingAESKey = qywxDevProperties.getEncodingAESKey();

            System.err.println("wx-client:sToken:"+sToken);
            System.err.println("wx-client:receiveid:"+receiveid);
            System.err.println("wx-client:sEncodingAESKey:"+sEncodingAESKey);

            WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, receiveid);

            String sMsg = wxcpt.DecryptMsg(sVerifyMsgSig, sVerifyTimeStamp, sVerifyNonce, xmlData);
            log.info("wx-clietn:after template-encrypt sMsg: " + sMsg);

            StringReader msgReader = new StringReader(sMsg);
            InputSource msgSource = new InputSource(msgReader);
            Document document = db.parse(msgSource);

            Element root = document.getDocumentElement();
            NodeList msgTypeNode = root.getElementsByTagName("MsgType");
            String msgType = msgTypeNode.item(0).getTextContent();
            log.info(">>>wx-client:msgType:"+msgType);
            switch (msgType){
                case "create_auth":
                case "reset_permanent_code":
                    break;
                case "suite_ticket":
                    setSuitTicket(root);
                    break;
            }
            result = "success";
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AesException e){
            e.printStackTrace();
        }

        return result;
    }


     /**
      * @date 2022-03-24 14:20
      * Description: SuiteTicket 缓存处理
      * Param:
      * return:
      **/
    private String setSuitTicket(Element root){
        NodeList nodelist = root.getElementsByTagName("SuiteTicket");
        String suitTicket = nodelist.item(0).getTextContent();
        System.err.println(">>>>>>>save-to-redis-suitTicket:"+suitTicket);
        redisTemplate.opsForValue().set(SUITTICKET_KEY_PREFIX + qywxDevProperties.getSuiteId(),suitTicket,1750, TimeUnit.SECONDS);
        //WxAssistDevServiceImpl.suiteTicket = suitTicket;
        return suitTicket;
    }
     /**
      * @date 2022-03-24 14:29
      * Description: 获取SuiteTicket
      * Param:
      * return:
      **/
    public String getSuiteTicket(){
        String result=null;
        result = (String)redisTemplate.opsForValue().get(SUITTICKET_KEY_PREFIX + qywxDevProperties.getSuiteId());
        //result = WxAssistDevServiceImpl.suiteTicket;
        log.info("redis-SuitTicket-key:"+SUITTICKET_KEY_PREFIX + qywxDevProperties.getSuiteId());
        log.info("get:SuitTicket:"+result);
        if(ObjectUtils.isEmpty(result)){
            log.error("suit_ticket为空");
        }
        return result;
    }
     /**
      * @date 2022-03-26 23:26
      * Description: suiteAccessToken 缓存操作
      * Param: suiteAccessToken 有效期2个小时，这里缓存7100秒
      * return:
      **/
    public String setSuiteAccessTokenToRedis(String suiteAccessToken){
        redisTemplate.opsForValue().set(SUITE_ACCESS_TOKEN_KEY_PREFIX + qywxDevProperties.getSuiteId(),suiteAccessToken,7100, TimeUnit.SECONDS);
        return suiteAccessToken;
    }

    private String getSuiteAccessTokenFromRedis(){
        String result=null;
        result = (String)redisTemplate.opsForValue().get(SUITE_ACCESS_TOKEN_KEY_PREFIX + qywxDevProperties.getSuiteId());
        //result = WxAssistDevServiceImpl.suiteTicket;
        log.info("get:SuiteAccessToken:"+result);
        if(ObjectUtils.isEmpty(result)){
            log.error("SuiteAccessToken为空");
        }
        return result;
    }


    //获取第三方应用凭证 suite_access_token
    public String getSuiteToken(){
        String result = null;
        result = getSuiteAccessTokenFromRedis();
        if(!ObjectUtils.isEmpty(result)){
            return result;
        }

        String suiteTicket = getSuiteTicket();
        if(ObjectUtils.isEmpty(suiteTicket)){
            System.err.println(">>suiteTicket is null");
            return null;
        }
        JSONObject postJson = new JSONObject();
        postJson.put("suite_id",qywxDevProperties.getSuiteId());
        postJson.put("suite_secret",qywxDevProperties.getSuiteSecret());
        postJson.put("suite_ticket",suiteTicket);
        //设置提交json格式数据
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> request = new HttpEntity(postJson, headers);
        ResponseEntity<JSONObject> responseEntity = restTemplate.postForEntity(qywxDevProperties.getSuiteTokenUrl(),request,JSONObject.class);
        log.info("call getSuiteToken api return:"+responseEntity.getBody().toJSONString());
        if(responseEntity.getStatusCode()== HttpStatus.OK){
            JSONObject responseBody = responseEntity.getBody();
            result = (String) responseBody.get("suite_access_token");
            setSuiteAccessTokenToRedis(result);
        }
        return result;

    }
     /**
      * @date 2022-03-25 16:56
      * Description: 从微信api接口获取永久授权码
      * Param:
      * return:
      **/
    public boolean getPermentCodeFromApi(String authCode){
        JSONObject postJson = new JSONObject();
        postJson.put("auth_code",authCode);
        log.error(postJson.toString());
        String suiteAccessToken = getSuiteToken();
        if(ObjectUtils.isEmpty(suiteAccessToken)){
            return false;
        }
        String url = String.format(qywxDevProperties.getPermanentCodeUrl(),suiteAccessToken);
        log.info("call getPermentCode api url:"+url);

        //设置提交json格式数据
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> request = new HttpEntity(postJson, headers);
        ResponseEntity<JSONObject> responseEntity = restTemplate.postForEntity(url,request,JSONObject.class);
        log.info("call getPermentCode api return:"+responseEntity.getBody().toJSONString());
        if(responseEntity.getStatusCode()== HttpStatus.OK){

            Map responseBody = responseEntity.getBody();
            if(responseBody.containsKey("errcode") && (Integer) responseBody.get("errcode") != 0){
                //调用接口出错
                log.error("getPermentCode error:"+responseBody.toString());
            }else {
                QywxThirdCompany company = new QywxThirdCompany();
                //获取永久授权码
                String permanenCode= (String) responseBody.get("permanent_code");
                //获取corpId
                Map authCorpInfo =(Map) responseBody.get("auth_corp_info");
                String corpId = (String) authCorpInfo.get("corpid");
                String corpName = (String) authCorpInfo.get("corp_name");
                String corpFullName = (String)authCorpInfo.get("corp_full_name");
                Integer subjectType = (Integer) authCorpInfo.get("subject_type");
                company.setCorpId(corpId);
                company.setCorpName(corpName);
                company.setCorpFullName(corpFullName);
                company.setPermanentCode(permanenCode);
                company.setSubjectType(subjectType);

                //获取agent
                Map authInfo = (Map) responseBody.get("auth_info");
                List agentList = (List) authInfo.get("agent");
                Map agent = (Map) agentList.get(0);
                Integer agentId = (Integer) agent.get("agentid");
                String agentName = (String) agent.get("name");
                company.setAgentId(agentId);
                company.setAgentName(agentName);
                company.setStatus(1);
                saveCompany(company);

                //因为永久授权码已经变化，需要删除redis中的 corp-access-token(此accessToken 是根据永久授权码调用接口获取的)
                Set<String> keys = redisTemplate.keys("corp-access-token" + "*");
                redisTemplate.delete(keys);
            }

        }
        return true;
    }

    @Cacheable(value = "third-company",key = "#corpId")
    public QywxThirdCompany getCompany(String corpId){
        return companyRepository.findByCorpId(corpId);
    }

    @CacheEvict(value = "third-company",key = "#company.corpId")
    public QywxThirdCompany  saveCompany(QywxThirdCompany company){
        QywxThirdCompany companyVo = getCompany(company.getCorpId());
        if(!ObjectUtils.isEmpty(companyVo)){
            // todo 更新company 属性
            companyVo.setPermanentCode(company.getPermanentCode());
            companyVo.setAgentId(company.getAgentId());
            companyVo.setAgentName(company.getAgentName());
            companyVo.setCorpName(company.getCorpName());
            return companyRepository.save(companyVo);
        }else {
            return companyRepository.save(company);
        }
    }

    @CacheEvict(value = "third-company",key = "#company.corpId")
    public QywxThirdCompany  updateCompany(QywxThirdCompany company){
        QywxThirdCompany companyVo = companyRepository.findByCorpId(company.getCorpId());
        if(!ObjectUtils.isEmpty(companyVo)){
            companyVo.setPermanentCode(company.getPermanentCode());
            return companyRepository.save(companyVo);
        }

        return null;
    }

    @Cacheable(value = "corp-access-token",key = "#corpId")
    public String getCorpAccessToken(String corpId){
        QywxThirdCompany company = getCompany(corpId);
        String corpsecret = company.getPermanentCode();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> entity = new HttpEntity(headers);
        String url = String.format(qywxDevProperties.getCorpAccessTokenUrl(),corpId,corpsecret);
        log.info("getCorpAccessTokenUrl:"+url);
        ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(url,HttpMethod.GET, entity,JSONObject.class);
        log.info("getCorpAccessTokenUrl-return:"+responseEntity.getBody().toJSONString());
        if(responseEntity.getStatusCode()== HttpStatus.OK){
            Map responseBody = responseEntity.getBody();
            if(responseBody.containsKey("errcode") && (Integer) responseBody.get("errcode") == 0){
                //access_token
                String accessToken= (String) responseBody.get("access_token");
                return accessToken;
            }
        }

        return null;
    }


}
