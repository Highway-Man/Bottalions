package com.automatadev.lasertanks;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.media.SoundPool;
import android.media.AudioManager;

import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {
	public static final String TAG = "makeapede";

	private static final int JOYSTICK_RESPONSE_TIME = 25;

	private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public static final int S_SHOOT = R.raw.stardestroyerblastersound2;
	public static final int S_HIT = R.raw.explosionsound2;
	public static final int S_DESTROYED = R.raw.tiefighterexplosions;

	private static final int annoyingcharlesvariablethatisannoyinglylongandannoyinglyisntformattedcorrectlyhahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahaha = 2000;

	private String address;
	private Thread btListenThread = new Thread(this::btListen);
	private BluetoothSocket btSocket;
	private InputStream stream;

	private ImageView dot;
	private TextView text;
	private ProgressDialog progress;

	private int dotSize;
	private float joystickHeight;
	private float joystickWidth;

	private int rightState = 255;
	private int leftState = 255;

	private boolean shouldStopBTListenThread = false;
	private boolean isBtConnected = false;

	private long t = 0;
	private long t1 = 0;

	private boolean isButtonPressed;
	private Vibrator vibrator;

	private SoundPool soundPool;
	private int soundShoot, soundHit, soundDestroyed;
	boolean loaded = false;

	private ProgressBar firstBar = null;
	private ProgressBar secondBar = null;
	private int health=100;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		address = getIntent().getStringExtra(DeviceListActivity.EXTRA_ADDRESS);

		joystickHeight = joystickWidth = (float) dpToPx(300);
		dotSize = dpToPx(20);

		dot = (ImageView) findViewById(R.id.dot);

		findViewById(R.id.joystick).setOnTouchListener(this::processJoystickTouchEvent);

		text = (TextView) findViewById(R.id.text);
		firstBar = (ProgressBar)findViewById(R.id.firstBar);
		text.setText("Health");

		findViewById(R.id.laser).setOnTouchListener(this::onLaserButtonClicked);

		findViewById(R.id.up).setOnTouchListener(this::onUpButtonClicked);
		findViewById(R.id.left).setOnTouchListener(this::onLeftButtonClicked);
		findViewById(R.id.right).setOnTouchListener(this::onRightButtonClicked);
		findViewById(R.id.down).setOnTouchListener(this::onDownButtonClicked);
        findViewById(R.id.turbo).setOnTouchListener(this::onTurboButtonClicked);

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Load the sound
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener((soundPool1, sampleId, status) -> loaded = true);
		soundShoot = soundPool.load(this, S_SHOOT, 2);
		soundHit = soundPool.load(this, S_HIT, 1);
		soundDestroyed = soundPool.load(this, S_DESTROYED, 3);

		new ConnectBT().execute();
	}

	protected void onDestroy() {
		super.onDestroy();

		disconnect();

		this.shouldStopBTListenThread = true;
	}

	private void disconnect() {
		if (this.btSocket != null) {
			try {
				this.btSocket.close();
			} catch (IOException e) {
				toast("Error");
			}
		}

		finish();
	}

	private void sendMessage() {
		String left = Integer.toString(leftState);
		while (left.length() < 3) {
			left = "0" + left;
		}

		String right = Integer.toString(rightState);
		while (right.length() < 3) {
			right = "0" + right;
		}

		String value = isButtonPressed ? "1" : "0";

		sendMessage(left + ":" + right + ":" + value);
	}

	private void sendMessage(String message) {
		if (btSocket != null) {
			try {
				byte[] bytes = message.getBytes();
				btSocket.getOutputStream().write(bytes);
			} catch (IOException e) {
				toast("Error");
			}
		}
	}

	private void btListen() {
		try {
			stream = btSocket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int available;

		while (!shouldStopBTListenThread) {
			try {
				available = stream.available();

				if (available > 0) {
					byte[] data = new byte[available];

					stream.read(data);

					String message = new String(data, "US-ASCII");
					runOnUiThread(() -> btMessageReceived(message));

					/*long skipped = stream.skip(available);

					while(skipped < available) {
						skipped += stream.skip(1);
					}*/
				}

				Thread.sleep(200);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void btMessageReceived(String message) {
		vibrator.vibrate(100);
		//play hit sound
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		float actualVolume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;
		// Is the sound loaded already?
		if (loaded) {
			soundPool.play(soundHit, volume, volume, 2, 0, 1f);
			toast("YOU'VE BEEN HIT!");
		}

		//update health bar
		health -= 10;
		if (health > 0) {
			//make the progress bar visible
			firstBar.setVisibility(View.VISIBLE);
			firstBar.setProgress(health);
		}else {
			firstBar.setProgress(0);
			//play explosion
			// Is the sound loaded already?
			if (loaded) {
				soundPool.play(soundDestroyed, maxVolume, maxVolume, 0, 0, 1f);
				toast("KABOOM!");
			}
		}

	}

    private boolean onTurboButtonClicked(View v, MotionEvent motionEvent) {
        if(health > 0) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    leftState=rightState=800;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    leftState=rightState=255;
                    break;
            }
        }
        else{
            leftState=rightState=255;
        }

        sendMessage();

        return true;
    }

	private boolean onUpButtonClicked(View v, MotionEvent motionEvent) {
		if(health > 0) {
			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					leftState=rightState=510;
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					leftState=rightState=255;
					break;
			}
		}
		else{
			leftState=rightState=255;
		}

		sendMessage();

		return true;
	}

	private boolean onLeftButtonClicked(View v, MotionEvent motionEvent) {
		if(health > 0) {
			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					leftState=255-150;
					rightState= 255+150;
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					leftState=rightState=255;
					break;
			}
		}
		else{
			leftState=rightState=255;
		}

		sendMessage();

		return true;
	}

	private boolean onRightButtonClicked(View v, MotionEvent motionEvent) {
		if(health > 0) {
			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					leftState = 255+150;
					rightState = 255-150;
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					leftState=rightState=255;
					break;
			}
		}
		else{
			leftState=rightState=255;
		}

		sendMessage();

		return true;
	}

	private boolean onDownButtonClicked(View v, MotionEvent motionEvent) {
		if(health > 0) {
			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					leftState=rightState=0;
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					leftState=rightState=255;
					break;
			}
		}
		else{
			leftState=rightState=255;
		}

		sendMessage();

		return true;
	}

	private boolean processJoystickTouchEvent(View view, MotionEvent motionEvent) {
		LayoutParams params = (LayoutParams) dot.getLayoutParams();

		if(health > 0) {

			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_MOVE:
					if (System.currentTimeMillis() - t > JOYSTICK_RESPONSE_TIME) {
						int x = (int) motionEvent.getX();
						int y = (int) motionEvent.getY();

						params.leftMargin = x - (dotSize / 2);
						params.topMargin = y - (dotSize / 2);

						dot.setLayoutParams(params);

						int forwardValue = (int) ((((joystickHeight / 2.0d) - y) / (joystickHeight / 2.0d)) * 510d);
						int turnValue = (int) (((x - (joystickWidth / 2.0d)) / (joystickWidth / 2.0d)) * 510d);

						leftState = forwardValue + turnValue;
						rightState = forwardValue - turnValue;

						leftState += 255;
						rightState += 255;

						leftState = Math.min(leftState, 510);
						rightState = Math.min(rightState, 510);

						leftState = Math.max(leftState, 0);
						rightState = Math.max(rightState, 0);

						sendMessage();

						t = System.currentTimeMillis();

						break;
					}
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					params.leftMargin = (int) ((joystickWidth / 2.0f) - ((float) (dotSize / 2)));
					params.topMargin = (int) ((joystickHeight / 2.0f) - ((float) (dotSize / 2)));

					dot.setLayoutParams(params);

					leftState = 255;
					rightState = 255;

					sendMessage();

					break;
			}
		} else{
			leftState = rightState = 255;
		}

		return true;
	}

	private boolean onLaserButtonClicked(View v, MotionEvent motionEvent) {
		if(health > 0) {
			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if(System.currentTimeMillis()-t1 > annoyingcharlesvariablethatisannoyinglylongandannoyinglyisntformattedcorrectlyhahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahahaha) {
						//play firing sound
						AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
						float actualVolume = (float) audioManager
								.getStreamVolume(AudioManager.STREAM_MUSIC);
						float maxVolume = (float) audioManager
								.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
						float volume = actualVolume / maxVolume;
						// Is the sound loaded already?
						if (loaded) {
							soundPool.play(soundShoot, volume, volume, 1, 0, 1f);
							toast("FIRE!");
						}
						vibrator.vibrate(30);
						isButtonPressed = true;

						t1 = System.currentTimeMillis();
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					isButtonPressed = false;
					break;
			}
		}
		else{
			isButtonPressed = false;
		}

		sendMessage();

		return true;
	}

	private void toast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
	}

	private int dpToPx(int dp) {
		return (int) TypedValue.applyDimension(1, (float) dp, getResources().getDisplayMetrics());
	}

	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean connectSuccess;

		private ConnectBT() {
			this.connectSuccess = true;
		}

		protected void onPreExecute() {
			progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait");
		}

		protected Void doInBackground(Void... devices) {
			try {
				if (btSocket == null || !isBtConnected) {
					btSocket = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address).createInsecureRfcommSocketToServiceRecord(MainActivity.uuid);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

					btSocket.connect();
				}
			} catch (IOException e) {
				this.connectSuccess = false;
			}
			return null;
		}

		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (this.connectSuccess) {
				toast("Connected.");

				isBtConnected = true;
				btListenThread.start();
			} else {
				toast("Connection Failed. Is it a SPP Bluetooth? Try again.");
				finish();
			}

			progress.dismiss();
		}
	}
}
