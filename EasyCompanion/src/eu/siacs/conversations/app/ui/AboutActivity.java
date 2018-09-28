package eu.siacs.conversations.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Boolean dark = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getString("theme", "light").equals("dark");
        int mTheme = dark ? eu.siacs.conversations.app.R.style.ConversationsTheme_Dark : eu.siacs.conversations.app.R.style.ConversationsTheme;
        setTheme(mTheme);

        setContentView(eu.siacs.conversations.app.R.layout.activity_about);
    }
}
