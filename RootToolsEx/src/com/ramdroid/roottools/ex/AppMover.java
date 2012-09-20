package com.ramdroid.roottools.ex;

import android.util.Log;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.ArrayList;

/**
 * APIs to move an app from one partition to another.
 *
 * The {@link AppMover} class makes sure that all calls to a shell are not
 * executed in the UI thread.
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
    public static void appExistsOnPartition(String packageName, String partition, ErrorCode.OutputListener listener) {

        // do a quick search if we don't need a root shell
        if (partition.equals(PARTITION_SYSTEM)) {
            int errorCode = Internal.appExistsOnPartition(null, packageName, partition);
            listener.onResult(errorCode, new ArrayList<String>());
        }
        else {
            // otherwise pull up a root shell in a separate thread
            new AsyncShell.Worker(
                    com.ramdroid.roottools.ex.AsyncShell.Worker.API_EX_APPEXISTSONPARTITION,
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
    public static void appFitsOnPartition(String packageName, String partition, ErrorCode.OutputListener listener) {
        new AsyncShell.Worker(
                com.ramdroid.roottools.ex.AsyncShell.Worker.API_EX_APPFITSONPARTITION,
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
    public static void installSystemApp(String packageName, ErrorCode.OutputListener listener) {
        new AsyncShell.Worker(
                com.ramdroid.roottools.ex.AsyncShell.Worker.API_EX_MOVEAPPEX,
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
    public static void removeSystemApp(String packageName, ErrorCode.OutputListener listener) {
        new AsyncShell.Worker(
                com.ramdroid.roottools.ex.AsyncShell.Worker.API_EX_MOVEAPPEX,
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
    public static void moveAppEx(String packageName, String partition, String target, int flags, ErrorCode.OutputListener listener) {
        new AsyncShell.Worker(
                com.ramdroid.roottools.ex.AsyncShell.Worker.API_EX_MOVEAPPEX,
                packageName,
                partition,
                target,
                listener).execute(flags);
    }

    /**
     * Blocking shell commands that are doing all the hard work.
     * They are called by the {@link AsyncShell.Worker} to avoid blocking the UI thread.
     */
    static class Internal {

        private static final String TAG = "AppMover";

        public static int appExistsOnPartition(ShellExec exec, String packageName, String partition) {
            int errorCode = ErrorCode.NOT_EXISTING;
            if (partition.equals(PARTITION_SYSTEM)) {
                File root = new File("/" + partition + "/app/");
                File[] files = root.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().contains(packageName)) {
                            Log.d(TAG, "Found " + f.getPath());
                            errorCode = ErrorCode.NONE;
                            break;
                        }
                    }
                }
            }
            else {
                return Internal.appExistsOnPartitionWithRoot(exec, packageName, partition);
            }
            return errorCode;
        }

        public static int appExistsOnPartitionWithRoot(ShellExec exec, final String packageName, String partition) {
            int errorCode = ErrorCode.NOT_EXISTING;

            int exitCode = exec.run("busybox ls /" + partition + "/app | grep " + packageName);
            if (exitCode == 0) {
                for (String line : exec.output) {
                    Log.d(TAG, line);
                    if (line.contains(packageName)) {
                        errorCode = ErrorCode.NONE;
                    }
                    else if (line.contains("fail")) {
                        errorCode = ErrorCode.BUSYBOX;
                    }
                }
            }
            else {
                errorCode = ErrorCode.BUSYBOX;
            }

            return errorCode;
        }

        public static int appFitsOnPartition(String packageName, String partition) {
            int errorCode = ErrorCode.INSUFFICIENT_SPACE;
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
                errorCode = ErrorCode.NONE;
            }
            return errorCode;
        }

        public static int moveAppEx(ShellExec exec, String packageName, String sourcePartition, String targetPartition, int flags) {
            int errorCode = ErrorCode.NONE;
            boolean needRemountSystem = (sourcePartition.equals(PARTITION_SYSTEM) || targetPartition.equals(PARTITION_SYSTEM));
            if (needRemountSystem) {
                if (!RootTools.remount("/system", "RW")) {
                    errorCode = ErrorCode.REMOUNT_SYSTEM;
                }
            }

            if (errorCode == ErrorCode.NONE) {
                // install or remove system app
                String shellCmd = "busybox mv /" + sourcePartition + "/app/" + packageName + "*.apk /" + targetPartition + "/app/";
                if ((flags & FLAG_OVERWRITE) != FLAG_OVERWRITE) {
                    errorCode = appExistsOnPartition(exec, packageName, targetPartition);
                    if (errorCode == ErrorCode.NONE) {
                        // App is already existing in target partition, so just remove it from source partition
                        shellCmd = "busybox rm /" + sourcePartition + "/app/" + packageName + "*.apk";
                    }
                    else if (errorCode == ErrorCode.NOT_EXISTING) {
                        errorCode = ErrorCode.NONE;
                    }
                }
                Log.d(TAG, shellCmd);

                if (errorCode == ErrorCode.NONE) {
                    // move APK to system partition or vice versa
                    errorCode = exec.run(shellCmd);
                }

                if (needRemountSystem) {
                    // mount R/O
                    RootTools.remount("/system", "RO");
                }
            }
            return errorCode;
        }
    }
}
