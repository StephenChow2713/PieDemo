package com.ht.piedemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;

import com.baidu.location.LocationClientOption;
import com.ht.piedemo.constants.PathConstant;
import com.ht.piedemo.service.LocationService;
import com.ht.piedemo.utils.GCJ02_BD09;
import com.ht.piedemo.view.RotateImageView;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;

import pie.core.DimensionMode;
import pie.core.GeoPoint;
import pie.core.GisNative;
import pie.core.MapRotateChangedListener;
import pie.core.MapView;
import pie.core.Point2D;
import pie.core.SceneMode;
import pie.core.Style;
import pie.core.TrackingLayer;
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
    private Button bt_switchMap, bt_Location, bt_mapZoomOut, bt_mapZoomIn;
    private boolean isSwitch = true;

    private OverlayManager overlayManager;
    private OverlayCircleOptions overlayCircleOptions;
    private RotateImageView compassButton;

    public LocationClient mLocationClient = null;
    private final int SDK_PERMISSION_REQUEST = 127;
    private String permissionInfo;
    private LocationService locationService;
    private MyLocationListener myListener = new MyLocationListener();
    public BDLocation mylocation;



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

        getPersimmions();
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener( myListener);
        initBDMap();
        locationService.start();


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
        mGeometryTrackingLayer = mBasicMapView.getTrackingLayer();
        bt_switchMap = (Button) findViewById(R.id.bt_switchMap);
        bt_switchMap.setOnClickListener(this);
        bt_Location = (Button) findViewById(R.id.bt_Location);
        bt_Location.setOnClickListener(this);
        bt_mapZoomOut = (Button) findViewById(R.id.bt_mapZoomOut);
        bt_mapZoomOut.setOnClickListener(this);
        bt_mapZoomIn = (Button) findViewById(R.id.bt_mapZoomIn);
        bt_mapZoomIn.setOnClickListener(this);

        compassButton = (RotateImageView) findViewById(R.id.riv_pie_fix_compass);
        compassButton.setOnClickListener(this);
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
        mBasicMapView
                .setMapRotateChangedListener(new AngleRotateChangedListener());
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

    protected void initBDMap() {
        // -----------location config ------------
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService = ((PieMapApiApplication) getApplication()).locationService;
        //注册监听
        locationService.registerListener( myListener);
        //默认的定位设置
//        locationService.setLocationOption(locationService.getDefaultLocationClientOption());

        LocationClientOption mOption = new LocationClientOption();
        mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
//        mOption.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        mOption.setScanSpan(3000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        mOption.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        mOption.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
        mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
        mOption.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        mOption.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        mOption.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        mOption.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        mOption.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        mOption.setIsNeedAltitude(false);//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用

        locationService.setLocationOption(mOption);


    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.bt_switchMap:
                switchMap();
                break;
            case R.id.bt_Location:
                location(mylocation);
//                locationService.start();
                break;
            case R.id.bt_mapZoomOut:
                mapZoomOut();
                break;
            case R.id.bt_mapZoomIn:
                mapZoomIn();
                break;
            case R.id.riv_pie_fix_compass:
                resetMapAngle();
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
     * 地图缩小
     */
    private void mapZoomOut() {

        mBasicMapView.zoomOut();
//        setOverly(mylocation);
    }

    /**
     * 地图放大
     */
    private void mapZoomIn() {
        mBasicMapView.zoomIn();
//        setOverly(mylocation);
    }

    private void setOverly(BDLocation location){
        //经度
        double longitude = location.getLongitude();
        //纬度
        double latitude = location.getLatitude();
        Point2D centerPoint2d = new Point2D(longitude,latitude);
        centerPoint2d = mBasicMapView.getPrjCoordSys().latLngToProjection(centerPoint2d);
        Point2D point2D = mBasicMapView.getPrjCoordSys().projectionTolatLng(centerPoint2d);
        overlayManager.deleteAllOverlay();
        overlayManager.addOverlayCircle(overlayCircleOptions.circleColor(Color.RED).point(point2D).radius(2));
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
    /**
     * 地图旋转角度改变监听
     *
     * @author pie
     */
    private class AngleRotateChangedListener implements
            MapRotateChangedListener {

        @Override
        public void onRotateChanged(double angle) {
            compassButton.setAngle(angle);
        }

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

    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
			/*
			 * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
            }
            // 读取电话状态权限
            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)){
                return true;
            }else{
                permissionsList.add(permission);
                return false;
            }

        }else{
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    private void sendMsg(final BDLocation location) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    location(location);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 定位到当前位置
     */
    private void location(BDLocation location) {

        if (location != null){

//            GCJ02_BD09.Gps gps = GCJ02_BD09.bd09_To_Gcj02(location.getLongitude(), location.getLatitude());
            //经度
            double longitude = location.getLongitude();
            //纬度
            double latitude = location.getLatitude();

            //392D centerPoint2d = new Point2D(116.3558578491,39.8712766642);
            Point2D centerPoint2d = new Point2D(longitude,latitude);
//            Point2D centerPoint2d = new Point2D(gps.lon,gps.lat);
            centerPoint2d = mBasicMapView.getPrjCoordSys().latLngToProjection(centerPoint2d);

//          setOverly(location);
            Point2D startPnt = mBasicMapView.getMapCenter();
            mBasicMapView.startPanAnimation(startPnt, centerPoint2d);
            addPointAtTrackLayer(centerPoint2d);
            mBasicMapView.setScale(2.105484291333595E-5);
//          mBasicMapView.setMapCenter(centerPoint2d);
        }

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

    private TrackingLayer mGeometryTrackingLayer;
    /**
     * 向跟踪层添加点 需要重新创建点对象、Style对象和Geometry对象
     */
    private void addPointAtTrackLayer(Point2D point) {

        mGeometryTrackingLayer.removeAllGeometry();
        GeoPoint geopt = new GeoPoint(point.x, point.y);
        Style pointStyle = new Style();
        pointStyle.markerStyle = 2;
        pointStyle.markerSize = 10;
        pointStyle.markerType = 1;
        geopt.setStyle(pointStyle);
        mGeometryTrackingLayer.addGeometry("p" + System.currentTimeMillis(), geopt);

    }
    /*****
     *
     * 定位结果回调，重写onReceiveLocation方法，可以直接拷贝如下代码到自己工程中修改
     *
     */
    public class  MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                mylocation = location;
//                Toast.makeText(MainActivity.this,"定位回调"+location.getLatitude(),Toast.LENGTH_SHORT).show();
//                sendMsg(location);
            }
        }
    }

}