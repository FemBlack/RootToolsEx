package com.ramdroid.roottools.ex;

/**
 *    Copyright 2012 by Ronald Ammann (ramdroid)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;

import java.util.List;

/**
 * Service that keeps running in the background and waits for new shell commands.
 *
 * The {@link ShellService} class makes sure that all calls to a shell are not
 * executed in the UI thread.
 */
public class ShellService extends Service {

    private static final String ACTION_SEND_SHELL_CMD = "com.ramdroid.roottools.ex.SEND_SHELL_CMD";

    private static final String REQUEST_RECEIVER_EXTRA = "ShellServiceRequestReceiverExtra";
    private static final int RESULT_ID_QUOTE = 42;

    private CommandReceiver mReceiver;
    private ShellExec mShellExec;
    private Context mContext;

    /**
     * Initializes the {@link ShellService}.
     *
     * @param context Context of the caller.
     * @param useRoot True if you need a root shell.
     */
    public static void start(Context context, boolean useRoot) {
        context.startService(new Intent(context, ShellService.class).putExtra("useRoot", useRoot));
    }

    /**
     * Stops all action in the {@link ShellService} and terminates the service.
     *
     * @param context Context of the caller.
     */
    public static void stop(Context context) {
        context.stopService(new Intent(context, ShellService.class));
    }

    /**
     * Sends a command to the {@link ShellService}. This is the straight-forward solution if you only need
     * to send one simple command at a time.
     *
     * Please note that the result is coming from a different thread. So you have to make
     * sure that result is send back to the UI thread, for instance by using a {@link android.os.Handler}.
     *
     * @param context Context of the caller.
     * @param cmd the command to execute in the shell.
     * @param listener Returns the command result.
     */
    public static void send(Context context, String cmd, final ErrorCode.OutputListener listener) {
        Intent i = new Intent(ACTION_SEND_SHELL_CMD);
        i.putExtra("api", ShellExec.API_SEND);
        i.putExtra("cmd", cmd);
        i.putExtra(REQUEST_RECEIVER_EXTRA, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_ID_QUOTE) {
                    int errorCode = resultData.getInt("errorCode");
                    List<String> output = resultData.getStringArrayList("output");
                    listener.onResult(errorCode, output);
                }
            }
        });
        context.sendBroadcast(i);
    }

    /**
     * Sends one or more commands to the {@link ShellService}. The {@link ParamBuilder} is used to construct
     * the command. In the future this allows to use more options like e.g. timeouts.
     *
     * Please note that the result is coming from a different thread. So you have to make
     * sure that result is send back to the UI thread, for instance by using a {@link android.os.Handler}.
     *
     * @param context Context of the caller.
     * @param api The API call to send.
     * @param params the {@link ParamBuilder} object
     * @param listener Returns the command result.
     */
    public static void send(Context context, int api, ParamBuilder params, final ErrorCode.OutputListener listener) {
        Intent i = new Intent(ACTION_SEND_SHELL_CMD);
        i.putExtra("api", api);
        i.putExtra("params", params);
        i.putExtra(REQUEST_RECEIVER_EXTRA, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_ID_QUOTE) {
                    int errorCode = resultData.getInt("errorCode");
                    List<String> output = resultData.getStringArrayList("output");
                    listener.onResult(errorCode, output);
                }
            }
        });
        context.sendBroadcast(i);
    }

    public static void sendPackageCommand(Context context, int api, ParamBuilder params, final ErrorCode.OutputListenerWithPackages listener) {
        Intent i = new Intent(ACTION_SEND_SHELL_CMD);
        i.putExtra("api", api);
        i.putExtra("params", params);
        i.putExtra(REQUEST_RECEIVER_EXTRA, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_ID_QUOTE) {
                    int errorCode = resultData.getInt("errorCode");
                    List<PackageInfoEx> packages = resultData.getParcelableArrayList("packages");
                    listener.onResult(errorCode, packages);
                }
            }
        });
        context.sendBroadcast(i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            // init shell
            boolean useRoot = intent.getBooleanExtra("useRoot", true);
            mShellExec = new ShellExec(useRoot);

            // register intent receiver
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SEND_SHELL_CMD);
            mReceiver = new CommandReceiver();
            registerReceiver(mReceiver, filter);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mShellExec.clear();
    }

    private class CommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            final int api = intent.getIntExtra("api", ShellExec.API_SEND);
            final String cmd = intent.getStringExtra("cmd");
            final ParamBuilder params = intent.getParcelableExtra("params");
            final ResultReceiver resultReceiver = intent.getParcelableExtra(REQUEST_RECEIVER_EXTRA);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mShellExec) {
                        int errorCode = ErrorCode.NONE;
                        if (api == ShellExec.API_SEND) {
                            if (cmd != null) {
                                mShellExec.run(cmd);
                            }
                            else if (params != null) {
                                mShellExec.run(params.getTimeout(), params.getCommands());
                            }
                        }
                        else {
                            mShellExec.callApi(api, context, params);
                        }

                        if (resultReceiver != null) {
                            Bundle resultData = new Bundle();
                            resultData.putInt("errorCode", errorCode);
                            if (api == ShellExec.API_SEND) {
                                resultData.putStringArrayList("output", mShellExec.output);
                            }
                            else {
                                resultData.putParcelableArrayList("packages", mShellExec.packages);
                            }
                            resultReceiver.send(RESULT_ID_QUOTE, resultData);
                        }
                    }
                }
            }).start();
        }
    }
}
