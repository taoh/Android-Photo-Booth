package com.mijoro.photofunhouse;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;

import com.mijoro.photofunhouse.GLLayer.PhotoListener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;

public class FunhouseActivity extends Activity implements PhotoListener {
	private GLLayer glView;
	private ViewGroup mToolbar;
	private boolean mToolbarShown = false;
	private Animation mShowAnimation, mHideAnimation;
	ImageButton mLastPicButton;
	private String mLastImageURI;
	private Handler mUIHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUIHandler = new Handler();
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        glView = (GLLayer)findViewById(R.id.FunhouseLayer);
        mToolbar = (ViewGroup)findViewById(R.id.Toolbar);
        mLastPicButton = (ImageButton)findViewById(R.id.last_pic_button);
        mLastPicButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mLastImageURI)), "image/png");
                startActivity(intent);
            }
        });
        
        CameraPreviewSink sink = new CameraPreviewSink();
        glView.setCameraPreviewSink(sink);
        glView.setPhotoListener(this);
        
        mShowAnimation = new AlphaAnimation(0.2f, 1.0f);
        mShowAnimation.setFillAfter(true);
        mShowAnimation.setDuration(200);
        mHideAnimation = new AlphaAnimation(1.0f, 0.2f);
        mHideAnimation.setFillAfter(true);
        mHideAnimation.setDuration(200);
        
        ImageButton cameraButton = (ImageButton)findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                glView.saveImage();
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.next_filter) {
            glView.nextProgram();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause() {
        glView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        glView.onResume();
        super.onResume();
    }

    public void photoTaken(final Bitmap b) {
        mUIHandler.post(new Runnable() {
            public void run() {
                Bitmap smallBitmap = Bitmap.createScaledBitmap(b, 70, 70, true);
                mLastPicButton.setImageBitmap(smallBitmap);
            }
        });

        File picsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        picsDir.mkdirs();
        String friendlydate = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date());
        friendlydate = friendlydate.replace(':', '_');
        String filename =  friendlydate+ ".jpg";
        File file = new File(picsDir, filename);
        
        mLastImageURI = file.getAbsolutePath();
        try {
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            b.compress(Bitmap.CompressFormat.JPEG, 90, out);
            System.out.println("You know i wrote that file to " + file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
