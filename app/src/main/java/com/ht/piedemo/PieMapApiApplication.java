package com.ht.piedemo;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.location.LocationManager;
import android.os.Vibrator;
import android.util.Log;


import com.ht.piedemo.service.LocationService;

import pie.core.Workspace;

/**
 * Created by 13468 on 2017/11/8.
 */

public class PieMapApiApplication extends Application {

    public static Workspace mWorkspace; // 全局的Workspace 最好整个应用只初始化一次
    public static LocationManager locationManager;

    public LocationService locationService;
    public Vibrator mVibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkspace = null;
        locationManager = ((LocationManager)getSystemService(Context.LOCATION_SERVICE));

        locationService = new LocationService(getApplicationContext());
        mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);

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
