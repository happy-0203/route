package cn.com.za.router.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.Map;
import java.util.Set;

import java.util.logging.LogRecord;

import cn.com.za.annotation.model.RouteMeta;
import cn.com.za.router.core.callback.NavigationCallback;
import cn.com.za.router.core.exception.NoRouteFoundException;
import cn.com.za.router.core.template.IRouteGroup;
import cn.com.za.router.core.template.IRouteRoot;
import cn.com.za.router.core.template.IService;
import cn.com.za.router.core.utils.ClassUtils;

public class ZaRouter {


    public static final String PACKAGE_OF_ROOT = "com.za.router.routes";
    private static final String SDK_NAME = "ZaRouter";
    private static final String SEPARATOR = "$$";
    private static final String SUFFIX_ROOT = "Root";
    private static final String TAG = "ZaRouter";
    private static ZaRouter mInstance;
    private Application mContext;

    private Handler mHandler = new Handler(Looper.getMainLooper());


    public static ZaRouter getInstance() {
        if (mInstance == null) {
            synchronized (ZaRouter.class) {
                if (mInstance == null) {
                    mInstance = new ZaRouter();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化
     *
     * @param application
     */
    public void init(Application application) {
        mContext = application;
        try {
            loadInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 分组制作表
     */
    private void loadInfo() throws Exception {

        //获取apt 生成的路由表的全类名
        Set<String> rootNameSet = ClassUtils.getFileNameByPackageName(mContext, PACKAGE_OF_ROOT);

        for (String className : rootNameSet) {
            if (className.startsWith(PACKAGE_OF_ROOT + "." + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
                //将root中注册的分组信息加入仓库
                IRouteRoot iRouteRoot = (IRouteRoot) Class.forName(className).getConstructor().newInstance();
                iRouteRoot.loadInto(Warehouse.groupsIndex);
            }
        }
        Map<String, Class<? extends IRouteGroup>> groupsIndex = Warehouse.groupsIndex;

        //打印映射表
        for (Map.Entry<String, Class<? extends IRouteGroup>> classEntry : groupsIndex.entrySet()) {
            Log.e(TAG, "Root映射表[ " + classEntry.getKey() + " : " + classEntry
                    .getValue() + "]");
        }

    }


    public Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException();
        } else {
            return build(path, extractGroup(path));
        }
    }

    public Postcard build(String path, String group) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new RuntimeException("路由地址无效!");
        } else {
            return new Postcard(path, group);
        }
    }


    /**
     * 获得组别
     *
     * @param path
     * @return
     */
    private String extractGroup(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new RuntimeException(path + " : 不能提取group.");
        }
        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new RuntimeException(path + " : 不能提取group.");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object navigation(Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {

        try {
            prepareCard(postcard);
        } catch (NoRouteFoundException e) {
            //没有找到
            if (null != callback) {
                callback.onLost(postcard);
            }
            return null;
        }
        //找到了路由
        if (null != callback) {
            callback.onFound(postcard);
        }

        switch (postcard.getType()) {
            case ACTIVITY:
                //跳转Activity
                final Context currentContext = null == context ? mContext : context;
                final Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());
                if (postcard.getFlags() != -1){
                    intent.setFlags(postcard.getFlags());
                } else if (!(currentContext instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (requestCode > 0) {
                            ActivityCompat.startActivityForResult((Activity) currentContext,
                                    intent, requestCode, postcard.getOptionsBundle());
                        } else {
                            ActivityCompat.startActivity(currentContext, intent, postcard.getOptionsBundle());
                            //currentContext.startActivity(intent,postcard.getOptionsBundle());
                        }

                        if ((0 != postcard.getEnterAnim() || 0 != postcard.getExitAnim()) &&
                                currentContext instanceof Activity) {
                            //老版本
                            ((Activity) currentContext).overridePendingTransition(postcard
                                            .getEnterAnim()
                                    , postcard.getExitAnim());
                        }
                        //跳转完成
                        if (null != callback) {
                            callback.onArrival(postcard);
                        }
                    }
                });

                break;
            case ISERVICE:
                return postcard.getService();
            default:
                break;
        }
        return null;
    }

    /**
     * 准备卡片
     *
     * @param card
     */
    private void prepareCard(Postcard card) {
        RouteMeta routeMeta = Warehouse.routes.get(card.getPath());
        //还没有准备
        if (routeMeta == null) {
            //从路由分组表中取出路由表
            Class<? extends IRouteGroup> groupMeta = Warehouse.groupsIndex.get(card.getGroup());
            if (groupMeta == null) {
                throw new NoRouteFoundException("没找到对应路由: " + card.getGroup() + " " +
                        card.getPath());
            }
            IRouteGroup iRouteGroup;
            try {
                iRouteGroup = groupMeta.getConstructor().newInstance();
            } catch (Exception e) {
                //路由分组表创建失败
                throw new RuntimeException("路由分组映射表记录失败", e);
            }
            iRouteGroup.loadInto(Warehouse.routes);
            //已经准备过了就可以移除了 (不会一直存在内存中)
            Warehouse.groupsIndex.remove(card.getGroup());
            //再次进入就会走else
            prepareCard(card);
        } else {
            //设置要跳转的类Activity,或者是需要实现的类IService
            card.setDestination(routeMeta.getDestination());
            card.setType(routeMeta.getType());
            switch (routeMeta.getType()) {
                case ISERVICE:
                    Class<?> destination = routeMeta.getDestination();
                    IService service = Warehouse.services.get(destination);
                    if (null == service) {
                        try {
                            //创建Service
                            service = (IService) destination.getConstructor().newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    card.setService(service);
                    break;
                default:
                    break;
            }
        }
    }


}
