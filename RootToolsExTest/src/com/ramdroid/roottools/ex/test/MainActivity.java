package com.ramdroid.roottools.ex.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Activity to provide a number of examples.
 */
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void onShellService(View v) {
        startActivity(new Intent(this, ShellServiceActivity.class));
    }

    public void onAsyncShell(View v) {
        startActivity(new Intent(this, AsyncShellActivity.class));
    }

    public void onAppManager(View v) {
        startActivity(new Intent(this, AppManagerActivity.class));
    }
}
