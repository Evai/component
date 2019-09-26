package com.evai.component;

import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.CacheAction;
import com.evai.component.entity.User;
import com.evai.component.service.UserService;
import com.evai.component.utils.SleepUtil;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author crh
 * @date 2019-09-20
 * @description
 */
@RestController
@RequestMapping("/test")
@AllArgsConstructor
public class TestController {

    private final UserService userService;

    @RequestMapping("/cache/id")
    @CacheAbleEntity(action = CacheAction.SELECT, keyId = "#id", keyNameClass = User.class)
    public User cache(Long id) {
        SleepUtil.seconds(1);
        return userService.getOneById(id);
    }

    @RequestMapping("/cache/select")
    public User cache(String username) {
        return userService.cacheGetOneByUsername(username);
    }

    @RequestMapping("/cache/update")
    @CacheAbleEntity(action = CacheAction.DEL, keyId = "#user.id", keyNameClass = User.class)
    public void update(User user) {
        userService.updateById(user);
    }

    @RequestMapping("/cache/insert")
    @CacheAbleEntity(action = CacheAction.INSERT_AUTO, keyId = "#user.id", keyNameClass = User.class)
    public void insert(User user) {
        userService.insert(user);
    }

    @RequestMapping("/cache/list")
    public List<User> cache() {
        return userService.getList();
    }


}
