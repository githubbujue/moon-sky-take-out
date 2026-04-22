package com.sky.service.impl;

import ch.qos.logback.core.status.Status;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private Properties pageHelperProperties;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Transactional
    @Override
    public void addDish(DishDTO dto) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto,dish);
        dishMapper.insert(dish);

        List<DishFlavor> dishFlavorList = dto.getFlavors();
        if (dishFlavorList != null && !dishFlavorList.isEmpty()) {
            dishFlavorList.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(dishFlavorList);
        }
    }

    @Override
    public PageResult page(DishPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(),dto.getPageSize());

        Page<DishVO> page =  dishMapper.list(dto);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Transactional
    @Override
    public void delete(List<Long> ids) {

        ids.forEach(dishId -> {
            Dish dish = dishMapper.selectId(dishId);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });

        Integer count = setmealDishMapper.countByDishId(ids);
        if(count>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        dishMapper.deleteBatch(ids);

        dishFlavorMapper.deleteBatch(ids);

    }

    @Override
    public DishVO getById(Long id) {
        DishVO dishVO = new DishVO();

        Dish dish = dishMapper.selectId(id);
        BeanUtils.copyProperties(dish,dishVO);

        List<DishFlavor> dishFlavorList = dishFlavorMapper.selectByDishId(id);
        dishVO.setFlavors(dishFlavorList);

        return dishVO;
    }

    @Transactional
    @Override
    public void update(DishDTO dto) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto,dish);
        dishMapper.update(dish);

        dishFlavorMapper.deleteByDishId(dto.getId());

        List<DishFlavor> flavors = dto.getFlavors();
        if(flavors!=null && !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        };

    }

    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        dishMapper.update(dish);

        if (status.equals(StatusConstant.DISABLE)) {
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);

            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && !setmealIds.isEmpty()) {
                setmealMapper.startOrStopBatch(setmealIds, StatusConstant.DISABLE);
            }
        }
    }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.listByCategoryId(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.listBy(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
