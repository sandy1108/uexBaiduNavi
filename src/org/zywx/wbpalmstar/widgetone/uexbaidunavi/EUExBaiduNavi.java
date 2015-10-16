package org.zywx.wbpalmstar.widgetone.uexbaidunavi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.baidu.navisdk.CommonParams;
import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.comapi.mapcontrol.BNMapController;
import com.baidu.navisdk.comapi.mapcontrol.MapParams;
import com.baidu.navisdk.comapi.routeguide.RouteGuideParams;
import com.baidu.navisdk.comapi.routeplan.BNRoutePlaner;
import com.baidu.navisdk.model.datastruct.RoutePlanNode;
import com.baidu.navisdk.model.modelfactory.NaviDataEngine;
import com.baidu.navisdk.model.modelfactory.RoutePlanModel;
import com.baidu.navisdk.ui.routeguide.BNavConfig;
import com.baidu.navisdk.ui.routeguide.BNavigator;
import com.baidu.navisdk.ui.widget.RoutePlanObserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.widgetone.uexbaidunavi.vo.DoublePoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EUExBaiduNavi extends EUExBase {

    private static final String BUNDLE_DATA = "data";
    private static final int MSG_INIT_BAIDU_NAVI = 1;
    private static final int MSG_START_ROUTE_PLAN = 2;
    private static final int MSG_START_NAVI = 3;
    private static final int MSG_EXIT_NAVI = 4;
    private RoutePlanModel mRoutePlanModel = null;
    private boolean mIsEngineInitSuccess = false;
    private static final int REQUEST_CODE_ACTIVITY=100;
    private String mSDCardPath = null;
    private static final String APP_FOLDER_NAME = "BaiduNavi";

    public EUExBaiduNavi(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void init(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_INIT_BAIDU_NAVI;
        mHandler.sendMessage(msg);
    }

    private void initMsg(String[] params) {
        initDirs();
        BaiduNaviManager.getInstance().setNativeLibraryPath(getSdcardDir() + "/BaiduNaviSDK_SO");
        BaiduNaviManager.getInstance().init((Activity) mContext, mSDCardPath, APP_FOLDER_NAME,
                new com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener() {
                    @Override
                    public void onAuthResult(int status, String msg) {
                        if (0 == status) {
                            cbInit(true);
                        } else {
                            cbInit(false);
                            BDebug.e("appcan", "key校验失败！！！！！！！！！！！！！！！！！！！");
                        }
                    }

                    public void initSuccess() {

                    }

                    public void initStart() {
                    }

                    public void initFailed() {
                        cbInit(false);
                    }
                }, null /*mTTSCallback*/);
    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(
                Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    public void startRoutePlan(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_START_ROUTE_PLAN;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void startRoutePlanMsg(String[] params) {
        String json = params[0];
        DoublePoint startNode=new DoublePoint();
        DoublePoint endNode=new DoublePoint();
        List<DoublePoint> throughNodes = null;
        int mode = 1;
        String startAddr = null;
        String endAddr = null;
        try {
			JSONObject jsonObject = new JSONObject(json);
			startAddr = jsonObject.optString("startAddr");
			endAddr = jsonObject.optString("endAddr");
			mode = jsonObject.optInt("route_mode", 1);
			JSONArray startArray = jsonObject.optJSONArray("startNode");
			if (startArray != null) {
				startNode.set(startArray.getDouble(0), startArray.getDouble(1));
			}
			JSONArray endArray = jsonObject.optJSONArray("endNode");
			if (endArray != null) {
				endNode.set(endArray.getDouble(0), endArray.getDouble(1));
			}
			JSONArray throughNodeArray = jsonObject.optJSONArray("throughNodes");
            if (throughNodeArray!=null){
                throughNodes=new ArrayList<DoublePoint>();
                for (int i=0;i<throughNodeArray.length();i++){
                    JSONArray tempJsonArray=throughNodeArray.getJSONArray(i);
                    DoublePoint doublePoint=new DoublePoint(tempJsonArray.getDouble(0),tempJsonArray.getDouble(1));
                    throughNodes.add(doublePoint);
                }
            }
        } catch (JSONException e) {
        }
        startCalcRoute(startAddr, startNode, endAddr, endNode, throughNodes, mode);

    }

    private void startCalcRoute(String sAddr,DoublePoint sPoint,String eAddr,DoublePoint ePoint,List<DoublePoint>
            throughNodes,int mode){
        //起点
        BNRoutePlanNode startNode = new BNRoutePlanNode(sPoint.x,sPoint.y, sAddr,null, BNRoutePlanNode.CoordinateType.GCJ02);
        //终点
        BNRoutePlanNode endNode = new BNRoutePlanNode(ePoint.x,sPoint.y,eAddr,null,BNRoutePlanNode.CoordinateType.GCJ02);
        //将起终点添加到nodeList
        ArrayList<BNRoutePlanNode> nodeList = new ArrayList<BNRoutePlanNode>();
        nodeList.add(startNode);
        if (throughNodes!=null&&!throughNodes.isEmpty()){
            for (DoublePoint doublePoint:throughNodes){
                nodeList.add(new BNRoutePlanNode(doublePoint.x,doublePoint.y,"",null,BNRoutePlanNode.CoordinateType.GCJ02));
            }
        }
        nodeList.add(endNode);
        BNRoutePlaner.getInstance().setObserver(new RoutePlanObserver((Activity) mContext, null));

        // 设置起终点并算路
        int baiduMode=getRealMode(mode);
        BaiduNaviManager.getInstance().launchNavigator((Activity) mContext, nodeList, baiduMode, true, new
                MyRoutePlanListener(startNode));
    }

    private int getRealMode(int mode) {
        switch (mode){
            case 1:
                return BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;
            case 2:
                return BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME;
            case 3:
                return BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST;
        }
        return 1;
    }

    public class MyRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;
        public MyRoutePlanListener(BNRoutePlanNode node){
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            cbPlanResult("1");
            BNMapController.getInstance().setLayerMode(
                    MapParams.Const.LayerMode.MAP_LAYER_MODE_ROUTE_DETAIL);
            mRoutePlanModel = (RoutePlanModel) NaviDataEngine.getInstance()
                    .getModel(CommonParams.Const.ModelName.ROUTE_PLAN);
        }
        @Override
        public void onRoutePlanFailed() {
            cbPlanResult("2");
        }
    }

    private void cbInit(boolean result){
        JSONObject resultObject=new JSONObject();
        try {
            resultObject.put("isSuccess",result);
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_INIT,resultObject.toString());
    }

    private void cbPlanResult(String result){
        JSONObject resultObject=new JSONObject();
        try {
            resultObject.put("resultCode",result);
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_START_ROUTE_PLAN,resultObject.toString());
    }

    private void onExitNavi(){
        callBackPluginJs(JsConst.ON_EXIT_NAVI,"");
    }

    public void startNavi(boolean isReal) {
        if (mRoutePlanModel == null) {
            Toast.makeText(mContext, "请先算路！", Toast.LENGTH_LONG).show();
            return;
        }
        // 获取路线规划结果起点
        RoutePlanNode startNode = mRoutePlanModel.getStartNode();
        // 获取路线规划结果终点
        RoutePlanNode endNode = mRoutePlanModel.getEndNode();
        if (null == startNode || null == endNode) {
            return;
        }
        // 获取路线规划算路模式
//        int calcMode = BNRoutePlaner.getInstance().getCalcMode();
        Bundle bundle = new Bundle();
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_VIEW_MODE,
                BNavigator.CONFIG_VIEW_MODE_INFLATE_MAP);
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_CALCROUTE_DONE,
                BNavigator.CONFIG_CLACROUTE_DONE);
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_START_X,
                startNode.getLongitudeE6());
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_START_Y,
                startNode.getLatitudeE6());
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_END_X, endNode.getLongitudeE6());
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_END_Y, endNode.getLatitudeE6());
        bundle.putString(BNavConfig.KEY_ROUTEGUIDE_START_NAME,
                mRoutePlanModel.getStartName(mContext, false));
        bundle.putString(BNavConfig.KEY_ROUTEGUIDE_END_NAME,
                mRoutePlanModel.getEndName(mContext, false));
//        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_CALCROUTE_MODE, calcMode);
        if (!isReal) {
            // 模拟导航
            bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_LOCATE_MODE,
                    RouteGuideParams.RGLocationMode.NE_Locate_Mode_RouteDemoGPS);
        } else {
            // GPS 导航
            bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_LOCATE_MODE,
                    RouteGuideParams.RGLocationMode.NE_Locate_Mode_GPS);
        }

        Intent intent = new Intent(mContext, BNavigatorActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent,REQUEST_CODE_ACTIVITY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==REQUEST_CODE_ACTIVITY){
            onExitNavi();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startNavi(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_START_NAVI;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void startNaviMsg(String[] params) {
        String json = params[0];
        String naviType=null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            naviType=jsonObject.getString("naviType");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        startNavi("1".equals(naviType));
    }


    public void exitNavi(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_START_NAVI;
        mHandler.sendMessage(msg);
    }

    private void exitNaviMsg(String[] params) {
        BNRouteGuideManager.getInstance().forceQuitNaviWithoutDialog();
    }

    @Override
    public void onHandleMessage(Message message) {
        if(message == null){
            return;
        }
        Bundle bundle=message.getData();
        switch (message.what) {

            case MSG_INIT_BAIDU_NAVI:
                initMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_START_ROUTE_PLAN:
                startRoutePlanMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_START_NAVI:
                startNaviMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_EXIT_NAVI:
                exitNaviMsg(bundle.getStringArray(BUNDLE_DATA));
            default:
                super.onHandleMessage(message);
        }
    }

    private void callBackPluginJs(String methodName, String jsonData){
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        onCallback(js);
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if ( mSDCardPath == null ) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if ( !f.exists() ) {
            try {
                f.mkdir();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}
