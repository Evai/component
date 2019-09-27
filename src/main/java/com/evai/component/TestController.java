package com.evai.component;

import com.evai.component.cache.annotation.CacheAble;
import com.evai.component.cache.annotation.CacheAbleConfig;
import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.CacheAction;
import com.evai.component.entity.User;
import com.evai.component.service.UserService;
import com.evai.component.utils.SleepUtil;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author crh
 * @date 2019-09-20
 * @description
 */
@RestController
@RequestMapping("/test")
@AllArgsConstructor
@CacheAbleConfig(keyNameClass = User.class)
public class TestController {

    private final UserService userService;

    @RequestMapping("/cache/id")
    @CacheAble(keyId = "#id")
    public User cache(Long id) {
        SleepUtil.seconds(1);
        return userService.getOneById(id);
    }

    @RequestMapping("/cache/select")
    public User cache(String username) {
        return userService.cacheGetOneByUsername(username);
    }

    @RequestMapping("/cache/update")
    @CacheAbleEntity(action = CacheAction.DEL, keyId = "#user.id")
    public void update(User user) {
        userService.updateById(user);
    }

    @RequestMapping("/cache/insert")
    @CacheAbleEntity(action = CacheAction.INSERT_AUTO, keyId = "#user.id")
    public void insert(User user) {
        userService.insert(user);
    }

    @RequestMapping("/cache/list")
    public List<User> cache() {
        return userService.getList();
    }

    @RequestMapping("/cache/map")
    @CacheAble(keyId = "#id")
    public Map<Long, User> map(Long id) {
        SleepUtil.seconds(1);
        Map<Long, User> map = Maps.newHashMap();
        map.put(id, userService.getOneById(id));
        return map;
    }


}
