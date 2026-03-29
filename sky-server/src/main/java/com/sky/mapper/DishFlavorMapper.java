package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


public interface DishFlavorMapper {

    void insertBatch(List<DishFlavor> dishFlavorList);

    void deleteBatch(List<Long> DishIds);

    List<DishFlavor> selectByDishId(Long disId);
}
