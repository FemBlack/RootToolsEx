package com.ramdroid.roottools.ex.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.ramdroid.roottools.ex.ParamBuilder;
import com.ramdroid.roottools.ex.ErrorCode;
import com.ramdroid.roottools.ex.ShellService;

import java.util.List;

public class ShellServiceActivity extends Activity {

    private Handler mHandler = new Handler();

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
        ShellService.start(this, true);
    }

    @Override
    public void onPause() {
        super.onPause();

        ShellService.stop(this);
    }

    public void doSomething(View v) {
        ((TextView) findViewById(R.id.result)).setText("");
        ShellService.send(this, "ls /data/data | grep com.ramdroid", mResultListener);
    }

    public void doSomeMore(View v) {
        ((TextView) findViewById(R.id.result)).setText("");
        ShellService.send(this, "ls /data/dalvik-cache | grep com.ramdroid", mResultListener);
    }

    private ErrorCode.OutputListener mResultListener = new ErrorCode.OutputListener() {
        @Override
        public void onResult(int errorCode, final List<String> output) {
            mHandler.post(new Runnable() {

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
    };

}
