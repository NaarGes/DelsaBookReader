# CameraApp
Simple Android application. You can capture a photo, then it is saved on memory and displayed in ImageView. It loads image in half resolution, but still it can consume some RAM, cause small slowdown, but that's OK.

In [MainActivity.java](https://github.com/PoprostuRonin/CameraApp/blob/master/app/src/main/java/com/poprosturonin/cameraapp/MainActivity.java) in method void onSaveInstanceState I put a small explanation about reloading bitmap after f.e. changing orientation.

You can download latest debug-signed .apk file from releases folder:
[Click here](/releases/)

Screenshot:

![Screenshot1](/screenshots/screenshot1.png)