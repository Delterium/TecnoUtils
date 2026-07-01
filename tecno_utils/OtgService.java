package xyz.delterium.tecno_utils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class OtgService extends Service {
    private static final String TAG = "OtgService";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private UsbManager mUsbManager;
    private boolean mIsTimerRunning = false;

    private final Runnable mOtgOffRunnable = () -> {
        Log.d(TAG, "Timeout reached. Disabling OTG power.");
        OtgUtils.setOtgEnabled(false);
        sendBroadcast(new Intent("xyz.delterium.tecno_utils.OTG_STATE_CHANGED"));
        stopSelf();
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                stopTimer();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                startTimerIfNecessary();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter, Context.RECEIVER_EXPORTED);
        startTimerIfNecessary();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTimerIfNecessary();
        return START_STICKY;
    }

    private void startTimerIfNecessary() {
        if (!OtgUtils.isOtgEnabled()) {
            stopSelf();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(OtgUtils.PREF_NAME, MODE_PRIVATE);
        int timeoutMinutes = prefs.getInt(OtgUtils.KEY_TIMEOUT, OtgUtils.DEFAULT_TIMEOUT);

        if (timeoutMinutes == -1) {
            stopTimer();
            return;
        }

        if (mUsbManager.getDeviceList() != null && !mUsbManager.getDeviceList().isEmpty()) {
            stopTimer();
            return;
        }

        if (!mIsTimerRunning) {
            long delayMillis = timeoutMinutes * 60 * 1000L;
            Log.d(TAG, "Starting OTG auto-off timer for " + timeoutMinutes + " minutes.");
            mHandler.postDelayed(mOtgOffRunnable, delayMillis);
            mIsTimerRunning = true;
        }
    }

    private void stopTimer() {
        if (mIsTimerRunning) {
            Log.d(TAG, "Stopping OTG auto-off timer.");
            mHandler.removeCallbacks(mOtgOffRunnable);
            mIsTimerRunning = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        stopTimer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}