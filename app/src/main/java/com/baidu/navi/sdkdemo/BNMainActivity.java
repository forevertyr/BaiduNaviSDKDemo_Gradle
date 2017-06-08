package com.baidu.navi.sdkdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.baidu.navisdk.adapter.BaiduNaviManager.RoutePlanListener;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BNMainActivity extends Activity {

    public static        List<Activity> activityList        = new LinkedList<Activity>();
    private static final String         APP_FOLDER_NAME     = "BNSDKFolder";
    public static final  String         ROUTE_PLAN_NODE     = "routePlanNode";
    private static final String[]       authBaseArr         = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String[]       authComArr          = {Manifest.permission.READ_PHONE_STATE};
    private static final int            authBaseRequestCode = 1;
    private static final int            authComRequestCode  = 2;

    private double  startLat          = 36.671906;
    private double  startLng          = 117.038293;
    private double  endLat            = 36.658194;
    private double  endLng            = 117.126488;
    private Button  mDb06ll           = null;
    private String  mSDCardPath       = null;
    private boolean hasInitSuccess    = false;
    private boolean hasRequestComAuth = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityList.add(this);
        setContentView(R.layout.activity_main);

        mDb06ll = (Button) findViewById(R.id.mDb06llNaviBtn);

        initListener();

        if (initDirs()) {
            initNavi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initListener() {
        if (mDb06ll != null) {
            mDb06ll.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (BaiduNaviManager.isNaviInited()) {
                        routeplanToNavi(CoordinateType.BD09LL);
                    } else {
                        startMapClient();
                    }
                }
            });
        }
    }

    /**
     * 调用第三方地图客户端
     */
    private void startMapClient() {
        if (isInstallByread("com.baidu.BaiduMap")) {
            startBaiduMapClient();
        } else if (isInstallByread("com.autonavi.minimap")) {
            startAmapClient();
        } else {
            Toast.makeText(BNMainActivity.this, "请安装百度地图或高德地图客户端", Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * 调用百度地图客户端
     * 参考官方文档:http://developer.baidu.com/map/uri-introandroid.htm
     */
    private void startBaiduMapClient() {
        Intent baiduClientIntent = null;
        try {
            baiduClientIntent = Intent.getIntent
                    ("intent://map/navi?location=" + endLat + "," + endLng +
                            "&type=TIME&src=thirdapp.navi.hndist.sydt#Intent;scheme=bdapp;" +
                            "package=com.baidu.BaiduMap;end");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        startActivity(baiduClientIntent);
    }

    /**
     * 调用高德地图客户端
     * 参考官方文档:http://lbs.amap.com/api/amap-mobile/gettingstarted
     */
    private void startAmapClient() {
        Intent amapClientIntent = new Intent();
        amapClientIntent.setAction(Intent.ACTION_VIEW);
        amapClientIntent.addCategory(Intent.CATEGORY_DEFAULT);
        //将功能Scheme以URI的方式传入data
        Uri uri = Uri.parse("androidamap://navi?sourceApplication=BNSDKDemo&poiname=fangheng&lat=  " + endLat + " &lon= " + endLng + "&dev=0&style=2");
        amapClientIntent.setData(uri);
        startActivity(amapClientIntent);
    }

    /**
     * 判断是否安装目标应用
     */
    private boolean isInstallByread(String packageName) {
        return new File("/data/data/" + packageName)
                .exists();
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    String authinfo = null;

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    // showToastMsg("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    // showToastMsg("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            // showToastMsg("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            // showToastMsg("TTSPlayStateListener : TTS play start");
        }
    };

    public void showToastMsg(final String msg) {
        BNMainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(BNMainActivity.this, msg, Toast.LENGTH_SHORT)
                     .show();
            }
        });
    }

    private boolean hasBasePhoneAuth() {
        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCompletePhoneAuth() {
        PackageManager pm = this.getPackageManager();
        for (String auth : authComArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initNavi() {
        BNOuterTTSPlayerCallback ttsCallback = null;
        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasBasePhoneAuth()) {
                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;
            }
        }

        BaiduNaviManager.getInstance()
                        .init(this, mSDCardPath, APP_FOLDER_NAME, new NaviInitListener() {
                            @Override
                            public void onAuthResult(int status, String msg) {
                                if (0 == status) {
                                    authinfo = "key校验成功!";
                                } else {
                                    authinfo = "key校验失败, " + msg;
                                }

                                BNMainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Toast.makeText(BNMainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            public void initSuccess() {
                                //Toast.makeText(BNMainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                                hasInitSuccess = true;
                                initSetting();
                            }

                            public void initStart() {
                                //Toast.makeText(BNMainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
                            }

                            public void initFailed() {
                                //Toast.makeText(BNMainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
                            }

                        }, null, ttsHandler, ttsPlayStateListener);

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState()
                       .equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory()
                              .toString();
        }
        return null;
    }

    private CoordinateType mCoordinateType = null;

    private void routeplanToNavi(CoordinateType coType) {
        mCoordinateType = coType;
        if (!hasInitSuccess) {
            //Toast.makeText(BNMainActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        // 权限申请
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // 保证导航功能完备
            if (!hasCompletePhoneAuth()) {
                if (!hasRequestComAuth) {
                    hasRequestComAuth = true;
                    this.requestPermissions(authComArr, authComRequestCode);
                    return;
                } else {
                    Toast.makeText(BNMainActivity.this, "没有完备的权限!", Toast.LENGTH_SHORT)
                         .show();
                }
            }

        }

        BNRoutePlanNode sNode = null;
        BNRoutePlanNode eNode = null;

        switch (coType) {
            case BD09LL: {
                sNode = new BNRoutePlanNode(startLng, startLat, "起点", null, coType);
                eNode = new BNRoutePlanNode(endLng, endLat, "终点", null, coType);
                break;
            }
        }

        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);
            BaiduNaviManager.getInstance()
                            .launchNavigator(this, list, 1, true, new MyRoutePlanListener(sNode));
        }
    }


    public class MyRoutePlanListener implements RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public MyRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
             */
            for (Activity ac : activityList) {

                if (ac.getClass()
                      .getName()
                      .endsWith("BNGuideActivity")) {
                    return;
                }
            }
            Intent intent = new Intent(BNMainActivity.this, BNGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);
        }

        @Override
        public void onRoutePlanFailed() {
            Toast.makeText(BNMainActivity.this, "算路失败", Toast.LENGTH_SHORT)
                 .show();
        }
    }

    private void initSetting() {
        // BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        BNaviSettingManager
                .setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        // BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
        Bundle bundle = new Bundle();
        // 必须设置APPID，否则会静音
        bundle.putString(BNCommonSettingParam.TTS_APP_ID, "9354030");
        BNaviSettingManager.setNaviSdkParam(bundle);
    }

    private BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {

        @Override
        public void stopTTS() {
            Log.e("test_TTS", "stopTTS");
        }

        @Override
        public void resumeTTS() {
            Log.e("test_TTS", "resumeTTS");
        }

        @Override
        public void releaseTTSPlayer() {
            Log.e("test_TTS", "releaseTTSPlayer");
        }

        @Override
        public int playTTSText(String speech, int bPreempt) {
            Log.e("test_TTS", "playTTSText" + "_" + speech + "_" + bPreempt);
            return 1;
        }

        @Override
        public void phoneHangUp() {
            Log.e("test_TTS", "phoneHangUp");
        }

        @Override
        public void phoneCalling() {
            Log.e("test_TTS", "phoneCalling");
        }

        @Override
        public void pauseTTS() {
            Log.e("test_TTS", "pauseTTS");
        }

        @Override
        public void initTTSPlayer() {
            Log.e("test_TTS", "initTTSPlayer");
        }

        @Override
        public int getTTSState() {
            Log.e("test_TTS", "getTTSState");
            return 1;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == authBaseRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                } else {
                    Toast.makeText(BNMainActivity.this, "缺少导航基本的权限!", Toast.LENGTH_SHORT)
                         .show();
                    return;
                }
            }
            initNavi();
        } else if (requestCode == authComRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                }
            }
            routeplanToNavi(mCoordinateType);
        }
    }

}
