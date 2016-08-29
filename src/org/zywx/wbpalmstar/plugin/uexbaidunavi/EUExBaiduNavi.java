package org.zywx.wbpalmstar.plugin.uexbaidunavi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.baidu.navisdk.adapter.BaiduNaviManager.RoutePlanListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.plugin.uexbaidunavi.util.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 百度导航插件入口类
 *
 * @author waka
 * @version createTime:2016年4月19日 下午4:53:30
 */
public class EUExBaiduNavi extends EUExBase {

    /*
     * 初始化百度导航
     */
    private String mSDCardPath = null;// SD卡目录，初始化百度导航需要
    private boolean isKeyCorrect = false;// key是否配置正确
    private String keyErrMsg = "";// key配置错误信息

    // 路径规划参数
    private GeographyPoint startNode;// 开始节点
    private String startAddr;// 开始地址
    private GeographyPoint endNode;// 结束节点
    private String endAddr;// 结束地址
    private List<GeographyPoint> throughNodes;// 经过节点
    private int preferenceMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;// 算路偏好常量，默认为推荐

    // 广播接收器，用来监听诱导Activity是否关闭
    private MyBroadcastReceiver mReceiver;

    // 回调
    private static final String CB_INIT = "uexBaiduNavi.cbInit";// 初始化回调
    private static final String CB_START_ROUTE_PLAN = "uexBaiduNavi.cbStartRoutePlan";// 开始路径规划回调
    private static final String ON_EXIT_NAVI = "uexBaiduNavi.onExitNavi";// 退出导航的监听方法

    // Handler
    private MyHandler ttsHandler = new MyHandler(this);

    /**
     * 内部TTS播报状态回传handler
     * <p/>
     * 静态Handler内部类，避免内存泄漏
     *
     * @author waka
     */
    private static class MyHandler extends Handler {

        // 对Handler持有的对象使用弱引用
        @SuppressWarnings("unused")
        private WeakReference<EUExBaiduNavi> wrEUExBaiduNavi;

        public MyHandler(EUExBaiduNavi euExBaiduNavi) {
            wrEUExBaiduNavi = new WeakReference<EUExBaiduNavi>(euExBaiduNavi);
        }

        public void handleMessage(Message msg) {

            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    BDebug.i("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    BDebug.i("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * 构造方法
     *
     * @param arg0
     * @param arg1
     */
    public EUExBaiduNavi(Context arg0, EBrowserView arg1) {
        super(arg0, arg1);
    }

    /**
     * clean
     */
    @Override
    protected boolean clean() {
        return false;
    }

    /**
     * 初始化
     *
     * @param params
     */
    public void init(String[] params) {

        BDebug.i("start");

        int callbackId=-1;
        if (params.length>1){//params[0]为iOS必传参数
            callbackId= Integer.parseInt(params[1]);
        }
        // 获得SD卡下的存放目录
        mSDCardPath = Environment.getExternalStorageDirectory().toString() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/" + Constant.APP_FOLDER_NAME;// 获得文件夹路径
        Util.checkFolderPath(mSDCardPath);
        BDebug.i("mSDCardPath = " + mSDCardPath);

        final int finalCallbackId = callbackId;
        BaiduNaviManager.getInstance().init((Activity) mContext, mSDCardPath, Constant.APP_FOLDER_NAME, new NaviInitListener() {

            @Override
            public void onAuthResult(int status, String msg) {

                keyErrMsg = msg;

                if (0 == status) {
                    isKeyCorrect = true;
                    BDebug.i("key校验成功!");
                } else {
                    isKeyCorrect = false;
                    BDebug.i("key校验失败, " + msg);
                }
            }

            /*
             * 需要注意的是，即使key校验失败，百度导航引擎也可以初始化成功
             */
            @Override
            public void initSuccess() {

                BDebug.i("百度导航引擎初始化成功");

                // initSetting
                BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
                BNaviSettingManager.setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
                BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
                BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
                BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);

                if (isKeyCorrect) {
                    cbInit(true,finalCallbackId,null);// 只有当key配置正确且引擎也初始化完成后才回调cbInit = true
                } else {
                    BDebug.i("key校验失败, " + keyErrMsg);
                    Toast.makeText(mContext, "key校验失败, " + keyErrMsg, Toast.LENGTH_LONG).show();
                    cbInit(false,finalCallbackId,"key校验失败, "+keyErrMsg);
                }

            }

            @Override
            public void initStart() {
                BDebug.i("百度导航引擎初始化开始");
            }

            @Override
            public void initFailed() {
                BDebug.i("百度导航引擎初始化失败");
                cbInit(false, finalCallbackId,"百度导航引擎初始化失败");
            }

        }, null, ttsHandler, null);
    }

    /**
     * cbInit
     *
     * @param flag
     */
    private void cbInit(boolean flag,int callbackId,String error) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isSuccess", flag);
        } catch (JSONException e) {
        }
        if(callbackId!=-1){
            callbackToJs(callbackId,false,flag?0:1,error);
        }else{
            jsCallback(CB_INIT, jsonObject.toString());
        }
      }

    /**
     * 开始路径规划
     *
     * @param params
     */
    public void startRoutePlan(String[] params) {

        BDebug.i("start");

        if (params.length < 1) {

            BDebug.i("params.length < 1");
            return;
        }

        BDebug.i(params[0]);

        // 清空之前的数据
        startNode = null;
        startAddr = null;
        endNode = null;
        endAddr = null;
        throughNodes = null;
        preferenceMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;
        int callbackId=-1;
        if (params.length>1){
            callbackId= Integer.parseInt(params[1]);
        }
        // 解析JSON数据
        String json = params[0];
        startNode = new GeographyPoint();
        endNode = new GeographyPoint();
        try {
            JSONObject jsonObject = new JSONObject(json);
            startAddr = jsonObject.optString("startAddr", "");
            endAddr = jsonObject.optString("endAddr", "");
            preferenceMode = Integer.valueOf(jsonObject.optString("mode", "1"));
            preferenceMode = Util.amendPreferenceMode(preferenceMode);// 修正算路偏好模式
            JSONArray startArray = jsonObject.optJSONArray("startNode");
            if (startArray != null) {
                startNode.set(startArray.getDouble(0), startArray.getDouble(1));
            }
            JSONArray endArray = jsonObject.optJSONArray("endNode");
            if (endArray != null) {
                endNode.set(endArray.getDouble(0), endArray.getDouble(1));
            }
            JSONArray throughNodeArray = jsonObject.optJSONArray("throughNodes");
            if (throughNodeArray != null) {
                throughNodes = new ArrayList<GeographyPoint>();
                for (int i = 0; i < throughNodeArray.length(); i++) {
                    JSONArray tempJsonArray = throughNodeArray.getJSONArray(i);
                    GeographyPoint doublePoint = new GeographyPoint(tempJsonArray.getDouble(0), tempJsonArray.getDouble(1));
                    throughNodes.add(doublePoint);
                }
            }
        } catch (JSONException e) {
            e.getStackTrace();
            BDebug.e(e);
            cbStartRoutePlan(RESULT_CODE_START_ROUTE_PLAN_FAIL, ERROR_INFO_START_ROUTE_PLAN_5,callbackId);
            return;
        }

        if (startNode == null || endNode == null) {
            BDebug.i("startNode == null || endNode == null");
            cbStartRoutePlan(RESULT_CODE_START_ROUTE_PLAN_FAIL, ERROR_INFO_START_ROUTE_PLAN_5,callbackId);
            return;
        }

        cbStartRoutePlan(RESULT_CODE_START_ROUTE_PLAN_SUCCESS, 0,callbackId);
    }

    public static final int RESULT_CODE_START_ROUTE_PLAN_SUCCESS = 1;// 路径规划成功
    public static final int RESULT_CODE_START_ROUTE_PLAN_FAIL = 2;// 路径规划失败
    public static final int RESULT_CODE_START_ROUTE_PLAN_CANCEL = 3;// 路径规划被取消

    public static final int ERROR_INFO_START_ROUTE_PLAN_1 = 1;// 获取地理位置失败
    public static final int ERROR_INFO_START_ROUTE_PLAN_2 = 2;// 无法发起算路
    public static final int ERROR_INFO_START_ROUTE_PLAN_3 = 3;// 定位服务未开启
    public static final int ERROR_INFO_START_ROUTE_PLAN_4 = 4;// 节点之间距离太近
    public static final int ERROR_INFO_START_ROUTE_PLAN_5 = 5;// 节点输入有误
    public static final int ERROR_INFO_START_ROUTE_PLAN_6 = 6;// 上次算路取消了,需要等一会儿

    /**
     * cbStartRoutePlan
     *
     */
    private void cbStartRoutePlan(int resultCode, int errorInfo,int callbackId) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("resultCode", resultCode);
            if (resultCode == RESULT_CODE_START_ROUTE_PLAN_FAIL) {
                jsonObject.put("errorInfo", errorInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(callbackId!=-1){
            callbackToJs(callbackId,false,resultCode==RESULT_CODE_START_ROUTE_PLAN_SUCCESS?0:errorInfo);
        }else{
            jsCallback(CB_START_ROUTE_PLAN, jsonObject.toString());
        }
     }

    /**
     * 开始导航
     *
     * @param params
     */
    public void startNavi(String[] params) {

        BDebug.i("");

        if (startNode == null || endNode == null) {
            BDebug.i("startNode == null || endNode == null");
            return;
        }

        // 开始节点
        BNRoutePlanNode sNode = null;
        // 结束节点
        BNRoutePlanNode eNode = null;
        if (startAddr.isEmpty()) {
            sNode = new BNRoutePlanNode(startNode.getLongitude(), startNode.getLatitude(), null, null);
        } else {
            sNode = new BNRoutePlanNode(startNode.getLongitude(), startNode.getLatitude(), startAddr, null);
        }
        if (endAddr.isEmpty()) {
            eNode = new BNRoutePlanNode(endNode.getLongitude(), endNode.getLatitude(), null, null);
        } else {
            eNode = new BNRoutePlanNode(endNode.getLongitude(), endNode.getLatitude(), endAddr, null);
        }

        if (sNode != null && eNode != null) {

            BDebug.i("sNode != null && eNode != null");

            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);

            if (throughNodes != null) {
                // 经过节点
                for (GeographyPoint throughNode : throughNodes) {
                    BNRoutePlanNode tNode = new BNRoutePlanNode(throughNode.getLongitude(), throughNode.getLatitude(), null, null);
                    if (tNode != null) {
                        list.add(tNode);
                    }
                }
            }

            list.add(eNode);
            BaiduNaviManager.getInstance().launchNavigator((Activity) mContext, list, preferenceMode, true, new DemoRoutePlanListener(sNode));
            BDebug.i("end");
        }
    }

    class DemoRoutePlanListener implements RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {

			/*
             * 设置途径点以及resetEndNode会回调该接口
			 */
            BDebug.i("");

            // 注册广播接收器
            mReceiver = new MyBroadcastReceiver(EUExBaiduNavi.this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constant.LOCAL_BROADCAST_BNAVIGATOR_ACTIVITY_DESTORY);
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
            lbm.registerReceiver(mReceiver, filter);

            // 跳转到原生诱导Activity
            Intent intent = new Intent(mContext, BNavigatorActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);

        }

        @Override
        public void onRoutePlanFailed() {

            BDebug.i("算路失败");
            Toast.makeText(mContext, "算路失败", Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * 退出导航
     *
     * @param params
     */
    public void exitNavi(String[] params) {
        BNRouteGuideManager.getInstance().forceQuitNaviWithoutDialog();
    }

    /**
     * 退出导航的监听方法
     */
    private void onExitNavi() {

        BDebug.i("");

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        mReceiver = null;

        jsCallback(ON_EXIT_NAVI, "");
    }

    /**
     * 给前端回调
     *
     * @param inCallbackName
     * @param inData
     */
    private void jsCallback(String inCallbackName, String inData) {

        String js = SCRIPT_HEADER + "if(" + inCallbackName + "){" + inCallbackName + "(" + "'" + inData + "'" + SCRIPT_TAIL;
        // mBrwView.loadUrl(js);
        onCallback(js);
    }

    private static class MyBroadcastReceiver extends BroadcastReceiver {

        private WeakReference<EUExBaiduNavi> wrEUExBaiduNavi;

        public MyBroadcastReceiver(EUExBaiduNavi uexBaiduNavi) {

            wrEUExBaiduNavi = new WeakReference<EUExBaiduNavi>(uexBaiduNavi);
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // 诱导Activity关闭广播
            if (action.equals(Constant.LOCAL_BROADCAST_BNAVIGATOR_ACTIVITY_DESTORY)) {

                wrEUExBaiduNavi.get().onExitNavi();
            }
        }

    }

}
