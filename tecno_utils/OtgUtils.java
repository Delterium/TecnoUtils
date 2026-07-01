package xyz.delterium.tecno_utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class OtgUtils {
    private static final String TAG = "OtgUtils";
    public static final String OTG_PATH = "/sys/devices/platform/odm/odm:tran_battery/OTG_CTL";
    
    public static final String PREF_NAME = "OtgSettings";
    public static final String KEY_TIMEOUT = "otg_timeout_value";
    public static final int DEFAULT_TIMEOUT = 10; 

    public static boolean isOtgEnabled() {
        try (BufferedReader reader = new BufferedReader(new FileReader(OTG_PATH))) {
            return "1".equals(reader.readLine().trim());
        } catch (Exception e) {
            Log.e(TAG, "Failed to read OTG state", e);
            return false;
        }
    }

    public static boolean setOtgEnabled(boolean enable) {
        try (FileWriter writer = new FileWriter(OTG_PATH)) {
            writer.write(enable ? "1" : "0");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write OTG state: " + enable, e);
            return false;
        }
    }

    public static void updateOtgService(Context context, boolean enabled) {
        Intent intent = new Intent(context, OtgService.class);
        if (enabled) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }
}