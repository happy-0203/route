package cn.com.za.aroutedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import cn.com.za.annotation.Route;

@Route(path = "/main/second")
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }
}
