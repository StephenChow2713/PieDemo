package com.ht.piedemo;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ht.piedemo.constants.PathConstant;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;

import pie.core.DimensionMode;
import pie.core.GisNative;
import pie.core.MapView;
import pie.core.Point2D;
import pie.core.SceneMode;
import pie.core.Workspace;
import pie.map.MapViews;
import pie.map.gesture.MapGestureController;
import pie.map.overlay.OverlayManager;
import pie.map.overlay.options.OverlayCircleOptions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String mapResourcePath;
    private SharedPreferences mPreferences;
    private static final String MAP_RESOURCE_NAME = "piesdk.zip";
    private String mapPath;
    private MapViews mMapViews; // 地图容器类，包含地图，建议使用
    private MapView mBasicMapView;// 地图View，可以从MapViews中获取
    private Button button, button2, button3, button4;
    private boolean isSwitch = true;

    private OverlayManager overlayManager;
    private OverlayCircleOptions overlayCircleOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        boolean needCopy = mPreferences.getBoolean("needCopyMapRes", true);
        if (needCopy) {
            new MapResourceAsyncTask().execute();
        }
        initGisNative();
        setContentView(R.layout.activity_main);
        initView();
        setMapView();
        openWorkspace();
        openMap("googlemap");

        overlayManager = new OverlayManager(mBasicMapView);
        overlayCircleOptions = new OverlayCircleOptions();
    }

    private void initData() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            mapResourcePath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/PIE/sdk";
        } else {
            Toast.makeText(this, "SD卡不可用，请确保SD卡可用", Toast.LENGTH_SHORT).show();
            finish();
        }
        mPreferences = getPreferences(Context.MODE_PRIVATE);
    }

    /**
     * 初始地图所需资源的路径
     * 确保地图资源已存在
     * 注意：初始化一定要在初始化MapView之前
     */
    private void initGisNative() {
        // 路径确保存在，确保地图资源存在
        mapPath = PathConstant.PATH_PIE_MAP_RES_DEFAULT;
        GisNative.init(this, mapPath);
    }

    private void initView() {
        mMapViews = (MapViews) findViewById(R.id.mvs_pie_basic_map);
        // 从地图容器中获取MapView
        mBasicMapView = mMapViews.getMapView();
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(this);
        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(this);
        button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(this);
    }

    private void setMapView() {
        DisplayMetrics metric = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int densityDpi = metric.densityDpi;
        // 设置设备分辨率，使地图更好的适应设备
        mBasicMapView.setDeviceDPI(densityDpi);
        // 设置地图的显示维度(二维或者三维模式,在打开地图之前设置)。
		mBasicMapView.setDimensionMode(DimensionMode.D3DMode);
//        mBasicMapView.setDimensionMode(DimensionMode.D2DMode);
    }

    /**
     * 打开工作区间，建议只打开一次
     */
    private void openWorkspace() {
        if (PieMapApiApplication.mWorkspace != null) {
            return;
        }
        PieMapApiApplication.mWorkspace = new Workspace();
        boolean isOpen = PieMapApiApplication.mWorkspace.open(mapPath
                + "workspace.xml");
        if (!isOpen) {
            Toast.makeText(this, "打开工作区间失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 打开地图，关联到工作区间
     *
     * @param name
     *            地图的名字
     */
    private void openMap(String name) {

        boolean flag = false;

        if (mBasicMapView != null) {
            // 关联工作空间
            mBasicMapView.attachWorkspace(PieMapApiApplication.mWorkspace);
            // 打开地图
            flag = mBasicMapView.openMap(name);
        }
        if (!flag) {
            Toast.makeText(this, "打开地图失败", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBasicMapView.setPitch(true);// 设置是否可俯仰
        mBasicMapView.setRotate(true);// 设置是否可旋转
//        设置地图的场景模式(平面或者球面模式等)。
        mBasicMapView.setSceneMode(SceneMode.PlaneMode);
//        地图的缩放默认比例
//        double scale_default = 6.089714055909923E-6;
//        设置地图的缩放比例
//        mBasicMapView.setScale(scale_default);
        //控制器
        mBasicMapView.setMapGestureController(new MapGestureController());
        // 地图全屏显示
        mBasicMapView.viewEntire();

    }



    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.button:
                switchMap();
                break;
            case R.id.button2:
                location();
                break;
            case R.id.button3:
                mapZoomIn();
                break;
            case R.id.button4:
                mapZoomOut();
                break;
            default:
                break;
        }

    }

    /**
     * 地图切换
     */
    private void switchMap() {
        if (isSwitch) {
            mBasicMapView.closeMap();
            mBasicMapView.openMap("googlemap");
            mBasicMapView.setSceneMode(SceneMode.SphereMode);
            mBasicMapView.setPitch(true);// 设置是否可俯仰
            mBasicMapView.setScale(mBasicMapView.getScale() * 2);
        } else {
            mBasicMapView.closeMap();
            mBasicMapView.openMap("googlemap");
            mBasicMapView.setSceneMode(SceneMode.PlaneMode);
            mBasicMapView.viewEntire();
        }
        isSwitch = !isSwitch;

    }

    /**
     * 定位到当前位置
     */
    private void location() {

        //39.8712766624,116.3558578491
        Point2D centerPoint2d = new Point2D(116.3558578491,39.8712766642);
        centerPoint2d = mBasicMapView.getPrjCoordSys().latLngToProjection(centerPoint2d);

//        Point2D point2D = mBasicMapView.getPrjCoordSys().projectionTolatLng(centerPoint2d);
        setOverly();

        mBasicMapView.setMapCenter(centerPoint2d);
        resetMapAngle();

    }

    private void setOverly(){
        Point2D centerPoint2d = new Point2D(116.3558578491,39.8712766642);
        centerPoint2d = mBasicMapView.getPrjCoordSys().latLngToProjection(centerPoint2d);
        Point2D point2D = mBasicMapView.getPrjCoordSys().projectionTolatLng(centerPoint2d);
        overlayManager.deleteAllOverlay();
        overlayManager.addOverlayCircle(overlayCircleOptions.circleColor(Color.RED).point(point2D).radius(2));
    }

    /**
     * 地图缩小
     */
    private void mapZoomOut() {

        mBasicMapView.zoomOut();
        setOverly();
    }

    /**
     * 地图放大
     */
    private void mapZoomIn() {
        mBasicMapView.zoomIn();
        setOverly();
    }

    private class MapResourceAsyncTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            boolean isSuccess = copyMapResource(mapResourcePath,
                    MAP_RESOURCE_NAME);
            if (isSuccess) {
                return 1;// 复制和解压成功
            } else {
                return 0;// 复制和解压失败
            }

        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            SharedPreferences.Editor editor = mPreferences.edit();
            switch (result) {
                case 0:
                    Toast.makeText(MainActivity.this, "复制地图资源失败，请稍后重试",
                            Toast.LENGTH_SHORT).show();
                    editor.putBoolean("needCopyMapRes", true);
                    editor.commit();
                    finish();
                    break;
                case 1:
                    Toast.makeText(MainActivity.this, "地图资源初始化成功",
                            Toast.LENGTH_SHORT).show();
                    editor.putBoolean("needCopyMapRes", false);
                    editor.commit();
                    break;

                default:
                    break;
            }
        }

    }

    /**
     * 复制 Assets文件夹下文件到SDK，并解压
     * @param savePath 复制和解压文件路径
     * @param assetsFileName assets 文件名字
     * @return 是否复制和解压成功
     */
    public boolean copyMapResource(String savePath, String assetsFileName) {
        // 确保路径存在
        File saveRootPath = new File(savePath);
        if (!saveRootPath.exists()) {
            saveRootPath.mkdirs();
        }

        File targetFile = new File(savePath + "/" + assetsFileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }

        File dir = new File(savePath + "/" + assetsFileName);

        try {

            InputStream is = this.getResources().getAssets()
                    .open(assetsFileName);
            FileOutputStream fos = new FileOutputStream(dir);
            byte[] buffer = new byte[8 * 1024];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        boolean flag = unZip(savePath + "/" + assetsFileName, savePath + "/");
        targetFile.delete();
        return flag;
    }

    // 解压缩
    public static boolean unZip(String archive, String decompressDir) {
        try {
            BufferedInputStream bufferedInputStream;
            ZipFile zf = new ZipFile(archive, "GBK");
            Enumeration<?> e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipEntry ze2 = (ZipEntry) e.nextElement();
                String entryName = ze2.getName();
                String path = decompressDir + "/" + entryName;
                if (ze2.isDirectory()) {
                    File decompressDirFile = new File(path);
                    if (!decompressDirFile.exists()) {
                        decompressDirFile.mkdirs();
                    }
                } else {
                    String fileDir = path.substring(0, path.lastIndexOf("/"));
                    File fileDirFile = new File(fileDir);
                    if (!fileDirFile.exists()) {
                        fileDirFile.mkdirs();
                    }
                    BufferedOutputStream bos = new BufferedOutputStream(
                            new FileOutputStream(decompressDir + "/"
                                    + entryName));
                    bufferedInputStream = new BufferedInputStream(
                            zf.getInputStream(ze2));
                    byte[] readContent = new byte[8 * 1024];
                    int readCount = bufferedInputStream.read(readContent);
                    while (readCount != -1) {
                        bos.write(readContent, 0, readCount);
                        readCount = bufferedInputStream.read(readContent);
                    }
                    bos.flush();
                    bos.close();
                }
            }
            zf.close();
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        // 先关闭地图，再销毁地图，再关闭工作区间
        if (mBasicMapView != null) {
            mBasicMapView.closeMap();
            mBasicMapView.destroyMapWindow();
            mBasicMapView = null;
        }
        super.onDestroy();
    }


    /**
     * 恢复地图指北
     */
    private void resetMapAngle() {

        int currAngle = (int) mBasicMapView.getRollAngle();
        if (currAngle == 0 || currAngle == 360) {
            return;
        }
        if (currAngle > 180) {
            mBasicMapView.startRotateAnimation(currAngle, 360);
        } else {
            mBasicMapView.startRotateAnimation(currAngle, 0);
        }
        mBasicMapView.setPitchAngle(0);

    }


    /*private void locationSetting() {
//        if (!PieMapApiApplication.locationManager.IsProviderEnabled(PieMapApiApplication.locationManager.NetworkProvider)) {
//            Intent intent = new Intent(Settings.ActionLocationSourceSettings);
//            StartActivity(intent);
//        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗
        String provider = PieMapApiApplication.locationManager.getBestProvider(criteria, true); // 获取GPS信息
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},0);
            }
        }else {
            Location location = PieMapApiApplication.locationManager.getLastKnownLocation(provider);
            if(location == null){
                location = PieMapApiApplication.locationManager.getLastKnownLocation(PieMapApiApplication.locationManager.NETWORK_PROVIDER);
                if (location != null) {
                    goToLocation(location);
                }else {
                    Point2D centerPoint2d = new Point2D(mLatitude,mLongitude);
                    centerPoint2d = mBasicMapView.getPrjCoordSys().latLngToProjection(centerPoint2d);
                    mBasicMapView.setMapCenter(centerPoint2d);
                }
            }else {
                goToLocation(location);
            }
        }

    }
    public void location2(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},0);
            }
        }else {
            PieMapApiApplication.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new mLocationListener());
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We now have permission to use the location
                PieMapApiApplication.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new mLocationListener());

            }
        }
    }

    class mLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            goToLocation(location);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
    }

    *//**
     * 移动地图至定位位置
     *//*
    private void goToLocation(Location location) {

        //39.8712766624,116.3558578491
//        Point2D centerPoint2d = new Point2D(116.3558578491,39.8712766642);
        Point2D centerPoint2d = new Point2D(location.getLatitude(),location.getLongitude());
        centerPoint2d = mBasicMapView.getPrjCoordSys().latLngToProjection(centerPoint2d);

        Point2D point2D = mBasicMapView.getPrjCoordSys().projectionTolatLng(centerPoint2d);

        OverlayManager overlayManager = new OverlayManager(mBasicMapView);
        OverlayCircleOptions overlayCircleOptions = new OverlayCircleOptions();
        overlayManager.addOverlayCircle(overlayCircleOptions.circleColor(Color.RED).point(point2D).radius(2));

        mBasicMapView.setMapCenter(centerPoint2d);
        resetMapAngle();

    }*/

}



