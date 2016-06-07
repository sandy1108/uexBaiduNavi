package org.zywx.wbpalmstar.plugin.uexbaidunavi;

import java.io.Serializable;

/**
 * Created by ylt on 15/4/7.
 * 
 * @change by waka on 16/04/19
 */
public class GeographyPoint implements Serializable {

	private static final long serialVersionUID = -2439131474614744504L;

	public double longitude;// 经度
	public double latitude;// 纬度

	public GeographyPoint() {

	}

	public GeographyPoint(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	public void set(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
}
