package org.rfcx.src_state;

public class BatteryState {

//	private static final String TAG = BatteryState.class.getSimpleName();
	
	private int level;
	private int scale;
	
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
	
}
