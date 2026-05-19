package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "User Dish APIs")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        String key = RedisConstant.DISH_CACHE_PREFIX + categoryId;
        Object cache = redisTemplate.opsForValue().get(key);
        if (cache instanceof String) {
            log.info("从Redis缓存中查询菜品数据: {}", key);
            List<DishVO> dishVOList = JSON.parseArray((String) cache, DishVO.class);
            return Result.success(dishVOList);
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);

        List<DishVO> dishVOList = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(dishVOList));
        log.info("菜品数据写入Redis缓存: {}", key);

        return Result.success(dishVOList);
    }
}
