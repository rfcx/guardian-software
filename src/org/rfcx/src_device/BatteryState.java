package org.rfcx.src_device;

import org.rfcx.rfcx_src_android.RfcxSource;

public class BatteryState {
	
	private int level;
	private int scale;
	private int temperature;
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setScale(int scale) {
		this.scale = scale;
	}
	
	public int getLevel() {
		return level;
	}
	
	public int getScale() {
		return scale;
	}
	
	public int getPercent() {
		return Math.round(100 * this.level / (float) this.scale);
	}
	
	public int getTemperature() {
		return temperature;
	}
	
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}
}
