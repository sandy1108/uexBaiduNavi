package org.zywx.wbpalmstar.plugin.uexbaidunavi;

import org.zywx.wbpalmstar.plugin.uexbaidunavi.util.MLog;

import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviBaseCallbackModel;
import com.baidu.navisdk.adapter.BaiduNaviCommonModule;
import com.baidu.navisdk.adapter.NaviModuleFactory;
import com.baidu.navisdk.adapter.NaviModuleImpl;
import com.baidu.navisdk.adapter.BNRouteGuideManager.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

/**
 * 百度导航Activity
 * 
 * @author waka
 *
 */
public class BNavigatorActivity extends Activity {

	/**
	 * 获得传过来的算路节点
	 */
	@SuppressWarnings("unused")
	private BNRoutePlanNode mBNRoutePlanNode = null;

	/**
	 * 百度导航公共模块
	 */
	private BaiduNaviCommonModule mBaiduNaviCommonModule = null;

	/**
	 * 是否使用通用接口
	 * 
	 * 对于导航模块有两种方式来实现发起导航。 1：使用通用接口来实现 2：使用传统接口来实现
	 */
	private boolean useCommonInterface = true;

	/**
	 * 导航监听器
	 */
	private OnNavigationListener mOnNavigationListener = new OnNavigationListener() {

		// 通用动作回调接口，避免接口总是变动
		@Override
		public void notifyOtherAction(int actionType, int arg1, int arg2, Object obj) {

			if (actionType == 0) {
				MLog.getIns().i("notifyOtherAction actionType = " + actionType + ",导航到达目的地！");
			}

			MLog.getIns().i("actionType:" + actionType + "arg1:" + arg1 + "arg2:" + arg2 + "obj:" + obj.toString());
		}

		// 导航过程结束
		@Override
		public void onNaviGuideEnd() {

			// 返回数据给上一个Activity
			Intent intent = new Intent();
			setResult(RESULT_OK, intent);// 正常结束
			finish();
		}
	};

	/**
	 * onCreate
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MLog.getIns().i("");

		View view = null;

		if (useCommonInterface) {

			// 使用通用接口
			mBaiduNaviCommonModule = NaviModuleFactory.getNaviModuleManager().getNaviCommonModule(NaviModuleImpl.BNaviCommonModuleConstants.ROUTE_GUIDE_MODULE, this,
					BNaviBaseCallbackModel.BNaviBaseCallbackConstants.CALLBACK_ROUTEGUIDE_TYPE, mOnNavigationListener);
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onCreate();
				view = mBaiduNaviCommonModule.getView();
			}

		} else {

			// 使用传统接口
			view = BNRouteGuideManager.getInstance().onCreate(this, mOnNavigationListener);
		}

		// 设置内容View
		if (view != null) {
			setContentView(view);
		}

		// 获取传过来的节点，暂时没什么用
		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				mBNRoutePlanNode = (BNRoutePlanNode) bundle.getSerializable(Constant.ROUTE_PLAN_NODE);
			}
		}
	}

	/**
	 * onPause
	 */
	@Override
	protected void onPause() {
		super.onPause();

		MLog.getIns().i("");

		if (useCommonInterface) {
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onPause();
			}
		} else {
			BNRouteGuideManager.getInstance().onPause();
		}
	}

	/**
	 * onResume
	 */
	@Override
	protected void onResume() {
		super.onResume();

		MLog.getIns().i("");

		if (useCommonInterface) {
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onResume();
			}
		} else {
			BNRouteGuideManager.getInstance().onResume();
		}
	}

	/**
	 * onStop
	 */
	@Override
	protected void onStop() {
		super.onStop();

		MLog.getIns().i("");

		if (useCommonInterface) {
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onStop();
			}
		} else {
			BNRouteGuideManager.getInstance().onStop();
		}
	}

	/**
	 * onDestroy
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		MLog.getIns().i("");

		if (useCommonInterface) {
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onDestroy();
			}
		} else {
			BNRouteGuideManager.getInstance().onDestroy();
		}

		Intent intent = new Intent(Constant.LOCAL_BROADCAST_BNAVIGATOR_ACTIVITY_DESTORY);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	/**
	 * onBackPressed
	 */
	@Override
	public void onBackPressed() {

		MLog.getIns().i("");

		if (useCommonInterface) {
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onBackPressed(false);
			}
		} else {
			BNRouteGuideManager.getInstance().onBackPressed(false);
		}
	}

	/**
	 * onConfigurationChanged
	 */
	@Override
	public void onConfigurationChanged(android.content.res.Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		MLog.getIns().i("");

		if (useCommonInterface) {
			if (mBaiduNaviCommonModule != null) {
				mBaiduNaviCommonModule.onConfigurationChanged(newConfig);
			}
		} else {
			BNRouteGuideManager.getInstance().onConfigurationChanged(newConfig);
		}
	}

}
