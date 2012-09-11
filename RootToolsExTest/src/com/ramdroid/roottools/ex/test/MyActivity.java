package com.ramdroid.roottools.ex.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.ramdroid.roottools.ex.AsyncShell;
import com.ramdroid.roottools.ex.ShellService;

import java.util.List;

public class MyActivity extends Activity {

    private Handler handler = new Handler();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ShellService.create(this, true, new AsyncShell.ResultListener() {
            @Override
            public void onFinished(int exitCode, final List<String> output) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        String result = "";
                        for (String s : output) {
                            result += s + "\n";
                        }
                        ((TextView) findViewById(R.id.result)).setText(result);
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        ShellService.destroy(this);
    }

    public void doSomething(View v) {
        ((TextView) findViewById(R.id.result)).setText("");
        ShellService.send(this, "ls /data/data | grep com.ramdroid");
    }
}
