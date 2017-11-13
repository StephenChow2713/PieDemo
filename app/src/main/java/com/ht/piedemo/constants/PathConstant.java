package com.ht.piedemo.constants;

import android.os.Environment;

/**
 * Created by 13468 on 2017/11/8.
 */

public class PathConstant {

    /**PATH_PIE_MAP_RES_DEFAULT 地图资源默认路径*/
    public static final String PATH_PIE_MAP_RES_DEFAULT = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/PIE/sdk/piesdk" + "/map/";
//    public static final String PATH_PIE_DEM_DATA_DEFAULT = Environment
//            .getExternalStorageDirectory().getAbsolutePath()
//            + "/PIE/sdk/piesdk" + "/map/dem/";
}
