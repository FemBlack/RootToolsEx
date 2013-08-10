package com.ramdroid.roottools.ex;

/**
 *    Copyright 2012-2013 by Ronald Ammann (ramdroid)

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
import android.os.Environment;
import android.util.Log;

import com.stericson.RootTools.RootTools;

import java.io.*;
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

    // Flags for reading package information
    public static final int FLAG_METADATA       = 0x0100;
    public static final int FLAG_LAUNCHABLE     = 0x0200;
    public static final int FLAG_IGNORE_ODEXED  = 0x0400;

    public static AppManager create(Context context) {
        return new AppManager(context, true);
    }

    public static AppManager createWithoutService(Context context) {
        return new AppManager(context, false);
    }

    public void destroy() {
        ShellService.stop(mContext);
    }

    private AppManager(Context context, boolean needService) {
        mContext = context;
        if (needService) {
            ShellService.start(context, true);
        }
    }

    private Context mContext;

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
    public void appExistsOnPartition(String packageName, String partition, ErrorCode.OutputListener listener) {

        // do a quick search if we don't need a root shell
        if (partition.equals(PARTITION_SYSTEM)) {
            int errorCode = Internal.appExistsOnPartition(null, mContext, packageName, partition);
            listener.onResult(errorCode, new ArrayList<String>());
        }
        else {
            // otherwise pull up a root shell in a separate thread
            ParamBuilder params = new ParamBuilder()
                    .addPackage(packageName)
                    .setPartition(partition);

            ShellService.send(mContext, ShellExec.API_EX_APPEXISTSONPARTITION, params, listener);
        }
    }

    /**
     * Checks if the APK fits on the specified partition.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param partition PARTITION_DATA or PARTITION_SYSTEM
     * @param listener returns the error code when job is finished
     */
    public void appFitsOnPartition(String packageName, String partition, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackage(packageName)
                .setPartition(partition);

        ShellService.send(mContext, ShellExec.API_EX_APPFITSONPARTITION, params, listener);
    }

    /**
     * Moves an APK from DATA to SYSTEM partition and updates the APK if already existing.
     * The available disk space is first checked on the SYSTEM partition before trying to move the APK.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param listener returns the error code when job is finished
     */
    public void installSystemApp(String packageName, ErrorCode.OutputListener listener) {
        installSystemAppEx(packageName, 0, listener);
    }

    /**
     * Moves an APK from DATA to SYSTEM partition and updates the APK if already existing.
     * The available disk space is first checked on the SYSTEM partition before trying to move the APK.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param addFlags use additional flags e.g. FLAG_REBOOT
     * @param listener returns the error code when job is finished
     */
    public void installSystemAppEx(String packageName, int addFlags, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackage(packageName)
                .setPartition(PARTITION_DATA)
                .setTarget(PARTITION_SYSTEM)
                .setFlags(FLAG_OVERWRITE | addFlags);

        ShellService.send(mContext, ShellExec.API_EX_MOVEAPPEX, params, listener);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param listener returns the error code when job is finished
     */
    public void uninstallSystemApp(String packageName, ErrorCode.OutputListener listener) {
        uninstallSystemAppEx(packageName, 0, listener);
    }

    /**
     * Moves an APK from SYSTEM to DATA partition. If the APK was updated in the DATA partition then this APK
     * remains untouched and only the APK on system is removed.
     *
     * @param packageName Package name of the App e.g. com.example.myapp
     * @param addFlags use additional flags e.g. FLAG_REBOOT
     * @param listener returns the error code when job is finished
     */
    public void uninstallSystemAppEx(String packageName, int addFlags, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackage(packageName)
                .setPartition(PARTITION_SYSTEM)
                .setTarget(PARTITION_DATA)
                .setFlags(addFlags);

        ShellService.send(mContext, ShellExec.API_EX_MOVEAPPEX, params, listener);
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
    public void moveAppEx(String packageName, String partition, String target, int flags, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackage(packageName)
                .setPartition(partition)
                .setTarget(target)
                .setFlags(flags);

        ShellService.send(mContext, ShellExec.API_EX_MOVEAPPEX, params, listener);
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
    public void moveAppEx(List<String> packages, String partition, String target, int flags, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackages(packages)
                .setPartition(partition)
                .setTarget(target)
                .setFlags(flags);

        ShellService.send(mContext, ShellExec.API_EX_MOVEAPPEX, params, listener);
    }

    /**
     * Moves a couple of packages to trash and generates a metafile containing the application icon.
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition Source partition
     * @param listener returns the error code when job is finished
     */
    public void moveAppToTrash(List<String> packages, String partition, ErrorCode.OutputListener listener) {
        moveAppEx(packages, partition, PARTITION_TRASH, 0, listener);
    }

    /**
     * Moves a couple of packages to trash and generates a metafile containing the application icon.
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition Source partition
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
     */
    public void moveAppToTrashEx(List<String> packages, String partition, int flags, ErrorCode.OutputListener listener) {
        moveAppEx(packages, partition, PARTITION_TRASH, flags, listener);
    }

    /**
     * Get the list of packages of a specified partition.
     *
     * When relying on PackageManager to retrieve the list of installed apps on system or data partition then we
     * get only the packages that were available at boot, or installed by Android's common install methods. However
     * if we manually move apps between partitions then PackageManager is not up-to-date anymore. Therefore we parse
     * the list of installed packages directly from the file system and add more information from PackageManager when
     * available.
     *
     * @param partition The partition of interest
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
     */
    public void getPackagesFromPartition(String partition, int flags, ErrorCode.OutputListenerWithPackages listener) {
        ParamBuilder params = new ParamBuilder()
                .setPartition(partition)
                .setFlags(flags);

        ShellService.sendPackageCommand(mContext, ShellExec.API_EX_GETPACKAGES, params, listener);
    }

    /**
     * Get package information for one package from a specified partition.
     *
     * When relying on PackageManager to retrieve the list of installed apps on system or data partition then we
     * get only the packages that were available at boot, or installed by Android's common install methods. However
     * if we manually move apps between partitions then PackageManager is not up-to-date anymore. Therefore we parse
     * the list of installed packages directly from the file system and add more information from PackageManager when
     * available.
     *
     * @param partition The partition of interest
     * @param packageName The package we want
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
     */
    public void getPackageFromPartition(String partition, String packageName, int flags, ErrorCode.OutputListenerWithPackages listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackage(packageName)
                .setPartition(partition)
                .setFlags(flags);

        ShellService.sendPackageCommand(mContext, ShellExec.API_EX_GETPACKAGES, params, listener);

    }

    /**
     * Returns the package info for one app.
     *
     * @param packageName The package name.
     * @param flags Additional flags e.g. FLAG_METADATA
     * @return the package information or null if not found
     */
    public PackageInfoEx getPackageFromTrash(String packageName, int flags) {
        ShellExec exec = new ShellExec(false);
        Internal.getPackagesFromPartition(exec, null, PARTITION_TRASH, packageName, flags);
        return exec.packages.size() > 0 ? exec.packages.get(0) : null;
    }

    /**
     * Returns the list of all packages in the trash.
     *
     * @param flags Additional flags e.g. FLAG_METADATA
     * @return the list of package information records
     */
    public ArrayList<PackageInfoEx> getPackagesFromTrash(int flags) {
        ShellExec exec = new ShellExec(false);
        Internal.getPackagesFromPartition(exec, null, PARTITION_TRASH, null, flags);
        return exec.packages;
    }

    /**
     * Remove packages from trash permanently. Erases cache and the data folder as well.
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param listener returns the error code when job is finished
     */
    public void wipePackageFromTrash(List<String> packages, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackages(packages)
                .setPartition(PARTITION_TRASH)
                .setFlags(FLAG_WIPECACHE | FLAG_WIPEDATA);

        ShellService.send(mContext, ShellExec.API_EX_WIPEAPP, params, listener);
    }

    /**
     * Remove packages from a partition permanently. The exact behavior can be customized with some flags.
     *
     * @param packages List of package names e.g. com.example.myapp, com.example.myapps2, ...
     * @param partition The partition to wipe the packages from e.g. PARTITION_TRASH
     * @param flags Additional flags
     * @param listener returns the error code when job is finished
     */
    public void wipePackageFromPartitionEx(List<String> packages, String partition, int flags, ErrorCode.OutputListener listener) {
        ParamBuilder params = new ParamBuilder()
                .addPackages(packages)
                .setPartition(partition)
                .setFlags(flags);

        ShellService.send(mContext, ShellExec.API_EX_WIPEAPP, params, listener);
    }

    static class PackageMetaData extends PackageInfoEx {

        private PackageMetaData(String filename) {
            mFilename = filename;
            mPackageName = "";
            mPartition = "";
            mLabel = "";
            mLaunchable = false;
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
        private static int createMetaFile(Context context, String packageName, String filename, String partition) {
            int errorCode = ErrorCode.NONE;
            if (context == null) {
                errorCode = ErrorCode.INVALID_CONTEXT;
            }

            PackageInfoEx packageInfo = new PackageInfoEx();
            packageInfo.mPartition = partition;

            // read application icon
            if (errorCode == ErrorCode.NONE) {
                try {
                    PackageManager pm = context.getPackageManager();
                    ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                    packageInfo.mLabel = pm.getApplicationLabel(ai).toString();
                    packageInfo.mIcon = ((BitmapDrawable) pm.getApplicationIcon(packageName)).getBitmap();
                } catch (PackageManager.NameNotFoundException e) {
                    errorCode = ErrorCode.NOT_EXISTING;
                }
            }

            // write bitmap into output file
            if (errorCode == ErrorCode.NONE) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packageInfo.mIcon.compress(Bitmap.CompressFormat.PNG, 100, stream);

                OutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(filename.replace(".apk", ".metadata")));
                    out.write(VERSION);
                    out.write(packageInfo.mLabel.length());
                    out.write(packageInfo.mLabel.getBytes());
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
        private static PackageInfoEx readMetaFile(String filename, int flags) {
            PackageInfoEx packageInfo = new PackageMetaData(filename.replace(".metadata", ".apk"));
            InputStream in = null;

            try {
                in = new BufferedInputStream(new FileInputStream(filename));
                int version = in.read();
                if (version == VERSION) {

                    // read application label
                    int len = in.read();
                    for (int i=0; i<len; ++i) {
                        packageInfo.mLabel += (char)in.read();
                    }

                    // read package name
                    len = in.read();
                    for (int i=0; i<len; ++i) {
                        packageInfo.mPackageName += (char)in.read();
                    }

                    // read source partition
                    len = in.read();
                    for (int i=0; i<len; ++i) {
                        packageInfo.mPartition += (char)in.read();
                    }

                    // read application icon
                    if ((flags & AppManager.FLAG_METADATA) > 0) {
                        byte[] buffer = new byte[100000];
                        len = in.read(buffer);
                        packageInfo.mIcon = BitmapFactory.decodeByteArray(buffer, 0, len);
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

        public static int appExistsOnPartition(ShellExec exec, Context context, String packageName, String partition) {
            ResultSet result = getPackageFilename(exec, context, packageName, partition);
            return result.errorCode;
        }

        private static ResultSet getPackageFilename(ShellExec exec, Context context, String packageName, String partition) {
            ResultSet result = new ResultSet();

            // first try to consult package manager
            PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                boolean found = (ai.sourceDir.startsWith(getPartitionPath(partition)) ||
                        (partition.equals(PARTITION_DATA) && ai.sourceDir.startsWith("/mnt/asec")));
                if (found) {
                    result.errorCode = ErrorCode.NONE;
                    result.filename = ai.sourceDir;
                }
            }
            catch (PackageManager.NameNotFoundException e) {
            }

            if (result.errorCode != ErrorCode.NONE) {
                // search manually
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

        public static int appFitsOnPartition(ShellExec exec, Context context, String packageName, String partition) {
            String source = null;
            String[] partitions = { PARTITION_DATA, PARTITION_SYSTEM, PARTITION_TRASH };
            for (String p : partitions) {
                if (!partition.equals(p)) {
                    if (appExistsOnPartition(exec, context, packageName, p) == ErrorCode.NONE) {
                        source = p;
                        break;
                    }
                }
            }

            if (source != null) {
                ResultSet result = packageFitsOnPartition(exec, context, packageName, source, partition);
                return result.errorCode;
            }
            else return ErrorCode.ALREADY_EXISTING;
        }

        private static ResultSet packageFitsOnPartition(ShellExec exec, Context context, String packageName, String source, String target) {

            // get required disk space for APK
            long apkSpace = 0;
            ResultSet result = getPackageFilename(exec, context, packageName, source);
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
                ResultSet result = packageFitsOnPartition(exec, context, packageName, sourcePartition, targetPartition);
                if (result.errorCode != ErrorCode.NONE) {
                    errorCode = result.errorCode;
                    break;
                }
                else if (result.filename.startsWith("/mnt/asec")) {
                    errorCode = ErrorCode.MNT_ASEC_NOT_SUPPORTED;
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

                        boolean isOdexed = new File(sourcePath.replace(".apk", ".odex")).exists();
                        if (targetPartition.equals(PARTITION_DATA)) {
                            // don't allow to move odex apps to data partition
                            if (isOdexed && (
                                    !targetPartition.equals(PARTITION_TRASH) ||
                                    !sourcePartition.equals(PARTITION_TRASH))
                                    ) {
                                errorCode = ErrorCode.ODEX_NOT_SUPPORTED;
                            }
                        }

                        if (errorCode == ErrorCode.NONE) {
                            String targetPath = sourcePath.replace(
                                    getPartitionPath(sourcePartition) + "/app/",
                                    getPartitionPath(targetPartition) + "/app/");

                            if (targetPartition.equals(PARTITION_TRASH)) {
                                // generate meta data for packages moved to trash
                                PackageMetaData.createMetaFile(context, packageName, targetPath, sourcePartition);
                            }

                            // prepare move command
                            String shellCmd = "busybox mv " + sourcePath + " " + targetPath;
                            if ((flags & FLAG_OVERWRITE) != FLAG_OVERWRITE) {
                                errorCode = appExistsOnPartition(exec, context, packageName, targetPartition);
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

                                if (sourcePartition.equals(PARTITION_TRASH)) {
                                    // don't forget to delete metafile
                                    File metafile = new File(sourcePath.replace(".apk", ".metadata"));
                                    if (metafile.exists()) {
                                        metafile.delete();
                                    }
                                }

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

        public static int getPackagesFromPartition(ShellExec exec, Context context, String partition, String packageName, int flags) {
            int errorCode = ErrorCode.NONE;

            PackageManager pm = null;
            List<PackageInfo> packages = null;
            if (!partition.equals(PARTITION_TRASH)) {
                pm = context.getPackageManager();
                packages = pm.getInstalledPackages(0);
            }

            if (partition.equals(PARTITION_DATA)) {
                // need to read packages from a root shell
                errorCode = exec.run("busybox ls " + getPartitionPath(partition) + "/app");
                if (errorCode == ErrorCode.NONE) {
                    for (String filename : exec.output) {
                        if (filename.endsWith(".apk")) {
                            if (packageName == null ||
                                    (packageName != null && equalsPackage(filename, packageName))) {
                                exec.packages.add(new PackageInfoEx(pm,
                                        getPartitionPath(PARTITION_DATA) + "/app/" + filename));
                            }
                        }
                    }
                }
            }
            else {
                File root = new File(getPartitionPath(partition) + "/app/");
                File[] files = root.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String filename = f.getPath();
                        if (partition.equals(PARTITION_TRASH)) {
                            // need to read package info from meta file
                            if (filename.endsWith(".metadata")) {
                                if (packageName == null ||
                                        (packageName != null && equalsFilename(f.getName(), packageName, ".metadata"))) {
                                    exec.packages.add(PackageMetaData.readMetaFile(filename, flags));
                                    if (packageName != null) {
                                        break;
                                    }
                                }
                            }
                        }
                        else if (partition.equals(PARTITION_SYSTEM)) {
                            if (filename.endsWith(".apk")) {
                                if ((flags & FLAG_IGNORE_ODEXED) > 0) {
                                    // skip odexed packages
                                    String odexFilename = filename.replace(".apk", ".odex");
                                    if (new File(odexFilename).exists()) {
                                        continue;
                                    }
                                }
                                if (packageName == null ||
                                        (packageName != null && equalsPackage(f.getName(), packageName))) {
                                    exec.packages.add(new PackageInfoEx(pm, filename));
                                }
                            }
                        }
                    }
                }
            }

            // filter out packages that can't be launched?
            if ((flags & FLAG_LAUNCHABLE) > 0) {
                int i = 0;
                while (i < exec.packages.size()) {
                    PackageInfoEx p = exec.packages.get(i);
                    if (!p.isLaunchable()) {
                        exec.packages.remove(i);
                    }
                    else {
                        i += 1;
                    }
                }
            }

            return errorCode;
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
