package com.sky.mapper;

import java.util.List;

public interface SetmealDishMapper {
    Integer countByDishId(List<Long> dishIds);
}
