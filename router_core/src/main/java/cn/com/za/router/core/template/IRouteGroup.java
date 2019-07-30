package cn.com.za.router.core.template;


import java.util.Map;

import cn.com.za.annotation.model.RouteMeta;

public interface IRouteGroup {

    void loadInto(Map<String, RouteMeta> atlas);
}
