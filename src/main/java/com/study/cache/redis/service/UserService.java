package com.study.cache.redis.service;

import com.study.cache.redis.annotations.RedisCache;
import com.study.cache.redis.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service // 默认 单实例
public class UserService {

    @Autowired
    JdbcTemplate jdbcTemplate; // spring提供jdbc一个工具（mybastis类似）

    @Autowired
    RedisTemplate redisTemplate;

    // 生产环境：第一次初始化，后续，根据数据变化，自动维护
    @PostConstruct // 演示需要 --- 启动的时候去初始化布隆过滤器。
    public void init() {
        // 1. 加载所有数据 TODO 演示
        for (int i = 0; i < 1; i++) {
            String userId = "10001";
            int hashValue = Math.abs(userId.hashCode());
            long index = (long) (hashValue % Math.pow(2, 32)); // 元素  和 数组的映射
            // 设置Redis里面二进制数据中的值，对应位置 为 1
            redisTemplate.opsForValue().setBit("user_bloom_filter", index, true);
        }
    }

    /**
     * 根据ID查询用户信息 (redis缓存，用户信息以json字符串格式存在(序列化))
     */
    // @Cacheable(value="user", key = "#userId")// 返回值 存到redis： value -- key: user::10001
    public User findUserById(String userId) throws Exception {
        // 提前查询
        int hashValue = Math.abs(userId.hashCode());
        long index = (long) (hashValue % Math.pow(2, 32)); // 元素  和 数组的映射
        Boolean result = redisTemplate.opsForValue().getBit("user_bloom_filter", index);
        if (!result) {
            System.out.println("数据不存在" + userId);
            return null;
        }

        // 压根就不存在
        // 1. 先读取缓存
        Object cacheValue = redisTemplate.opsForValue().get(userId);
        if (cacheValue != null) {
            System.out.println("###从缓存读取数据");
            return (User) cacheValue;
        }
        // 2. 没有缓存读取数据 --- 连接，并发 支撑非常小 ---
        String sql = "select * from tb_user_base where uid=?";
        User user = jdbcTemplate.queryForObject(sql, new String[]{userId}, new BeanPropertyRowMapper<>(User.class));
        System.out.println("***从数据库读取数据");
        // 3. 设置缓存
        redisTemplate.opsForValue().set(userId, user);
        return user;
    }

    @CacheEvict(value = "user", key = "#user.uid") // 方法执行结束，清除缓存
    public void updateUser(User user) {
        String sql = "update tb_user_base set uname = ? where uid=?";
        jdbcTemplate.update(sql, new String[]{user.getUname(), user.getUid()});
    }

    /**
     * 根据ID查询用户名称
     */
    // 我自己实现一个类似的注解
    @RedisCache(value = "uname", key = "#userId") // 缓存
    public String findUserNameById(String userId) {
        // 查询数据库
        String sql = "select uname from tb_user_base where uid=?";
        String uname = jdbcTemplate.queryForObject(sql, new String[]{userId}, String.class);

        return uname;
    }
}
