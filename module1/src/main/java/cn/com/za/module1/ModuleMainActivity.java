package cn.com.za.module1;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import cn.com.za.annotation.Route;
import cn.com.za.base.BaseService;
import cn.com.za.router.core.Postcard;
import cn.com.za.router.core.ZaRouter;

@Route(path = "/module1/test")
public class ModuleMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_main);

        Bundle bundle = getIntent().getExtras();
        String msg = bundle.getString("msg");


        TextView tv = findViewById(R.id.tv);
        tv.setText(msg);
    }

    public void showToast(View view) {

        BaseService baseService = (BaseService) ZaRouter.getInstance().build("/module2/service").navigation(this);
        baseService.test();
    }
}
