package com.ramdroid.roottools.ex;

import android.os.AsyncTask;
import android.util.Log;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * APIs to move an app from one partition to another.
 *
 */
public class AppMover {

    public static final String PARTITION_DATA   = "data";
    public static final String PARTITION_SYSTEM = "system";

    public static final int FLAG_OVERWRITE      = 1;
    public static final int FLAG_CHECKSPACE     = 2;

    /**
     * Check if an app is installed on a particular partition or not.
     *
     * When searching for an APK on the SYSTEM partition then root access is not required and we can just use
     * Java functions to parse the files.
     *
     * When searching for an APK on the DATA partition then we have no read access and need to launch a busybox
     * command instead.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition PARTITION_DATA or PARTITION_SYSTEM
     * @param listener returns the error code when job is finished
     */
    public static void appExistsOnPartition(String packageName, String partition, ResultListener listener) {

        // do a quick search if we don't need a root shell
        if (partition.equals(PARTITION_SYSTEM)) {
            int errorCode = Internal.appExistsOnPartition(null, packageName, partition);
            listener.onFinished(errorCode, new ArrayList<String>());
        }
        else {
            // otherwise pull up a root shell in a separate thread
            new Worker(
                    Worker.API_APPEXISTSONPARTITION,
                    packageName,
                    partition,
                    listener).execute();
        }
    }

    /**
     * Checks if the APK fits on the specified partition.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition PARTITION_DATA or PARTITION_SYSTEM
     * @param listener returns the error code when job is finished
     */
    public static void appFitsOnPartition(String packageName, String partition, ResultListener listener) {
        new Worker(
                Worker.API_APPFITSONPARTITION,
                packageName,
                partition,
                listener).execute();
    }

    /**
     * Moves an APK from DATA to SYSTEM partition and updates the APK if already existing.
     * The available disk space is first checked on the SYSTEM partition before trying to move the APK.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param listener returns the error code when job is finished
     */
    public static void installSystemApp(String packageName, ResultListener listener) {
        new Worker(
                Worker.API_MOVEAPPEX,
                packageName,
                PARTITION_DATA,
                PARTITION_SYSTEM,
                listener).execute(FLAG_OVERWRITE | FLAG_CHECKSPACE);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param listener returns the error code when job is finished
     */
    public static void removeSystemApp(String packageName, ResultListener listener) {
        new Worker(
                Worker.API_MOVEAPPEX,
                packageName,
                PARTITION_SYSTEM,
                PARTITION_DATA,
                listener).execute(0);
    }

    /**
     * Moves a package from one partition to another.
     *
     * The exact behavior can be customized using additional flags:
     *
     * FLAG_OVERWRITE   --> Overwrite the target if already existing
     * FLAG_CHECKSPACE  --> Check available diskspace before moving
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition Source partition
     * @param target Target partition
     * @param flags Additional flags
     * @param listener listener returns the error code when job is finished
     */
    public static void moveAppEx(String packageName, String partition, String target, int flags, ResultListener listener) {
        new Worker(
                Worker.API_MOVEAPPEX,
                packageName,
                partition,
                target,
                listener).execute(flags);
    }

    /**
     * Worker to execute all shell commands in a separate thread.
     */
    private static class Worker extends AsyncTask<Integer, Void, Integer> {

        private static final int API_APPEXISTSONPARTITION   = 1;
        private static final int API_APPFITSONPARTITION     = 2;
        private static final int API_MOVEAPPEX              = 3;

        private String packageName;
        private String partition;
        private String target;
        private ResultListener listener;
        private ArrayList<String> output = new ArrayList<String>();
        private int api;

        private Worker(int api, String packageName, String partition, ResultListener listener) {
            this.api = api;
            this.packageName = packageName;
            this.partition = partition;
            this.listener = listener;
        }

        private Worker(int api, String packageName, String partition, String target, ResultListener listener) {
            this.api = api;
            this.packageName = packageName;
            this.partition = partition;
            this.target = target;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Integer... flags) {

            // TODO: use AsyncShell.Exec instead

            // get ourselves a root shell
            int errorCode = ErrorCode.NONE;
            Shell rootShell = null;
            try {
                rootShell = RootTools.getShell(true);
            } catch (IOException e) {
                output.add(e.toString());
                errorCode = ErrorCode.NO_ROOT_SHELL;
            } catch (TimeoutException e) {
                output.add(e.toString());
                errorCode = ErrorCode.NO_ROOT_SHELL;
            }

            if (rootShell != null) {
                // fire some action
                if (api == API_APPEXISTSONPARTITION) {
                    errorCode = Internal.appExistsOnPartition(rootShell, packageName, partition);
                }
                else if (api == API_APPFITSONPARTITION) {
                    errorCode = Internal.appFitsOnPartition(packageName, partition);
                }
                else if (api == API_MOVEAPPEX) {
                    if ((flags[0] & FLAG_CHECKSPACE) == FLAG_CHECKSPACE) {
                        errorCode = Internal.appFitsOnPartition(packageName, target);
                    }
                    if (errorCode == ErrorCode.NONE) {
                        errorCode = Internal.moveAppEx(rootShell, packageName, partition, target, flags[0]);
                    }
                }

                // close shell
                try {
                    rootShell.close();
                } catch (IOException e) {
                }
            }

            return errorCode;
        }

        protected void onPostExecute(Integer errorCode) {
            if (listener != null) {
                listener.onFinished(errorCode, output);
            }
        }
    }

    /**
     * Blocking shell commands that are doing all the hard work.
     * They are called by the {@link Worker} to avoid blocking the UI thread.
     */
    static class Internal {

        private static final String TAG = "AppMover";

        private static class ErrorWrapper {

            ErrorWrapper(int value) {
                this.value = value;
            }
            int value;
        }

        private static int appExistsOnPartition(Shell rootShell, String packageName, String partition) {
            final ErrorWrapper errorCode = new ErrorWrapper(ErrorCode.NOT_EXISTING);
            if (partition.equals(PARTITION_SYSTEM)) {
                File root = new File("/" + partition + "/app/");
                File[] files = root.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().contains(packageName)) {
                            Log.d(TAG, "Found " + f.getPath());
                            errorCode.value = ErrorCode.NONE;
                            break;
                        }
                    }
                }
            }
            else {
                return Internal.appExistsOnPartitionWithRoot(rootShell, packageName, partition);
            }
            return errorCode.value;
        }

        private static int appExistsOnPartitionWithRoot(Shell rootShell, final String packageName, String partition) {
            final ErrorWrapper errorCode = new ErrorWrapper(ErrorCode.NOT_EXISTING);
            Command command = new Command(0, "busybox ls /" + partition + "/app | grep " + packageName) {

                @Override
                public void output(int id, String line) {
                    if (id == 0 && line != null && line.length() > 0) {
                        Log.d(TAG, line);
                        if (line.contains(packageName)) {
                            errorCode.value = ErrorCode.NONE;
                        }
                        else if (line.contains("fail")) {
                            errorCode.value = ErrorCode.BUSYBOX;
                        }
                    }
                }
            };
            try {
                rootShell.add(command).waitForFinish();
            }
            catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                errorCode.value = ErrorCode.BUSYBOX;
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                errorCode.value = ErrorCode.BUSYBOX;
            }

            return errorCode.value;
        }

        private static int appFitsOnPartition(String packageName, String partition) {
            final ErrorWrapper errorCode = new ErrorWrapper(ErrorCode.INSUFFICIENT_SPACE);
            long freeDiskSpace = RootTools.getSpace("/" + partition);
            long apkSpace = 0;

            String appPartition = PARTITION_DATA;
            if (partition.equals(PARTITION_DATA)) {
                appPartition = PARTITION_SYSTEM;
            }

            File apk1 = new File("/" + appPartition + "/app/" + packageName + "-1.apk");
            File apk2 = new File("/" + appPartition + "/app/" + packageName + "-2.apk");
            apkSpace = apk1.length() / 1024;
            if (apkSpace < 1) {
                apkSpace = apk2.length() / 1024;
            }

            Log.d(TAG, "Available disk space on " + partition + ": " + freeDiskSpace);
            Log.d(TAG, "Required disk space for APK: " + apkSpace);

            if ((apkSpace > 0) && (freeDiskSpace > apkSpace)) {
                errorCode.value = ErrorCode.NONE;
            }
            return errorCode.value;
        }

        private static int moveAppEx(Shell rootShell, String packageName, String sourcePartition, String targetPartition, int flags) {
            final ErrorWrapper errorCode = new ErrorWrapper(ErrorCode.NONE);
            boolean needRemountSystem = (sourcePartition.equals(PARTITION_SYSTEM) || targetPartition.equals(PARTITION_SYSTEM));
            if (needRemountSystem) {
                if (!RootTools.remount("/system", "RW")) {
                    errorCode.value = ErrorCode.REMOUNT_SYSTEM;
                }
            }

            if (errorCode.value == ErrorCode.NONE) {
                // install or remove system app
                String shellCmd = "busybox mv /" + sourcePartition + "/app/" + packageName + "*.apk /" + targetPartition + "/app/";
                if ((flags & FLAG_OVERWRITE) != FLAG_OVERWRITE) {
                    errorCode.value = appExistsOnPartition(rootShell, packageName, sourcePartition);
                    if (errorCode.value == ErrorCode.NONE) {
                        // App is already existing in target partition, so just remove it from source partition
                        shellCmd = "busybox rm /" + sourcePartition + "/app/" + packageName + "*.apk";
                    }
                    else if (errorCode.value == ErrorCode.NOT_EXISTING) {
                        errorCode.value = ErrorCode.NONE;
                    }
                }
                Log.d(TAG, shellCmd);

                if (errorCode.value == ErrorCode.NONE) {
                    // move APK to system partition or vice versa
                    Command command = new Command(1, new String[] { shellCmd }) {

                        @Override
                        public void output(int id, String line) {
                            if (id == 1 && line != null && line.length() > 0) {
                                Log.d(TAG, line);
                            }
                        }
                    };
                    try {
                        rootShell.add(command).waitForFinish();
                    }
                    catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        errorCode.value = ErrorCode.BUSYBOX;
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        errorCode.value = ErrorCode.BUSYBOX;
                    }
                }

                if (needRemountSystem) {
                    // mount R/O
                    RootTools.remount("/system", "RO");
                }
            }
            return errorCode.value;
        }
    }
}
