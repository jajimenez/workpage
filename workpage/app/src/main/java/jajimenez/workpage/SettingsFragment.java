package jajimenez.workpage;

import android.preference.PreferenceFragment;
import android.os.Bundle;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences.
        addPreferencesFromResource(R.xml.preferences);
    }
}