package com.txbdc.seniordesignble;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

public class MainActivity extends AppCompatActivity {

    //create class variable for button, debug tag, ble adapter, and ble scanner
    Button button;
    String TAG = "default debug tag";
    BluetoothAdapter adapter;
    BluetoothLeScanner scanner;
    BluetoothDevice sensorDevice;
    BluetoothGatt sensorGattProfile;
    private BluetoothGattCharacteristic NRF_CHARACTERISTIC;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    boolean scanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // assign value to button value to button
        // so that it is a reference to the button in your layout (activity_main.xml)
        button = (Button) findViewById(R.id.button);

        // ask the user for for permission for coarse location, necessary for bluetooth(only once)
        // and ask for permission to write to external storage
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_COARSE_LOCATION);
        setContentView(R.layout.activity_main);
    }

    //request permission tag will always return here
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                    //once given permission get a reference to the bluetooth adapter and the scanner
                    adapter = BluetoothAdapter.getDefaultAdapter();
                    scanner = adapter.getBluetoothLeScanner();
                    // now you can use the methods in the BTadapter and and BT scanner libraries
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            //add code to handle dismiss
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void buttonClicked(View v) {
        if(!scanning){
            scanning = true;                    //set our scanning boolean to true
            scanner.startScan(bleScanCallback); //starts scan and returns anything to scan callback
        }
        if(scanning){
            Log.v(TAG, "already scanning for devices");
        }

    }

    ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "LE Scan Failed: " + errorCode);
        }

        private void processResult(ScanResult device){
            Log.i(TAG, "New LE Device: " + device.getDevice().getName() + " @ " + device.getRssi() + " Address " + device.getDevice().getAddress());
            String deviceName;
            deviceName = device.getDevice().getName();
            if(deviceName != null){
                if(deviceName.equals("JohnCougarMellenc")){
                    //if you find the device you want stop scanning and save a reference to that device
                    scanner.stopScan(bleScanCallback);
                    scanning = false;
                    sensorDevice = device.getDevice();

                    // connect to the gatt profile of that device so that you can get its data
                    // and make calls to it that will return in bleGattCallback
                    sensorGattProfile = sensorDevice.connectGatt(getApplicationContext(),false, bleGattCallback);
                }
            }
        }
    };
    public final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            byte[] temp = characteristic.getValue();
            int MSB = temp[1] << 8;
            int LSB = temp[0] & 0x000000FF;
            int val = MSB | LSB;
            float eulerZ = val * 0.0625f;
            MSB = temp[3] << 8;
            LSB = temp[2] & 0x000000FF;
            val = MSB | LSB;
            float eulerY = val * 0.0625f;
            MSB = temp[5] << 8;
            LSB = temp[4] & 0x000000FF;
            val = MSB | LSB;
            float eulerX = val * 0.0625f;

            // this is where you will need to decide what to do with the data received from the sensors
            //for now I will just print the data to the Android Monitor
            Log.v(TAG, "x: " + eulerX + "y: " + eulerY + "z: " + eulerZ);

        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            int disconnected = 0;
            int connecting  = 1;
            int connected = 2;
            if (newState == disconnected) {
            } else if (newState == connecting) {
            } else if (newState == connected) {
                Log.v(TAG, "device connected");
                // if we get a successful connection then we need to discover everything that the
                // bluetooth device (our sensor) has to offer
                gatt.discoverServices();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "charRead");
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.v(TAG, "services discovered");

            //after discovering the services on the ble device we need to set up the device
            //to send notifications. you do not have to do it this way. You could also read the characteristic
            //at a given interval, but this will set up the device to push new data to the phone every time
            //it reads new data.
            // here is what the next 12 or so lines is doing
            // iterate through services
            // iterate through characteristics
            // find the one with that UUID
            // change its value so that it sends notifications
            // if this is successful any changes to the device will result in a callback to onCharacteristicChanged

            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (int i = 0; i < characteristics.size(); i++) {
                    if (characteristics.get(i).getUuid().toString().equals("0000beef-1212-efde-1523-785fef13d123")) {
                        NRF_CHARACTERISTIC = service.getCharacteristic(UUID.fromString("0000beef-1212-efde-1523-785fef13d123"));
                        gatt.setCharacteristicNotification(NRF_CHARACTERISTIC, true);
                        UUID dUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor notifyDescriptor = NRF_CHARACTERISTIC.getDescriptor(dUUID);
                        notifyDescriptor.setValue(ENABLE_NOTIFICATION_VALUE);
                        boolean b = gatt.writeDescriptor(notifyDescriptor);
                        Log.v(TAG, String.valueOf(b));
                    }
                }
            }
        }
    };
}
