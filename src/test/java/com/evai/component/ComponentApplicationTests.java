package com.evai.component;

import com.evai.component.cache.CacheProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ComponentApplicationTests {

    @Autowired
    CacheProperties cacheProperties;

    @Test
    public void contextLoads() {
        System.out.println("====================================");
        System.out.println(cacheProperties);
    }

}
