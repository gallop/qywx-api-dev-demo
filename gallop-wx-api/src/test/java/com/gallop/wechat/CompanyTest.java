package com.gallop.wechat;

import com.gallop.wechat.entity.QywxThirdCompany;
import com.gallop.wechat.service.WxAssistDevServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * author gallop
 * date 2022-03-26 19:56
 * Description:
 * Modified By:
 */
@SpringBootTest
public class CompanyTest {
    @Resource
    private WxAssistDevServiceImpl service;

    @Test
    public void saveCompanyTest(){
        QywxThirdCompany company = new QywxThirdCompany();
        company.setCorpId("aaa123456");
        company.setCorpName("测试企业");
        company.setAgentId(12345);
        company.setAgentName("健康监测系统");
        company.setSubjectType(2);
        company.setPermanentCode("abd");
        company.setStatus(1);

        service.saveCompany(company);
    }

    @Test
    public void getCompanyTest(){
        String corpId = "aaa123456";
        QywxThirdCompany company = service.getCompany(corpId);
        System.out.println("company:"+company.toString());
    }
    @Test
    public void updateCompanyTest(){
        QywxThirdCompany company = new QywxThirdCompany();
        company.setCorpId("aaa123456");
        company.setPermanentCode("abcdefgh");

        service.updateCompany(company);

    }
}
