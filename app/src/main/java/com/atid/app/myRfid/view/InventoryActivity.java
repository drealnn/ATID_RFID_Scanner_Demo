package com.atid.app.myRfid.view;

import com.atid.app.myRfid.GlobalInfo;
import com.atid.app.myRfid.R;
import com.atid.app.myRfid.adapter.TagListAdapter;
import com.atid.app.myRfid.view.base.AccessActivity;
import com.atid.app.myRfid.view.base.ActionActivity;
import com.atid.lib.dev.ATRfidReader;
import com.atid.lib.dev.rfid.exception.ATRfidReaderException;
import com.atid.lib.dev.rfid.type.ActionState;
import com.atid.lib.dev.rfid.type.ResultCode;
import com.atid.lib.dev.rfid.type.TagType;

import android.content.Intent;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

public class InventoryActivity extends ActionActivity implements
		OnCheckedChangeListener {

	private static final int READ_MEMORY_ACTIVITY = 1;
	private static final int WRITE_MEMORY_ACTIVITY = 2;
	private static final int LOCK_MEMORY_ACTIVITY = 3;

	// ------------------------------------------------------------------------
	// Member Variable
	// ------------------------------------------------------------------------

	private ListView lstTags;
	private CheckBox chkDisplayPc;
	private CheckBox chkContinuousMode;
	private CheckBox chkReportRssi;

	private TextView txtCount;
	private Button btnAction;

	private TagListAdapter adpTags;

	private MenuItem mnuReadMemory;
	private MenuItem mnuWriteMemory;
	private MenuItem mnuLockMemory;

	private boolean mIsReportRssi;

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	public InventoryActivity() {
		super();

		TAG = InventoryActivity.class.getSimpleName();
		mView = R.layout.activity_inventory;

		mIsReportRssi = false;
	}

	// ------------------------------------------------------------------------
	// Activity Event Handler
	// ------------------------------------------------------------------------

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.continue_mode:
			GlobalInfo.setContinuousMode(chkContinuousMode.isChecked());
			break;
		case R.id.display_pc:
			GlobalInfo.setDisplayPc(chkDisplayPc.isChecked());
			adpTags.setDisplayPc(GlobalInfo.isDisplayPc());
			break;
		case R.id.report_rssi:
			adpTags.setVisibleRssi(chkReportRssi.isChecked());
			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.tag_list
				&& mReader.getAction() == ActionState.Stop) {
			getMenuInflater().inflate(R.menu.context_menu, menu);

			mnuReadMemory = menu.findItem(R.id.read_memory);
			mnuWriteMemory = menu.findItem(R.id.write_memory);
			mnuLockMemory = menu.findItem(R.id.lock_memory);

			switch (getTagType()) {
			case Tag6C:
				mnuReadMemory.setVisible(true);
				mnuWriteMemory.setVisible(true);
				mnuLockMemory.setVisible(true);
				break;
			case Tag6B:
				mnuReadMemory.setVisible(true);
				mnuWriteMemory.setVisible(false);
				mnuLockMemory.setVisible(false);
				break;
			default:
				mnuReadMemory.setVisible(false);
				mnuWriteMemory.setVisible(false);
				mnuLockMemory.setVisible(false);
				break;
			}
			enableWidgets(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent intent;
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		int position = menuInfo.position;

		if (position < 0)
			return false;

		enableWidgets(false);
		mnuReadMemory.setEnabled(false);
		mnuWriteMemory.setEnabled(false);
		mnuLockMemory.setEnabled(false);

		String tag = adpTags.getItem(position);

		switch (item.getItemId()) {
		case R.id.read_memory:
			intent = new Intent(this, ReadMemoryActivity.class);
			intent.putExtra(AccessActivity.KEY_EPC, tag);
			startActivityForResult(intent, READ_MEMORY_ACTIVITY);
			break;
		case R.id.write_memory:
			intent = new Intent(this, WriteMemoryActivity.class);
			intent.putExtra(AccessActivity.KEY_EPC, tag);
			startActivityForResult(intent, WRITE_MEMORY_ACTIVITY);
			break;
		case R.id.lock_memory:
			intent = new Intent(this, LockMemoryActivity.class);
			intent.putExtra(AccessActivity.KEY_EPC, tag);
			startActivityForResult(intent, LOCK_MEMORY_ACTIVITY);
			break;
		}
		return true;
	}

	@Override
	public void onContextMenuClosed(Menu menu) {
		enableWidgets(true);
	}

	// ------------------------------------------------------------------------
	// Reader Event Handler
	// ------------------------------------------------------------------------

	@Override
	public void onReaderActionChanged(ATRfidReader reader, ActionState action) {

		if (action == ActionState.Stop) {
			adpTags.shutDown();
		} else {
			adpTags.start();
		}

		enableWidgets(true);

		Log.i(TAG, String.format("EVENT. onReaderActionchanged(%s)", action));
	}

	@Override
	public void onReaderReadTag(ATRfidReader reader, String tag, float rssi) {

		adpTags.addItem(tag, rssi);
		txtCount.setText(String.format("%d", adpTags.getCount()));
		playSuccess();

		Log.i(TAG,
				String.format("EVENT. onReaderReadTag([%s], %.2f)", tag, rssi));
	}

	// ------------------------------------------------------------------------
	// Reader Control Methods
	// ------------------------------------------------------------------------
	
	// Start Action
	protected void startAction() {

		ResultCode res;
		TagType tagType = getTagType();

		enableWidgets(false);

		if (chkContinuousMode.isChecked()) {
			// Multi Reading
			switch (tagType) {
			case Tag6C:
				if ((res = mReader.inventory6cTag()) != ResultCode.NoError) {
					Log.e(TAG,
							String.format(
									"ERROR. startAction() - Failed to start inventory 6C tag [%s]",
									res));
					enableWidgets(true);
					return;
				}
				break;
			case Tag6B:
				if ((res = mReader.inventory6bTag()) != ResultCode.NoError) {
					Log.e(TAG,
							String.format(
									"ERROR. startAction() - Failed to start inventory 6B tag [%s]",
									res));
					enableWidgets(true);
					return;
				}
				break;
			}
		} else {
			// Single Reading
			switch (tagType) {
			case Tag6C:
				if ((res = mReader.readEpc6cTag()) != ResultCode.NoError) {
					Log.e(TAG,
							String.format(
									"ERROR. startAction() - Failed to start read 6C tag [%s]",
									res));
					enableWidgets(true);
					return;
				}
				break;
			case Tag6B:
				if ((res = mReader.readEpc6bTag()) != ResultCode.NoError) {
					Log.e(TAG,
							String.format(
									"ERROR. startAction() - Failed to start read 6B tag [%s]",
									res));
					enableWidgets(true);
					return;
				}
				break;
			}
		}
		Log.i(TAG, "INFO. startAction()");
	}

	// ------------------------------------------------------------------------
	// Override Widgets Control Methods
	// ------------------------------------------------------------------------

	@Override
	// Clear Widgets
	protected void clear() {
		adpTags.clear();
		txtCount.setText(String.format("%d", adpTags.getCount()));

		Log.i(TAG, "INFO. clear()");
	}

	// Initialize Activity Widgets
	@Override
	protected void initWidgets() {
		super.initWidgets();

		// Initialize Tag List View
		lstTags = (ListView) findViewById(R.id.tag_list);
		adpTags = new TagListAdapter(this);
		lstTags.setAdapter(adpTags);
		lstTags.setOnItemSelectedListener(this);
		registerForContextMenu(lstTags);

		// Display PC Check Box
		chkDisplayPc = (CheckBox) findViewById(R.id.display_pc);
		chkDisplayPc.setOnCheckedChangeListener(this);

		// Continuous Mode Check Box
		chkContinuousMode = (CheckBox) findViewById(R.id.continue_mode);
		chkContinuousMode.setOnCheckedChangeListener(this);

		// Display RSSI Check Box
		chkReportRssi = (CheckBox) findViewById(R.id.report_rssi);
		chkReportRssi.setOnCheckedChangeListener(this);

		// Tag Count
		txtCount = (TextView) findViewById(R.id.tag_count);

		// Action Button
		btnAction = (Button) findViewById(R.id.action);
		btnAction.setOnClickListener(this);

		Log.i(TAG, "INFO. initWidgets()");
	}

	// Eanble Activity Widgets
	@Override
	protected void enableWidgets(boolean enabled) {
		super.enableWidgets(enabled);

		if (mReader.getAction() == ActionState.Stop) {
			chkDisplayPc.setEnabled(enabled);
			chkContinuousMode.setEnabled(enabled);
			chkReportRssi.setEnabled(enabled);
			btnAction.setText(R.string.action_inventory);
		} else {
			chkDisplayPc.setEnabled(false);
			chkContinuousMode.setEnabled(false);
			chkReportRssi.setEnabled(false);
			btnAction.setText(R.string.action_stop);
		}
		btnAction.setEnabled(enabled);
	}

	// Initialize Reader
	@Override
	protected void initReader() {
		super.initReader();

		// Get Report RSSI
		try {
			mIsReportRssi = mReader.getReportRssi();
		} catch (ATRfidReaderException e) {
			Log.e(TAG, String.format(
					"ERROR. initReader() - Failed to get report RSSI [%s]",
					e.getCode()), e);
		}
		Log.i(TAG, String.format("INFO. initReader() - [Report RSSI : %s]",
				mIsReportRssi));

		Log.i(TAG, "INFO initReader()");
	}

	// Activated Reader
	@Override
	protected void activateReader() {
		super.activateReader();

		chkDisplayPc.setChecked(GlobalInfo.isDisplayPc());
		chkContinuousMode.setChecked(GlobalInfo.isContinuousMode());
		chkReportRssi.setChecked(mIsReportRssi);
		adpTags.setDisplayPc(GlobalInfo.isContinuousMode());
		adpTags.setVisibleRssi(chkReportRssi.isChecked());

		enableWidgets(true);

		Log.i(TAG, "INFO. activateReader()");
	}
}
