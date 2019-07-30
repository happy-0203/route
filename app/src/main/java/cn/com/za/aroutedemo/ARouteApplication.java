package cn.com.za.aroutedemo;

import android.app.Application;
import android.content.Context;

import cn.com.za.base.BaseApplication;
import cn.com.za.router.core.ZaRouter;

public class ARouteApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化路由
        ZaRouter.getInstance().init(this);
    }


}
