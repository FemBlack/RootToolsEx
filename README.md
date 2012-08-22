RootToolsEx
===========

This library is based on the great RootTools library by Stericson and Co. Of course this is not an attempt to replace RootTools at all but instead it adds more high-level functionality on top of RootTools.

Design
======

In the RootTools project the RootTools class is used as the one and only interface to access the library's functionality. In RootToolsEx there is no such wrapper class. Instead each feature comes with it's own class for direct use.

The classes in RootToolsEx are not doing any checks if root access is available or not. It's your job to do this in your own app, as you'll probably do anyway.

Features
========

Bear with me but in the initial version there's not so many features yet... Of course more features will be added as I continue to extract and pimp up more features that are/will be used in my own apps.

#1 ErrorReport
==============

The `ErrorReport` class creates an error report that can be forwarded to the developer (usually I guess by email) using Android's intent chooser. The Builder pattern is used to let you customize the options as you need it. Here is an example as used in App Quarantine:

        ErrorReport report = new ErrorReport.Builder(context)
                .setChooserTitle(context.getString(R.string.senderrorreport))
                .setEmailAddress("by-email-address-for-error-reports@gmail.com")
                .setEmailSubject("App Quarantine error report")
                .setEmailText("Hi ramdroid, here's my error report!")
                .build();
        report.send();

Important: The `WRITE_EXTERNAL_STORAGE` permission is needed!


