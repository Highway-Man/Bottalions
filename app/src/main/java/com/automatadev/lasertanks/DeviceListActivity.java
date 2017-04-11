package com.automatadev.lasertanks;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
	public static String EXTRA_ADDRESS = "device_address";

	Button btnPaired;
	ListView deviceList;
	private BluetoothAdapter myBluetooth;

	private OnItemClickListener myListClickListener = (parent, v, position, id) -> {
		String info = ((TextView) v).getText().toString();
		String address = info.substring(info.length() - 17);
		Intent i = new Intent(DeviceListActivity.this, MainActivity.class);
		i.putExtra(DeviceListActivity.EXTRA_ADDRESS, address);
		DeviceListActivity.this.startActivity(i);
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_device_list);

		this.btnPaired = (Button) findViewById(R.id.laser);
		this.deviceList = (ListView) findViewById(R.id.listView);

		this.myBluetooth = BluetoothAdapter.getDefaultAdapter();

		if (this.myBluetooth == null) {
			Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
			finish();
		} else if (!this.myBluetooth.isEnabled()) {
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
		}

		this.btnPaired.setOnClickListener(v -> listPairedDevices());
	}

	private void listPairedDevices() {
		Set<BluetoothDevice> pairedDevices = this.myBluetooth.getBondedDevices();

		ArrayList<String> list = new ArrayList<>();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice bt : pairedDevices) {
				list.add(bt.getName() + "\n" + bt.getAddress());
			}
		} else {
			Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
		}

		this.deviceList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
		this.deviceList.setOnItemClickListener(this.myListClickListener);
	}
}
