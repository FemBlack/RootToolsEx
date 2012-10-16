package com.ramdroid.roottools.ex.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.ramdroid.roottools.ex.CommandBuilder;
import com.ramdroid.roottools.ex.ErrorCode;
import com.ramdroid.roottools.ex.ErrorCode.OutputListener;
import com.ramdroid.roottools.ex.ShellService;

import java.util.List;

public class ShellServiceActivity extends Activity {

    private Handler handler = new Handler();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shellservice);

    }

    @Override
    public void onResume() {
        super.onResume();
        ShellService.start(this, true, new ErrorCode.OutputListenerWithId() {
            @Override
            public void onResult(int id, int errorCode, final List<String> output) {
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

        ShellService.stop(this);
    }

    public void doSomething(View v) {
        ((TextView) findViewById(R.id.result)).setText("");
        ShellService.send(this, 42, "ls /data/data | grep com.ramdroid");
    }

    public void doSomeMore(View v) {
        ((TextView) findViewById(R.id.result)).setText("");
        ShellService.send(this, 43, new CommandBuilder().add("ls /data/dalvik-cache | grep com.ramdroid"));
    }

}
