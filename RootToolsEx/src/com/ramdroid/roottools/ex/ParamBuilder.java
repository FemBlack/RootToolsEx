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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder interface to create shell commands.
 *
 * Created so we can add more options (e.g. timeouts) in the future
 * without creating dozens of send(...) functions in {@link AsyncShell}
 */
public class ParamBuilder implements Parcelable {

    private ArrayList<String> mCommands = new ArrayList<String>();
    private int mTimeout = 0;
    private List<String> mPackages = new ArrayList<String>();
    private String mPartition;
    private String mTarget;
    private int mFlags;

    public ParamBuilder() {
    }

    public int getTimeout() {
        return mTimeout;
    }

    public String[] getCommands() {
        return mCommands.toArray(new String[mCommands.size()]);
    }

    public String getFirstPackage() {
        if (mPackages.size() > 0) {
            return mPackages.get(0);
        }
        return null;
    }

    public List<String> getPackages() {
        return mPackages;
    }

    public String getPartition() {
        return mPartition;
    }

    public String getTarget() {
        return mTarget;
    }

    public int getFlags() {
        return mFlags;
    }

    public void sync(ParamBuilder other) {
        mCommands = other.mCommands;
        mTimeout = other.mTimeout;
        mPackages = other.mPackages;
        mPartition = other.mPartition;
        mTarget = other.mTarget;
        mFlags = other.mFlags;
    }

    /**
     * Add a command to be executed in the shell.
     * Call this function multiple times for each command.
     *
     * @param command One shell command line
     * @return the {@link ParamBuilder} object
     */
    public ParamBuilder addCommand(String command) {
        mCommands.add(command);
        return this;
    }

    /**
     * Set a custom timeout. If no timeout is set then the
     * default value is used.
     *
     * @param timeout timeout in milliseconds
     * @return the {@link ParamBuilder} object
     */
    public ParamBuilder setTimeout(int timeout) {
        mTimeout = timeout;
        return this;
    }

    public ParamBuilder addPackages(List<String> packages) {
        mPackages.addAll(packages);
        return this;
    }

    public ParamBuilder addPackage(String packageName) {
        mPackages.add(packageName);
        return this;
    }

    public ParamBuilder setPartition(String partition) {
        mPartition = partition;
        return this;
    }

    public ParamBuilder setTarget(String partition) {
        mTarget = partition;
        return this;
    }

    public ParamBuilder setFlags(int flags) {
        mFlags = flags;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStringList(mCommands);
        out.writeInt(mTimeout);
        out.writeStringList(mPackages);
        out.writeString(mPartition);
        out.writeString(mTarget);
        out.writeInt(mFlags);
    }

    public static final Parcelable.Creator<ParamBuilder> CREATOR = new Parcelable.Creator<ParamBuilder>() {
        public ParamBuilder createFromParcel(Parcel in) {
            return new ParamBuilder(in);
        }

        public ParamBuilder[] newArray(int size) {
            return new ParamBuilder[size];
        }
    };

    private ParamBuilder(Parcel in) {
        in.readStringList(mCommands);
        mTimeout = in.readInt();
        in.readStringList(mPackages);
        mPartition = in.readString();
        mTarget = in.readString();
        mFlags = in.readInt();
    }

}
