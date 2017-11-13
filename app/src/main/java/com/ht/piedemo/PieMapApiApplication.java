package com.ht.piedemo;

import android.app.Application;
import android.content.Context;
import android.location.LocationManager;
import android.util.Log;

import pie.core.Workspace;

/**
 * Created by 13468 on 2017/11/8.
 */

public class PieMapApiApplication extends Application {

    public static Workspace mWorkspace; // 全局的Workspace 最好整个应用只初始化一次
    public static LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkspace = null;
        locationManager = ((LocationManager)getSystemService(Context.LOCATION_SERVICE));
    }

    @Override
    public void onTerminate() {
        Log.i("zrc", "onTerminate");
        if (PieMapApiApplication.mWorkspace != null) {
            PieMapApiApplication.mWorkspace.close();
            PieMapApiApplication.mWorkspace = null;
        }
        Log.i("zrc", "onDestroy");
        super.onTerminate();
    }
}
