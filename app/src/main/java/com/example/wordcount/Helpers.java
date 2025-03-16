package com.example.wordcount;

import android.content.Context;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Helpers {

    static float getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager == null) {
            return -1; // Return -1 if BatteryManager is not available
        }

        long energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);

        if (energy <= 0) {
            return -1; // Some devices may not support this property
        }

        return energy / 1_000_000.0f; // Convert µJ to J (microjoules to joules)
    }

    static String getCurrentTimeUTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
    static long getProcessCpuTime() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/self/stat"));
            String[] stats = reader.readLine().split(" ");
            reader.close();
            long uptime = Long.parseLong(stats[13]);  // User mode time
            long time = Long.parseLong(stats[14]);  // Kernel mode time
            return uptime + time;  // Total CPU time used
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return 0;
        }
    }
}
