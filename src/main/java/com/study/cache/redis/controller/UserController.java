package com.study.cache.redis.controller;

import com.study.cache.redis.pojo.User;
import com.study.cache.redis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    /**
     * 查看所有信息
     */
    @RequestMapping("/findUserById")
    public User findUserById(String userId) throws Exception {
        return userService.findUserById(userId);
    }

    /**
     * 查看姓名
     */
    @RequestMapping("/findUserNameById")
    public String findUserNameById(String userId) {
        return userService.findUserNameById(userId);
    }

}
