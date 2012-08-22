package com.ramdroid.roottools.ex;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.File;
import java.io.IOException;

/**
 * Generates an error report and sends an intent to send it by email or another service of choice.
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

    public static final int ERROR_NONE                  = 0;
    public static final int ERROR_WHILE_CREATE          = 1;
    public static final int ERROR_ACCESS_OUTPUTFILE     = 2;
    public static final int ERROR_MISSING_EMAILADDRESS  = 3;

    private final Context context;
    private final String chooserTitle;
    private final String emailAddress;
    private final String emailSubject;
    private final String emailText;
    private final String logTool;
    private final String logParams;
    private final String logFile;

    public ErrorReport(final Builder builder) {
        context = builder.context;
        chooserTitle = builder.chooserTitle;
        emailAddress = builder.emailAddress;
        emailSubject = builder.emailSubject;
        emailText = builder.emailText;
        logTool = builder.logTool;
        logParams = builder.logParams;
        logFile = builder.logFile;
    }

    /**
     * Creates the error report and opens the intent chooser dialog.
     *
     * @return Returns 0 if successful, otherwise an error code.
     */
    public int send() {
        int result = verify();
        if (result == ERROR_NONE) {
            result = createReport();
        }
        if (result == ERROR_NONE) {
            sendIntent();
        }
        return result;
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
        }

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

    private int createReport() {
        int result = ERROR_NONE;
        String outputFile = getOutputFile();
        if (outputFile == null) {
            result = ERROR_ACCESS_OUTPUTFILE;
        }
        if (result == ERROR_NONE) {
            Command command = new Command(0, new String[] { logTool + " " + logParams + " > " + outputFile }) {

                @Override
                public void output(int id, String line) {
                }
            };
            try {
                Shell rootShell = RootTools.getShell(true);
                rootShell.add(command).waitForFinish();
                rootShell.close();
            }
            catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                result = ERROR_WHILE_CREATE;
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                result = ERROR_WHILE_CREATE;
            }
        }
        return result;
    }

    private int verify() {
        if (emailAddress.isEmpty()) {
            return ERROR_MISSING_EMAILADDRESS;
        }
        return ERROR_NONE;
    }
}
