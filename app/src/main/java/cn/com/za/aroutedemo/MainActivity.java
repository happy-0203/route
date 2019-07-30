package cn.com.za.aroutedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import cn.com.za.annotation.Route;
import cn.com.za.router.core.ZaRouter;

@Route(path = "/main/home")
public class MainActivity extends AppCompatActivity {

    private Button mBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn = findViewById(R.id.btn);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZaRouter.getInstance().build("/module1/test").withString("msg",
                        "从MainActivity").navigation(MainActivity.this);
            }
        });
    }

}
