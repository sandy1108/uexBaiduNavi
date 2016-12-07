package org.zywx.wbpalmstar.plugin.uexbaidunavi.util;

import com.baidu.navisdk.adapter.BaiduNaviManager;

import org.zywx.wbpalmstar.base.BDebug;

import java.io.File;

/**
 * Util
 *
 * @author waka
 * @version createTime:2016年4月19日 下午5:08:25
 */
public class Util {

    /**
     * 检查文件夹路径是否存在，不存在则创建
     *
     * @param folderPath
     * @return 返回创建结果
     */
    public static boolean checkFolderPath(String folderPath) {

        File file = new File(folderPath);
        if (!file.exists()) {
            BDebug.i("file.exists() == false");
            return file.mkdirs();
        }
        BDebug.i("file.exists() == true");
        return true;
    }

    /**
     * 修正算路偏好常量
     *
     * @param preferenceMode
     * @return
     */
    public static int amendPreferenceMode(int preferenceMode) {

        int amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;

        switch (preferenceMode) {

            // 1.推荐
            case 1:
                amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;
                break;

            // 2.高速优先
            case 2:
                amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME;
                break;

            // 3.少走高速
            case 3:
                amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST;
                break;

            // 8.少收费
            case 8:
                amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL;
                break;

            // 16.躲避拥堵
            case 16:
                amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM;
                break;

            default:
                amendMode = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;
                break;
        }

        return amendMode;
    }
}
