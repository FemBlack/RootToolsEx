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

    private CommandReceiver receiver;
    private ShellExec shellExec;
    private ResultReceiver resultReceiver;

    /**
     * Starts the {@link ShellService} and prepares the result listener.
     *
     * Results from commands that are send to the service later are returned to the caller
     * in the {@link ErrorCode.OutputListener}.
     *
     * Please note that the result is coming from a different thread. So you have to make
     * sure that result is send back to the UI thread, for instance by using a {@link android.os.Handler}.
     *
     * @param context Context of the caller.
     * @param useRoot True if you need a root shell.
     * @param listener Returns the command result.
     */
    public static void start(Context context, boolean useRoot, final ErrorCode.OutputListener listener) {
        Intent i = new Intent(context, ShellService.class);
        i.putExtra("useRoot", useRoot);
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
        context.startService(i);
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
     * Sends a command to the {@link ShellService}. This is the
     * straight-forward solution if you only need to send one
     * simple command at a time.
     *
     * @param context Context of the caller.
     * @param cmd the command to execute in the shell.
     */
    public static void send(Context context, String cmd) {
        Bundle data = new Bundle();
        data.putString("cmd", cmd);
        Intent i = new Intent(ACTION_SEND_SHELL_CMD);
        i.putExtras(data);
        context.sendBroadcast(i);
    }

    /**
     * Sends one or more commands to the {@link ShellService}.
     * The {@link CommandBuilder} is used to construct the command.
     * In the future this allows to use more options like e.g. timeouts.
     *
     * @param context Context of the caller.
     * @param builder the {@link CommandBuilder} object
     */
    public static void send(Context context, CommandBuilder builder) {
        Bundle data = new Bundle();
        data.putParcelable("builder", builder);
        Intent i = new Intent(ACTION_SEND_SHELL_CMD);
        i.putExtras(data);
        context.sendBroadcast(i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        resultReceiver = intent.getParcelableExtra(REQUEST_RECEIVER_EXTRA);

        // init shell
        boolean useRoot = intent.getBooleanExtra("useRoot", true);
        shellExec = new ShellExec(useRoot);

        // register intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_SHELL_CMD);
        receiver = new CommandReceiver();
        registerReceiver(receiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        shellExec.destroy();
    }

    private class CommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle data = intent.getExtras();
            final String cmd = data.getString("cmd");
            final CommandBuilder builder = data.getParcelable("builder");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (shellExec) {
                        int errorCode = ErrorCode.NONE;
                        if (cmd != null) {
                            shellExec.run(cmd);
                        }
                        else if (builder != null) {
                            String[] commands = builder.commands.toArray(new String[builder.commands.size()]);
                            shellExec.run(commands);
                        }

                        if (resultReceiver != null) {
                            Bundle resultData = new Bundle();
                            resultData.putInt("errorCode", errorCode);
                            resultData.putStringArrayList("output", shellExec.output);
                            resultReceiver.send(RESULT_ID_QUOTE, resultData);
                        }
                    }
                }
            }).start();
        }
    }
}
