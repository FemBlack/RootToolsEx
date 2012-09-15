package com.ramdroid.roottools.ex;

import android.os.AsyncTask;
import com.stericson.RootTools.RootTools;

/**
 * Wrapper around some common RootTools APIs.
 *
 * The {@link Common} class makes sure that all calls to a shell are not
 * executed in the UI thread.
 *
 * Note: there are also some APIs used by {@link AppMover} that are not
 * part of the RootTools APIs.
 */
public class Common {

    /**
     * Check if root access is available and if we can get access.
     *
     * @param listener Returns the result: NONE if OK, or NO_ROOT_ACCESS on failure
     */
    public static void gotRoot(ResultListener listener) {
        new Worker(Worker.API_GOTROOT, listener).execute();
    }

    /**
     * Check if busybox is available.
     *
     * @param listener Returns the result: NONE if OK, or BUSYBOX on failure
     */
    public static void gotBusybox(ResultListener listener) {
        new Worker(Worker.API_GOTBUSYBOX, listener).execute();
    }

    /**
     * Worker to execute all shell commands in a separate thread.
     */
    public static class Worker extends AsyncTask<Integer, Void, Integer> {

        // RootTools wrapper APIs
        public static final int API_GOTROOT                     = 1;
        public static final int API_GOTBUSYBOX                  = 2;

        // More APIs that are not part of the RootTools classes
        public static final int API_EX_APPEXISTSONPARTITION     = 100;
        public static final int API_EX_APPFITSONPARTITION       = 101;
        public static final int API_EX_MOVEAPPEX                = 102;

        private int api;
        private String packageName;
        private String partition;
        private String target;
        private ResultListener listener;
        private AsyncShell.Exec exec;

        public Worker(int api, ResultListener listener) {
            this.api = api;
            this.listener = listener;
        }

        public Worker(int api, String packageName, String partition, ResultListener listener) {
            this.api = api;
            this.packageName = packageName;
            this.partition = partition;
            this.listener = listener;
        }

        public Worker(int api, String packageName, String partition, String target, ResultListener listener) {
            this.api = api;
            this.packageName = packageName;
            this.partition = partition;
            this.target = target;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Integer... flags) {

            int errorCode = ErrorCode.NONE;
            exec = new AsyncShell.Exec(true);

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
                listener.onFinished(errorCode, exec.output);
            }
        }
    }
}
