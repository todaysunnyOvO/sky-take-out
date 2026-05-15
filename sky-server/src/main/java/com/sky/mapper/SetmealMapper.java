package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
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
     * 新增套餐
     *
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据套餐id和状态查询套餐数量
     *
     * @param ids
     * @param status
     * @return
     */
    Integer countByIdsAndStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 根据id批量删除套餐
     *
     * @param ids
     */
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    SetmealVO getById(Long id);

    /**
     * 动态修改套餐
     *
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

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
