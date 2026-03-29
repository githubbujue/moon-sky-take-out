package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface SetmealMapper {

    Integer countByCategoryId(Long id);
}
