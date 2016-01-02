# CameraApp
Simple Android application. You can capture a photo, then it is saved on memory and displayed in ImageView. It loads image in full resolution so it can consume some RAM, but that's ok.

In [MainActivity.java](https://github.com/PoprostuRonin/CameraApp/blob/master/app/src/main/java/com/poprosturonin/cameraapp/MainActivity.java) in method void onSaveInstanceState I put a small explanation about reloading bitmap after f.e. changing orientation.