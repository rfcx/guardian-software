package org.rfcx.src_database;

import android.content.Context;

public class TransmitDb {

	public TransmitDb(Context context) {
		
	}
	
	private static final String TAG = TransmitDb.class.getSimpleName();
	static final int VERSION = 1;
	static final String DATABASE = "transmit";
	static final String C_CREATED_AT = "created_at";
	static final String C_SUCCESS = "success";
	static final String CREATE_CLMNS = "(" + C_CREATED_AT + " DATETIME, " + C_SUCCESS + " BOOLEAN " + ")";
	
	// log of all attempted transmissions
	public class DbTransmitLog {
	
	}
}
