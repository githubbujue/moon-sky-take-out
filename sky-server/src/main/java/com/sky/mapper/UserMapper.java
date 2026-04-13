package com.sky.mapper;

import com.sky.entity.User;

public interface UserMapper {
    User selectByOpenId(String openid);

    void insert(User user);
}
