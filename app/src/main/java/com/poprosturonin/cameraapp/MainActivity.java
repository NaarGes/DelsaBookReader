package com.poprosturonin.cameraapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int MEDIA_TYPE_IMAGE = 1;

    //Keys to save our instance
    private static final String PHOTO_URI_KEY = "photo_uri_352";

    private Uri photoURI;
    private Bitmap bitmap;
    private MediaPlayer mp;
    private String url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPhoto();
            }
        });

        mp = new MediaPlayer();
        ImageView play_pause = (ImageView) findViewById(R.id.button);
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mp.isPlaying())
                    mp.pause();
                else
                    mp.start();
            }
        });

    }

    /** Get bitmap from {@link #photoURI#getPath()} and load it to imageView */
    public void setBitmap()
    {
        try {
            //We load half resolution photo, it is still something so it may eat some RAM, cause small lag,
            //but that's OK
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeFile(photoURI.getPath(),options);
            if (bitmap != null) {
                // send bitmap to server
                Toast.makeText(getApplicationContext(), "Sending image...", Toast.LENGTH_SHORT).show();
                final MainActivity temp = this;
                Thread net = new Thread() {
                    public void run() {
                        Log.i("tag", "bitmap is not null");
                        url = "";
                        UploadTask ut = new UploadTask();
                        ut.doInBackground(bitmap);
                        while(url.isEmpty()) {
                            try {
                                currentThread().sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.i("URL fetching ...", "URL didn't fetch.");
                        }
                        Log.i("url received", url);
                        try {
                            //
                            temp.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Voice received!", Toast.LENGTH_SHORT).show();
                                }
                            });

                            mp.reset();
                            mp.setDataSource(getApplicationContext(), Uri.parse(url));
                            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mp.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                net.start();

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Starts camera intent to capture image
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

    // Create a File for saving an image or video
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

        //Solution: read it from memory again
        if(photoURI != null) //It may be useful
            outState.putParcelable(PHOTO_URI_KEY, photoURI);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //System didn't kill our app, but we already vanish our
        //bitmap so we need to restore it.
        //Note: onRestoreInstanceState won't be called, that's why we are here
//        if(photoURI != null)
//            setBitmap();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.containsKey(PHOTO_URI_KEY)) {
            photoURI = savedInstanceState.getParcelable(PHOTO_URI_KEY);
//            setBitmap(); //Restore out bitmap
        }
    }


    private class UploadTask extends AsyncTask<Bitmap, Void, Void> {

        private static final String TAG = "MainActivity";

        protected Void doInBackground(Bitmap... bitmaps) {
            if (bitmaps[0] == null)
                return null;

            Bitmap bitmap = bitmaps[0];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // convert Bitmap to ByteArrayOutputStream
            InputStream in = new ByteArrayInputStream(stream.toByteArray()); // convert ByteArrayOutputStream to ByteArrayInputStream

            DefaultHttpClient httpclient = new DefaultHttpClient();
            try {
                HttpPost httppost = new HttpPost("http://192.168.43.133:5000/files/"); // server
                MultipartEntity reqEntity = new MultipartEntity();
                reqEntity.addPart("myFile", /*System.currentTimeMillis() +*/ ".jpg", in);
                httppost.setEntity(reqEntity);

                Log.i(TAG, "request " + httppost.getRequestLine());
                HttpResponse response = null;
                try {
                    response = httpclient.execute(httppost);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    Log.i(TAG, "response is NULL: " + (response == null? "true": "false"));
                    if (response != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                        try {
                            url = reader.readLine();
                            Log.i("URL fetched", url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // new codes added in 31 mordad for show page number
                        Log.i(TAG, "response " + response.getStatusLine().toString());
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                }
            } finally {

            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
        }
    }

}