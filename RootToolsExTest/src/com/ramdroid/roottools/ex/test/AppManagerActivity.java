package com.ramdroid.roottools.ex.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.ramdroid.roottools.ex.AppManager;
import com.ramdroid.roottools.ex.AsyncShell;
import com.ramdroid.roottools.ex.CommandBuilder;
import com.ramdroid.roottools.ex.ErrorCode;
import com.ramdroid.roottools.ex.ErrorCode.OutputListener;

import java.util.List;

public class AppManagerActivity extends Activity {

    private TextView textResult;
    private EditText editPackageName;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appmanager);
        textResult = (TextView) findViewById(R.id.result);
        editPackageName = (EditText) findViewById(R.id.packageName);
        editPackageName.setText(getPackageName());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void appExistsOnData(View v) {
        textResult.setText("");

        AppManager.appExistsOnPartition(editPackageName.getText().toString(), AppManager.PARTITION_DATA, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                textResult.setText("Result error code: " + errorCode);
            }
        });
    }

    public void appExistsOnSystem(View v) {
        textResult.setText("");

        AppManager.appExistsOnPartition(editPackageName.getText().toString(), AppManager.PARTITION_SYSTEM, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                textResult.setText("Result error code: " + errorCode);
            }
        });
    }
}
