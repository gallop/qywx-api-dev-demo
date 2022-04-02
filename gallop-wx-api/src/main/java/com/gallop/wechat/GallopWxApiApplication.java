package com.gallop.wechat;

import com.gallop.wechat.config.RedisConfig;
import com.gallop.wechat.entity.QywxThirdCompany;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

@SpringBootApplication
public class GallopWxApiApplication {
    @Autowired
    private RedisTemplate redisTemplate;

    public static void main(String[] args) {
        SpringApplication.run(GallopWxApiApplication.class, args);
    }

    //@Bean
    public ApplicationRunner runner() {
        return args -> {
            QywxThirdCompany company = new QywxThirdCompany();
            company.setCorpId("abcdefg123");
            company.setCorpName("企业测试号");
            company.setAgentId(10007);
            company.setAgentName("项目质量管理测试四系统");

            //redis 队列发送消息
            redisTemplate.convertAndSend(RedisConfig.AUTH_CODE_TOPIC,company);
            System.err.println("Publisher authCode-queue Topic... ");
        };
    }

}
