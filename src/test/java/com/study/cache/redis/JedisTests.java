package com.study.cache.redis;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisTests {
    @Test
    public void test0() {
        // java客户端示例。 jedis初学者友好，操作和控制台类似
        Jedis jedis = new Jedis("192.168.97.101", 6379);
        String result = jedis.get("hello"); // get key
        System.out.println(result);

        // pool
        JedisPool jedisPool = new JedisPool();

        // controller -- 用户关注
        // 更新某个用户的粉丝数量
        // u10001 ---> 1000
        jedis.incr("u10001"); // incr
    }
}
