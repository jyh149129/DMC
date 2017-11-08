package com.cy.dlna.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class LoadingActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loading);
		th.start();
	}

	protected void onRestart() {
		super.onRestart();
		exit();
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	public void exit() {
		finish();
		// Runtime.getRuntime().exit(0);

		exitThread.start();
	}

	/*
	 * CN:结束虚拟机，解决再次进入时闪退现象；延迟是为了注销UPNPService工作的完成
	 */
	Thread exitThread = new Thread() {
		public void run() {
			try {
				sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Runtime.getRuntime().exit(0);
			}
		};

	};

	Thread th = new Thread() {
		public void run() {
			try {
				sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Intent i = new Intent(LoadingActivity.this, MainActivity.class);
				startActivity(i);
			}
		};
	};

}
