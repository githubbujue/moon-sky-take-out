package com.sky.controller.admin;


import com.sky.dto.UserLoginDTO;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "店铺相关接口")
@RequestMapping("/admin/shop")
@RestController("adminShopController")
public class ShopController {
    private static final String KEY = "SHOP_STATUS";
    private final RedisTemplate redisTemplate;

    public ShopController(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PutMapping("/{status}")
    @ApiOperation("设置店铺状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺状态为:{}", status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取的店铺状态为:{}", status == 1 ? "营业中": "打烊中");
        return Result.success(status);
    }
}
