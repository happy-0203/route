package cn.com.za.base;

import android.app.Application;
import android.content.Context;

public class BaseApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getApplication() {
        return mContext;
    }
}
