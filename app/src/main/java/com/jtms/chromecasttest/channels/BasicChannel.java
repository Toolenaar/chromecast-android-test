package com.jtms.chromecasttest.channels;

import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

/**
 * Created by jochem on 2/10/2015.
 */
public class BasicChannel implements Cast.MessageReceivedCallback {
    private static final String TAG = "BasicMessageChannel";

    public String getNamespace() {
        return "urn:x-cast:com.jtms.chromecasttest";
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace,
                                  String message) {
        Log.d(TAG, "onMessageReceived: " + message);
    }
}

