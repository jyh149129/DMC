package com.cy.dlna.activity;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.model.meta.Device;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.cy.dlna.util.LogTool;

public class CyGetSourceActivity extends Activity {

	private ListView lv;
	private List<Device> serviceList;
	private Button button;

	public void onCreate(Bundle saved) {
		super.onCreate(saved);

		setContentView(R.layout.activity_getdevice);
		init();

		LogTool.i("MainActivity.deviceList.size == "
				+ MainActivity.deviceList.size());
		LogTool.i("playerList.size == " + serviceList.size());
		if (serviceList.size() > 0) {
			lv.setAdapter(new MyAdapter(this));
			lv.setOnItemClickListener(new MyListener());
		}

	}

	private void init() {
		button = (Button) findViewById(R.id.btFinish);
		lv = (ListView) findViewById(R.id.lvGetDevice);
		serviceList = new ArrayList<Device>();

		if (MainActivity.deviceList == null
				&& MainActivity.deviceList.size() == 0) {
			button.setVisibility(View.VISIBLE);
			return;
		}

		for (Device d : MainActivity.deviceList) {
			if (d.getType().getType().equals(MainActivity.DEVICETYPE_SERVICE)) {
				serviceList.add(d);
			}
		}

		if (serviceList.size() == 0) {
			button.setVisibility(View.VISIBLE);
		}

	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btFinish:
			setResult(MainActivity.REQUEST_CODE_GET_SOURCE_RESULT_FAIL, null);
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			setResult(MainActivity.REQUEST_CODE_GET_SOURCE_RESULT_FAIL, null);
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private class MyListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long arg3) {
			int len = serviceList.size();
			if (len > 0 && position >= 0 && position < len) {
				MainActivity.cyDeviceService = serviceList.get(position);
				setResult(MainActivity.REQUEST_CODE_GET_SOURCE_RESULT_SUCCESS,
						null);
				finish();
			}
		}
	}

	private class MyAdapter extends BaseAdapter {
		private Context context;

		public MyAdapter(Context context) {
			this.context = context;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return serviceList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return serviceList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Device d = serviceList.get(position);
			LayoutInflater flater = LayoutInflater.from(context);

			if (convertView == null) {
				convertView = flater.inflate(R.layout.lv_item_source, null);
			}
			TextView tv = (TextView) convertView
					.findViewById(R.id.tv_lv_device);

			tv.setText(d.getDetails().getFriendlyName());

			return convertView;
		}

	}

}
