package com.sky.controller.user;

import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "User Shop APIs")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/status")
    @ApiOperation("Get shop status")
    public Result<Integer> getStatus() {
        Integer status = getShopStatus();
        log.info("Get user shop status: {}", status);
        return Result.success(status);
    }

    private Integer getShopStatus() {
        Object status = redisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS);
        if (status == null) {
            return StatusConstant.ENABLE;
        }
        if (status instanceof Number) {
            return ((Number) status).intValue();
        }
        return Integer.valueOf(status.toString());
    }
}
