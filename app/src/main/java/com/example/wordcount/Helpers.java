package com.example.wordcount;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Helpers {

    private static final long CLOCK_TICKS_PER_SEC = getClockTicksPerSecond(); // 100 for most Android/Linux
    private static final long CORES = getNumberOfCores(); // 100 for most Android/Linux
    public static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static int getClockTicksPerSecond() {
        try {
            Process process = Runtime.getRuntime().exec("getconf CLK_TCK");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            return line != null ? Integer.parseInt(line.trim()) : 100;  // Default 100
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return 100;  // Default
        }
    }

    static float getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus == null) return -1;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) return -1;  // Return -1 if unable to fetch values

        return (level / (float) scale) * 100;  // Convert to percentage
    }


//    static float getBatteryLevel(Context context) {
//        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
//        if (batteryManager == null) {
//            return -1; // Return -1 if BatteryManager is not available
//        }
//
//        long energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
//
//        if (energy <= 0) {
//            return -1; // Some devices may not support this property
//        }
//
//        return energy / 1_000_000.0f; // Convert µJ to J (microjoules to joules)
//    }

    static String getCurrentTimeUTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
    static float getBatteryCurrentNow(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager == null) {
            return -1; // BatteryManager is not available
        }

        long currentMicroAmps = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        if (currentMicroAmps == 0 || currentMicroAmps == Long.MIN_VALUE) {
            return -1; // Device does not support this property
        }

        return currentMicroAmps / 1_000.0f;  // Convert from µA (microamperes) to mA (milliamperes)
    }


    static long getProcessCpuTime() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/self/stat"));
            String[] stats = reader.readLine().split(" ");
            reader.close();

            long utime = Long.parseLong(stats[13]);  // User mode time (in clock ticks)
            long stime = Long.parseLong(stats[14]);  // Kernel mode time (in clock ticks)
            long utimechild = Long.parseLong(stats[15]);  // User mode time (in clock ticks) by child process
            long stimechild = Long.parseLong(stats[16]);  // Kernel mode time (in clock ticks) by child process

            long totalTimeTicks = utime + stime;  // Total CPU time in clock ticks
            totalTimeTicks += utimechild + stimechild;
            return (totalTimeTicks * 1000L) / (CLOCK_TICKS_PER_SEC*CORES);  // Convert ticks to milliseconds
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    static double calculateCPUUtilization(long elapsedTime, long cpuTime) {
        if (elapsedTime == 0) return 0;  // Prevent division by zero
        double utilization = ((double) cpuTime / elapsedTime) * 100;
        return Math.round(utilization * 1000.0) / 1000.0;  // Round to 3 decimal places
    }

    public static float getBatteryUsage(Context context, float startCurrent, float endCurrent, float totalTime) {
        float avgCurrent = (Math.abs(startCurrent) + Math.abs(endCurrent)) / 2; // Avg current in mA
        float voltage = getBatteryVoltage(context); // Approximate battery voltage (modify as needed)
        float timeSeconds = totalTime / 1000.0f; // Convert elapsed time to seconds
        Log.d("POWER", "startCurrent: " + startCurrent);
        Log.d("POWER", "endCurrent: " + endCurrent);
        Log.d("POWER", "totalTime: " + totalTime);
        Log.d("POWER", "voltage: " + voltage);
        float batteryUsed = (avgCurrent * voltage * timeSeconds) / 3600; // Energy in mWh
        return (float) (Math.round(batteryUsed * 1000.0) / 1000.0);
    }

    static float getBatteryVoltage(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus == null) {
            return 3.85f; // Error case
        }

        int voltageMillivolts = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3850);
        return voltageMillivolts / 1000.0f; // Convert mV to V
    }


}
