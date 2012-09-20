package com.ramdroid.roottools.ex;

import android.os.AsyncTask;
import com.stericson.RootTools.RootTools;

/**
 * Wrapper around some common RootTools APIs.
 *
 * The {@link AsyncShell} class makes sure that all calls to a shell are not
 * executed in the UI thread.
 *
 * Note: there are also some APIs used by {@link AppMover} that are not
 * part of the RootTools APIs.
 */
public class AsyncShell {

    /**
     * Check if root access is available and if we can get access.
     *
     * @param listener Returns the result: NONE if OK, or NO_ROOT_ACCESS on failure
     */
    public static void gotRoot(ErrorCode.OutputListener listener) {
        new Worker(Worker.API_GOTROOT, listener).execute();
    }

    /**
     * Check if busybox is available.
     *
     * @param listener Returns the result: NONE if OK, or BUSYBOX on failure
     */
    public static void gotBusybox(ErrorCode.OutputListener listener) {
        new Worker(Worker.API_GOTBUSYBOX, listener).execute();
    }

    /**
     * Send a command to a shell.
     *
     * @param useRoot true if you need a root shell
     * @param command the command
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, String command, ErrorCode.OutputListener listener) {
        new Worker(Worker.API_EX_SEND, useRoot, command, listener).execute();    
    }

    /**
     * Send a list of commands to a shell.
     *
     * @param useRoot true if you need a root shell
     * @param commands the commands
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, String[] commands, ErrorCode.OutputListener listener) {
        new Worker(Worker.API_EX_SEND, useRoot, commands, listener).execute();
    }

    /**
     * Worker to execute all shell commands in a separate thread.
     */
    public static class Worker extends AsyncTask<Integer, Void, Integer> {

        // RootTools wrapper APIs
        public static final int API_GOTROOT                     = 1;
        public static final int API_GOTBUSYBOX                  = 2;

        // More APIs that are not part of the RootTools classes
        public static final int API_EX_SEND                     = 100;
        public static final int API_EX_APPEXISTSONPARTITION     = 101;
        public static final int API_EX_APPFITSONPARTITION       = 102;
        public static final int API_EX_MOVEAPPEX                = 103;

        private int api;
        private String packageName;
        private String partition;
        private String target;
        private String[] commands;
        private ErrorCode.OutputListener listener;
        private ShellExec exec;
        private boolean useRoot;

        public Worker(int api, ErrorCode.OutputListener listener) {
            this.api = api;
            this.listener = listener;
            this.useRoot = true;
        }
        
        public Worker(int api, boolean useRoot, String command, ErrorCode.OutputListener listener) {
            this.api = api;  
            this.useRoot = useRoot;
            this.commands[0] = command;
            this.listener = listener;
        }

        public Worker(int api, boolean useRoot, String[] commands, ErrorCode.OutputListener listener) {
            this.api = api;
            this.useRoot = useRoot;
            this.commands = commands;
            this.listener = listener;
        }

        public Worker(int api, String packageName, String partition, ErrorCode.OutputListener listener) {
            this.api = api;
            this.packageName = packageName;
            this.partition = partition;
            this.listener = listener;
            this.useRoot = true;
        }

        public Worker(int api, String packageName, String partition, String target, ErrorCode.OutputListener listener) {
            this.api = api;
            this.packageName = packageName;
            this.partition = partition;
            this.target = target;
            this.listener = listener;
            this.useRoot = true;
        }

        @Override
        protected Integer doInBackground(Integer... flags) {

            int errorCode = ErrorCode.NONE;
            exec = new ShellExec(useRoot);

            // fire up some action
            if (api == API_GOTROOT) {
                boolean gotRoot = false;
                try {
                    if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
                        gotRoot = true;
                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                errorCode = gotRoot ? ErrorCode.NONE : ErrorCode.NO_ROOT_ACCESS;
            }
            else if (api == API_GOTBUSYBOX) {
                String version = RootTools.getBusyBoxVersion();
                boolean available = (version != null && version.length() > 0);
                errorCode = available ? ErrorCode.NONE : ErrorCode.BUSYBOX;
            }
            else if (api == API_EX_SEND) {
                errorCode = exec.run(commands);
            }
            else if (api == API_EX_APPEXISTSONPARTITION) {
                errorCode = AppMover.Internal.appExistsOnPartition(exec, packageName, partition);
            }
            else if (api == API_EX_APPFITSONPARTITION) {
                errorCode = AppMover.Internal.appFitsOnPartition(packageName, partition);
            }
            else if (api == API_EX_MOVEAPPEX) {
                if ((flags[0] & AppMover.FLAG_CHECKSPACE) == AppMover.FLAG_CHECKSPACE) {
                    errorCode = AppMover.Internal.appFitsOnPartition(packageName, target);
                }
                if (errorCode == ErrorCode.NONE) {
                    errorCode = AppMover.Internal.moveAppEx(exec, packageName, partition, target, flags[0]);
                }
            }

            exec.destroy();

            return errorCode;
        }

        protected void onPostExecute(Integer errorCode) {
            if (listener != null) {
                listener.onResult(errorCode, exec.output);
            }
        }
    }
}
