package org.mixare;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class process_status extends Activity implements OnClickListener {
	Intent intent;
	String intent_title="";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.process_status);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        intent = getIntent();
        intent_title = intent.getExtras().getString("Title").toString();
        
        Button btnClose = (Button) findViewById(R.id.btnClose);
        Button btnDec = (Button) findViewById(R.id.btnDec);
        TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
        
        tvTitle.setText(intent_title);
        tvTitle.setTextSize(30);
        
        btnClose.setOnClickListener(this);
        btnDec.setOnClickListener(this);
        
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnClose:
			finish();
			break;
		case R.id.btnDec:
			Intent intent = new Intent(process_status.this, process_dec.class); 
			startActivityForResult(intent, 100);
			break;
				
		}
	}
}
