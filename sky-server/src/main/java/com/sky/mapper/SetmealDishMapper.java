package com.sky.mapper;

import com.sky.entity.SetmealDish;

import java.util.List;

public interface SetmealDishMapper {
    Integer countByDishId(List<Long> dishIds);

    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBySetmealIds(List<Long> setmealIds);

    List<SetmealDish> getBySetmealId(Long id);

    void deleteBySetmealId(Long setmealId);
}
