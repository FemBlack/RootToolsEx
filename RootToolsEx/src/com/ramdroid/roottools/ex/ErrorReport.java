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
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an error report and sends an intent to send it by email or another service of choice.
 * All shell action is executed in a separate thread so you don't have to bother.
 *
 * In the most simple solution you just have to add a valid email address:
 *
 * ErrorReport report = new ErrorReport.Builder(context)
 *          .setEmailAddress("your.email@gmail.com")
 *          .build();
 * report.send();
 *
 * There are more options like changing email subject/text, title of the chooser dialog.
 * You can even choose a different log option. By default logcat is being used.
 * */
public class ErrorReport {

    private final static String TAG = "ErrorReport";

    private final Context context;
    private final String chooserTitle;
    private final String emailAddress;
    private final String emailSubject;
    private final String emailText;
    private final String logTool;
    private final String logParams;
    private final String logFile;
    private final boolean includeSystemInfo;
    private final boolean includeRunningProcesses;
    private final boolean includePartitionInfo;

    public ErrorReport(final Builder builder) {
        context = builder.context;
        chooserTitle = builder.chooserTitle;
        emailAddress = builder.emailAddress;
        emailSubject = builder.emailSubject;
        emailText = builder.emailText;
        logTool = builder.logTool;
        logParams = builder.logParams;
        logFile = builder.logFile;
        includeSystemInfo = builder.includeSystemInfo;
        includeRunningProcesses = builder.includeRunningProcesses;
        includePartitionInfo = builder.includePartitionInfo;
    }

    /**
     * Creates the error report and opens the intent chooser dialog.
     *
     * @param listener Returns the error code or ERROR_NONE if successful
     */
    public void send(ErrorCode.Listener listener) {
        int errorCode = verify();
        if (errorCode == ErrorCode.NONE) {
            new Worker(context, listener).execute();
        }
        else listener.onResult(errorCode);
    }

    private int verify() {
        if (emailAddress.length() < 1) {
            return ErrorCode.MISSING_EMAILADDRESS;
        }

        // check required permission
        if (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return ErrorCode.MISSING_PERMISSION;
        }

        // check if external storage is available
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return ErrorCode.NO_EXTERNAL_STORAGE;
        }

        return ErrorCode.NONE;
    }

    /**
     * Builder to setup the options for the {@link ErrorReport} class.
     */
    public static class Builder {

        private Context context;
        private String chooserTitle;
        private String emailAddress;
        private String emailSubject;
        private String emailText;
        private String logTool;
        private String logParams;
        private String logFile;
        private boolean includeSystemInfo;
        private boolean includeRunningProcesses;
        private boolean includePartitionInfo;

        public Builder(Context context) {
            this.context = context;
            initDefaults();
        }

        /**
         * Set the title of the intent chooser dialog.
         * If not title is set then the default title will be used.
         *
         * @param chooserTitle Chooser dialog title.
         * @return Returns the {@link Builder}.
         */
        public Builder setChooserTitle(String chooserTitle) {
            this.chooserTitle = chooserTitle;
            return this;
        }

        /**
         * Set the email address that receives the error report.
         * This function is mandatory and needs to be called!
         *
         * @param emailAddress Email address of the receiver.
         * @return Returns the {@link Builder}.
         */
        public Builder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        /**
         * Set the email subject line.
         * If not set then the default subject will be used.
         *
         * @param emailSubject
         * @return Returns the {@link Builder}.
         */
        public Builder setEmailSubject(String emailSubject) {
            this.emailSubject = emailSubject;
            return this;
        }

        /**
         * Set some text that appears in the email.
         * If not set then the default text will be used.
         *
         * @param emailText
         * @return Returns the {@link Builder}.
         */
        public Builder setEmailText(String emailText) {
            this.emailText = emailText;
            return this;
        }

        /**
         * Set the tool that creates the error report.
         * By default logcat is used.
         *
         * @param logTool An alternative binary file name
         * @return Returns the {@link Builder}.
         */
        public Builder setLogTool(String logTool) {
            this.logTool = logTool;
            return this;
        }

        /**
         * Set the parameters for the log tool.
         * By default logcat is called with -d -v time
         * -d means that logcat immediately returns when the log has been parsed.
         * -v time means that the current time is added to each line.
         * @param logParams The new parameters for the log tool.
         * @return Returns the {@link Builder}.
         */
        public Builder setLogParams(String logParams) {
            this.logParams = logParams;
            return this;
        }

        /**
         * Change the name of the log file that is send to the receiver.
         * If not set then the default name will be used.
         * @param logFile The new log file name.
         * @return Returns the {@link Builder}.
         */
        public Builder setLogFile(String logFile) {
            this.logFile = logFile;
            return this;
        }

        /**
         * Don't include the header with system information.
         * @return Returns the {@link Builder}.
         */
        public Builder hideSystemInfo() {
            this.includeSystemInfo = false;
            return this;
        }

        /**
         * Include a list of running processes in the header.
         * @return Returns the {@link Builder}.
         */
        public Builder includeRunningProcesses() {
            this.includeRunningProcesses = true;
            return this;
        }

        /**
         * Include a list of all partitions of the device including disk space statistics.
         * @return Returns the {@link Builder}.
         */
        public Builder includePartitionInfo() {
            this.includePartitionInfo = true;
            return this;
        }

        /**
         * @return Returns a configured {@link ErrorReport} class.
         */
        public ErrorReport build() {
            return new ErrorReport(this);
        }

        private void initDefaults() {
            chooserTitle = "Send error report";
            emailSubject = "Error report by RootToolsEx";
            emailText = "Hi there, here's my crash report!";
            logTool = "logcat";
            logParams = "-d -v time";
            logFile = "errorlog.txt";
            includeSystemInfo = true;
            includeRunningProcesses = false;
            includePartitionInfo = false;
        }

    }

    /**
     * Worker to execute all shell commands in a separate thread.
     */
    private class Worker extends AsyncTask<Integer, Void, Integer> {

        private Context context;
        private ErrorCode.Listener listener;
        private ShellExec exec;

        public Worker(Context context, ErrorCode.Listener listener) {
            this.context = context;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Integer... flags) {
            int errorCode = ErrorCode.NONE;
            exec = new ShellExec(true);

            // initialize output file
            final String outputFile = getOutputFile();
            if (outputFile == null) {
                errorCode = ErrorCode.ACCESS_OUTPUTFILE;
            }

            if (errorCode == ErrorCode.NONE) {
                // get version information of su binary
                String suVersion = "Unknown";
                if (includeSystemInfo) {
                    errorCode = exec.run(new String[] { "su -v"});
                    if (errorCode == ErrorCode.NONE && exec.output.size() > 0) {
                        suVersion = exec.output.get(0);
                    }
                }

                // add header with system information
                createSystemInfo(outputFile, suVersion);

                // add partition info?
                if (includePartitionInfo) {
                    errorCode = exec.run(new String[] { "df"});
                    if (errorCode == ErrorCode.NONE && exec.output.size() > 0) {
                        List<String> logLines = exec.output;
                        logLines.add("");
                        logLines.add("");
                        addLogLines(outputFile, true, logLines);
                    }
                }

                // run log tool
                final boolean append = includeSystemInfo || includeRunningProcesses;
                final String logCommand = logTool + " " + logParams + (append ? " >> " : " > ") + outputFile;
                errorCode = exec.run(new String[] { logCommand });
                if (errorCode != ErrorCode.NONE) {
                    addLogLines(outputFile, true, exec.output);
                }
            }
            exec.destroy();
            return errorCode;
        }

        private int createSystemInfo(String outputFile, String suVersion) {
            int result = ErrorCode.NONE;
            List<String> logLines = new ArrayList<String>();

            if (includeSystemInfo) {
                ApplicationInfo appInfo = null;
                PackageInfo pInfo = null;
                try {
                    final PackageManager pm = context.getPackageManager();
                    appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
                    pInfo = pm.getPackageInfo(context.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    result = ErrorCode.INVALID_CONTEXT;
                }
                if (result == ErrorCode.NONE) {
                    CharSequence appLabel = context.getPackageManager().getApplicationLabel(appInfo);
                    logLines.add("App version:      " + appLabel + " " + pInfo.versionName + " (" + pInfo.versionCode + ")");
                    logLines.add("Android:          " + Build.VERSION.RELEASE + " (" + Build.VERSION.CODENAME + ")");
                    logLines.add("Build ID:         " + Build.DISPLAY);
                    logLines.add("Manufacturer:     " + Build.MANUFACTURER);
                    logLines.add("Model:            " + Build.MODEL);
                    logLines.add("Brand:            " + Build.BRAND);
                    logLines.add("Device:           " + Build.DEVICE);
                    logLines.add("Product:          " + Build.PRODUCT);
                    logLines.add("Su binary:        " + suVersion);
                }
                logLines.add("");
                logLines.add("");
            }

            if (includeRunningProcesses) {
                final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                final List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
                logLines.add("Running processes:");
                logLines.add("");
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    logLines.add(processInfo.processName + " (" + processInfo.pid + ")");
                }
                logLines.add("");
                logLines.add("");
            }

            return addLogLines(outputFile, false, logLines);
        }

        private int addLogLines(String outputFile, boolean append, List<String> logLines) {
            int result = ErrorCode.NONE;
            if (logLines.size() > 0) {
                // write into output file
                FileOutputStream f = null;
                try {
                    f = new FileOutputStream(new File(outputFile), append);
                    for(String line : logLines) {
                        f.write((line + "\n").getBytes());
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    result = ErrorCode.WRITE_SYSTEM_INFO;
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    result = ErrorCode.WRITE_SYSTEM_INFO;
                }
                finally {
                    try {
                        if (f != null) {
                            f.flush();
                            f.close();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        result = ErrorCode.WRITE_SYSTEM_INFO;
                    }

                }
            }
            return result;
        }

        private String getOutputFile() {
            String outputFile = null;
            try {
                File ext = context.getExternalFilesDir(null);
                if (ext.isDirectory()) {
                    File f = new File(ext, logFile);
                    outputFile = f.getPath();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return outputFile;
        }

        private void sendIntent() {
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {emailAddress});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject);
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailText);
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + getOutputFile()));

            Intent launchIntent = Intent.createChooser(emailIntent, chooserTitle);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        }

        protected void onPostExecute(Integer errorCode) {
            if (errorCode != ErrorCode.ACCESS_OUTPUTFILE) {
                sendIntent();
            }

            if (listener != null) {
                listener.onResult(errorCode);
            }
        }
    }
}
