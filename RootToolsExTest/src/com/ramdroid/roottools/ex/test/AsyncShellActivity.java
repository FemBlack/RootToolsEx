package com.ramdroid.roottools.ex.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import com.ramdroid.roottools.ex.AsyncShell;
import com.ramdroid.roottools.ex.CommandBuilder;
import com.ramdroid.roottools.ex.ErrorCode;
import com.ramdroid.roottools.ex.ErrorCode.OutputListener;
import com.ramdroid.roottools.ex.ShellService;

import java.util.List;

public class AsyncShellActivity extends Activity {

    private TextView result;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asyncshell);
        result = (TextView) findViewById(R.id.result);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void gotRoot(View v) {
        result.setText("");
        AsyncShell.gotRoot(new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                if (errorCode == ErrorCode.NONE) {
                    result.setText(R.string.root_OK);
                }
                else {
                    result.setText(R.string.root_FAILED);
                }
            }
        });
    }

    public void gotBusybox(View v) {
        result.setText("");
        AsyncShell.gotBusybox(new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                if (errorCode == ErrorCode.NONE) {
                    result.setText(R.string.busybox_OK);
                }
                else {
                    result.setText(R.string.busybox_FAILED);
                }
            }
        });
    }

    public void doSomething(View v) {
        result.setText("");

        AsyncShell.send(new CommandBuilder()
                .useRoot()
                .add("ls /data/data | grep com.ramdroid")
                ,
                new OutputListener() {

            @Override
            public void onResult(int errorCode, List<String> output) {
                String result = "";
                for (String s : output) {
                    result += s + "\n";
                }
                ((TextView) findViewById(R.id.result)).setText(result);
            }
        });
    }
}
