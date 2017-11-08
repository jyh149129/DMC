package com.cy.dlna.activity;

import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.support.avtransport.callback.GetMediaInfo;
import org.teleal.cling.support.avtransport.callback.GetPositionInfo;
import org.teleal.cling.support.avtransport.callback.GetTransportInfo;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.contentdirectory.ui.ContentBrowseActionCallback;
import org.teleal.cling.support.model.MediaInfo;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.TransportInfo;
import org.teleal.cling.support.model.TransportState;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.R.bool;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.util.TypedProperties.ParseException;
import com.cy.dlna.util.LogTool;

public class CySourceListActivity extends Activity {

	private Device playerDevice;
	private Device serviceDevice;
	private ListView lv;
	private AndroidUpnpService upnpService;

	private ActionCallback setURIAction;
	private ActionCallback playAction;
	private ActionCallback stopAction;
	private ActionCallback positionAction;
	private ActionCallback transportAction;
	private ActionCallback pauseAction;

	private ActionCallback getVolumeAction;
	private ActionCallback setVolumeAction;
	private ActionCallback MediaInfoAction;
	private ActionCallback setSeekAction;

	private ImageView ibPlayOrPause;

	Service sourceService;
	Service playerService;
	Service rendererService;

	private ContentItem mContentItem;
	private ArrayAdapter<ContentItem> contentListAdapter;
	/*
	 * 储存打开过的目录，以便返回，API里面为找到直接返回上级目录的方法。
	 */
	private Map<Integer, ContentItem> mContentItemMap;
	private int foldLever = 0;

	private static final int MSG_INIT_ACTIVES = 1;
	private static final int MSG_BOTTON_PLAY = 10;
	private static final int MSG_BOTTON_PAUSE = 11;
	private static final int MSG_SET_VOLUME = 20;
	private static final int MSG_GET_MEDIAINFO = 30;
	private static final int MSG_GET_SEEKINFO = 40;

	private static final String TRANSPORT_STATE_STOPPED = "STOPPED";
	private static final String TRANSPORT_STATE_PLAYING = "PLAYING";
	private static final String TRANSPORT_STATE_PAUSED_PLAYBACK = "PAUSED_PLAYBACK";

	private static final String UDA_SERVICE_ID_AV_TRANSPORT = "AVTransport";
	private static final String UDA_SERVICE_ID_RENDERING_CONTROL = "RenderingControl";
	

	private static long volumeSet = 0;
	private static boolean volumeUp;

	private boolean canPlayNext = false;

	private static final int TIMEOUT_INIT_ACTIVES = 1000;
	
	private PositionInfo position;
	
	private MediaInfo mediaInfo;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_INIT_ACTIVES:
				try {
					initPlayActions();
				} catch (java.lang.IllegalArgumentException e) {
					e.printStackTrace();
					LogTool.i(playerService.getClass().toString());
					initService();
					mHandler.sendEmptyMessageDelayed(MSG_INIT_ACTIVES,
							TIMEOUT_INIT_ACTIVES);
				}
				break;
			case MSG_BOTTON_PLAY:
				ibPlayOrPause.setImageResource(R.drawable.ib_start);
				break;
			case MSG_BOTTON_PAUSE:
				ibPlayOrPause.setImageResource(R.drawable.ib_pause);
				break;
			case MSG_SET_VOLUME:
				changeVolume(volumeSet);
				break;
			case MSG_GET_MEDIAINFO :
				upnpService.getControlPoint().execute(positionAction);
				break;
			case MSG_GET_SEEKINFO:
				changeSeekPostion(fr_ff);
				break;
			default:
				break;
			}
		};
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sourcelist);

		init();

		cyOpenFolder(0);
		mHandler.sendEmptyMessageDelayed(MSG_INIT_ACTIVES, TIMEOUT_INIT_ACTIVES);
	}

	public void onDestroy() {
		super.onDestroy();
		cyStop(); 
	}

	private void init() {
		serviceDevice = MainActivity.cyDeviceService;
		playerDevice = MainActivity.cyDevicePlayer;
		upnpService = MainActivity.upnpService;
		initService();
		ibPlayOrPause = (ImageView) findViewById(R.id.btPlayOrPause);
		lv = (ListView) findViewById(R.id.lvGetSource);
		contentListAdapter = new MyAdapter(this,
				android.R.layout.simple_list_item_1);
		lv.setAdapter(contentListAdapter);
		lv.setOnItemClickListener(new MyListener());
		mContentItemMap = new HashMap<Integer, ContentItem>();
	}

	private void initService() {
		playerService = playerDevice.findService(new UDAServiceId(
				UDA_SERVICE_ID_AV_TRANSPORT));
		rendererService = playerDevice.findService(new UDAServiceId(
				UDA_SERVICE_ID_RENDERING_CONTROL));

		sourceService = serviceDevice.findService(new UDAServiceType(
				"ContentDirectory"));
	}

	private void playNext(boolean isNext) {
		int count = contentListAdapter.getCount();
		if (count <= 0)
			return;
		int position = contentListAdapter.getPosition(mContentItem);

		int nextPosition = getNextPosition(count, position, isNext);
		mContentItem = contentListAdapter.getItem(nextPosition);
		cyPlay();
	}

	private int getNextPosition(int count, int position, boolean isNext) {
		if (isNext) {
			position++;
			if (position == count)
				position = 0;
		} else {
			position--;
			if (position == -1)
				position = (count - 1);
		}
		return position;
	}

	private void cyPlay() {
		String uri = mContentItem.getItem().getFirstResource().getValue();
		setURIAction = new SetAVTransportURI(playerService, uri, mContentItem
				.getItem().toString()) {
			public void failure(ActionInvocation invocation,
					UpnpResponse operation, String defaultMsg) {
				LogTool.e(" setAVTransportURIAction failure");
			}

			public void success(ActionInvocation invocation) {
				super.success(invocation);
				LogTool.i(" setAVTransportURIAction success");
				upnpService.getControlPoint().execute(playAction);
			}
		};
		upnpService.getControlPoint().execute(setURIAction);

	}

	private void cyPause() {
		if (pauseAction != null)
			upnpService.getControlPoint().execute(pauseAction);
	}

	private void cyStop() {
		if (stopAction != null)
			upnpService.getControlPoint().execute(stopAction);
		mHandler.sendEmptyMessage(MSG_BOTTON_PLAY);
	}

	private void cyPlayOrPause() {
		if (transportAction != null)
			upnpService.getControlPoint().execute(transportAction);
	}

	private boolean fr_ff;
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btLast:
			if (canPlayNext)
				playNext(false);
			break;
		case R.id.btNext:
			if (canPlayNext)
				playNext(true);
			break;
		case R.id.btPlayOrPause:
			LogTool.i("cyPlayOrPause clicked");
			cyPlayOrPause();
			break;
		case R.id.btStop:
			cyStop();
			break;
		case R.id.bt_vo_down:
			fr_ff = false;
			upnpService.getControlPoint()
			  .execute(MediaInfoAction);
			break;
		case R.id.bt_vo_up:
			fr_ff = true;
			upnpService.getControlPoint()
			  .execute(MediaInfoAction);
		default:
			break;
		}
	}

	protected Container createRootContainer(Service service) {
		Container rootContainer = new Container();
		rootContainer.setId("0");
		rootContainer.setTitle("Content Directory on "
				+ service.getDevice().getDisplayString());
		return rootContainer;
	}

	/*
	 * 创建资源目录
	 */
	private void cyOpenFolder(int lever) {
		if (lever == 0) {
			upnpService.getControlPoint().execute(
					new ContentBrowseActionCallback(CySourceListActivity.this,
							sourceService, createRootContainer(sourceService),
							contentListAdapter));
		} else {
			mContentItem = mContentItemMap.get(lever);
			upnpService.getControlPoint().execute(
					new ContentBrowseActionCallback(CySourceListActivity.this,
							mContentItem.getService(), mContentItem
									.getContainer(), contentListAdapter));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (foldLever > 0) {
				foldLever--;
				cyOpenFolder(foldLever);
			} else {
				finish();
			}
			canPlayNext = false;
			return true;

		case KeyEvent.KEYCODE_VOLUME_UP:
			volumeUp = true;
			upnpService.getControlPoint().execute(getVolumeAction);
			return true;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			volumeUp = false;
			upnpService.getControlPoint().execute(getVolumeAction);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	/*
	 * Actions need startup delayer,or playerService is null.
	 */
	private void initPlayActions() {
		LogTool.i(playerService.getClass().toString());
		if (playerService == null) {
			LogTool.i("playerService is null");
			mHandler.sendEmptyMessageDelayed(MSG_INIT_ACTIVES,
					TIMEOUT_INIT_ACTIVES);
			return;
		}
		playAction = new Play(playerService) {
			public void failure(ActionInvocation invocation,
					UpnpResponse operation, String defaultMsg) {
				LogTool.e(" playAction failure");
			}

			public void success(ActionInvocation invocation) {
				super.success(invocation);
				LogTool.i(" playAction success");
				mHandler.sendEmptyMessage(MSG_BOTTON_PAUSE);
			}
		};
		stopAction = new Stop(playerService) {
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				LogTool.i("stop action fail ");
			}
		};
		transportAction = new GetTransportInfo(playerService) {
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				LogTool.e("GetTransportInfo failure");
			}

			public void received(ActionInvocation invocation,
					TransportInfo transportInfo) {

				if (transportInfo.getCurrentTransportState() == TransportState.PLAYING) {
					LogTool.i("playing state to pause ");
					cyPause();
				} else if (transportInfo.getCurrentTransportState() == TransportState.PAUSED_PLAYBACK) {
					upnpService.getControlPoint().execute(playAction);
				} else {
					if (mContentItem != null && !mContentItem.isContainer()) {
						cyPlay();
					}
				}
				String state = transportInfo.getCurrentTransportState()
						.getValue();
				LogTool.i("TransportInfo  getCurrentTransportState == "
						+ transportInfo.getCurrentTransportState());
			}
		};
		pauseAction = new Pause(playerService) {
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				LogTool.e("pauseAction  failure");
			}

			@Override
			public void success(ActionInvocation invocation) {
				// TODO Auto-generated method stub
				super.success(invocation);
				mHandler.sendEmptyMessage(MSG_BOTTON_PLAY);
			}
		};
		positionAction = new GetPositionInfo(playerService) {
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				LogTool.e("positionAction  failure");
			}

			public void received(ActionInvocation invocation,
					PositionInfo positionInfo) {
				position = positionInfo;
				LogTool.e(position.toString());
				mHandler.sendEmptyMessage(MSG_GET_SEEKINFO);
			}
		};
		
		MediaInfoAction = new GetMediaInfo(playerService) {
			
			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
				// TODO Auto-generated method stub
				LogTool.e("MediaInfoAction  failure");
			}
			
			@Override
			public void received(ActionInvocation invocation, MediaInfo media) {
				// TODO Auto-generated method stub
				mediaInfo = media;
				LogTool.e(mediaInfo.toString());
				mHandler.sendEmptyMessage(MSG_GET_MEDIAINFO);
			}
		};

		getVolumeAction = new GetVolume(rendererService) {

			@Override
			public void received(ActionInvocation actionInvocation,
					int currentVolume) {
				// TODO Auto-generated method stub
				LogTool.i("GetVolume received & currentVolume == "
						+ currentVolume);
				if (volumeUp) {
					volumeSet = (currentVolume + 7);
				} else {
					volumeSet = (currentVolume - 7);
				}
				mHandler.sendEmptyMessage(MSG_SET_VOLUME);
				LogTool.i("GetVolume received & volumeSet == " + volumeSet);

			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				LogTool.e("GetVolume fail");
			}
		};

	}

	private void changeVolume(long volumeSet2) {
		if (volumeSet2 < 0)
			volumeSet2 = 0;
		if (volumeSet2 > 100)
			volumeSet2 = 100;
		setVolumeAction = new SetVolume(rendererService, volumeSet2) {
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				LogTool.e("SetVolume fail");
			}

			@Override
			public void success(ActionInvocation invocation) {
				// TODO Auto-generated method stub
				super.success(invocation);
			}
		};
		upnpService.getControlPoint().execute(setVolumeAction);
	}
	
	
	private void changeSeekPostion(boolean b) {
		String time = null;
		if(b) {
			time = showTime(getIntTime(position.getRelTime()) + 5);
		}else {
			time = showTime(getIntTime(position.getRelTime()) - 5);
		}
		setSeekAction = new Seek(playerService,time) {
			@Override
			public void success(ActionInvocation invocation) {
				// TODO Auto-generated method stub
				super.success(invocation);
			}
			
			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
				// TODO Auto-generated method stub
				LogTool.e("SetSeek fail");
			}
		};
		upnpService.getControlPoint().execute(setSeekAction);
	}
	

	private class MyListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			mContentItem = contentListAdapter.getItem(position);
			if (mContentItem.isContainer()) {
				upnpService.getControlPoint().execute(
						new ContentBrowseActionCallback(
								CySourceListActivity.this, mContentItem
										.getService(), mContentItem
										.getContainer(), contentListAdapter));
				foldLever++;
				mContentItemMap.put(foldLever, mContentItem);
			} else {
				canPlayNext = true;
				// String Uri = mContentItem.getItem().getFirstResource()
				// .getValue();
				try {
					cyPlay();
				} catch (java.lang.IllegalArgumentException e) {
					e.printStackTrace();
					LogTool.i(playerService.getClass().toString());
					initService();
				}
			}
		}
	}

	private class MyAdapter extends ArrayAdapter<ContentItem> {
		int[] imageResource;
		private Context context;

		public MyAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			this.context = context;
			imageResource = new int[] { R.drawable.folder,
					R.drawable.file_video, R.drawable.file_audio,
					R.drawable.file_photo };
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater flater = LayoutInflater.from(context);
			ContentItem item = contentListAdapter.getItem(position);
			ImageView iv;
			TextView tv;
			if (convertView == null) {
				convertView = flater.inflate(R.layout.lv_item_files, null);
			}
			iv = (ImageView) convertView.findViewById(R.id.iv_lv_pic);
			tv = (TextView) convertView.findViewById(R.id.tv_lv_file);

			iv.setImageResource(getPic(item));
			String str = item.toString();
			tv.setText(str);
			return convertView;
		}

		private int getPic(ContentItem item) {
			if (item.isContainer()) {
				return imageResource[0];
			} else {
				return imageResource[Integer.parseInt(item.getItem()
						.getParentID())];
			}
		}
	}
	
  private long getIntTime(String time) {
//	SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
//	Date date = null;
//	Date sDate = null;
//	try { 
//		date = format.parse(time);
//		sDate = format.parse("00:00:00"); 
//	}catch (ParseException e){ 
//		e.printStackTrace(); 
//	} catch (java.text.ParseException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//	 return ((date.getTime()-sDate.getTime())/1000);
	  String spStr[] = time.split(":");  
	  return Integer.valueOf(spStr[0]) * 3600 
			  + Integer.valueOf(spStr[1]) * 60 
			  + Integer.valueOf(spStr[2]);
}

	private String showTime(long time) {
		int hour = (int) (time / 3600);
		int mine = (int) (time % 3600 / 60);
		int mis = (int) (time % 60);
		return hour + ":" + mine + ":" + mis;
   }

}
