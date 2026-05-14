package com.xsytrance.vaib.audio

/**
 * Simple EQ preset definitions for vAIb cards.
 *
 * [bands] are five-band levels in millibels, ordered bass→treble.
 * Approximate centre frequencies: 60 Hz, 230 Hz, 910 Hz, 3.6 kHz, 14 kHz.
 * Values are clamped to the device's actual supported range at runtime.
 * Range assumes a typical ±1500 mB device maximum.
 */
enum class EqPreset(val label: String, val bands: ShortArray) {
    FLAT        ("Flat",        shortArrayOf(   0,    0,    0,    0,    0)),
    DEEP_BASS   ("Deep Bass",   shortArrayOf( 800,  500,    0, -300, -400)),
    CHILL       ("Chill",       shortArrayOf( 400,  200, -100,  200,  400)),
    BRIGHT      ("Bright",      shortArrayOf(-300, -100,  200,  500,  700)),
    FOCUS       ("Focus",       shortArrayOf(-400,  100,  700,  400, -200)),
    COSMIC      ("Cosmic",      shortArrayOf( 600,  100,  300,  100,  600)),
    NIGHT_DRIVE ("Night Drive", shortArrayOf( 700,  400, -200,  300,  600)),
}
