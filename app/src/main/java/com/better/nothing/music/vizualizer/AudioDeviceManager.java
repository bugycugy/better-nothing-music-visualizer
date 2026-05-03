package com.better.nothing.music.vizualizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;

/**
 * Manages audio device callbacks and latency compensation settings.
 */
public class AudioDeviceManager extends AudioDeviceCallback {

    private static final String PREFS_NAME = "glyph_visualizer_prefs";
    private static final String PREF_LATENCY_PREFIX = "latency_device_";
    private static final String PREF_LATENCY_ROUTE_PREFIX = "latency_route_";

    private final Context context;
    private final Runnable latencyCallback;

    public AudioDeviceManager(Context context, Runnable latencyCallback) {
        this.context = context;
        this.latencyCallback = latencyCallback;
    }

    @Override
    public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
        latencyCallback.run();
    }

    @Override
    public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
        latencyCallback.run();
    }

    public static int loadLatencyCompensationMs(Context context, int device) {
        return getPreferences(context).getInt(latencyPreferenceKey(device), 0);
    }

    public static int loadLatencyCompensationMs(Context context, int device, String routeKey) {
        if (routeKey == null || routeKey.isBlank()) {
            return loadLatencyCompensationMs(context, device);
        }

        SharedPreferences preferences = getPreferences(context);
        String preferenceKey = routeLatencyPreferenceKey(device, routeKey);
        if (preferences.contains(preferenceKey)) {
            return preferences.getInt(preferenceKey, 0);
        }
        return loadLatencyCompensationMs(context, device);
    }

    public static void saveLatencyCompensationMs(Context context, int device, int latencyMs) {
        getPreferences(context)
                .edit()
                .putInt(latencyPreferenceKey(device), latencyMs)
                .apply();
    }

    public static void saveLatencyCompensationMs(Context context, int device, String routeKey, int latencyMs) {
        if (routeKey == null || routeKey.isBlank()) {
            saveLatencyCompensationMs(context, device, latencyMs);
            return;
        }

        getPreferences(context)
                .edit()
                .putInt(routeLatencyPreferenceKey(device, routeKey), latencyMs)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String latencyPreferenceKey(int device) {
        return PREF_LATENCY_PREFIX + device;
    }

    private static String routeLatencyPreferenceKey(int device, String routeKey) {
        return PREF_LATENCY_ROUTE_PREFIX + device + "_" + routeKey;
    }
}