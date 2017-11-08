package com.cy.dlna.activity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.PersonWithRole;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.WriteStatus;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.ImageItem;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.cling.support.model.item.VideoItem;
import org.teleal.common.util.MimeType;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cy.dlna.server.ContentNode;
import com.cy.dlna.server.ContentTree;
import com.cy.dlna.server.CyUpnpService;
import com.cy.dlna.server.MediaServer;
import com.cy.dlna.util.LogTool;

public class MainActivity extends Activity {

	public static List<Device> deviceList = new ArrayList<Device>();
	private CyDeviceRegistryListener deviceListener = new CyDeviceRegistryListener();;
	public static AndroidUpnpService upnpService;
	private TextView tvShowPlayer, tvShowSource;
	private MediaServer mediaServer;

	/**
	 * 一个播放的设备。
	 */
	public static Device cyDevicePlayer;

	/**
	 * 一个提供媒体的服务器
	 */
	public static Device cyDeviceService;

	public static final int REQUEST_CODE_GET_PLAYER = 10;
	public static final int REQUEST_CODE_GET_PLAYER_RESULT_SUCCESS = 11;
	public static final int REQUEST_CODE_GET_PLAYER_RESULT_FAIL = 12;
	public static final int REQUEST_CODE_GET_SOURCE = 20;
	public static final int REQUEST_CODE_GET_SOURCE_RESULT_SUCCESS = 21;
	public static final int REQUEST_CODE_GET_SOURCE_RESULT_FAIL = 22;

	private static boolean serverPrepared = false;

	public static final String DEVICETYPE_RENDERER = "MediaRenderer";
	public static final String DEVICETYPE_SERVICE = "MediaServer";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * LoggingUtil.resetRootHandler(new FixedAndroidHandler());
		 * Logger.getLogger("org.teleal.cling").setLevel(Level.INFO);
		 */
		setContentView(R.layout.activity_main);
		long startTime = System.currentTimeMillis();
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);
		long endTime = System.currentTimeMillis();
		tvShowPlayer = (TextView) findViewById(R.id.tvShowPlayer);
		tvShowSource = (TextView) findViewById(R.id.tvShowSource);

		getApplicationContext().bindService(
				new Intent(this, CyUpnpService.class), serviceConnection,
				Context.BIND_AUTO_CREATE);
		LogTool.i("endTime -  startTime == " + (endTime - startTime));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (upnpService != null) {
			upnpService.getRegistry().removeListener(deviceListener);
		}
		getApplicationContext().unbindService(serviceConnection);

	}

	/*
	 * @Override public boolean onKeyDown(int keyCode, KeyEvent event) { // TODO
	 * Auto-generated method stub switch (keyCode) { case KeyEvent.KEYCODE_BACK:
	 * MainActivity.this.finish();
	 * 
	 * return true; } return super.onKeyDown(keyCode, event); }
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btPlayer:
			Intent in = new Intent(MainActivity.this, CyGetPlayerActivity.class);
			startActivityForResult(in, REQUEST_CODE_GET_PLAYER);
			break;
		case R.id.btSource:
			Intent in2 = new Intent(MainActivity.this,
					CyGetSourceActivity.class);
			startActivityForResult(in2, REQUEST_CODE_GET_SOURCE);
			break;
		case R.id.btFile:
			if (cyDevicePlayer == null) {
				Toast.makeText(this,
						getString(R.string.toastmsg_choise_source),
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (cyDeviceService == null) {
				Toast.makeText(this,
						getString(R.string.toastmsg_choise_player),
						Toast.LENGTH_SHORT).show();
				return;
			}
			Intent in3 = new Intent(MainActivity.this,
					CySourceListActivity.class);
			startActivity(in3);
			break;

		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (resultCode) {
		case REQUEST_CODE_GET_PLAYER_RESULT_FAIL:
			Toast.makeText(this, "获取播放设备失败", Toast.LENGTH_LONG).show();
			break;
		case REQUEST_CODE_GET_PLAYER_RESULT_SUCCESS:
			tvShowPlayer.setText(cyDevicePlayer.getDetails().getFriendlyName());
			break;
		case REQUEST_CODE_GET_SOURCE_RESULT_FAIL:
			Toast.makeText(this, "获取媒体源失败", Toast.LENGTH_LONG).show();
			break;
		case REQUEST_CODE_GET_SOURCE_RESULT_SUCCESS:
			tvShowSource
					.setText(cyDeviceService.getDetails().getFriendlyName());
			break;
		}

	}

	/**
	 * 重新搜索设备
	 */
	private void searchNetwork() {
		cyDevicePlayer = null;
		cyDeviceService = null;
		tvShowSource.setText("");
		tvShowPlayer.setText("");
		if (upnpService == null) {
			LogTool.e("upunservice is null");
			return;
		}
		upnpService.getRegistry().removeAllRemoteDevices();
		upnpService.getControlPoint().search();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_settings) {
			searchNetwork();
		}
		if(item.getItemId() == R.id.menu_control) {
			startActivity(new Intent(this, ControlActivity.class)
			.putExtra("upnp_devices", cyDevicePlayer.getDetails().getFriendlyName()));
		}
		return false;
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;

			if (mediaServer == null) {
				try {
					mediaServer = new MediaServer(getLocalIpAddress());
					upnpService.getRegistry()
							.addDevice(mediaServer.getDevice());
					prepareMediaServer();

				} catch (Exception ex) {
					// TODO: handle exception
					LogTool.e("Creating demo device failed");
					ex.printStackTrace();
				}
			}

			// Refresh the list with all known devices
			deviceList.clear();
			for (Device device : upnpService.getRegistry().getDevices()) {
				deviceListener.deviceAdded(device);
			}

			// Getting ready for future device advertisements
			upnpService.getRegistry().addListener(deviceListener);

			// Search asynchronously for all devices
			upnpService.getControlPoint().search();
			LogTool.i("onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName className) {
			LogTool.i("onServiceDisconnected");
			upnpService = null;
		}
	};

	private void prepareMediaServer() {

		if (serverPrepared)
			return;

		ContentNode rootNode = ContentTree.getRootNode();
		// Video Container
		Container videoContainer = new Container();
		videoContainer.setClazz(new DIDLObject.Class("object.container"));
		videoContainer.setId(ContentTree.VIDEO_ID);
		videoContainer.setParentID(ContentTree.ROOT_ID);
		videoContainer.setTitle("Videos");
		videoContainer.setRestricted(true);
		videoContainer.setWriteStatus(WriteStatus.NOT_WRITABLE);
		videoContainer.setChildCount(0);

		rootNode.getContainer().addContainer(videoContainer);
		rootNode.getContainer().setChildCount(
				rootNode.getContainer().getChildCount() + 1);
		ContentTree.addNode(ContentTree.VIDEO_ID, new ContentNode(
				ContentTree.VIDEO_ID, videoContainer));

		Cursor cursor;
		String[] videoColumns = { MediaStore.Video.Media._ID,
				MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DATA,
				MediaStore.Video.Media.ARTIST,
				MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.SIZE,
				MediaStore.Video.Media.DURATION,
				MediaStore.Video.Media.RESOLUTION };
		cursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				videoColumns, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				String id = ContentTree.VIDEO_PREFIX
						+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Video.Media._ID));
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
				String creator = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST));
				String filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
				String mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
				long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
				long duration = cursor
						.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
				String resolution = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION));
				Res res = new Res(new MimeType(mimeType.substring(0,
						mimeType.indexOf('/')), mimeType.substring(mimeType
						.indexOf('/') + 1)), size, "http://"
						+ mediaServer.getAddress() + "/" + id);
				res.setDuration(duration / (1000 * 60 * 60) + ":"
						+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
						+ (duration % (1000 * 60)) / 1000);
				res.setResolution(resolution);

				VideoItem videoItem = new VideoItem(id, ContentTree.VIDEO_ID,
						title, creator, res);
				videoContainer.addItem(videoItem);
				videoContainer
						.setChildCount(videoContainer.getChildCount() + 1);
				ContentTree.addNode(id,
						new ContentNode(id, videoItem, filePath));

				LogTool.v("added video item " + title + "from " + filePath);
			} while (cursor.moveToNext());
		}

		// Audio Container
		Container audioContainer = new Container(ContentTree.AUDIO_ID,
				ContentTree.ROOT_ID, "Audios", "GNaP MediaServer",
				new DIDLObject.Class("object.container"), 0);
		audioContainer.setRestricted(true);
		audioContainer.setWriteStatus(WriteStatus.NOT_WRITABLE);
		rootNode.getContainer().addContainer(audioContainer);
		rootNode.getContainer().setChildCount(
				rootNode.getContainer().getChildCount() + 1);
		ContentTree.addNode(ContentTree.AUDIO_ID, new ContentNode(
				ContentTree.AUDIO_ID, audioContainer));

		String[] audioColumns = { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE,
				MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM };
		cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				audioColumns, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				String id = ContentTree.AUDIO_PREFIX
						+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Audio.Media._ID));
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				String creator = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				String filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
				String mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
				long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
				long duration = cursor
						.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
				String album = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
				Res res = new Res(new MimeType(mimeType.substring(0,
						mimeType.indexOf('/')), mimeType.substring(mimeType
						.indexOf('/') + 1)), size, "http://"
						+ mediaServer.getAddress() + "/" + id);
				res.setDuration(duration / (1000 * 60 * 60) + ":"
						+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
						+ (duration % (1000 * 60)) / 1000);

				// Music Track must have `artist' with role field, or
				// DIDLParser().generate(didl) will throw nullpointException
				MusicTrack musicTrack = new MusicTrack(id,
						ContentTree.AUDIO_ID, title, creator, album,
						new PersonWithRole(creator, "Performer"), res);
				audioContainer.addItem(musicTrack);
				audioContainer
						.setChildCount(audioContainer.getChildCount() + 1);
				ContentTree.addNode(id, new ContentNode(id, musicTrack,
						filePath));

				LogTool.v("added audio item " + title + "from " + filePath);
			} while (cursor.moveToNext());
		}

		// Image Container
		Container imageContainer = new Container(ContentTree.IMAGE_ID,
				ContentTree.ROOT_ID, "Images", "GNaP MediaServer",
				new DIDLObject.Class("object.container"), 0);
		imageContainer.setRestricted(true);
		imageContainer.setWriteStatus(WriteStatus.NOT_WRITABLE);
		rootNode.getContainer().addContainer(imageContainer);
		rootNode.getContainer().setChildCount(
				rootNode.getContainer().getChildCount() + 1);
		ContentTree.addNode(ContentTree.IMAGE_ID, new ContentNode(
				ContentTree.IMAGE_ID, imageContainer));

		String[] imageColumns = { MediaStore.Images.Media._ID,
				MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATA,
				MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.SIZE };
		cursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				imageColumns, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				String id = ContentTree.IMAGE_PREFIX
						+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Images.Media._ID));
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE));
				String creator = "unkown";
				String filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
				String mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
				long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));

				Res res = new Res(new MimeType(mimeType.substring(0,
						mimeType.indexOf('/')), mimeType.substring(mimeType
						.indexOf('/') + 1)), size, "http://"
						+ mediaServer.getAddress() + "/" + id);

				ImageItem imageItem = new ImageItem(id, ContentTree.IMAGE_ID,
						title, creator, res);
				imageContainer.addItem(imageItem);
				imageContainer
						.setChildCount(imageContainer.getChildCount() + 1);
				ContentTree.addNode(id,
						new ContentNode(id, imageItem, filePath));

				LogTool.v("added image item " + title + "from " + filePath);
			} while (cursor.moveToNext());
		}

		serverPrepared = true;
	}

	// FIXME: now only can get wifi address
	private InetAddress getLocalIpAddress() throws UnknownHostException {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return InetAddress.getByName(String.format("%d.%d.%d.%d",
				(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
	}

	/*
	 * 设备列表监听器
	 */
	protected class CyDeviceRegistryListener extends DefaultRegistryListener {

		/* Discovery performance optimization for very slow Android devices! */

		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry,
				RemoteDevice device) {
			LogTool.i("remoteDeviceDiscoveryStarted");
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
				final RemoteDevice device, final Exception ex) {
			LogTool.i("remoteDeviceDiscoveryFailed :: "
					+ "Discovery failed of '"
					+ device.getDisplayString()
					+ "': "
					+ (ex != null ? ex.toString()
							: "Couldn't retrieve device/service descriptors"));

			deviceRemoved(device);
		}

		/*
		 * End of optimization, you can remove the whole block if your Android
		 * handset is fast (>= 600 Mhz)
		 */

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			LogTool.i("remoteDeviceAdded");
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			LogTool.i("remoteDeviceRemoved");
			deviceRemoved(device);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			LogTool.i("localDeviceAdded");
			deviceAdded(device);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			LogTool.i("localDeviceRemoved");
			deviceRemoved(device);
		}

		public void deviceAdded(final Device device) {
			runOnUiThread(new Runnable() {
				public void run() {
					int position = deviceList.indexOf(device);
					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						deviceList.remove(device);
						deviceList.add(position, device);
					} else {
						deviceList.add(device);
					}
				}
			});
		}

		public void deviceRemoved(final Device device) {
			runOnUiThread(new Runnable() {
				public void run() {
					deviceList.remove(device);
				}
			});
		}

		/*
		 * public boolean deviceIsRenderer(final Device device) { String type =
		 * device.getType().getType(); return DEVICETYPE_RENDERER.equals(type);
		 * }
		 */
	}

}
