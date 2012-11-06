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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;

import com.stericson.RootTools.RootTools;

import java.io.*;
import java.net.URI;
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

    // Flags for moving apps
    public static final int FLAG_OVERWRITE      = 0x0001;
    public static final int FLAG_REBOOT         = 0x0002;

    // Flags for wiping trash
    public static final int FLAG_WIPEDATA       = 0x0010;
    public static final int FLAG_WIPECACHE      = 0x0020;

    // Flags for reading from trash
    public static final int FLAG_METADATA       = 0x0100;

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
                listener).execute(FLAG_OVERWRITE);
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
                listener).execute(FLAG_OVERWRITE | addFlags);
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
     * FLAG_REBOOT      --> Immediately reboot device when done
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition Source partition
     * @param target Target partition
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
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
     * Moves a couple of packages to trash and generates a metafile containing the application icon.
     *
     * @param context The application context is needed for accessing package manager
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition Source partition
     * @param listener returns the error code when job is finished
     */
    public static void moveAppToTrash(Context context, List<String> packages, String partition, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                context,
                packages,
                partition,
                PARTITION_TRASH,
                listener).execute(0);
    }

    /**
     * Moves a couple of packages to trash and generates a metafile containing the application icon.
     *
     * @param context The application context is needed for accessing package manager
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition Source partition
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
     */
    public static void moveAppToTrashEx(Context context, List<String> packages, String partition, int flags, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_MOVEAPPEX,
                context,
                packages,
                partition,
                PARTITION_TRASH,
                listener).execute(flags);
    }

    /**
     * Returns the package info for one app.
     *
     * @param packageName The package name.
     * @param flags Additional flags e.g. FLAG_METADATA
     * @return the package information
     */
    public static TrashPackageInfo getPackageFromTrash(String packageName, int flags) {
        return Internal.getPackageFromTrash(packageName, flags);
    }

    /**
     * Returns the list of all packages in the trash.
     *
     * @param flags Additional flags e.g. FLAG_METADATA
     * @return the list of package information records
     */
    public static ArrayList<TrashPackageInfo> getPackagesFromTrash(int flags) {
        return Internal.getPackagesFromTrash(flags);
    }

    /**
     * Remove packages from trash permanently. Erases cache and the data folder as well.
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param listener returns the error code when job is finished
     */
    public static void wipePackageFromTrash(List<String> packages, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_WIPEAPP,
                packages,
                PARTITION_TRASH,
                listener).execute(FLAG_WIPECACHE | FLAG_WIPEDATA);
    }

    /**
     * Remove packages from a partition permanently. The exact behavior can be customized with some flags.
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition The partition to wipe the packages from e.g. PARTITION_TRASH
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
     */
    public static void wipePackageFromPartitionEx(List<String> packages, String partition, int flags, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(
                ShellExec.API_EX_WIPEAPP,
                packages,
                partition,
                listener).execute(0);
    }

    /**
     * The PackageManager APIs including PackageInfo (containing the application's name and icon) obviously isn't
     * available anymore after an app has been moved to trash. Therefore a separate file is maintained for each trashed
     * package that contains such information.
     *
     * The packages can be parsed with the getPackagesFromTrash() function in {@link AppManager}.
     */
    public static class TrashPackageInfo {

        private final static int VERSION = 1;

        private String filename;
        private String packageName;
        private String partition;
        private String label;
        private Bitmap icon;

        public TrashPackageInfo() {
            this.filename = "";
            this.packageName = "";
            this.partition = "";
            this.label = "";
        }

        /**
         * @return The full path to the APK.
         */
        public String getFilename() {
            return filename;
        }

        /**
         * @return The package name of the app.
         */
        public String getPackageName() {
            return packageName;
        }

        /**
         * @return The partition the APK was originally existing on
         */
        public String getPartition() {
            return partition;
        }

        /**
         * @return The full name of the app.
         */
        public String getLabel() {
            return label;
        }

        /**
         * @return The default icon of the app..
         */
        public Bitmap getIcon() {
            return icon;
        }

        private TrashPackageInfo(String filename) {
            this.filename = filename;
            this.packageName = "";
            this.partition = "";
            this.label = "";
        }

        /**
         * Generate a meta file for a package.
         *
         * @param context The application context.
         * @param packageName The package name that identifies the app.
         * @param filename The filename of the APK.
         * @param partition The source partition of the APK
         * @return The error code or ErrorCode.NONE when successful.
         */
        private static int put(Context context, String packageName, String filename, String partition) {
            int errorCode = ErrorCode.NONE;
            if (context == null) {
                errorCode = ErrorCode.INVALID_CONTEXT;
            }

            TrashPackageInfo packageInfo = new TrashPackageInfo();
            packageInfo.partition = partition;

            // read application icon
            if (errorCode == ErrorCode.NONE) {
                try {
                    PackageManager pm = context.getPackageManager();
                    ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                    packageInfo.label = pm.getApplicationLabel(ai).toString();
                    packageInfo.icon = ((BitmapDrawable) pm.getApplicationIcon(packageName)).getBitmap();
                } catch (PackageManager.NameNotFoundException e) {
                    errorCode = ErrorCode.NOT_EXISTING;
                }
            }

            // write bitmap into output file
            if (errorCode == ErrorCode.NONE) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packageInfo.icon.compress(Bitmap.CompressFormat.PNG, 100, stream);

                OutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(filename.replace(".apk", ".metadata")));
                    out.write(VERSION);
                    out.write(packageInfo.label.length());
                    out.write(packageInfo.label.getBytes());
                    out.write(packageName.length());
                    out.write(packageName.getBytes());
                    out.write(partition.length());
                    out.write(partition.getBytes());
                    out.write(stream.toByteArray());
                }
                catch(FileNotFoundException e) {
                }
                catch (IOException e) {
                }

                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
            return errorCode;
        }

        /**
         * Read the package info from a meta file. By default only the package name and application label is read.
         * If you need to get the default icon as well then you have to add FLAG_METADATA in the flags.
         *
         * @param filename The meta file name.
         * @param flags Additional flags.
         * @return The package info containing all details.
         */
        private static TrashPackageInfo get(String filename, int flags) {
            TrashPackageInfo packageInfo = new TrashPackageInfo(filename.replace(".metadata", ".apk"));
            InputStream in = null;

            try {
                in = new BufferedInputStream(new FileInputStream(filename));
                int version = in.read();
                if (version == VERSION) {

                    // read application label
                    int len = in.read();
                    for (int i=0; i<len; ++i) {
                        packageInfo.label += (char)in.read();
                    }

                    // read package name
                    len = in.read();
                    for (int i=0; i<len; ++i) {
                        packageInfo.packageName += (char)in.read();
                    }

                    // read source partition
                    len = in.read();
                    for (int i=0; i<len; ++i) {
                        packageInfo.partition += (char)in.read();
                    }

                    // read application icon
                    if ((flags & FLAG_METADATA) > 0) {
                        byte[] buffer = new byte[100000];
                        len = in.read(buffer);
                        packageInfo.icon = BitmapFactory.decodeByteArray(buffer, 0, len);
                    }
                }
            }
            catch(FileNotFoundException e) {
            }
            catch (IOException e) {
            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }

            return packageInfo;
        }
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
            return equalsFilename(line, packageName, ".apk");
        }

        private static boolean equalsFilename(String line, String packageName, String ext) {
            boolean found = false;
            if (line.endsWith(ext)) {
                int sep = line.lastIndexOf("-");
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

            if (result.errorCode == ErrorCode.NONE) {
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

        public static int moveAppEx(ShellExec exec, Context context, List<String> packages, String sourcePartition, String targetPartition, int flags) {
            int errorCode = ErrorCode.NONE;

            // check disk space
            HashMap<String, String> table = new HashMap<String, String>();
            for (String packageName : packages) {
                ResultSet result = packageFitsOnPartition(exec, packageName, sourcePartition, targetPartition);
                if (result.errorCode != ErrorCode.NONE) {
                    errorCode = result.errorCode;
                    break;
                }
                table.put(packageName, result.filename);
            }

            if (errorCode == ErrorCode.NONE) {

                // create trash if not existing
                if (targetPartition.equals(PARTITION_TRASH)) {
                    File trash = new File(getPartitionPath(targetPartition) + "/app");
                    if (!trash.isDirectory()) {
                        if (!trash.mkdirs()) {
                            errorCode = ErrorCode.NO_EXTERNAL_STORAGE;
                        }
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
                        // check for valid source path
                        String sourcePath = table.get(packageName);
                        if (sourcePath == null) {
                            errorCode = ErrorCode.INTERNAL;
                        }
                        // is this app odexed?
                        boolean isOdexed = new File(sourcePath.replace(".apk", ".odex")).exists();
                        if (isOdexed && (
                                !targetPartition.equals(PARTITION_TRASH) ||
                                !sourcePartition.equals(PARTITION_TRASH))
                                ) {
                            errorCode = ErrorCode.ODEX_NOT_SUPPORTED;
                        }

                        if (errorCode == ErrorCode.NONE) {
                            String targetPath = sourcePath.replace(
                                    getPartitionPath(sourcePartition) + "/app/",
                                    getPartitionPath(targetPartition) + "/app/");

                            if (targetPartition.equals(PARTITION_TRASH)) {
                                // generate meta data for packages moved to trash
                                TrashPackageInfo.put(context, packageName, targetPath, sourcePartition);
                            }

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
                            if (errorCode == ErrorCode.NONE) {
                                // move odex files to trash
                                if (isOdexed && (
                                        targetPartition.equals(PARTITION_TRASH) ||
                                        sourcePartition.equals(PARTITION_TRASH))
                                        ) {
                                    shellCmd = "busybox mv " +
                                            sourcePath.replace(".apk", ".odex") + " " +
                                            targetPath.replace(".apk", ".odex");
                                    errorCode = exec.run(shellCmd);
                                }
                            }
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


        private static TrashPackageInfo getPackageFromTrash(String packageName, int flags) {
            TrashPackageInfo output = null;
            File root = new File(getPartitionPath(PARTITION_TRASH) + "/app/");
            File[] files = root.listFiles();
            if (files != null) {
                for (File f : files) {
                    String filename = f.getPath();
                    if (filename.endsWith(".metadata")) {
                        if (equalsFilename(f.getName(), packageName, ".metadata")) {
                            output = TrashPackageInfo.get(filename, flags);
                            break;
                        }
                    }
                }
            }
            return output;
        }

        private static ArrayList<TrashPackageInfo> getPackagesFromTrash(int flags) {
            ArrayList<TrashPackageInfo> output = new ArrayList<TrashPackageInfo>();
            File root = new File(getPartitionPath(PARTITION_TRASH) + "/app/");
            File[] files = root.listFiles();
            if (files != null) {
                for (File f : files) {
                    String filename = f.getPath();
                    if (filename.endsWith(".metadata")) {
                        TrashPackageInfo info = TrashPackageInfo.get(filename, flags);
                        output.add(info);
                    }
                }
            }
            return output;
        }

        public static int wipePackages(ShellExec exec, List<String> packages, String partition, int flags) {
            int errorCode = ErrorCode.NONE;
            File root = new File(getPartitionPath(partition) + "/app/");
            File[] files = root.listFiles();
            if (files != null) {
                for (String packageName : packages) {
                    boolean found = false;
                    for (File f : files) {
                        if (equalsPackage(f.getName(), packageName)) {
                            f.delete();

                            // don't forget odex file
                            File odexfile = new File(f.getPath().replace(".apk", ".odex"));
                            if (odexfile.exists()) {
                                odexfile.delete();
                            }

                            // don't forget metafile, too
                            File metafile = new File(f.getPath().replace(".apk", ".metadata"));
                            if (metafile.exists()) {
                                metafile.delete();
                            }

                            found = true;
                        }
                    }
                    if (!found) {
                        exec.output.add("Package not found: " + packageName);
                        errorCode = ErrorCode.NOT_EXISTING;
                    }
                }
            }

            if (errorCode == ErrorCode.NONE) {
                if ((flags & FLAG_WIPECACHE) > 0) {
                    final String CACHE = "/data/dalvik-cache/";
                    errorCode = exec.run("busybox ls " + CACHE);
                    if (errorCode == ErrorCode.NONE) {

                        // parse files in cache
                        List<String> queue = new ArrayList<String>();
                        for (String line : exec.output) {
                            String[] sl = line.split("@");
                            for (String s : sl) {
                                if (s.endsWith(".apk")) {
                                    for (String packageName : packages) {
                                        if (equalsPackage(s, packageName)) {
                                            queue.add(line);
                                        }
                                    }
                                    break;
                                }
                            }
                        }

                        // remove them one by one
                        for (String filename : queue) {
                            errorCode = exec.run("busybox rm " + CACHE + filename);
                            if (errorCode != ErrorCode.NONE) {
                                break;
                            }
                        }
                    }
                }
            }

            if (errorCode == ErrorCode.NONE) {
                if ((flags & FLAG_WIPEDATA) > 0) {
                    final String DATA = "/data/data/";
                    for (String packageName : packages) {
                        errorCode = exec.run("busybox rm -r " + DATA + packageName);
                        if (errorCode != ErrorCode.NONE) {
                            break;
                        }
                    }
                }
            }

            return errorCode;
        }
    }
}
