package com.atid.app.myRfid.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.atid.app.myRfid.R;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TagListAdapter extends BaseAdapter {

	private static final String TAG = TagListAdapter.class.getSimpleName();

	public static final int PC_LEN = 4;
	private static final int UPDATE_TIME = 500;

	// ------------------------------------------------------------------------
	// Member Variable
	// ------------------------------------------------------------------------

	private LayoutInflater mInflater;

	private ArrayList<TagListItem> mList;
	private HashMap<String, TagListItem> mMap;

	private boolean mIsDisplayPc;
	private boolean mIsVisibleRssi;

	private Handler mHandler;
	private Thread mThread;
	private boolean mIsAliveThread;

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	public TagListAdapter(Context context) {
		super();

		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mList = new ArrayList<TagListItem>();
		mMap = new HashMap<String, TagListItem>();

		mIsDisplayPc = true;
		mIsVisibleRssi = false;

		mHandler = new Handler();
		mThread = null;
		mIsAliveThread = false;
	}

	// ------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------

	public void clear() {
		mList.clear();
		mMap.clear();
		notifyDataSetChanged();
	}

	public boolean isDisplayPc() {
		return mIsDisplayPc;
	}

	public void setDisplayPc(boolean enabled) {
		mIsDisplayPc = enabled;
		notifyDataSetChanged();
	}

	public boolean isVisibleRssi() {
		return mIsVisibleRssi;
	}

	public void setVisibleRssi(boolean visibled) {
		mIsVisibleRssi = visibled;
		notifyDataSetChanged();
	}

	public void start() {
		mThread = new Thread(mUpdateTimerProc);
		mThread.start();

		Log.i(TAG, "INFO. start()");
	}

	public void shutDown() {
		if (mThread == null)
			return;

		mIsAliveThread = false;
		synchronized (mThread) {
			mThread.notify();
		}
		try {
			mThread.join();
		} catch (InterruptedException e) {
			Log.e(TAG, "INFO. shutDown() - Failed to join update thread", e);
		}
		mThread = null;

		Log.i(TAG, "INFO. shutDown()");
	}

	private Runnable mUpdateTimerProc = new Runnable() {

		@Override
		public void run() {
			mIsAliveThread = true;

			while (mIsAliveThread) {
				mHandler.post(mUpdateProc);
				try {
					synchronized (mThread) {
						mThread.wait(UPDATE_TIME);
					}
				} catch (InterruptedException e) {
				}
			}
		}

	};

	private Runnable mUpdateProc = new Runnable() {

		@Override
		public void run() {
			notifyDataSetChanged();
		}

	};

	public void addItem(String tag, double rssi) {
		TagListItem item = null;

		Log.i(TAG, String.format("INFO. addItem([%s], %.1f", tag, rssi));

		if ((item = mMap.get(tag)) == null) {
			item = new TagListItem(tag, rssi);
			mList.add(item);
			mMap.put(tag, item);
		} else {
			item.updateTag(rssi);
		}
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public String getItem(int position) {
		return mList.get(position).getTag();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TagListViewHolder holder;

		if (null == convertView) {
			convertView = mInflater.inflate(R.layout.item_tag_list, parent,
					false);
			holder = new TagListViewHolder(convertView);
		} else {
			holder = (TagListViewHolder) convertView.getTag();
		}
		holder.setItem(mList.get(position), mIsDisplayPc, mIsVisibleRssi);

		return convertView;
	}

	// ------------------------------------------------------------------------
	// Internal Class TagListItem
	// ------------------------------------------------------------------------

	private class TagListItem {

		private String mTag;
		private double mRssi;
		private int mCount;

		public TagListItem(String tag, double rssi) {
			mTag = tag;
			mRssi = rssi;
			mCount = 1;
		}

		public String getTag() {
			return mTag;
		}

		public double getRssi() {
			return mRssi;
		}

		public int getCount() {
			return mCount;
		}

		public void updateTag(double rssi) {
			mRssi = rssi;
			mCount++;
		}
	}

	// ------------------------------------------------------------------------
	// Internal Class TagListViewHolder
	// ------------------------------------------------------------------------

	private class TagListViewHolder {

		private TextView txtTag;
		private TextView txtRssi;
		private TextView txtCount;

		public TagListViewHolder(View parent) {
			txtTag = (TextView) parent.findViewById(R.id.tag_value);
			txtRssi = (TextView) parent.findViewById(R.id.rssi_value);
			txtCount = (TextView) parent.findViewById(R.id.tag_count);
			parent.setTag(this);
		}

		public void setItem(TagListItem item, boolean displayPc,
				boolean visibleRssi) {
			if (displayPc) {
				txtTag.setText(item.getTag());
			} else {
				txtTag.setText(item.getTag().substring(PC_LEN));
			}
			txtRssi.setVisibility(visibleRssi ? View.VISIBLE : View.GONE);
			if (visibleRssi) {
				txtRssi.setText(String.format("%.1f dB", item.getRssi()));
			}
			txtCount.setText(String.format("%d", item.getCount()));
		}
	}
}
