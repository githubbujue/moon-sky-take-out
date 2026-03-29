package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void addDish(DishDTO dot);

    PageResult page(DishPageQueryDTO dto);

    void delete(List<Long> ids);

    DishVO getById(Long id);

    void update(DishDTO dto);
}
