package org.zywx.wbpalmstar.widgetone.uexbaidunavi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.baidu.navisdk.BNaviEngineManager;
import com.baidu.navisdk.BaiduNaviManager;
import com.baidu.navisdk.CommonParams;
import com.baidu.navisdk.comapi.mapcontrol.BNMapController;
import com.baidu.navisdk.comapi.mapcontrol.MapParams;
import com.baidu.navisdk.comapi.routeguide.RouteGuideParams;
import com.baidu.navisdk.comapi.routeplan.BNRoutePlaner;
import com.baidu.navisdk.comapi.routeplan.IRouteResultObserver;
import com.baidu.navisdk.comapi.routeplan.RoutePlanParams;
import com.baidu.navisdk.model.NaviDataEngine;
import com.baidu.navisdk.model.RoutePlanModel;
import com.baidu.navisdk.model.datastruct.RoutePlanNode;
import com.baidu.navisdk.ui.routeguide.BNavConfig;
import com.baidu.navisdk.ui.routeguide.BNavigator;
import com.baidu.navisdk.ui.widget.RoutePlanObserver;
import com.baidu.nplatform.comapi.basestruct.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.widgetone.uexbaidunavi.vo.DoublePoint;

import java.util.ArrayList;
import java.util.List;

public class EUExBaiduNavi extends EUExBase {

    private static final String BUNDLE_DATA = "data";
    private static final int MSG_INIT_BAIDU_NAVI = 1;
    private static final int MSG_START_ROUTE_PLAN = 2;
    private static final int MSG_START_NAVI = 3;
    private RoutePlanModel mRoutePlanModel = null;
    private boolean mIsEngineInitSuccess = false;

    public EUExBaiduNavi(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void initBaiduNavi(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_INIT_BAIDU_NAVI;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void initBaiduNaviMsg(String[] params) {
        String json = params[0];
        String baiduAPIKey=null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            baiduAPIKey=jsonObject.getString("baiduAPIKey");
            //初始化导航引擎
            BaiduNaviManager.getInstance().
                    initEngine((Activity) mContext, getSdcardDir(), mNaviEngineInitListener, baiduAPIKey, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(
                Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private BNaviEngineManager.NaviEngineInitListener mNaviEngineInitListener = new BNaviEngineManager.NaviEngineInitListener() {
        public void engineInitSuccess() {
            //导航初始化是异步的，需要一小段时间，以这个标志来识别引擎是否初始化成功，为true时候才能发起导航
            mIsEngineInitSuccess = true;
        }

        public void engineInitStart() {
        }

        public void engineInitFail() {
        }
    };

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
        List<GeoPoint> throughNodes = null;
        String mode = null;
        String startAddr = null;
        String endAddr = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            startAddr=jsonObject.getString("startAddr");
            endAddr=jsonObject.getString("endAddr");
            mode=jsonObject.getString("route_mode");
            JSONArray startArray=jsonObject.getJSONArray("startNode");
            if (startAddr!=null){
                startNode.set(startArray.getDouble(0),startArray.getDouble(1));
            }
            JSONArray endArray=jsonObject.getJSONArray("endNode");
            if (endArray!=null){
                endNode.set(endArray.getDouble(0),endArray.getDouble(1));
            }
            JSONArray throughNodeArray=jsonObject.getJSONArray("throughNodes");
            if (throughNodeArray!=null){
                throughNodes=new ArrayList<GeoPoint>();
                for (int i=0;i<throughNodeArray.length();i++){
                    JSONArray tempJsonArray=throughNodeArray.getJSONArray(i);
                    DoublePoint doublePoint=new DoublePoint(tempJsonArray.getDouble(0),tempJsonArray.getDouble(1));
                    throughNodes.add(getGeoPointFromDouble(doublePoint));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //路径规划
        GeoPoint sPoint=getGeoPointFromDouble(startNode);
        GeoPoint ePoint=getGeoPointFromDouble(endNode);
        if (TextUtils.isEmpty(mode)){
            mode="1";
        }
        startCalcRoute(Integer.valueOf(mode), startAddr, sPoint, endAddr, ePoint, throughNodes);

    }

    private GeoPoint getGeoPointFromDouble(DoublePoint doublePoint){
        return new GeoPoint((int)(doublePoint.getX()*1E5),(int)(doublePoint.getY()*1E5));
    }

    private void startCalcRoute(int routeMode,String sAddr,GeoPoint sPoint,String eAddr,GeoPoint ePoint,List<GeoPoint> throughNodes){
        //起点
        RoutePlanNode startNode = new RoutePlanNode(sPoint,
                RoutePlanNode.FROM_MAP_POINT, sAddr, sAddr);
        //终点
        RoutePlanNode endNode = new RoutePlanNode(ePoint,
                RoutePlanNode.FROM_MAP_POINT, eAddr,eAddr);
        //将起终点添加到nodeList
        ArrayList<RoutePlanNode> nodeList = new ArrayList<RoutePlanNode>(2);
        nodeList.add(startNode);
        if (throughNodes!=null&&!throughNodes.isEmpty()){
            for (GeoPoint geoPoint:throughNodes){
                nodeList.add(new RoutePlanNode(geoPoint,RoutePlanNode.FROM_MAP_POINT,"",""));
            }
        }
        nodeList.add(endNode);
        BNRoutePlaner.getInstance().setObserver(new RoutePlanObserver((Activity)mContext, null));
        //设置算路方式
        BNRoutePlaner.getInstance().setCalcMode(routeMode);
        // 设置算路结果回调
        BNRoutePlaner.getInstance().setRouteResultObserver(mRouteResultObserver);
        // 设置起终点并算路
        boolean ret = BNRoutePlaner.getInstance().setPointsToCalcRoute(
                nodeList, CommonParams.NL_Net_Mode.NL_Net_Mode_OnLine);
        if(!ret){
            Toast.makeText(mContext, "规划失败", Toast.LENGTH_SHORT).show();
        }
        BNRoutePlaner.getInstance().setRouteResultObserver(mRouteResultObserver);
    }


    private IRouteResultObserver mRouteResultObserver = new IRouteResultObserver() {

        @Override
        public void onRoutePlanYawingSuccess() {

        }

        @Override
        public void onRoutePlanYawingFail() {

        }

        @Override
        public void onRoutePlanSuccess() {
            cbPlanResult("1");
            BNMapController.getInstance().setLayerMode(
                    MapParams.Const.LayerMode.MAP_LAYER_MODE_ROUTE_DETAIL);
            mRoutePlanModel = (RoutePlanModel) NaviDataEngine.getInstance()
                    .getModel(CommonParams.Const.ModelName.ROUTE_PLAN);
        }

        @Override
        public void onRoutePlanFail() {
            cbPlanResult("2");
        }

        @Override
        public void onRoutePlanCanceled() {
            cbPlanResult("3");
        }

        @Override
        public void onRoutePlanStart() {

        }

    };

    private void cbPlanResult(String result){
        JSONObject resultObject=new JSONObject();
        try {
            resultObject.put("resultCode",result);
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_START_ROUTE_PLAN,resultObject.toString());
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
        int calcMode = BNRoutePlaner.getInstance().getCalcMode();
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
        bundle.putInt(BNavConfig.KEY_ROUTEGUIDE_CALCROUTE_MODE, calcMode);
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
        startActivity(intent);
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

    @Override
    public void onHandleMessage(Message message) {
        if(message == null){
            return;
        }
        Bundle bundle=message.getData();
        switch (message.what) {

            case MSG_INIT_BAIDU_NAVI:
                initBaiduNaviMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_START_ROUTE_PLAN:
                startRoutePlanMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_START_NAVI:
                startNaviMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            default:
                super.onHandleMessage(message);
        }
    }

    private void callBackPluginJs(String methodName, String jsonData){
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        onCallback(js);
    }

}
