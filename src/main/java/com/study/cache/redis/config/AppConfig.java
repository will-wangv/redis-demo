package com.study.cache.redis.config;

import com.study.cache.redis.controller.WebsocketChatHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

// 用这个类 代替了xml文件
@Configuration //
@EnableWebSocket
public class AppConfig {

    @Value("${spring.redis.host}") // springel
    private String host;

    @Value("${spring.redis.port}")
    int port;

    // 创建对象，spring托管 <bean ...
    @Bean
    public JedisPool jedisPool() {
        JedisPool jedisPool = new JedisPool(host, port);
        return jedisPool;
    }

    // TODO 待完善
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(6379);
        RedisConnectionFactory redisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration);
        return redisConnectionFactory;
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);//
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8); // key -- string
        // redisTemplate.setValueSerializer();
        return redisTemplate;
    }

    // websocket拦截
    @Bean
    public WebSocketConfigurer webSocketConfigurer(WebsocketChatHandler websocketChatHandler) {
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
                registry.addHandler(websocketChatHandler, "/ws/chat").setAllowedOrigins("*");
            }
        };
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        CacheManager cacheManager = new RedisCacheManager(redisCacheWriter, redisCacheConfiguration);
        return cacheManager;
    }
}
