package com.mijoro.photofunhouse;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.ViewGroup;
import android.view.Window;

public class FunhouseActivity extends Activity {
	private GLLayer glView;
	private ViewGroup mToolbar;
	private boolean mToolbarShown = false;
	private Animation mShowAnimation, mHideAnimation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        glView = (GLLayer)findViewById(R.id.FunhouseLayer);
        mToolbar = (ViewGroup)findViewById(R.id.Toolbar);
        
        CameraPreviewSink sink = new CameraPreviewSink();
        glView.setCameraPreviewSink(sink);
        
        findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                glView.nextProgram();
            }
        });
        findViewById(R.id.previous_button).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                glView.previousProgram();
            }
        });
        
        mShowAnimation = new AlphaAnimation(0.2f, 1.0f);
        mShowAnimation.setFillAfter(true);
        mShowAnimation.setDuration(200);
        mHideAnimation = new AlphaAnimation(1.0f, 0.2f);
        mHideAnimation.setFillAfter(true);
        mHideAnimation.setDuration(200);
        mToolbar.startAnimation(mHideAnimation);
        glView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mToolbar.startAnimation(mToolbarShown ? mHideAnimation : mShowAnimation);
                mToolbarShown = !mToolbarShown;
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
      //  glView.onResume();
        super.onResume();
    }
}
