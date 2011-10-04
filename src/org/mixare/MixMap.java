/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package org.mixare;

import java.util.ArrayList;
import java.util.List;

import org.mixare.data.DataHandler;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MixMap extends MapActivity implements OnTouchListener, LocationListener{
	
	public static final String TAG = "MixareMap";

	private static List<Overlay> mapOverlays;
	private Drawable drawable;

	private static List<Marker> markerList;
	private static DataView dataView;
	private static GeoPoint startPoint;

	private MixContext mixContext;
	private MapView mapView;

	static MixMap map;
	private static Context thisContext;
	private static TextView searchNotificationTxt;
	public static List<Marker> originalMarkerList;
	
	//added by bogus 
	private float mHeading = 0;
	private SensorManager mSensorManager;
	private LocationManager locationMgr;
	
    private RotateView mRotateView;
    
    private MyLocationOverlay mMyLocationOverlay;
    
    private PowerManager.WakeLock mWakeLock;
    
    private boolean isGpsEnabled;
 
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataView = MixView.dataView;
		mixContext = dataView.getContext();
		setMarkerList(dataView.getDataHandler().getMarkerList());
		map = this;
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		locationMgr=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000,10, this);
		
        mRotateView = new RotateView(this);
        
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"PowerManager.WakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK)");

		setMapContext(this);
		mapView= new MapView(this, "0RoAqcK5q9k9W5ugymbqejKB_SeOfTNW_ylp4hw");
		mapView.setBuiltInZoomControls(true);
		mapView.setClickable(true);
		mapView.setSatellite(false);
		mapView.setEnabled(true);
		
		//this.setContentView(mapView);
		mRotateView.addView(mapView);
        setContentView(mRotateView);
 
		setStartPoint();
		createOverlay();

		if (dataView.isFrozen()){
			searchNotificationTxt = new TextView(this);
			searchNotificationTxt.setWidth(MixView.dWindow.getWidth());
			searchNotificationTxt.setPadding(10, 2, 0, 0);			
			searchNotificationTxt.setText(getString(DataView.SEARCH_ACTIVE_1)+" "+ mixContext.getDataSourcesStringList() + getString(DataView.SEARCH_ACTIVE_2));
			searchNotificationTxt.setBackgroundColor(Color.DKGRAY);
			searchNotificationTxt.setTextColor(Color.WHITE);

			searchNotificationTxt.setOnTouchListener(this);
			addContentView(searchNotificationTxt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
		
		
	}
	
	public void getData()
	{
		dataView = MixView.dataView;
		mixContext = dataView.getContext();
		setMarkerList(dataView.getDataHandler().getMarkerList());
		Log.d(TAG,"count = "+dataView.getDataHandler().getMarkerCount());
	}

	public void setStartPoint() {
		Location location = mixContext.getCurrentLocation();
		MapController controller;

		double latitude = location.getLatitude()*1E6;
		double longitude = location.getLongitude()*1E6;
		
		//Log.d("MapView", "latitude: "+latitude+" longitude: "+longitude );

		controller = mapView.getController();
		startPoint = new GeoPoint((int)latitude, (int)longitude);
		controller.setCenter(startPoint);
		controller.setZoom(18);
	}

	public void createOverlay(){
		mapOverlays=mapView.getOverlays();
		OverlayItem item; 
		//drawable = this.getResources().getDrawable(R.drawable.icon_map);
		drawable = this.getResources().getDrawable(R.drawable.ar_poi_1);
		MixOverlay mixOverlay = new MixOverlay(this, drawable);

		for(Marker marker:markerList) {
			if(marker.isActive()) {
				GeoPoint point = new GeoPoint((int)(marker.getLatitude()*1E6), (int)(marker.getLongitude()*1E6));
				item = new OverlayItem(point, "", "");
				mixOverlay.addOverlay(item);
			}
		}
		//Solved issue 39: only one overlay with all marker instead of one overlay for each marker
		mapOverlays.add(mixOverlay);

		MixOverlay myOverlay;
		drawable = this.getResources().getDrawable(R.drawable.loc_icon);
		myOverlay = new MixOverlay(this, drawable);

		item = new OverlayItem(startPoint, "Your Position", "");
		myOverlay.addOverlay(item);
		mapOverlays.add(myOverlay); 
		
		//mMyLocationOverlay = new MyLocationOverlay(this, mapView);
		//mMyLocationOverlay.enableCompass();
		
		//mapOverlays.add(mMyLocationOverlay);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int base = Menu.FIRST;
		/*define the first*/
		MenuItem item1 =menu.add(base, base, base, getString(DataView.MAP_MENU_NORMAL_MODE)); 
		MenuItem item2 =menu.add(base, base+1, base+1, getString(DataView.MAP_MENU_SATELLITE_MODE));
		MenuItem item3 =menu.add(base, base+2, base+2, getString(DataView.MAP_MY_LOCATION)); 
		MenuItem item4 =menu.add(base, base+3, base+3, getString(DataView.MENU_ITEM_2)); 
		//MenuItem item5 =menu.add(base, base+4, base+4, getString(DataView.MENU_CAM_MODE)); 

		/*assign icons to the menu items*/
		item1.setIcon(android.R.drawable.ic_menu_gallery);
		item2.setIcon(android.R.drawable.ic_menu_mapmode);
		item3.setIcon(android.R.drawable.ic_menu_mylocation);
		item4.setIcon(android.R.drawable.ic_menu_view);
		//item5.setIcon(android.R.drawable.ic_menu_camera);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		/*Satellite View*/
		case 1:
			mapView.setSatellite(false);
			break;
			/*street View*/
		case 2:		
			mapView.setSatellite(true);
			break;
			/*go to users location*/
		case 3:
			setStartPoint();
			break;
			/*List View*/
		case 4:
			createListView();
			finish();
			break;
			/*back to Camera View*/
		case 5:
			finish();
			break;
		}
		return true;
	}

	public void createListView(){
		MixListView.setList(2);
		if (dataView.getDataHandler().getMarkerCount() > 0) {
			Intent intent1 = new Intent(MixMap.this, MixListView.class); 
			startActivityForResult(intent1, 42);
		}
		/*if the list is empty*/
		else{
			Toast.makeText( this, DataView.EMPTY_LIST_STRING_ID, Toast.LENGTH_LONG ).show();			
		}
	}

//	public static ArrayList<Marker> getMarkerList(){
//		return markerList;
//	}

	public void setMarkerList(List<Marker> maList){
		markerList = maList;
	}

	public DataView getDataView(){
		return dataView;
	}

//	public static void setDataView(DataView view){
//		dataView= view;
//	}

//	public static void setMixContext(MixContext context){
//		ctx= context;
//	}
//
//	public static MixContext getMixContext(){
//		return ctx;
//	}

	public List<Overlay> getMapOverlayList(){
		return mapOverlays;
	}

	public void setMapContext(Context context){
		thisContext= context;
	}

	public Context getMapContext(){
		return thisContext;
	}

	public void startPointMsg(){
		Toast.makeText(getMapContext(), DataView.MAP_CURRENT_LOCATION_CLICK, Toast.LENGTH_LONG).show();
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void doMixSearch(String query) {
		DataHandler jLayer = dataView.getDataHandler();
		if (!dataView.isFrozen()) {
			originalMarkerList = jLayer.getMarkerList();
			MixListView.originalMarkerList = jLayer.getMarkerList();
		}
		markerList = new ArrayList<Marker>();

		for(int i = 0; i < jLayer.getMarkerCount(); i++) {
			Marker ma = jLayer.getMarker(i);

			if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase())!=-1){
				markerList.add(ma);
			}
		}
		if(markerList.size()==0){
			Toast.makeText( this, getString(DataView.SEARCH_FAILED_NOTIFICATION), Toast.LENGTH_LONG ).show();
		}
		else{
			jLayer.setMarkerList(markerList);
			dataView.setFrozen(true);

			finish();
			Intent intent1 = new Intent(this, MixMap.class); 
			startActivityForResult(intent1, 42);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		dataView.setFrozen(false);
		dataView.getDataHandler().setMarkerList(originalMarkerList);

		searchNotificationTxt.setVisibility(View.INVISIBLE);
		searchNotificationTxt = null;
		finish();
		Intent intent1 = new Intent(this, MixMap.class); 
		startActivityForResult(intent1, 42);

		return false;
	}
	
	//added by bogus 	
	/*
	public boolean onKeyDown(int keyCode, KeyEvent event){

		  switch(keyCode){
		  case KeyEvent.KEYCODE_BACK:
		   String alertTitle = getResources().getString(R.string.app_name);

		   new AlertDialog.Builder(MixMap.this)
		   .setTitle(alertTitle)
		   .setMessage("종료하겠습니까?")
		   .setPositiveButton("예", new DialogInterface.OnClickListener(){
		    
		    public void onClick(DialogInterface dialog, int which){
		     moveTaskToBack(true); // 본Activity finish후 다른 Activity가 뜨는 걸 방지.
		     finish();
		     //android.os.Process.killProcess(android.os.Process.myPid()); 
		     // -> 해당 어플의 프로세스를 강제 Kill시킨다.
		    }    
		   })
		   .setNegativeButton("아니오", null)
		   .show();
		  }
		 
		  return true;
	}
	//*/

	public void execProcessStatus()
	{
		Intent intent1 = new Intent(MixMap.this, process_status.class); 
		intent1.putExtra("Title", "None");
		startActivityForResult(intent1, 100);
	}
	
	public void execProcessStatus(String title)
	{
		Intent intent1 = new Intent(MixMap.this, process_status.class); 
		intent1.putExtra("Title", title);
		startActivityForResult(intent1, 100);
	}
	
	@Override
    protected void onResume() {
        super.onResume();
       
        mSensorManager.registerListener(myOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
        //myOverlay.enableMyLocation();
        
        this.mWakeLock.acquire();

        locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000,10, this);

    }
	
	@Override
	protected void onPause() {
		super.onPause();
		this.mWakeLock.release();
		locationMgr.removeUpdates(this);
        locationMgr = null;
        mixContext.downloadManager.stop();
	}

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(myOrientationListener);
        //myOverlay.disableMyLocation();
        super.onStop();
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		//this.mWakeLock.release();
		//locationMgr.removeUpdates(this);
        //locationMgr = null;
        //mixContext.downloadManager.stop();
	}
	
	final SensorEventListener myOrientationListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                mHeading = sensorEvent.values[0];
                mRotateView.invalidate();
                
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    
	private class RotateView extends ViewGroup  {
        private Matrix mMatrix = new Matrix();
                private float[] mTemp = new float[2]; 
    private static final float SQ2 = 1.414213562373095f;

    public RotateView(Context context) {
        super(context);
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(-mHeading, getWidth() * 0.5f, getHeight() * 0.5f);
        canvas.getMatrix().invert(mMatrix);
       super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getWidth();
        final int height = getHeight();
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            final int childWidth = view.getMeasuredWidth();
            final int childHeight = view.getMeasuredHeight();
            final int childLeft = (width - childWidth) / 2;
            final int childTop = (height - childHeight) / 2;
            view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int sizeSpec;
        if (w > h) {
            sizeSpec = MeasureSpec.makeMeasureSpec((int) (w * SQ2), MeasureSpec.EXACTLY);
        } else {
            sizeSpec = MeasureSpec.makeMeasureSpec((int) (h * SQ2), MeasureSpec.EXACTLY);
        }
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(sizeSpec, sizeSpec);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float[] temp = mTemp;
        temp[0] = event.getX();
        temp[1] = event.getY();
        mMatrix.mapPoints(temp);
        event.setLocation(temp[0], temp[1]);
        return super.dispatchTouchEvent(event);
    }

 }


	@Override
	public void onLocationChanged(Location location) {
		
		try {
			Log.v(TAG,"Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy());
			if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
				synchronized (mixContext.curLoc) {
					mixContext.curLoc = location;
				}
				if(!dataView.isFrozen())
					dataView.getDataHandler().onLocationChanged(location);
				// If we have moved more than radius/3 km away from the 
				// location where the last download occured we should start 
				// a fresh download
				Location lastLoc=mixContext.getLocationAtLastDownload();
				if(lastLoc==null)
					mixContext.setLocationAtLastDownload(location);
				else {
					float threshold = dataView.getRadius()*1000f/3f;
					Log.v(TAG,"Location Change: "+" threshold "+threshold+" distanceto "+location.distanceTo(lastLoc));
					if(location.distanceTo(lastLoc)>threshold)  {
						Log.d(TAG,"Restarting download due to location change");
						dataView.doStart();
					}	
				}
				isGpsEnabled = true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		isGpsEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		isGpsEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

	public boolean isGpsEnabled() {
		return isGpsEnabled;
	}

}


class MixOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	private MixMap mixMap;

	public MixOverlay(MixMap mixMap, Drawable marker){
		super (boundCenterBottom(marker));
		//need to call populate here. See
		//http://code.google.com/p/android/issues/detail?id=2035
		populate();
		this.mixMap = mixMap;
	}

	@Override
	protected OverlayItem createItem(int i) {
		return overlayItems.get(i);
	}

	@Override
	public int size() {
		return overlayItems.size();
	}

	@Override
	protected boolean onTap(int index){
		Log.d("Mixare","onTap : "+size());
		if (size() == 1)
			mixMap.startPointMsg();
		else if (mixMap.getDataView().getDataHandler().getMarker(index).getURL() !=  null) {
			/*
			String url = mixMap.getDataView().getDataHandler().getMarker(index).getURL();
			Log.d("MapView", "opern url: "+url);
			try {
				if (url != null && url.startsWith("webpage")) {
					String newUrl = MixUtils.parseAction(url);
					mixMap.getDataView().getContext().loadWebPage(newUrl, mixMap.getMapContext());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/  
			mixMap.execProcessStatus(mixMap.getDataView().getDataHandler().getMarker(index).getTitle());
		}

		return true;
	}

	public void addOverlay(OverlayItem overlay) {
		overlayItems.add(overlay);
		populate();
	}
	
	public void draw(Canvas canvas, MapView mapv, boolean shadow){
        super.draw(canvas, mapv, shadow);
	}
}

