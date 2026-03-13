package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class PreferenceConfigurationAmbilightTest {

    private Context context;
    private SharedPreferences prefs;

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void readPreferences_usesAmbilightDefaults() {
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        assertTrue(config.enableAmbilight);
        assertEquals(0.22f, config.ambilightIntensity, 0.0001f);
        assertEquals(0.35f, config.ambilightSpread, 0.0001f);
        assertEquals(0.65f, config.ambilightSmoothing, 0.0001f);
        assertEquals(1, config.ambilightQuality);
    }

    @Test
    public void readPreferences_readsAmbilightOverrides() {
        prefs.edit()
                .putBoolean("checkbox_enable_ambilight", false)
                .putInt("ambilight_intensity", 40)
                .putInt("ambilight_spread", 55)
                .putInt("ambilight_smoothing", 20)
                .putString("ambilight_quality", "2")
                .commit();

        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        assertFalse(config.enableAmbilight);
        assertEquals(0.40f, config.ambilightIntensity, 0.0001f);
        assertEquals(0.55f, config.ambilightSpread, 0.0001f);
        assertEquals(0.20f, config.ambilightSmoothing, 0.0001f);
        assertEquals(2, config.ambilightQuality);
    }

    @Test
    public void readPreferences_fallsBackOnInvalidAmbilightQuality() {
        prefs.edit()
                .putString("ambilight_quality", "invalid")
                .commit();

        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        assertEquals(1, config.ambilightQuality);
    }
}
