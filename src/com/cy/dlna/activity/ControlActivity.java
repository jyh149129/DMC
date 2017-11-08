package com.cy.dlna.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ControlActivity extends Activity implements OnClickListener{

	private TextView share_prompt;
	private ImageView btn_prev,top,btn_next,buttom,
	        vol_down_button,vol_up_button,
	        fr_button,ff_button,play_pause_button;
	private String DeviceName;
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		setContentView(R.layout.sky_pai_share);
		getTakeOut();
		findView();
		setListener();
	}
	
	private void getTakeOut() {
		DeviceName = getIntent().getStringExtra("upnp_devices");
	}
	
	private void findView() {
		share_prompt = (TextView) findViewById(R.id.share_prompt); //设备名
		btn_prev = (ImageView) findViewById(R.id.btn_prev); // 向左
		top = (ImageView) findViewById(R.id.top); //向上
		btn_next = (ImageView) findViewById(R.id.btn_next); //向右
		buttom = (ImageView) findViewById(R.id.buttom); // 向下
		vol_down_button = (ImageView) findViewById(R.id.vol_down_button); // 声音减少
		vol_up_button = (ImageView) findViewById(R.id.vol_up_button);  // 声音增大
		fr_button = (ImageView) findViewById(R.id.fr_button); //快退
		ff_button = (ImageView) findViewById(R.id.ff_button); // 快进
		play_pause_button = (ImageView) findViewById(R.id.play_pause_button); // 暂停
		share_prompt.setText(DeviceName);
	}
	
	private void setListener() {
		btn_prev.setOnClickListener(this);
		top.setOnClickListener(this);
		btn_next.setOnClickListener(this);
		buttom.setOnClickListener(this);
		vol_down_button.setOnClickListener(this);
		vol_up_button.setOnClickListener(this);
		fr_button.setOnClickListener(this);
		ff_button.setOnClickListener(this);
		play_pause_button.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		switch (view.getId()) {
		case R.id.btn_prev:
			
			break;
		case R.id.top :
			
			break;
		case R.id.btn_next:
			
			break;
		case R.id.buttom:
			
			break;
		case R.id.vol_down_button:
			
			break;
		case R.id.vol_up_button:
			
			break;
		case R.id.fr_button:
			
			break;
		case R.id.ff_button:
			
			break;
		case R.id.play_pause_button:
			
			break;
			
		default:
			break;
		}
	}
}
