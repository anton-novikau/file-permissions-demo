File Permissions Demo
===================


This project is intended to demonstrate file moving issue on some of the Android M powered devices. The issue is the following: 

According to the [documentation](https://developer.android.com/reference/android/Manifest.permission.html#WRITE_EXTERNAL_STORAGE) all files that are stored in the ```/sdcard/Android/data/<package>/files``` directory do not require ```STORAGE``` permission to have access to it starting from API 19. And it is true if your application creates a file in this directory. But if you **move** a file from any external storage folder to this application specific external storage directory, this file continue to be inaccessible when ```STORAGE``` permission is revoked as it were in the previous folder (on the common external storage). 

The issue is reproducible on Android Emulator with Android 6.0 and on Nexus 9 with Android 6.0. On any device or emulator with Android 7.0+ we didn't manage to reproduce it. 

## How to use it

 1. Press "**Load**" button to download an image  to ```/sdcard/File Permissions``` directory.
 2. Select one of the migration modes: 
	 - **Java** to move file from one directory to another with ```File.renameTo(File)```
	 - **Native** to move file with C++ method ```rename(char*, char*)``` form ```stdio.h```
	 - **Copy** to copy file content from one directory to another and remove the original file
 3. Press "**Migrate**" button with one of the modes selected to move the downloaded image into ```/sdcard/Android/data/<package>/files/File Permissions``` directory
 4. Go to app's settings -> Permissions and revoke ```STORAGE``` permission
 5. Open application again

In the result the image you moved to app's directory with **Java** or **Native** migration method won't be loaded when application restored unlike the image that was moved with **Copy**. So the problem is that on some devices with Android M file owner is transferred after move, but actually it should be changed to the OS user of your application like it happens after copying. 
