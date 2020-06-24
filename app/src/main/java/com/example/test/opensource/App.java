package com.example.test.opensource;

import android.app.Application;

import yourpck.test.TestKt;


/**
 * Created by luojinlongjjj on 2019/5/8
 **/
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TestKt.INSTANCE.printf();
    }


}
