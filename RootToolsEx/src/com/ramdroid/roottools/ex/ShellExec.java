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
import android.os.AsyncTask;
import android.util.Log;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;


/**
 * Wrapper around the new shell interface from RootTools.
 *
 * Calls to {@link ShellExec} are blocking the current thread.
 * Only for internal use in {@link AsyncShell} and {@link ShellService}
 */
class ShellExec {

    // RootTools wrapper APIs
    public static final int API_GOTROOT                     = 1;
    public static final int API_GOTBUSYBOX                  = 2;
    public static final int API_SEND                        = 3;

    // More APIs that are not part of the RootTools classes
    public static final int API_EX_APPEXISTSONPARTITION     = 101;
    public static final int API_EX_APPFITSONPARTITION       = 102;
    public static final int API_EX_MOVEAPPEX                = 103;
    public static final int API_EX_WIPEAPP                  = 104;
    public static final int API_EX_GETPACKAGES              = 105;

    public ArrayList<String> output = new ArrayList<String>();
    public ArrayList<PackageInfoEx> packages = new ArrayList<PackageInfoEx>();

    private Shell mShell;
    private boolean mUseRoot;
    private int mErrorCode;
    private Boolean mCommandFinished;

    private static int mCommandId = 0;

    public ShellExec(boolean useRoot) {
        mUseRoot = useRoot;
    }

    public int run(String... command) {
        return run(0, command);
    }

    public int run(int timeout, String... command) {
        mErrorCode = ErrorCode.COMMAND_FAILED;
        mCommandFinished = false;
        mCommandId += 1;
        output.clear();
        Command cmd = new Command(mCommandId, timeout, command) {

            @Override
            public void commandOutput(int id, String line) {
                if (id == mCommandId && line != null && line.length() > 0) {
                    Debug.log(this, "ID " + id + ": " + line);
                    output.add(line);
                }
            }

            @Override
            public void commandTerminated(int id, String reason) {
                output.add(reason);
                synchronized (mCommandFinished) {
                    mCommandFinished = true;
                }
            }

            @Override
            public void commandCompleted(int id, int exitCode) {
                if (exitCode == 0) {
                    mErrorCode = ErrorCode.NONE;
                }
                synchronized (mCommandFinished) {
                    mCommandFinished = true;
                }
            }
        };
        Debug.log(this, "Cmd " + mCommandId + ": " + cmd.getCommand());

        RootTools.handlerEnabled = false;
        try {
            mShell = RootTools.getShell(mUseRoot);
            mShell.add(cmd).getCommand();

        } catch (IOException e) {
            output.add(e.toString());
        } catch (RootDeniedException e) {
            output.add(e.toString());
            mErrorCode = ErrorCode.NO_ROOT_ACCESS;
        } catch (TimeoutException e) {
            output.add(e.toString());
            mErrorCode = ErrorCode.TIMEOUT;
        }

        if (timeout == 0) {
            timeout = 5000;
        }
        int intervals = timeout / 100;
        for (int i=0; i<intervals; ++i) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            synchronized (mCommandFinished) {
                if (mCommandFinished) {
                    break;
                }
            }

        }

        return mErrorCode;
    }

    public int callApi(int api, ParamBuilder builder, Integer... flags) {
        int errorCode = ErrorCode.NONE;
        if (api == API_GOTROOT) {
            boolean gotRoot = false;
            try {
                if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
                    gotRoot = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            errorCode = gotRoot ? ErrorCode.NONE : ErrorCode.NO_ROOT_ACCESS;
        }
        else if (api == API_GOTBUSYBOX) {
            String version = RootTools.getBusyBoxVersion();
            boolean available = (version != null && version.length() > 0);
            errorCode = available ? ErrorCode.NONE : ErrorCode.BUSYBOX;
        }
        else if (api == API_SEND) {
            errorCode = run(builder.getTimeout(), builder.getCommands());
        }
        clear();
        return errorCode;
    }

    public int callApi(int api, Context context, ParamBuilder builder, Integer... flags) {
        int errorCode = ErrorCode.NONE;

        int f = 0;
        if (flags.length > 0) {
            f = flags[0];
        }

        if (api == API_EX_APPEXISTSONPARTITION) {
            errorCode = AppManager.Internal.appExistsOnPartition(
                    this,
                    context,
                    builder.getFirstPackage(),
                    builder.getPartition());
        }
        else if (api == API_EX_APPFITSONPARTITION) {
            errorCode = AppManager.Internal.appFitsOnPartition(
                    this,
                    context,
                    builder.getFirstPackage(),
                    builder.getPartition());
        }
        else if (api == API_EX_MOVEAPPEX) {
            errorCode = AppManager.Internal.moveAppEx(
                    this,
                    context,
                    builder.getPackages(),
                    builder.getPartition(),
                    builder.getTarget(),
                    f);
        }
        else if (api == API_EX_WIPEAPP) {
            errorCode = AppManager.Internal.wipePackages(
                    this,
                    builder.getPackages(),
                    builder.getPartition(),
                    f);
        }
        else if (api == API_EX_GETPACKAGES) {
            errorCode = AppManager.Internal.getPackagesFromPartition(
                    this,
                    context,
                    builder.getPartition(),
                    builder.getFirstPackage(),
                    f);
        }
        clear();
        return errorCode;
    }

    public void clear() {
        try {
            if (mShell != null) {
                mShell.close();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Worker to execute all shell commands in a separate thread.
     */
    public static class Worker extends AsyncTask<Integer, Void, Integer> {

        private int mApi;
        private ErrorCode.OutputListener mListener;
        private ErrorCode.OutputListenerWithPackages mListenerEx;
        private ShellExec mExec;
        private boolean mUseRoot;
        private ParamBuilder mBuilder = new ParamBuilder();

        public Worker(int api, ErrorCode.OutputListener listener) {
            mApi = api;
            mListener = listener;
            mUseRoot = true;
        }

        public Worker(int api, boolean useRoot, String command, ErrorCode.OutputListener listener) {
            mApi = api;
            mUseRoot = useRoot;
            mListener = listener;
            mBuilder.addCommand(command);
        }

        public Worker(int api, boolean useRoot, ParamBuilder builder, ErrorCode.OutputListener listener) {
            mApi = api;
            mUseRoot = useRoot;
            mListener = listener;
            mBuilder.sync(builder);
        }

        public Worker(int api, ParamBuilder builder, ErrorCode.OutputListenerWithPackages listener) {
            mApi = api;
            mUseRoot = true;
            mListenerEx = listener;
            mBuilder.sync(builder);
        }

        @Override
        protected Integer doInBackground(Integer... flags) {
            mExec = new ShellExec(mUseRoot);
            int errorCode = mExec.callApi(mApi, mBuilder, flags);
            return errorCode;
        }

        protected void onPostExecute(Integer errorCode) {
            if (mListener != null) {
                if (mExec.output == null) {
                    mExec.output = new ArrayList<String>();
                }
                mListener.onResult(errorCode, mExec.output);
            }
            else if (mListenerEx != null) {
                if (mExec.packages == null) {
                    mExec.packages = new ArrayList<PackageInfoEx>();
                }
                mListenerEx.onResult(errorCode, mExec.packages);
            }
        }
    }
}
