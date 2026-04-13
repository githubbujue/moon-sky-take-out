package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(UserLoginDTO dto) {
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("appid",weChatProperties.getAppid());
        paramMap.put("secret",weChatProperties.getSecret());
        paramMap.put("js_code",dto.getCode());
        paramMap.put("grant_type","authorization_code");
        //发送请求
        String res = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session",paramMap);

        //解析响应结果
        JSONObject jsonObject = JSON.parseObject(res);
        String openid = jsonObject.get("openid").toString();
        if (openid==null || openid.isEmpty()){
            throw new LoginFailedException(MessageConstant.USER_NOT_LOGIN);
        }

        //判断是否新用户对此进行处理
        User user = userMapper.selectByOpenId(openid);
        if (user==null){
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
            user.setName(openid.substring(0,5));
            userMapper.insert(user);
        }

        return user;
    }
}
