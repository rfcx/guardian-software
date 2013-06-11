package org.rfcx.src_sleep;

import org.rfcx.src_audio.AudioState;

public class SleepState {

	private static final String TAG = SleepState.class.getSimpleName();

	public static final boolean SLEEP_TIMER_ENABLED = true;
	
	private boolean isSleeping = false;
	
	public void setSleep(boolean isSleeping) {
		this.isSleeping = isSleeping;
	}
	
	public boolean isSleeping() {
		return this.isSleeping;
	}
	
}
