package com.study.cache.redis.controller;

import io.lettuce.core.RedisClient;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 接收消息
@Component
public class WebsocketChatHandler extends TextWebSocketHandler {

    public static Map<String, List<WebSocketSession>> roomUserMap = new ConcurrentHashMap<>();

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 进入房间 -- 连接建立
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("新用户来了：" + session);
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();// /redis-study-03/ws/chat?userId=12001
        String roomId = uriComponents.getQueryParams().getFirst("roomId");
        String userId = uriComponents.getQueryParams().getFirst("userId");
        String score = uriComponents.getQueryParams().getFirst("score");

        // session保存本地
        List<WebSocketSession> roomUsers = roomUserMap.get(roomId);
        if (roomUsers == null) { // 本服务器该房间，第一个人进来，初始化一个list // TODO 注意此处 线程不安全，没有考虑并发
            roomUsers = new ArrayList<>();
            roomUserMap.put(roomId, roomUsers);
        }
        roomUsers.add(session);

        // 新增 -- 保存 redis
        redisTemplate.opsForZSet().add("roominfo::" + roomId, userId, Double.valueOf(score));

    }

    /**
     * 退出房间 -- 连接建立
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("用户走了：" + session);
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();// /redis-study-03/ws/chat?userId=12001
        String roomId = uriComponents.getQueryParams().getFirst("roomId");
        String userId = uriComponents.getQueryParams().getFirst("userId");
        String score = uriComponents.getQueryParams().getFirst("score");
        redisTemplate.opsForZSet().remove("roominfo::" + roomId, userId);
        roomUserMap.get(roomId).remove(session);
    }

    /**
     * 收到消息
     *
     * @param session
     * @param message
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        // 接收客户端发过来的消息
        System.out.println("收到消息：" + session + ">>" + message);
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();// /redis-study-03/ws/chat?userId=12001
        String roomId = uriComponents.getQueryParams().getFirst("roomId");
        String userId = uriComponents.getQueryParams().getFirst("userId");
        // TODO 保存到Redis 形式？ streams
        RedisClient redisClient = RedisClient.create("redis://192.168.97.101:6379"); // 每一次都创建
        StatefulRedisConnection<String, String> connect = redisClient.connect(); // 肯定会用连接池
        RedisCommands<String, String> redisSyncCommands = connect.sync();
        redisSyncCommands.xadd("room:msg:" + roomId, "roomId", roomId, "userId", userId, "content", message.getPayload());
    }

    // 启动系统后，执行
    @PostConstruct // 初始化触发，spring默认是单实例，调用一次
    public void init() {
        new Thread(() -> {
            RedisClient redisClient = RedisClient.create("redis://192.168.97.101:6379");
            StatefulRedisConnection<String, String> connect = redisClient.connect();
            RedisCommands<String, String> redisSyncCommands = connect.sync();
            // 指定读取消息，哪些消息 5 -- 不同的房间
            while (true) {
                try {
                    // 订阅当前服务器上面用户对应 --- 所有房间
                    Set<String> strings = roomUserMap.keySet();
                    ArrayList<XReadArgs.StreamOffset<String>> streamOffsets = new ArrayList<>();
                    for (String roomId : strings) {
                        streamOffsets.add(XReadArgs.StreamOffset.latest("room:msg:" + roomId));
                    }
                    if (streamOffsets.size() > 0) {
                        List<StreamMessage<String, String>> stream_sms_send = redisSyncCommands.xread(XReadArgs.Builder.block(1000), streamOffsets.toArray(new XReadArgs.StreamOffset[]{}));
                        for (StreamMessage<String, String> stringStringStreamMessage : stream_sms_send) {
                            recvRedisMessage(stringStringStreamMessage);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    // 推送消息（其他人发的消息，推送到直播间）
    // 触发，如果redis里面有数据，则触发执行
    public void recvRedisMessage(StreamMessage<String, String> data) throws IOException {
        // StreamMessage[room:msg:1001:1568109044849-0]{roomId=1001,userId=tony, content=美女真漂亮}
        // 推送这条消息 --
        // 1. 获取roomId
        // 2. 根据房间找session

        String roomId = data.getBody().get("roomId");
        String content = data.getBody().get("content");
        String userId = data.getBody().get("userId");
        String message = userId + "说：" + content;

        List<WebSocketSession> webSocketSessions = roomUserMap.get(roomId);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            webSocketSession.sendMessage(new TextMessage(message));
        }
    }
}
