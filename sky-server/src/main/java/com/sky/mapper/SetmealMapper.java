package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐数量
     *
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 根据菜品id查询关联套餐数量
     *
     * @param dishIds
     * @return
     */
    @Select("<script>" +
            "select count(id) from setmeal_dish where dish_id in " +
            "<foreach collection='dishIds' item='dishId' open='(' separator=',' close=')'>" +
            "#{dishId}" +
            "</foreach>" +
            "</script>")
    Integer countByDishIds(@Param("dishIds") List<Long> dishIds);

}
