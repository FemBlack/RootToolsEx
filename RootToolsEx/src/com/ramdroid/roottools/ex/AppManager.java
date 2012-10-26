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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * APIs to move an app from one partition to another.
 *
 * The {@link AppManager} class makes sure that all calls to a shell are not
 * executed in the UI thread.
 */
public class AppManager {

    public static final String PARTITION_DATA   = "data";
    public static final String PARTITION_SYSTEM = "system";
    public static final String PARTITION_TRASH  = ".roottools.trash";

    public static final int FLAG_OVERWRITE      = 1;
    public static final int FLAG_CHECKSPACE     = 2;
    public static final int FLAG_REBOOT         = 4;

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
            new ShellExec.Worker(
                    ShellExec.API_EX_APPEXISTSONPARTITION,
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
        new ShellExec.Worker(
                ShellExec.API_EX_APPFITSONPARTITION,
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
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                packageName,
                PARTITION_DATA,
                PARTITION_SYSTEM,
                listener).execute(FLAG_OVERWRITE | FLAG_CHECKSPACE);
    }

    /**
     * Moves an APK from DATA to SYSTEM partition and updates the APK if already existing.
     * The available disk space is first checked on the SYSTEM partition before trying to move the APK.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param addFlags use additional flags e.g. FLAG_REBOOT
     * @param listener returns the error code when job is finished
     */
    public static void installSystemAppEx(String packageName, int addFlags, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                packageName,
                PARTITION_DATA,
                PARTITION_SYSTEM,
                listener).execute(FLAG_OVERWRITE | FLAG_CHECKSPACE | addFlags);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param listener returns the error code when job is finished
     */
    public static void uninstallSystemApp(String packageName, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                packageName,
                PARTITION_SYSTEM,
                PARTITION_DATA,
                listener).execute(0);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param addFlags use additional flags e.g. FLAG_REBOOT
     * @param listener returns the error code when job is finished
     */
    public static void uninstallSystemAppEx(String packageName, int addFlags, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                packageName,
                PARTITION_SYSTEM,
                PARTITION_DATA,
                listener).execute(addFlags);
    }

    /**
     * Moves a package from one partition to another.
     *
     * The exact behavior can be customized using additional flags:
     *
     * FLAG_OVERWRITE   --> Overwrite the target if already existing
     * FLAG_CHECKSPACE  --> Check available diskspace before moving
     * FLAG_REBOOT      --> Immediately reboot device when done
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition Source partition
     * @param target Target partition
     * @param flags Additional flags
     * @param listener listener returns the error code when job is finished
     */
    public static void moveAppEx(String packageName, String partition, String target, int flags, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                packageName,
                partition,
                target,
                listener).execute(flags);
    }

    /**
     * Moves a couple of packages from one partition to another.
     *
     * The exact behavior can be customized using additional flags:
     *
     * FLAG_OVERWRITE   --> Overwrite the target if already existing
     * FLAG_CHECKSPACE  --> Check available diskspace before moving
     * FLAG_REBOOT      --> Immediately reboot device when done
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition Source partition
     * @param target Target partition
     * @param flags Additional flags
     * @param listener listener returns the error code when job is finished
     */
    public static void moveAppEx(List<String> packages, String partition, String target, int flags, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                packages,
                partition,
                target,
                listener).execute(flags);
    }

    /**
     * Blocking shell commands that are doing all the hard work.
     * They are called by the {@link ShellExec.Worker} to avoid blocking the UI thread.
     */
    static class Internal {

        private static final String TAG = "AppManager";

        private static class ResultSet {
            int errorCode;
            String filename;

            public ResultSet() {
                this.errorCode = ErrorCode.NOT_EXISTING;
                this.filename = null;
            }

            public ResultSet(int errorCode) {
                this.errorCode = errorCode;
                this.filename = null;
            }
        }

        public static int appExistsOnPartition(ShellExec exec, String packageName, String partition) {
            ResultSet result = getPackageFilename(exec, packageName, partition);
            return result.errorCode;
        }

        private static ResultSet getPackageFilename(ShellExec exec, String packageName, String partition) {
            ResultSet result = new ResultSet();
            if (!partition.equals(PARTITION_DATA)) {
                File root = new File(getPartitionPath(partition) + "/app/");
                File[] files = root.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (equalsPackage(f.getName(), packageName)) {
                            result.errorCode = ErrorCode.NONE;
                            result.filename = f.getPath();
                            break;
                        }
                    }
                }
            }
            else {
                result = Internal.getPackageFilenameWithRoot(exec, packageName, partition);
            }
            return result;
        }

        private static ResultSet getPackageFilenameWithRoot(ShellExec exec, final String packageName, String partition) {
            ResultSet result = new ResultSet();
            result.errorCode = exec.run("busybox ls " + getPartitionPath(partition) + "/app | grep " + packageName);
            if (result.errorCode == ErrorCode.NONE) {
                result.errorCode = ErrorCode.NOT_EXISTING;
                for (String line : exec.output) {
                    Log.d(TAG, line);
                    if (equalsPackage(line, packageName)) {
                        result.errorCode = ErrorCode.NONE;
                        result.filename = getPartitionPath(partition) + "/app/" + line;
                        break;
                    }
                    else if (line.contains("fail")) {
                        result.errorCode = ErrorCode.BUSYBOX;
                    }
                }
            }
            else if (result.errorCode == ErrorCode.COMMAND_FAILED) {
                // not found --> this is not an error
                result.errorCode = ErrorCode.NOT_EXISTING;
            }
            else {
                Log.d(TAG, "Error code: " + result.errorCode);
                for (String line : exec.output) {
                    Log.d(TAG, line);
                }
            }

            return result;
        }

        private static boolean equalsPackage(String line, String packageName) {
            boolean found = false;
            if (line.endsWith(".apk")) {
                int sep = line.lastIndexOf("-");
                if (sep <= 0) {
                    sep = line.lastIndexOf("~");
                }
                if (sep > 0) {
                    String parsedPackageName = line.substring(0, sep);
                    if (parsedPackageName.equals(packageName)) {
                        found = true;
                    }
                }
            }
            return found;
        }

        public static int appFitsOnPartition(ShellExec exec, String packageName, String partition) {
            String source = null;
            String[] partitions = { PARTITION_DATA, PARTITION_SYSTEM, PARTITION_TRASH };
            for (String p : partitions) {
                if (!partition.equals(p)) {
                    if (appExistsOnPartition(exec, packageName, p) == ErrorCode.NONE) {
                        source = p;
                        break;
                    }
                }
            }

            if (source != null) {
                ResultSet result = packageFitsOnPartition(exec, packageName, source, partition);
                return result.errorCode;
            }
            else return ErrorCode.ALREADY_EXISTING;
        }

        private static ResultSet packageFitsOnPartition(ShellExec exec, String packageName, String source, String target) {

            // get required disk space for APK
            long apkSpace = 0;
            ResultSet result = getPackageFilename(exec, packageName, source);
            if (result.errorCode == ErrorCode.NONE) {
                apkSpace = new File(result.filename).length();
                Log.d(TAG, "Found " + result.filename + " --> required disk space: " + apkSpace + " bytes");
            }
            else result.errorCode = ErrorCode.NOT_EXISTING;

            if (target.equals(PARTITION_TRASH)) {
                // special handling for SD card
                if (!RootTools.hasEnoughSpaceOnSdCard(apkSpace)) {
                    result.errorCode = ErrorCode.INSUFFICIENT_SPACE;
                }
            }
            else {
                // check if APK fits on target partition
                long freeDiskSpace = RootTools.getSpace(getPartitionPath(target)) * 1024;
                Log.d(TAG, "Available disk space on " + target + ": " + freeDiskSpace + " bytes");

                if ((apkSpace < 1) || (freeDiskSpace < apkSpace)) {
                    result.errorCode = ErrorCode.INSUFFICIENT_SPACE;
                }
            }
            return result;
        }

        private static String getPartitionPath(String partition) {
            String path = "/" + partition;
            if (partition.equals(PARTITION_TRASH)) {
                path = Environment.getExternalStorageDirectory().getPath() + "/" + PARTITION_TRASH;
            }
            return path;
        }

        public static int moveAppEx(ShellExec exec, List<String> packages, String sourcePartition, String targetPartition, int flags) {
            int errorCode = ErrorCode.NONE;

            // check disk space
            HashMap<String, String> table = new HashMap<String, String>();
            for (String packageName : packages) {
                ResultSet result = packageFitsOnPartition(exec, packageName, sourcePartition, targetPartition);
                if ((flags & AppManager.FLAG_CHECKSPACE) == AppManager.FLAG_CHECKSPACE) {
                    errorCode = result.errorCode;
                }
                table.put(packageName, result.filename);
                if (errorCode != ErrorCode.NONE) {
                    break;
                }
            }

            // create trash if not existing
            if (targetPartition.equals(PARTITION_TRASH)) {
                File trash = new File(getPartitionPath(targetPartition) + "/app");
                if (!trash.isDirectory()) {
                    if (!trash.mkdirs()) {
                        errorCode = ErrorCode.NO_EXTERNAL_STORAGE;
                    }
                }
            }

            if (errorCode == ErrorCode.NONE) {

                // mount system partition if needed
                boolean needRemountSystem = (sourcePartition.equals(PARTITION_SYSTEM) || targetPartition.equals(PARTITION_SYSTEM));
                if (needRemountSystem) {
                    if (!RootTools.remount("/system", "RW")) {
                        errorCode = ErrorCode.REMOUNT_SYSTEM;
                    }
                }

                if (errorCode == ErrorCode.NONE) {

                    for (String packageName : packages) {
                        String sourcePath = table.get(packageName);
                        String targetPath = sourcePath.replace(
                                getPartitionPath(sourcePartition) + "/app/",
                                getPartitionPath(targetPartition) + "/app/");

                        // prepare move command
                        String shellCmd = "busybox mv " + sourcePath + " " + targetPath;
                        if ((flags & FLAG_OVERWRITE) != FLAG_OVERWRITE) {
                            errorCode = appExistsOnPartition(exec, packageName, targetPartition);
                            if (errorCode == ErrorCode.NONE) {
                                // App is already existing in target partition, so just remove it from source partition
                                shellCmd = "busybox rm " + sourcePath;
                            }
                            else if (errorCode == ErrorCode.NOT_EXISTING) {
                                errorCode = ErrorCode.NONE;
                            }
                        }
                        if (errorCode == ErrorCode.NONE) {
                            // execute in shell
                            errorCode = exec.run(shellCmd);
                        }
                        if (errorCode != ErrorCode.NONE) {
                            // immediately abort on any errors
                            break;
                        }
                    }

                    if (needRemountSystem) {
                        // mount R/O
                        RootTools.remount("/system", "RO");
                    }

                    if (errorCode == ErrorCode.NONE) {
                        if ((flags & FLAG_REBOOT) > 0) {
                            exec.run("reboot");
                        }
                    }
                }
            }
            return errorCode;
        }
    }
}
