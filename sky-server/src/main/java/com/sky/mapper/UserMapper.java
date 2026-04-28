package com.sky.mapper;

import com.sky.entity.User;

import java.util.Map;

public interface UserMapper {
    User selectByOpenId(String openid);

    void insert(User user);

    User getById(Long userId);

    Integer countByMap(Map map);
}
