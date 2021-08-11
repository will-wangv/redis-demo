package com.study.cache.redis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

// mvc
@RestController
public class ChatController {


    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 返回土豪列表/人数
     */
    @RequestMapping("/chat/roomInfo")
    public Object roomInfo(String roomId) {
        Long count = redisTemplate.opsForZSet().zCard("roominfo::" + roomId);
        System.out.println("房间号：" + roomId + ", 当前人数：" + count);
        // 查询 等级排序之后 最靠前的 两个用户 //
        Set set = redisTemplate.opsForZSet().reverseRangeByScore("roominfo::" + roomId, 5, 10, 0, 2);
        for (Object o : set) {
            System.out.println(o.toString());
        }
        return "sum: " + count;
    }

}
