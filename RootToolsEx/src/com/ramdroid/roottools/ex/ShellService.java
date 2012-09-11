package com.ramdroid.roottools.ex;

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
 */
public class ShellService extends Service {

    public static final String ACTION_SEND_SHELL_CMD = "com.ramdroid.roottools.ex.SEND_SHELL_CMD";

    public static final String REQUEST_RECEIVER_EXTRA = "ShellServiceRequestReceiverExtra";
    public static final int RESULT_ID_QUOTE = 42;

    private CommandReceiver receiver;
    private AsyncShell.Exec shellExec;
    private int commandId;
    private ResultReceiver resultReceiver;

    public static void create(Context context, final AsyncShell.ResultListener listener) {
        Intent i = new Intent(context, ShellService.class);
        i.putExtra(REQUEST_RECEIVER_EXTRA, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_ID_QUOTE) {
                    int exitCode = resultData.getInt("exitCode");
                    List<String> output = resultData.getStringArrayList("output");
                    listener.onFinished(exitCode, output);
                }
            }
        });
        context.startService(i);
    }

    public static void destroy(Context context) {
        context.stopService(new Intent(context, ShellService.class));
    }

    public static void send(Context context, String cmd) {
        Intent i = new Intent(ACTION_SEND_SHELL_CMD);
        i.putExtra("cmd", cmd);
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
        shellExec = new AsyncShell.Exec(useRoot);
        commandId = 0;

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

    class CommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String cmd = intent.getStringExtra("cmd");
            commandId += 1;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int exitCode = shellExec.run(commandId, cmd);

                    if (resultReceiver != null) {
                        Bundle resultData = new Bundle();
                        resultData.putInt("exitCode", exitCode);
                        resultData.putStringArrayList("output", shellExec.output);
                        resultReceiver.send(RESULT_ID_QUOTE, resultData);
                    }
                }
            }).start();
        }
    }
}
