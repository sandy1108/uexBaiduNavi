package org.zywx.wbpalmstar.plugin.uexbaidunavi;

/**
 * 常量类
 * 
 * @author waka
 * @version createTime:2016年4月20日 上午9:12:05
 */
public class Constant {

	// 储存在SD卡下的文件夹名称
	public static final String APP_FOLDER_NAME = "BaiduNavi";

	// 传递给导航Activity的数据key值
	public static final String ROUTE_PLAN_NODE = "routePlanNode";// 算路节点

	// RequestCode
	public static final int REQUEST_CODE_BNAVIGATOR_ACTIVITY = 10001;// 诱导Activity的RequestCode

	// 本地广播
	public static final String LOCAL_BROADCAST_BNAVIGATOR_ACTIVITY_DESTORY = "org.zywx.wbpalmstar.widgetone.uexbaidunavi.LOCAL_BROADCAST_BNAVIGATOR_ACTIVITY_DESTORY";// 诱导Activity关闭时发出的广播
}
