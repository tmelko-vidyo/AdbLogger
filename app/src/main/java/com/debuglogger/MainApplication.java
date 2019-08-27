package com.debuglogger;

import android.app.Application;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /* Initial log collection inti. Once per lifecycle */
        LogCollector.getInstance().init(getApplicationContext());
    }
}