package xyz.delterium.tecno_utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SettingsActivity extends Activity {

    private Switch mOtgSwitch;
    private Spinner mTimeoutSpinner;
    private ListView mDeviceListView;
    private TextView mEmptyView;
    private UsbManager mUsbManager;
    private ArrayAdapter<String> mDeviceAdapter;
    private List<String> mDeviceNames = new ArrayList<>();
    private SharedPreferences mPrefs;

    private final int[] mTimeoutValues = {5, 10, 20, 30, -1}; 

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("xyz.delterium.tecno_utils.OTG_STATE_CHANGED".equals(action)) {
                mOtgSwitch.setChecked(OtgUtils.isOtgEnabled());
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                       UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                updateDeviceList();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPrefs = getSharedPreferences(OtgUtils.PREF_NAME, MODE_PRIVATE);

        mOtgSwitch = findViewById(R.id.otg_switch);
        mTimeoutSpinner = findViewById(R.id.timeout_spinner);
        mDeviceListView = findViewById(R.id.device_list);
        mEmptyView = findViewById(R.id.empty_view);

        String[] timeoutLabels = {
                getString(R.string.timeout_5_min),
                getString(R.string.timeout_10_min),
                getString(R.string.timeout_20_min),
                getString(R.string.timeout_30_min),
                getString(R.string.timeout_never)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeoutLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeoutSpinner.setAdapter(spinnerAdapter);

        int currentTimeout = mPrefs.getInt(OtgUtils.KEY_TIMEOUT, OtgUtils.DEFAULT_TIMEOUT);
        int selectionIndex = 1; 
        for (int i = 0; i < mTimeoutValues.length; i++) {
            if (mTimeoutValues[i] == currentTimeout) {
                selectionIndex = i;
                break;
            }
        }
        mTimeoutSpinner.setSelection(selectionIndex);

        mTimeoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedValue = mTimeoutValues[position];
                mPrefs.edit().putInt(OtgUtils.KEY_TIMEOUT, selectedValue).apply();
                if (OtgUtils.isOtgEnabled()) {
                    OtgUtils.updateOtgService(SettingsActivity.this, true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceNames);
        mDeviceListView.setAdapter(mDeviceAdapter);

        mOtgSwitch.setChecked(OtgUtils.isOtgEnabled());
        mOtgSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            OtgUtils.setOtgEnabled(isChecked);
            OtgUtils.updateOtgService(this, isChecked);
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("xyz.delterium.tecno_utils.OTG_STATE_CHANGED");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mStateReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOtgSwitch.setChecked(OtgUtils.isOtgEnabled());
        updateDeviceList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStateReceiver);
    }

    private void updateDeviceList() {
        mDeviceNames.clear();
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        if (deviceList == null || deviceList.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mDeviceListView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mDeviceListView.setVisibility(View.VISIBLE);
            for (UsbDevice device : deviceList.values()) {
                String manufacturer = device.getManufacturerName();
                String product = device.getProductName();
                
                if (manufacturer == null) {
                    manufacturer = getString(R.string.unknown_manufacturer);
                }
                if (product == null) {
                    product = getString(R.string.unknown_device);
                }
                
                String info = getString(R.string.device_info_format, 
                                        manufacturer, 
                                        product, 
                                        String.format("%04X", device.getVendorId()), 
                                        String.format("%04X", device.getProductId()));
                mDeviceNames.add(info);
            }
        }
        mDeviceAdapter.notifyDataSetChanged();
    }
}