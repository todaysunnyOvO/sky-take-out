package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 批量插入套餐菜品关系
     *
     * @param setmealDishes
     */
    void insertBatch(@Param("setmealDishes") List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id批量删除套餐菜品关系
     *
     * @param setmealIds
     */
    void deleteBySetmealIds(@Param("setmealIds") List<Long> setmealIds);

    /**
     * 根据套餐id查询套餐菜品关系
     *
     * @param setmealId
     * @return
     */
    List<SetmealDish> getBySetmealId(Long setmealId);

    /**
     * 根据套餐id和菜品状态查询菜品数量
     *
     * @param setmealIds
     * @param status
     * @return
     */
    Integer countBySetmealIdsAndDishStatus(@Param("setmealIds") List<Long> setmealIds, @Param("status") Integer status);
}
