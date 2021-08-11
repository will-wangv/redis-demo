package com.study.cache.redis;

import io.lettuce.core.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

public class StreamTests {
    // stream 流，5.0新特性，redisTemplate、jedis还没有完善的支持,Redisson和Lettuce支持了
    // 我们使用springboot中默认的redis客户端Lettuce
    // 添加： XADD mystream * sensor-id 1234 temperature 19.8
    // 遍历： XRANGE mystream - + COUNT 2
    // 消费：XREAD COUNT 2 STREAMS mystream 0
    // 阻塞式消费： XREAD BLOCK 0 STREAMS mystream $
    // 创建消费者组：   XGROUP CREATE mystream mygroup $
    // 分组消费： XREADGROUP GROUP mygroup Alice COUNT 1 STREAMS mystream >
    // 消费确认： XACK mystream mygroup 1526569495631-0
    // 查看未确认的消息： XPENDING mystream mygroup - + 10
    // 重新认领消费：XCLAIM mystream mygroup Alice 3600000 1526569498055-0
    // XINFO 查看stream信息，监控
    @Test
    public void producer() throws InterruptedException {
        RedisClient redisClient = RedisClient.create("redis://192.168.97.101:6379");
        StatefulRedisConnection<String, String> connect = redisClient.connect();
        RedisCommands<String, String> redisSyncCommands = connect.sync();
        redisSyncCommands.xadd("room:msg:1001", "userId", "tony", "content", "美女真漂亮");
    }

    // 普通消费 -- 连接创建之后的消息
    @Test
    public void consumer() {
        RedisClient redisClient = RedisClient.create("redis://192.168.97.101:6379");
        StatefulRedisConnection<String, String> connect = redisClient.connect();
        RedisCommands<String, String> redisSyncCommands = connect.sync();
        // 指定读取消息，哪些消息
        List<StreamMessage<String, String>> stream_sms_send = redisSyncCommands.xread(XReadArgs.Builder.block(10000), XReadArgs.StreamOffset.latest("room:msg:1001"));
        for (StreamMessage<String, String> stringStringStreamMessage : stream_sms_send) {
            System.out.println(stringStringStreamMessage);
        }
    }

    // 分组消费 类似kafka的group概念。 如果集群就要用这个了。
    @Test
    public void createGroup() {
        RedisClient redisClient = RedisClient.create("redis://192.168.97.101:6379");
        StatefulRedisConnection<String, String> connect = redisClient.connect();
        RedisCommands<String, String> redisSyncCommands = connect.sync();
        // 创建分组
        redisSyncCommands.xgroupCreate(XReadArgs.StreamOffset.from("room:1001", "0"), "group_1");
    }

    @Test
    public void consumerGroup() {
        RedisClient redisClient = RedisClient.create("redis://192.168.97.101:6379");
        StatefulRedisConnection<String, String> connect = redisClient.connect();
        RedisCommands<String, String> redisSyncCommands = connect.sync();
        // 按组消费
        List<StreamMessage<String, String>> xreadgroup = redisSyncCommands.xreadgroup(Consumer.from("group_1", "consumer_1"), XReadArgs.StreamOffset.lastConsumed("room:1001"));
        for (StreamMessage<String, String> message : xreadgroup) {
            System.out.println(message);
            // 告知redis，消息已经完成了消费
            redisSyncCommands.xack("room:1001", "group_1", message.getId());
        }
    }
}
