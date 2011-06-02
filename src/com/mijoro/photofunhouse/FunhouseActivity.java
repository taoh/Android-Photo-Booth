package com.mijoro.photofunhouse;

import android.app.Activity;
import android.os.Bundle;

public class FunhouseActivity extends Activity {
	private GLLayer glView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glView = new GLLayer(this);
        setContentView(glView);
    }
}
