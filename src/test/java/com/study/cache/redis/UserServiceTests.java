package com.study.cache.redis;

import com.study.cache.redis.pojo.User;
import com.study.cache.redis.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = {"classpath:applicationContext.xml"})
@WebAppConfiguration("src/main/resources")
public class UserServiceTests {
    @Autowired
    UserService userService;

    @Test
    public void test0() throws Exception {
        User user = userService.findUserById("10001");
        System.out.println(user.getUname());
    }

    // 模拟2000并发场景
    @Test
    public void benchMark() throws InterruptedException, IOException {
        // J.U.C 并发编程 --- 栅栏机制
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2000);
        for (int i = 0; i < 2000; i++) {
            String id = i + "_null"; // 生成一堆必然不存在的数据ID
            new Thread(() -> {
                try {
                    cyclicBarrier.await(); // 等待，栅栏拦住线程，等待栅栏开启，2000线程执行到这段代码
                    User user = userService.findUserById(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        System.in.read(); // 不关闭程序
    }


}
