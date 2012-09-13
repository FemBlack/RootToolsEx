package com.ramdroid.roottools.ex;

import android.util.Log;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Moves an app from one partition to another.
 *
 * You could use the following high level functions:
 *
 * 1. Install as system app / Update system app
 *
 *    AppMover.installSystemApp("com.example.myapp");
 *
 * 2. Remove a system app (move back to DATA, if APK on DATA was not updated)
 *
 *    AppMover.removeSystemApp("com.example.myapp");
 *
 * If you need a different behaviour you can use the moveAppEx function.
 *
 * Note:
 * Before moving apps you should call appFitsOnPartition(...) to make sure the
 * APK fits on the target partition.
 */
public class AppMover {

    private static final String TAG = "AppMover";

    public static final String PARTITION_DATA           = "data";
    public static final String PARTITION_SYSTEM         = "system";

    public static final int FLAG_OVERWRITE              = 1;

    // TODO: move below functions into a private sub class
    // TODO: public wrapper functions that execute the private functions in a separate thread

    private static class ErrorWrapper {

        ErrorWrapper(int value) {
            this.value = value;
        }
        int value;
    }

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
     * @return the error code or ERROR_NONE if OK
     */
    public static int appExistsOnPartition(String packageName, String partition) {
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
            errorCode.value = appExistsOnPartitionWithRoot(packageName, partition);
        }
        return errorCode.value;
    }

    private static int appExistsOnPartitionWithRoot(final String packageName, String partition) {
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
            Shell rootShell = RootTools.getShell(true);
            rootShell.add(command).waitForFinish();
            rootShell.close();
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            errorCode.value = ErrorCode.BUSYBOX;
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            errorCode.value = ErrorCode.BUSYBOX;
        } catch (TimeoutException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            errorCode.value = ErrorCode.BUSYBOX;
        }

        return errorCode.value;
    }

    /**
     * Checks if the APK fits on the specified partition.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition PARTITION_DATA or PARTITION_SYSTEM
     * @return the error code or ERROR_NONE if OK
     */
    public static int appFitsOnPartition(String packageName, String partition) {
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

    /**
     * Moves an APK from DATA to SYSTEM partition and updates the APK if already existing.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @return the error code or ERROR_NONE if OK
     */
    public static int installSystemApp(String packageName) {
        return moveAppEx(packageName, PARTITION_DATA, PARTITION_SYSTEM, FLAG_OVERWRITE);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @return the error code or ERROR_NONE if OK
     */
    public static int removeSystemApp(String packageName) {
        return moveAppEx(packageName, PARTITION_SYSTEM, PARTITION_DATA, 0);
    }

    public static int moveAppEx(String packageName, String sourcePartition, String targetPartition, int flags) {
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
                errorCode.value = appExistsOnPartition(packageName, sourcePartition);
                if (errorCode.value == ErrorCode.NONE) {
                    // App is already existing in target partition, so just remove it from source partition
                    shellCmd = "busybox rm /" + sourcePartition + "/app/" + packageName + "*.apk";
                }
            }
            Log.d(TAG, shellCmd);

            if (errorCode.value == ErrorCode.NONE) {
                // move APK to system partition or vice versa
                Command command = new Command(1, shellCmd) {

                    @Override
                    public void output(int id, String line) {
                        if (id == 1 && line != null && line.length() > 0) {
                            Log.d(TAG, line);
                        }
                    }
                };
                try {
                    Shell rootShell = RootTools.getShell(true);
                    rootShell.add(command).waitForFinish();
                    rootShell.close();
                }
                catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    errorCode.value = ErrorCode.BUSYBOX;
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    errorCode.value = ErrorCode.BUSYBOX;
                } catch (TimeoutException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    errorCode.value = ErrorCode.BUSYBOX;
                }

                if (needRemountSystem) {
                    // mount R/O
                    RootTools.remount("/system", "RO");
                }
            }
        }
        return errorCode.value;
    }
}
