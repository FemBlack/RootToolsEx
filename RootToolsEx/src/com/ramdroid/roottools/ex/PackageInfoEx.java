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
import android.os.Parcel;
import android.os.Parcelable;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: ramdroid
 * Date: 11/13/12
 * Time: 6:59 AM
 * To change this template use File | Settings | File Templates.
 */

/**
 * The PackageManager APIs including PackageInfo (containing the application's name and icon) obviously isn't
 * available anymore after an app has been moved to trash. Therefore a separate file is maintained for each trashed
 * package that contains such information.
 *
 * The packages can be parsed with the getPackagesFromTrash() function in {@link AppManager}.
 */
public class PackageInfoEx implements Parcelable {

    protected final static int VERSION = 1;

    protected String mFilename;
    protected String mPackageName;
    protected String mPartition;
    protected String mLabel;
    protected Bitmap mIcon;
    protected boolean mLaunchable;

    public PackageInfoEx() {
        mFilename = "";
        mPackageName = "";
        mPartition = "";
        mLabel = "";
        mLaunchable = false;
    }

    public PackageInfoEx(PackageManager pm, String filename) {
        PackageInfo packageInfo = pm.getPackageArchiveInfo(filename, 0);
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA);
            mLabel = pm.getApplicationLabel(ai).toString();
        }
        catch (PackageManager.NameNotFoundException e) {
            mLabel = packageInfo.packageName;
        }
        mFilename = filename;
        mPackageName = packageInfo.packageName;
        mLaunchable = (pm.getLaunchIntentForPackage(mPackageName) != null);

        if (filename.contains(AppManager.PARTITION_SYSTEM)) {
            mPartition = AppManager.PARTITION_SYSTEM;
        }
        else mPartition = AppManager.PARTITION_DATA;
    }

    /**
     * @return The full path to the APK.
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * @return The package name of the app.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * @return The partition the APK was originally existing on
     */
    public String getPartition() {
        return mPartition;
    }

    /**
     * @return The full name of the app.
     */
    public String getLabel() {
        return mLabel;
    }

    /**
     * @return The default icon of the app..
     */
    public Bitmap getIcon() {
        return mIcon;
    }

    /**
     * @return The package can be launched?
     */
    public boolean isLaunchable() {
        return mLaunchable;
    }

    @Override
    public int describeContents() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mFilename);
        out.writeString(mPackageName);
        out.writeString(mPartition);
        out.writeString(mLabel);
        out.writeParcelable(mIcon, 0);
        out.writeInt(mLaunchable ? 1 : 0);
    }

    public static final Parcelable.Creator<PackageInfoEx> CREATOR = new Parcelable.Creator<PackageInfoEx>() {
        public PackageInfoEx createFromParcel(Parcel in) {
            return new PackageInfoEx(in);
        }

        public PackageInfoEx[] newArray(int size) {
            return new PackageInfoEx[size];
        }
    };

    private PackageInfoEx(Parcel in) {
        mFilename = in.readString();
        mPackageName = in.readString();
        mPartition = in.readString();
        mLabel = in.readString();
        mIcon = in.readParcelable(Bitmap.class.getClassLoader());
        mLaunchable = (in.readInt() == 1);
    }

    }
