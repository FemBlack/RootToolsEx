package com.ramdroid.roottools.ex;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Service that keeps running in the background and waits for new shell commands.
 */
public class ShellService extends Service {

    public static final String SEND_SHELL_CMD = "com.ramdroid.roottools.ex.SEND_SHELL_CMD";

    private CommandReceiver receiver;
    private AsyncShell.Exec shellExec;
    private int commandId;

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // init shell
        boolean useRoot = intent.getBooleanExtra("useRoot", true);
        shellExec = new AsyncShell.Exec(useRoot);
        commandId = 0;

        // register intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SEND_SHELL_CMD);
        receiver = new CommandReceiver();
        registerReceiver(receiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        unregisterReceiver(receiver);
        shellExec.destroy();
    }

    class CommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String cmd = intent.getStringExtra("cmd");
            commandId += 1;

            new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    shellExec.run(commandId, cmd);
                }
            }).start();
        }
    }
}
