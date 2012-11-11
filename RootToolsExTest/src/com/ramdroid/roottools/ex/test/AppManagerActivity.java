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

    private TextView mTextResult;
    private EditText mEditPackageName;
    private AppManager mAppManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appmanager);
        mTextResult = (TextView) findViewById(R.id.result);
        mEditPackageName = (EditText) findViewById(R.id.packageName);
        mEditPackageName.setText(getPackageName());
        mAppManager = AppManager.create(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAppManager.destroy();
    }

    public void appExistsOnData(View v) {
        mTextResult.setText("");

        mAppManager.appExistsOnPartition(mEditPackageName.getText().toString(), AppManager.PARTITION_DATA, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                mTextResult.setText("Result error code: " + errorCode);
            }
        });
    }

    public void appExistsOnSystem(View v) {
        mTextResult.setText("");

        mAppManager.appExistsOnPartition(mEditPackageName.getText().toString(), AppManager.PARTITION_SYSTEM, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                mTextResult.setText("Result error code: " + errorCode);
            }
        });
    }

    public void appExistsInTrash(View v) {
        mTextResult.setText("");

        mAppManager.appExistsOnPartition(mEditPackageName.getText().toString(), AppManager.PARTITION_TRASH, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                mTextResult.setText("Result error code: " + errorCode);
            }
        });
    }

    public void appFitsOnData(View v) {
        mTextResult.setText("");

        mAppManager.appFitsOnPartition(mEditPackageName.getText().toString(), AppManager.PARTITION_DATA, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                mTextResult.setText("Result error code: " + errorCode);
            }
        });
    }

    public void appFitsOnSystem(View v) {
        mTextResult.setText("");

        mAppManager.appFitsOnPartition(mEditPackageName.getText().toString(), AppManager.PARTITION_SYSTEM, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                mTextResult.setText("Result error code: " + errorCode);
            }
        });
    }

    public void appFitsInTrash(View v) {
        mTextResult.setText("");

        mAppManager.appFitsOnPartition(mEditPackageName.getText().toString(), AppManager.PARTITION_TRASH, new OutputListener() {
            @Override
            public void onResult(int errorCode, List<String> output) {
                mTextResult.setText("Result error code: " + errorCode);
            }
        });
    }
}
