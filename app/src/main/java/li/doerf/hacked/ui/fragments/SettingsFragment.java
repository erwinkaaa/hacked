package li.doerf.hacked.ui.fragments;


import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import li.doerf.hacked.BuildConfig;
import li.doerf.hacked.CustomEvent;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.utils.SynchronizationHelper;


/**
 * Created by moo on 01/12/15.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String LOGTAG = getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference versionPreference = findPreference("version");
        versionPreference.setSummary(String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        Preference devicePreference = findPreference("device");
        devicePreference.setSummary(String.format("%s %s / API %s", Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~Settings");
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(LOGTAG, "preference changed: " + key);
        if ( getString(R.string.pref_key_sync_enable).equals( key) ||
                getString( R.string.pref_key_sync_interval).equals( key) ||
                getString(R.string.pref_key_sync_via_cellular).equals(key)
                ) {
            boolean enabled = SynchronizationHelper.scheduleSync(getActivity().getApplicationContext());

            if ( enabled) {
                ((HackedApplication) getActivity().getApplication()).trackCustomEvent(CustomEvent.BACKGROUND_SYNC_ENABLED);
            } else {
                ((HackedApplication) getActivity().getApplication()).trackCustomEvent(CustomEvent.BACKGROUND_SYNC_DISABLED);
            }
        }
    }

}
