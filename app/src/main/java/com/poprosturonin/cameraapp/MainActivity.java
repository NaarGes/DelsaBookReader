package com.poprosturonin.cameraapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventListener;

public class MainActivity extends AppCompatActivity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int MEDIA_TYPE_IMAGE = 1;

    //Keys to save our instance
    private static final String PHOTO_URI_KEY = "photo_uri_352";

    private Uri photoURI;
    private ImageView imageView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPhoto();
            }
        });
    }

    /** Get bitmap from {@link #photoURI#getPath()} and load it to imageView */
    public void setBitmap()
    {
        if(bitmap != null)
            bitmap.recycle();

        try {
            //We load half resolution photo, it is still something so it may eat some RAM, cause small lag,
            //but that's OK
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeFile(photoURI.getPath(),options);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Starts camera intent to capture image */
    public void getPhoto()
    {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        photoURI = Uri.fromFile(getOutputMediaFile(MEDIA_TYPE_IMAGE)); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK) {
                //Proceed
                setBitmap();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                photoURI = null; //No photo - no Uri
            } else {
                // Image capture failed, advise user
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
                photoURI = null; //No photo - no Uri
            }
        }
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        File mediaStorageDir;

        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //External (like sdcard, Pictures/CameraApp/)
            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "CameraApp");
        }
        else
        {
            //Camera Apps cannot access internal memory. So we can't save our image
            Toast.makeText(this,"External storage required",Toast.LENGTH_LONG).show();
            return null;
        }

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("CameraApp", "Failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            Log.d("CameraApp", "Bad signature!");
            return null;
        }

        return mediaFile;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        imageView = (ImageView) findViewById(R.id.imageView);

        //Bitmap.
        //Do not put bitmap in the bundle. It will exceed limit of size, causing
        //bad red things. We don't want it.
        //Yes I tried it. It didn't work.

        //Solution: read it from memory again
        if(bitmap != null) //If we have a bitmap
        {
            imageView.setImageDrawable(null); //Release bitmap from imageView
            bitmap.recycle();
        }

        if(photoURI != null) //It may be useful (like method 1)
        {
            outState.putParcelable(PHOTO_URI_KEY, photoURI);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //System didn't kill our app, but we already vanish our
        //bitmap so we need to restore it.
        if(photoURI != null)
            setBitmap();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imageView = (ImageView) findViewById(R.id.imageView);

        if(savedInstanceState.containsKey(PHOTO_URI_KEY)) {
            photoURI = savedInstanceState.getParcelable(PHOTO_URI_KEY);
            setBitmap(); //Restore out bitmap
        }
    }
}
