package com.atid.app.myRfid.view;

import com.atid.app.myRfid.R;
import com.atid.app.myRfid.adapter.MemoryListAdapter;
import com.atid.app.myRfid.adapter.SpinnerAdapter;
import com.atid.app.myRfid.view.base.ReadWriteMemoryActivity;
import com.atid.lib.dev.ATRfidReader;
import com.atid.lib.dev.rfid.type.ActionState;
import com.atid.lib.dev.rfid.type.BankType;
import com.atid.lib.dev.rfid.type.ResultCode;
import com.atid.lib.dev.rfid.type.TagType;

import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

public class ReadMemoryActivity extends ReadWriteMemoryActivity {

	// ------------------------------------------------------------------------
	// Member Variable
	// ------------------------------------------------------------------------

	private ListView lstReadValue;
	private Spinner spnLength;
	private Button btnAction;

	private MemoryListAdapter adpReadValue;
	private SpinnerAdapter adpLength;

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	public ReadMemoryActivity() {
		super();

		TAG = ReadMemoryActivity.class.getSimpleName();
		mView = R.layout.activity_read_memory;
	}

	// ------------------------------------------------------------------------
	// Reader Event Handler
	// ------------------------------------------------------------------------

	@Override
	public void onReaderResult(ATRfidReader reader, ResultCode code,
			ActionState action, String epc, String data) {
		super.onReaderResult(reader, code, action, epc, data);
		
		if (code != ResultCode.NoError) {
			adpReadValue.clear();
		} else {
			int offset = getOffset();
			adpReadValue.setOffset(offset);
			adpReadValue.setValue(data);
		}
		
		Log.i(TAG, String.format("EVENT. onReaderResult(%s, %s, [%s], [%s]",
				code, action, epc, data));
	}

	// ------------------------------------------------------------------------
	// Reader Control Methods
	// ------------------------------------------------------------------------

	// Start Action
	@Override
	protected void startAction() {
		
		ResultCode res;
		TagType tagType = getTagType();
		BankType bank;
		int offset = getOffset();
		int length = getLength();
		String password = getPassword();
		
		clear();
		enableWidgets(false);
		
		switch (tagType) {
		case Tag6C:
			bank = getBank();
			if ((res = mReader.readMemory6c(bank, offset, length, password)) != ResultCode.NoError) {
				Log.e(TAG,
						String.format(
								"ERROR. startAction() - Failed to read memory 6C tag [%s]",
								res));
				enableWidgets(true);
				return;
			}
			break;
		case Tag6B:
			if ((res = mReader.readMemory6b(offset, length)) != ResultCode.NoError) {
				Log.e(TAG,
						String.format(
								"ERROR. startAction() - Failed to read memory 6B tag [%s]",
								res));
				enableWidgets(true);
				return;
			}
			break;
		}
		
		Log.i(TAG, "INFO. startAction()");
	}

	// ------------------------------------------------------------------------
	// Override Widgets Control Methods
	// ------------------------------------------------------------------------

	// Clear Widgets
	@Override
	protected void clear() {
		super.clear();
		
		adpReadValue.clear();
	}

	// Initialize Activity Widgets
	@Override
	protected void initWidgets() {
		super.initWidgets();
		
		// Initialize Read Value
		lstReadValue = (ListView)findViewById(R.id.read_memory);
		adpReadValue = new MemoryListAdapter(this);
		lstReadValue.setAdapter(adpReadValue);
		
		// Initialize Length
		spnLength = (Spinner)findViewById(R.id.length);
		adpLength = new SpinnerAdapter(this, android.R.layout.simple_list_item_1);
		for (int i = 1; i <= MAX_ADDRESS; i++) {
			adpLength.addItem(i, String.format("%d word", i));
		}
		spnLength.setAdapter(adpLength);
		spnLength.setOnItemSelectedListener(this);
		
		// Initialize Action Button
		btnAction = (Button)findViewById(R.id.action);
		btnAction.setOnClickListener(this);
		
		setBank(BankType.EPC);
		setOffset(2);
		setLength(2);
	}

	// Eanble Activity Widgets
	@Override
	protected void enableWidgets(boolean enabled) {
		super.enableWidgets(enabled);
		
		if (mReader.getAction() == ActionState.Stop) {
			lstReadValue.setEnabled(enabled);
			spnLength.setEnabled(enabled);
			btnAction.setText(R.string.action_read);
		} else {
			lstReadValue.setEnabled(false);
			spnLength.setEnabled(false);
			btnAction.setText(R.string.action_stop);
		}
		btnAction.setEnabled(enabled);
	}

	// Get Length
	private int getLength() {
		int position = spnLength.getSelectedItemPosition();
		return adpLength.getValue(position);
	}
	
	private void setLength(int length) {
		int position = adpLength.indexOf(length);
		spnLength.setSelection(position);
	}
}
