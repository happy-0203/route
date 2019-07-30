package cn.com.za.module2;

import android.widget.Toast;

import cn.com.za.annotation.Route;
import cn.com.za.base.BaseApplication;
import cn.com.za.base.BaseService;

@Route(path = "/module2/service")
public class TestService implements BaseService {
    @Override
    public void test() {
        Toast.makeText(BaseApplication.getApplication(),
                "我是模块2的Toast", Toast.LENGTH_SHORT).show();
    }
}
