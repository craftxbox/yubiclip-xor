package com.yubico.yubiclip;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by dain on 2/17/14.
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        

    }
}
