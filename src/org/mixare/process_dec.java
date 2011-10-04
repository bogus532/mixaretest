package org.mixare;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class process_dec extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.process_dec);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

}
