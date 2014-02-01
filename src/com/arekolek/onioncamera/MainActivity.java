
package com.arekolek.onioncamera;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    static final String TAG = "OnionCamera";

    private Preview preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preview = (Preview) findViewById(R.id.preview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        preview.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.pause();
    }

}
