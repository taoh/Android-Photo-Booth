package com.mijoro.photofunhouse;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

/**
 * Application's main Activity. Does nothing special apart from putting the app
 * in fullscreen mode and creating a GLLayer object as well as a Preview object.
 * 
 * @author Niels
 *
 */
public class GLCamTest extends Activity {
	private CamLayer mPreview;
	private GLLayer glView;
    
    /** Called when the activity is first created. */
    @Override
    public void onResume() {
        super.onResume();
        
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        final Window win = getWindow(); 
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        glView=new GLLayer(this);
        
		mPreview = new CamLayer(this, glView);
		glView.setCamLayer(mPreview);

        setContentView(glView);
        addContentView(mPreview, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }
    protected void onPause() {
    	super.onPause();
    }
}
