package com.atid.app.myRfid.view.base;

import com.atid.lib.dev.ATRfidManager;
import com.atid.lib.dev.ATRfidReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class ReaderView extends Activity {
	
	protected String TAG = "ReaderView";;
	
	private ATRfidReader reader = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "INFO. onCreate()");

		// Initialize RFID Reader
		this.reader = ATRfidManager.getInstance();
	}

	@Override
	protected void onDestroy() {
		
		Log.i(TAG, "INFO. onDestroy()");
		
		// Stop Operation
		this.reader.stop();
		// Deinitalize RFID reader Instance
		ATRfidManager.onDestroy();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		ATRfidManager.wakeUp();
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		ATRfidManager.sleep();
		super.onStop();
	}

	// Get Reader
	protected ATRfidReader getReader() {
		return this.reader;
	}
}
