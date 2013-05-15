package com.volkersfreunde.phoneDrone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import WS.koordINWS;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class HelloADKActivity extends Activity implements Runnable,
		OnSeekBarChangeListener, SensorEventListener, OnCheckedChangeListener,
		LocationListener {

	private static final String TAG = "PhoneDrone";

	private static final String tag = "sensor";
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private LocationManager lm = null;
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	private SeekBar AILESeekBar; /* SERVO1 */
	private SeekBar incoming1SeekBar;
	private EditText incoming1EditText;
	private EditText AILEEditText;

	private SeekBar ELEVSeekBar;
	private SeekBar incoming2SeekBar;
	private EditText incoming2EditText;
	private EditText ELEVEditText;

	private SeekBar RUDDSeekBar;
	private SeekBar incoming3SeekBar;
	private EditText incoming3EditText;
	private EditText RUDDEditText;

	private SeekBar THROSeekBar; /* SERVO4 */
	private SeekBar incoming4SeekBar;
	private EditText incoming4EditText;
	private EditText THROEditText;

	private EditText mTemperature; // for our temperature input
	private EditText mAirspeed;

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private PowerManager mPowerManager;
	private WindowManager mWindowManager;
	private Display mDisplay;
	private WakeLock mWakeLock;

	private CheckBox updateValuesCheckbox;

	Handler handler = new Handler();

	private CheckBox mAutoPilotCheckBox;
	private boolean mAutoPilotTurnedOn;

	private float mSensorX;
	private float mSensorY;
	private float mSensorZ;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	public static final byte AILE_COMMAND = 2;
	public static final byte ELEV_COMMAND = 3;
	public static final byte RUDD_COMMAND = 5;
	public static final byte THRO_COMMAND = 6;
	public static final byte SEND_UPDATES_COMMAND = 4;

	private static final byte COMMAND_TEMPERATURE = 0x4;
	private static final byte TARGET_PIN = 0x0;

	private static final byte COMMAND_AIRSPEED = 0x7;
	private static final byte TARGET_PIN2 = 0x2;

	File temp, speed, AILE_command, ELEV_command, THRO_command, RUDD_command,
			Accel, Baro, Orient, Magnet, GPS;

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}

		setContentView(R.layout.main);

		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

		mAutoPilotCheckBox = (CheckBox) findViewById(R.id.autopilot_check_box);
		mAutoPilotCheckBox.setChecked(false);
		mAutoPilotCheckBox.setOnCheckedChangeListener(this);

		mAutoPilotTurnedOn = false;

		incoming1SeekBar = (SeekBar) findViewById(R.id.incoming1_seek_bar);
		incoming1SeekBar.setMax(2000);
		incoming1SeekBar.setEnabled(false);

		AILEEditText = (EditText) findViewById(R.id.servo1_editText);

		incoming1EditText = (EditText) findViewById(R.id.incoming1_editText);

		AILESeekBar = (SeekBar) findViewById(R.id.AILE_seek_bar);
		AILESeekBar.setMax(2000);
		AILESeekBar.setProgress(1000);
		AILESeekBar.setOnSeekBarChangeListener(this);
		AILESeekBar.setEnabled(false);

		incoming2SeekBar = (SeekBar) findViewById(R.id.incoming2_seek_bar);
		incoming2SeekBar.setMax(2000);
		incoming2SeekBar.setEnabled(false);

		ELEVEditText = (EditText) findViewById(R.id.servo2_editText);

		incoming2EditText = (EditText) findViewById(R.id.incoming2_editText);

		ELEVSeekBar = (SeekBar) findViewById(R.id.ELEV_seek_bar);
		ELEVSeekBar.setMax(2000);
		ELEVSeekBar.setProgress(1000);
		ELEVSeekBar.setOnSeekBarChangeListener(this);
		ELEVSeekBar.setEnabled(false);

		incoming3SeekBar = (SeekBar) findViewById(R.id.incoming3_seek_bar);
		incoming3SeekBar.setMax(2000);
		incoming3SeekBar.setEnabled(false);

		RUDDEditText = (EditText) findViewById(R.id.servo3_editText);

		incoming3EditText = (EditText) findViewById(R.id.incoming3_editText);

		RUDDSeekBar = (SeekBar) findViewById(R.id.RUDD_seek_bar);
		RUDDSeekBar.setMax(2000);
		RUDDSeekBar.setProgress(1000);
		RUDDSeekBar.setOnSeekBarChangeListener(this);
		RUDDSeekBar.setEnabled(false);

		incoming4SeekBar = (SeekBar) findViewById(R.id.incoming4_seek_bar);
		incoming4SeekBar.setMax(2000);
		incoming4SeekBar.setEnabled(false);

		THROEditText = (EditText) findViewById(R.id.servo4_editText);

		incoming4EditText = (EditText) findViewById(R.id.incoming4_editText);

		THROSeekBar = (SeekBar) findViewById(R.id.THRO_seek_bar);
		THROSeekBar.setMax(2000);
		THROSeekBar.setProgress(1000);
		THROSeekBar.setOnSeekBarChangeListener(this);
		THROSeekBar.setEnabled(false);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);

		updateValuesCheckbox = (CheckBox) findViewById(R.id.updateValues_Checkbox);
		updateValuesCheckbox.setChecked(false);
		updateValuesCheckbox.setOnCheckedChangeListener(this);

		mTemperature = (EditText) findViewById(R.id.Temperature_editText); //
		mAirspeed = (EditText) findViewById(R.id.Airspeed_editText);

		File filePath = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
				"szenzooor");

		if (!filePath.exists()) {
			if (!filePath.mkdirs()) {
				Log.d("logg", "failed to create directory");

			}
		}

		temp = new File(filePath.getPath() + File.separator + "temp.txt");
		speed = new File(filePath.getPath() + File.separator + "speed.txt");
		AILE_command = new File(filePath.getPath() + File.separator
				+ "AILE_command.txt");
		ELEV_command = new File(filePath.getPath() + File.separator
				+ "ELEV_command.txt");
		THRO_command = new File(filePath.getPath() + File.separator
				+ "THRO_command.txt");
		RUDD_command = new File(filePath.getPath() + File.separator
				+ "RUDD_command.txt");
		Orient = new File(filePath.getPath() + File.separator + "Orient.txt");
		Accel = new File(filePath.getPath() + File.separator + "Accel.txt");
		Magnet = new File(filePath.getPath() + File.separator + "Magnet.txt");
		Baro = new File(filePath.getPath() + File.separator + "Baro.txt");
		GPS = new File(filePath.getPath() + File.separator + "GPS.txt");

	}

	public void updateIncomingSeekBar(final int value, final int servoIndex) { // servoindex
																				// also
																				// for
																				// other
																				// sensors
																				// (temp
																				// and
																				// airspeed)
		handler.post(new Runnable() {

			@Override
			public void run() {
				switch (servoIndex) {
				case 0:
					HelloADKActivity.this.incoming1EditText.setText("" + value);
					if (value != -1) {
						HelloADKActivity.this.incoming1SeekBar
								.setProgress(value);
					}
					break;
				case 1:
					HelloADKActivity.this.incoming2EditText.setText("" + value);
					if (value != -1) {
						HelloADKActivity.this.incoming2SeekBar
								.setProgress(value);
					}

					break;

				case 2:
					HelloADKActivity.this.incoming4EditText.setText("" + value);
					if (value != -1) {
						HelloADKActivity.this.incoming4SeekBar
								.setProgress(value);
					}
					break;

				case 3:
					HelloADKActivity.this.incoming3EditText.setText("" + value);
					if (value != -1) {
						HelloADKActivity.this.incoming3SeekBar
								.setProgress(value);
					}
					break;

				case 4:
					HelloADKActivity.this.mTemperature.setText("" + value);
					break;

				case 5:
					HelloADKActivity.this.mAirspeed.setText("" + value);
					break;
				}

			}
		});
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(tag, "Location Changed");

		String[] data1 = { Double.toString(location.getLongitude()),
				Double.toString(location.getLatitude()),
				Double.toString(location.getAltitude()),
				Float.toString(location.getAccuracy()),
				Long.toString(location.getTime()) };
		appendToFile2(GPS, data1);

		koordINWS koordws = new koordINWS();
		// koordws.addKoord(location.getLongitude(), location.getLatitude(),
		// location.getAltitude(),
		// Double.parseDouble(Float.toString(location.getAccuracy())),
		// Long.toString(location.getTime()));

		if (koordws.addKoord(location.getLongitude(), location.getLatitude(),
				location.getAltitude(),
				Double.parseDouble(Float.toString(location.getAccuracy())),
				Long.toString(location.getTime()))) {

			Toast.makeText(getApplicationContext(), "Succesfully put",
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "Something gone wrong :(",
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		/* this is called if/when the GPS is disabled in settings */
		Log.v(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.v(tag, "Enabled");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			Toast.makeText(this, "Status Changed: Out of Service",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			Toast.makeText(this, "Status Changed: Temporarily Unavailable",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			Toast.makeText(this, "Status Changed: Available",
					Toast.LENGTH_SHORT).show();
			break;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}

	private void startSensorListener() {
		mAutoPilotTurnedOn = true;
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_UI);
	}

	private void stopSensorListener() {
		mSensorManager.unregisterListener(this);
		mAutoPilotTurnedOn = false;// jjjj
	}

	protected void appendToFile(File filePath, String data) {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath,
					true))); // true means: "append"

			pw.println(data);

		} catch (IOException e) {
			// Report problem or handle it
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	protected void appendToFile2(File filePath, String[] data) {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath,
					true))); // true means: "append"

			for (int i = 0; i < data.length; i++) {
				pw.println(data[i]);

			}
		} catch (IOException e) {
			// Report problem or handle it
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	@Override
	public void onResume() {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
		super.onResume();
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
				SensorManager.SENSOR_DELAY_NORMAL);
		mWakeLock.acquire();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		stopSensorListener();
		lm.removeUpdates(this);
		mSensorManager.unregisterListener(this);
		// and release our wake-lock
		mWakeLock.release();
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "DemoKit");
			thread.start();
			Log.d(TAG, "accessory opened");
			enableControls(true);
			sendUpdatesCommand(updateValuesCheckbox.isChecked());
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void enableControls(boolean enable) {
		AILESeekBar.setEnabled(enable);
		ELEVSeekBar.setEnabled(enable);
		RUDDSeekBar.setEnabled(enable);
		THROSeekBar.setEnabled(enable);
		updateValuesCheckbox.setEnabled(enable);
	}

	private void closeAccessory() {
		enableControls(false);

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	public void sendCommand(byte command, byte value1, byte value2) {
		byte[] buffer = new byte[3];

		buffer[0] = command;
		buffer[1] = value1;
		buffer[2] = value2;
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	@Override
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[9];
		int incoming = 0;
		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);

			} catch (IOException e) {
				break;
			}
			Log.d(TAG, "[0]:" + buffer[0] + " [1]:" + buffer[1] + " [2]:"
					+ buffer[2] + " [3]:" + buffer[3] + " [4]:" + buffer[4]);
			switch (buffer[0]) {
			case COMMAND_TEMPERATURE:
				if (buffer[1] == TARGET_PIN) {
					final float temperatureValue = (((buffer[2] & 0xFF) << 24)
							+ ((buffer[3] & 0xFF) << 16)
							+ ((buffer[4] & 0xFF) << 8) + (buffer[5] & 0xFF)) / 10;
					// set up the message to the original int value
					// char c = (char) temperatureValue;
					// mTemperature.setText(c);
					appendToFile(temp, Float.toString(temperatureValue));
					incoming = (int) temperatureValue;
					HelloADKActivity.this.updateIncomingSeekBar(
							Math.round(incoming), 4);
				}
				break;

			case COMMAND_AIRSPEED:
				if (buffer[1] == TARGET_PIN2) {
					final double airspeedValue = (((buffer[2] & 0xFF) << 24)
							+ ((buffer[3] & 0xFF) << 16)
							+ ((buffer[4] & 0xFF) << 8) + (buffer[5] & 0xFF)) / 100.00;
					appendToFile(speed, Double.toString(airspeedValue));
					incoming = (int) airspeedValue;
					HelloADKActivity.this.updateIncomingSeekBar(
							Math.round(incoming), 5);
				}
				break;

			case 1:
				incoming = buffer[1] * 256;
				incoming += buffer[2];
				appendToFile(AILE_command, Integer.toString(incoming));
				Log.d(TAG, "current pwm1 is " + incoming);
				HelloADKActivity.this.updateIncomingSeekBar(incoming, 0);
				incoming = buffer[3] * 256;
				incoming += buffer[4];
				appendToFile(ELEV_command, Integer.toString(incoming));
				HelloADKActivity.this.updateIncomingSeekBar(incoming, 1);
				Log.d(TAG, "current pwm2 is " + incoming);
				incoming = buffer[5] * 256;
				incoming += buffer[6];
				appendToFile(THRO_command, Integer.toString(incoming));
				HelloADKActivity.this.updateIncomingSeekBar(incoming, 2);
				Log.d(TAG, "current pwm4 is " + incoming);
				incoming = buffer[7] * 256;
				incoming += buffer[8];
				appendToFile(RUDD_command, Integer.toString(incoming));
				HelloADKActivity.this.updateIncomingSeekBar(incoming, 3);
				Log.d(TAG, "current pwm5 is " + incoming);
				break;
			default:
				Log.d(TAG, "unknown msg: " + buffer[0]);
				break;
			}

		}
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
		byte highProgress = (byte) (progress / 256);
		byte lowProgress = (byte) (progress & 0x0f);
		if (arg0 == AILESeekBar) {
			if (!mAutoPilotTurnedOn) {
				sendCommand(AILE_COMMAND, lowProgress, highProgress);
			}
			AILEEditText.setText("" + (progress - 1000));

		} else if (arg0 == ELEVSeekBar) {
			if (!mAutoPilotTurnedOn) {
				sendCommand(ELEV_COMMAND, lowProgress, highProgress);
			}
			ELEVEditText.setText("" + (progress - 1000));
		}

		else if (arg0 == RUDDSeekBar) {
			if (!mAutoPilotTurnedOn) {
				sendCommand(RUDD_COMMAND, lowProgress, highProgress);
			}
			RUDDEditText.setText("" + (progress - 1000));
		}

		else if (arg0 == THROSeekBar) {
			if (!mAutoPilotTurnedOn) {
				sendCommand(THRO_COMMAND, lowProgress, highProgress);
			}
			THROEditText.setText("" + (progress - 1000));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// We do nothing
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// we do nothing
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
				String[] data1 = { Float.toString(event.values[0]),
						Float.toString(event.values[1]),
						Float.toString(event.values[2]), "\n" };
				appendToFile2(Orient, data1);

			}
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				String[] data2 = { Float.toString(event.values[0]),
						Float.toString(event.values[1]),
						Float.toString(event.values[2]), "\n" };
				appendToFile2(Magnet, data2);
			}
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				String[] data3 = { Float.toString(event.values[0]),
						Float.toString(event.values[1]),
						Float.toString(event.values[2]), "\n" };
				appendToFile2(Accel, data3);
			}
			if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
				String[] data4 = { Float.toString(event.values[0]), "\n" };
				appendToFile2(Baro, data4);
			}
		}

		// if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		// switch (mDisplay.getRotation()) {
		// case Surface.ROTATION_0:
		// mSensorX = event.values[0];
		// mSensorY = event.values[1];
		// mSensorZ = event.values[2];
		// break;
		// case Surface.ROTATION_90:
		// mSensorX = -event.values[1];
		// mSensorY = event.values[0];
		// mSensorZ = event.values[2];
		// break;
		// case Surface.ROTATION_180:
		// mSensorX = -event.values[0];
		// mSensorY = -event.values[1];
		// mSensorZ = event.values[2];
		// break;
		// case Surface.ROTATION_270:
		// mSensorX = event.values[1];
		// mSensorY = -event.values[0];
		// mSensorZ = event.values[2];
		// break;
		// }
		//
		// int progress = 1000 + (int) (mSensorX * 200);
		// byte highProgress = (byte) (progress / 256);
		// byte lowProgress = (byte) (progress & 0x0f);
		// sendCommand(ELEV_COMMAND, lowProgress, highProgress);
		// ELEVSeekBar.setProgress(progress);
		// }
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean checked) {
		if (arg0 == updateValuesCheckbox) {
			sendUpdatesCommand(checked);
			Log.d(TAG, "onCheckedChanged");
			incoming1SeekBar.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming2SeekBar.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming3SeekBar.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming4SeekBar.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming1EditText.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming2EditText.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming3EditText.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
			incoming4EditText.setVisibility(checked ? View.VISIBLE
					: View.INVISIBLE);
		} else if (arg0 == mAutoPilotCheckBox) {
			if (checked) {
				startSensorListener();
			} else {
				stopSensorListener();
			}
		}

	}

	private void sendUpdatesCommand(boolean checked) {
		sendCommand(SEND_UPDATES_COMMAND, checked ? (byte) 1 : 0,
				checked ? (byte) 1 : 0);

	}
}
