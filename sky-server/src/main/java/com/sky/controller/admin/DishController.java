package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@Api(tags = "菜品相关接口")
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("新增菜品")
    @PostMapping
    public Result addDish(@RequestBody DishDTO dto) {
        log.info("新增菜品:{}", dto);
        dishService.addDish(dto);
        //缓存优化
        redisTemplate.delete("dish_" + dto.getCategoryId());
        return Result.success();
    }

    @ApiOperation("分页查询菜品列表")
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dto){
        log.info("分页查询菜品列表:{}",dto);
        PageResult pageResult = dishService.page(dto);
        return Result.success(pageResult);
    }

    @ApiOperation("删除菜品")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除菜品：{}",ids);
        dishService.delete(ids);

        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        return Result.success();
    }

    @ApiOperation("根据id查询菜品")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id){
        log.info("回显菜品:{}",id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }

    @ApiOperation("更新菜品信息")
    @PutMapping
    public Result update(@RequestBody DishDTO dto){
        log.info("更新菜品:{}",dto);
        dishService.update(dto);
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("菜品启售停售")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("菜品启售停售：{}，{}", status, id);
        dishService.startOrStop(status, id);

        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品：{}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

}
