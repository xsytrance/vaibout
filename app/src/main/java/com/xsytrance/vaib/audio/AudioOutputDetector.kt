package com.xsytrance.vaib.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * Detects whether the current audio output is safe for personal listening
 * (headphones, headsets, USB audio) vs. public (speaker, earpiece).
 *
 * Used for headphone-safe startup autoplay decisions.
 */
object AudioOutputDetector {

    /**
     * Returns true if a headphone or personal audio device is currently connected.
     * Checks wired headphones, wired headset, USB audio, and Bluetooth A2DP/SCO/BLE.
     */
    fun hasPersonalAudioOutput(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val safeTypes = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            // BLE headset — may not be available on all compile SDKs, guarded
            0x1D, // TYPE_BLE_HEADSET (API 31+)
            0x1E, // TYPE_BLE_SPEAKER (API 31+)
        )

        return devices.any { it.type in safeTypes }
    }
}
