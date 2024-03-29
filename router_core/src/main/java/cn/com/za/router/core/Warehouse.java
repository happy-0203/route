package cn.com.za.router.core;


import java.util.HashMap;
import java.util.Map;

import cn.com.za.annotation.model.RouteMeta;
import cn.com.za.router.core.template.IRouteGroup;
import cn.com.za.router.core.template.IService;

/**
 * @author Lance
 * @date 2018/2/22
 */

public class Warehouse {

    // root 映射表 保存分组信息
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();

    // group 映射表 保存组中的所有数据
    static Map<String, RouteMeta> routes = new HashMap<>();

    // group 映射表 保存组中的所有数据
    static Map<Class, IService> services = new HashMap<>();
    // TestServiceImpl.class , TestServiceImpl 没有再反射


}
