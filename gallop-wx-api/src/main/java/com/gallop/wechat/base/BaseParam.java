package com.gallop.wechat.base;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * author gallop
 * date 2021-11-02 10:47
 * Description: 通用基础参数，相关实体参数校验可继承此类
 */
@Data
public class BaseParam implements Serializable {
    private static final long serialVersionUID = 7094973966311300434L;

     /**
      * 页码
      **/
    private Integer page = 1;
     /**
      * 每页条数
      **/
    private Integer pageSize = 10;
    /**
     * 搜索值
     */
    private String searchValue;

    /**
     * 数据权限
     */
    private List<Long> dataScope;

    /**
     * 开始时间
     */
    private String searchBeginTime;

    /**
     * 结束时间
     */
    private String searchEndTime;

    /**
     * 状态（字典 0正常 1停用 2删除）
     */
    private Integer searchStatus;

    /**
     * 参数校验分组：分页
     */
    public @interface page {
    }

    /**
     * 参数校验分组：列表
     */
    public @interface list {
    }

    /**
     * 参数校验分组：下拉
     */
    public @interface dropDown {
    }

    /**
     * 参数校验分组：增加
     */
    public @interface add {
    }

    /**
     * 参数校验分组：编辑
     */
    public @interface edit {
    }

    /**
     * 参数校验分组：更新信息
     */
    public @interface updateInfo {
    }

    /**
     * 参数校验分组：修改密码
     */
    public @interface updatePwd {
    }

    /**
     * 参数校验分组：重置密码
     */
    public @interface resetPwd {
    }

    /**
     * 参数校验分组：修改头像
     */
    public @interface updateAvatar {
    }

    /**
     * 参数校验分组：删除
     */
    public @interface delete {
    }

    /**
     * 参数校验分组：详情
     */
    public @interface detail {
    }

    /**
     * 参数校验分组：授权角色
     */
    public @interface grantRole {
    }

    /**
     * 参数校验分组：修改状态
     */
    public @interface changeStatus {
    }

    /**
     * 参数校验分组：授权菜单
     */
    public @interface grantMenu {
    }
}
