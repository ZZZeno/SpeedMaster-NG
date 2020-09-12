package com.prime.shimakaze

import com.prime.shimakaze.BuildConfig
import com.prime.shimakaze.R


import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById(R.id.settingstoolbar) as androidx.appcompat.widget.Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display the fragment as the main content.
        fragmentManager
                .beginTransaction()
                .replace(R.id.content_frame, SettingsFragment())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)

            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is PreferenceGroup) {
                    for (j in 0 until preference.preferenceCount) {
                        updatePreference(preference.getPreference(j))
                    }
                } else {
                    updatePreference(preference)
                }
            }
        }

        private fun updatePreference(preference: Preference) {
            if (preference.key == "version") {
                preference.setSummary(String.format("v%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            }
        }


    }
}


