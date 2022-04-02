package com.gallop.wechat.sso;

import com.gallop.wechat.base.BaseResult;
import com.gallop.wechat.base.BaseResultUtil;
import com.gallop.wechat.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * author gallop
 * date 2021-06-17 16:40
 * Description:
 * Modified By:
 */

public abstract class SsoServiceBasic implements SsoService{
    @Autowired
    RedisTemplate redisTemplate;



    @Override
    public BaseResult returnspot(User user, HttpServletRequest request, HttpServletResponse response) {
        Map returnMap = new HashMap();
        returnMap.put("user", user);
        String uuid = UUID.randomUUID().toString();
        returnMap.put("loginToken", uuid);

        return BaseResultUtil.success(returnMap);
    }

}
