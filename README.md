RootToolsEx
===========

This library is based on the great RootTools library by Stericson and Co. Of course this is not an attempt to replace RootTools at all but instead it adds more high-level functionality on top of RootTools.

The main purpose of the library is to make sure that shell commands are always executed in a separate thread. Therefore you can always call the APIs without blocking the main UI thread. Otherwise even the most simple functions like checking for root availablity might break your app. 

Here's some things you can do:

- Check if root access is available
- Check if busybox is installed
- Execute shell commands
- Send many root commands over time using a service
- Move apps between data/system partition
- Generate logcat error reports

In this repository you will find both the main `RootToolsEx` library and a test app `RootToolsExTest` that demonstrates usage of some APIs in the library.

In the WIKI you can read a more detailed description on all available APIs.

License
=======

RootToolsEx is licensed under the Apache License. Feel free to use it in your free or paid Android apps. Please add a little note in your license info screen that you are using RootToolsEx and if possible include a link to this Github page. Vice versa you can send me a message so I can mention your app here as well.
