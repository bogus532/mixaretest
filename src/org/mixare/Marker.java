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

import java.net.URLDecoder;
import java.text.DecimalFormat;

import org.mixare.data.DataSource;
import org.mixare.gui.PaintScreen;
import org.mixare.gui.ScreenLine;
import org.mixare.gui.ScreenObj;
import org.mixare.gui.TextObj;
import org.mixare.reality.PhysicalPlace;
import org.mixare.render.Camera;
import org.mixare.render.MixVector;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

abstract public class Marker implements Comparable<Marker> {

	private String ID;
	protected String title;
	private boolean underline = false;
	private String URL;
	protected PhysicalPlace mGeoLoc;
	// distance from user to mGeoLoc in meters
	protected double distance;
	// From which datasource does this marker originate
	protected DataSource.DATASOURCE datasource;
	private boolean active;

	// Draw properties
	protected boolean isVisible;
//	private boolean isLookingAt;
//	private boolean isNear;
//	private float deltaCenter;
	public MixVector cMarker = new MixVector();
	protected MixVector signMarker = new MixVector();
//	private MixVector oMarker = new MixVector();
	
	protected MixVector locationVector = new MixVector();
	private MixVector origin = new MixVector(0, 0, 0);
	private MixVector upV = new MixVector(0, 1, 0);
	private ScreenLine pPt = new ScreenLine();

	protected Label txtLab = new Label();
	protected TextObj textBlock;
	
        //bogus
	public float radiusM = 0;
	public float radarW  =0;
	public float radarH  =0;
	private float rx =0;
	private float ry =0;
	public MixVector originVector = new MixVector();
	
			
	public Marker(String title, double latitude, double longitude, double altitude, String link, DataSource.DATASOURCE datasource) {
		super();

		this.active = false;
		this.title = title;
		this.mGeoLoc = new PhysicalPlace(latitude,longitude,altitude);
		if (link != null && link.length() > 0) {
			URL = "webpage:" + URLDecoder.decode(link);
			this.underline = true;
		}
		this.datasource = datasource;
		
		this.ID=datasource+"##"+title; //mGeoLoc.toString();
	}
	
	public String getTitle(){
		return title; 
	}

	public String getURL(){
		return URL;
	}

	public double getLatitude() {
		return mGeoLoc.getLatitude();
	}
	
	public double getLongitude() {
		return mGeoLoc.getLongitude();
	}
	
	public double getAltitude() {
		return mGeoLoc.getAltitude();
	}
	
	public MixVector getLocationVector() {
		return locationVector;
	}
	
	public MixVector getOriginVector() {
		return originVector;
	}
	
	public void setOriginVector(float x, float y, float z) {
		originVector.set(x,y,z);
	}
	
	public void setCenter(float x, float y)
	{
		rx = x ;
		ry = y;
	}
	
	public DataSource.DATASOURCE getDatasource() {
		return datasource;
	}

	public void setDatasource(DataSource.DATASOURCE datasource) {
		this.datasource = datasource;
	}
	
	private void cCMarker(MixVector originalPoint, Camera viewCam, float addX, float addY) {

		// Temp properties
		MixVector tmpa = new MixVector(originalPoint);
		MixVector tmpc = new MixVector(upV);
		tmpa.add(locationVector); //3 
		tmpc.add(locationVector); //3
		tmpa.sub(viewCam.lco); //4
		tmpc.sub(viewCam.lco); //4
		tmpa.prod(viewCam.transform); //5
		tmpc.prod(viewCam.transform); //5

		MixVector tmpb = new MixVector();
		viewCam.projectPoint(tmpa, tmpb, addX, addY); //6
		cMarker.set(tmpb); //7
		viewCam.projectPoint(tmpc, tmpb, addX, addY); //6
		signMarker.set(tmpb); //7
	}

	private void calcV(Camera viewCam) {
		isVisible = false;
//		isLookingAt = false;
//		deltaCenter = Float.MAX_VALUE;

		if (cMarker.z < -1f) {
			isVisible = true;

			if (MixUtils.pointInside(cMarker.x, cMarker.y, 0, 0,
					viewCam.width, viewCam.height)) {

//				float xDist = cMarker.x - viewCam.width / 2;
//				float yDist = cMarker.y - viewCam.height / 2;
//				float dist = xDist * xDist + yDist * yDist;

//				deltaCenter = (float) Math.sqrt(dist);
//
//				if (dist < 50 * 50) {
//					isLookingAt = true;
//				}
			}
		}
	}

	public void update(Location curGPSFix) {
		// An elevation of 0.0 probably means that the elevation of the
		// POI is not known and should be set to the users GPS height
		// Note: this could be improved with calls to 
		// http://www.geonames.org/export/web-services.html#astergdem 
		// to estimate the correct height with DEM models like SRTM, AGDEM or GTOPO30
		if(mGeoLoc.getAltitude()==0.0)
			mGeoLoc.setAltitude(curGPSFix.getAltitude());
		 
		// compute the relative position vector from user position to POI location
		PhysicalPlace.convLocToVec(curGPSFix, mGeoLoc, locationVector);
	}

	public void calcPaint(Camera viewCam, float addX, float addY) {
		cCMarker(origin, viewCam, addX, addY);
		calcV(viewCam);
	}

//	private void calcPaint(Camera viewCam) {
//		cCMarker(origin, viewCam, 0, 0);
//	}

	private boolean isClickValid(float x, float y) {
		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);
		//if the marker is not active (i.e. not shown in AR view) we don't have to check it for clicks
		if (!isActive())
			return false;
		
		//TODO adapt the following to the variable radius!
		pPt.x = x - signMarker.x;
		pPt.y = y - signMarker.y;
		pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.x += txtLab.getX();
		pPt.y += txtLab.getY();

		float objX = txtLab.getX() - txtLab.getWidth() / 2;
		float objY = txtLab.getY() - txtLab.getHeight() / 2;
		float objW = txtLab.getWidth();
		float objH = txtLab.getHeight();

		if (pPt.x > objX && pPt.x < objX + objW && pPt.y > objY
				&& pPt.y < objY + objH) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isClickValid_radar(float x, float y,float marX,float marY, float rotation,float barheight) {
		
		//if the marker is not active (i.e. not shown in AR view) we don't have to check it for clicks
		if (!isActive())
			return false;
		float margin = 50;
		//TODO adapt the following to the variable radius!
		pPt.x = marX;
		pPt.y = marY;
		Log.d("fClick2","orig :x:"+pPt.x+", y:"+pPt.y);
		//pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.rotate2(Math.toRadians((rotation)),rx,ry);
		Log.d("fClick2","changed r :"+rotation+", x:"+pPt.x+", y:"+pPt.y+", rx= "+rx+",ry= "+ry);
		Log.d("fClick2","changed :x:"+pPt.x+", y:"+pPt.y);
		
		pPt.y += barheight;
		
		float objX = pPt.x-margin;
		float objY = pPt.y-margin;
		float objW = pPt.x+margin;
		float objH = pPt.y+margin;
		//Log.d("Click","x:"+x+", y:"+y+", getX:"+txtLab.getX()+", getY:"+txtLab.getY());
		//Log.d("MixView","pPt.x:"+pPt.x+", pPt.y:"+pPt.y+", objX:"+objX+", objY:"+objY+", objW:"+objW+", objH:"+objH);
		//Log.d("MixView","pPt.x:"+pPt.x+", pPt.y:"+pPt.y+", objX:"+objX+objW+", objY:"+objY+objH);

		if (x > objX && x < objW && y > objY
				&& y < objH) {
			Log.d("fClick2","Valid");
			return true;
		} else {
			return false;
		}
	}
	
	
	public void draw(PaintScreen dw) {
		
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
		Bitmap bitmap = DataSource.building;
		if(bitmap!=null) {
		//	dw.paintBitmap(bitmap, cMarker.x - maxHeight/1.5f, cMarker.y - maxHeight/1.5f);
		//	dw.paintBitmap(bitmap, cMarker.x, cMarker.y);
		}
		//bogus
		//drawCircle(dw);
		//drawTextBlock(dw);
		//Log.d("cMarker","cMarker.x = "+cMarker.x+", cMarker.y = "+cMarker.y);
	}
	
	public void draw_radar_point(PaintScreen dw,float x,float z,float scale, float radius) {
		
		Bitmap bitmap = DataSource.building;
		
		radiusM = radius;
		//Log.d("TextBlock","before : x = "+x+", y = "+z);
		if(bitmap!=null) {
			x = x - bitmap.getWidth()/scale;
			z = z - bitmap.getHeight()+10;
			
			dw.paintBitmap(bitmap, x, z);
		}
		//Log.d("TextBlock","after : x = "+x+", y = "+z);
		drawTextBlock2(dw,x,z);
		//drawTextBlock(dw);
	}

	public void drawCircle(PaintScreen dw) {

		if (isVisible) {
			//float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
			float maxHeight = dw.getHeight();
			dw.setStrokeWidth(maxHeight / 100f);
			dw.setFill(false);
			dw.setColor(DataSource.getColor(datasource));
			
			//draw circle with radius depending on distance
			//0.44 is approx. vertical fov in radians 
			double angle = 2.0*Math.atan2(10,distance);
			double radius = Math.max(Math.min(angle/0.44 * maxHeight, maxHeight),maxHeight/25f);
			//double radius = angle/0.44d * (double)maxHeight;
	
			dw.paintCircle(cMarker.x, cMarker.y, (float)radius);
		}
	}
	
	public void drawTextBlock(PaintScreen dw) {
		//TODO: grandezza cerchi e trasparenza
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		//TODO: change textblock only when distance changes
		String textStr="";

		double d = distance;
		DecimalFormat df = new DecimalFormat("@#");
		if(d<1000.0) {
			textStr = title + " ("+ df.format(d) + "m)";			
		}
		else {
			d=d/1000.0;
			textStr = title + " (" + df.format(d) + "km)";
		}
		
		textBlock = new TextObj(textStr, Math.round(maxHeight / 2f) + 1,
				250, dw, underline);

		if (isVisible) {
			
			dw.setColor(DataSource.getColor(datasource));

			float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);

			txtLab.prepare(textBlock);

			dw.setStrokeWidth(1f);
			dw.setFill(true);
			
			dw.paintObj(txtLab, signMarker.x - txtLab.getWidth()
					/ 2, signMarker.y + maxHeight, currentAngle + 90, 1);
		}

	}
	
	public void drawTextBlock2(PaintScreen dw,float x,float y) {
		
		float maxHeight = Math.round(dw.getWidth() / 10f) + 1;

		//TODO: change textblock only when distance changes
		String textStr="";

		double d = distance;
		DecimalFormat df = new DecimalFormat("@#");
		if(d<1000.0) {
			textStr = title + " ("+ df.format(d) + "m)";			
		}
		else {
			d=d/1000.0;
			textStr = title + " (" + df.format(d) + "km)";
		}
		
		textBlock = new TextObj(textStr, Math.round(maxHeight / 2f) + 1,
				250, dw, underline);

		//if (isVisible) {
		if (true) {
			
			dw.setColor(DataSource.getColor(datasource));

			txtLab.prepare(textBlock);

			dw.setStrokeWidth(1f);
			dw.setFill(true);
			
			//dw.paintObj2(txtLab, x - txtLab.getWidth()/ 2, y + maxHeight, 0, 1);
			dw.paintObj2(txtLab, x, y, 0, 1);
			//Log.d("TextBlock","textblock x = "+(x - txtLab.getWidth()/ 2)+", y = "+(y + maxHeight));
			//Log.d("TextBlock","x= "+x+", y= "+y);
		}

	}
	
	public void setRadarPoiSize(float w, float h)
	{
		radarW = w;
		radarH = h;
	}

	public boolean fClick(float x, float y, MixContext ctx, MixState state) {
		boolean evtHandled = false;

		if (isClickValid(x, y)) {
			evtHandled = state.handleEvent(ctx, URL);
			//Log.d("MixView","fClick");
		}
		return evtHandled;
	}
	
	public boolean fClick(float x, float y, MixContext ctx, MixState state,float w, float h,float r,float barheight) {
		boolean evtHandled = false;
		//Log.d("fClick2","touch :x = "+x+", y = "+y+", w = "+w+", h = "+h);
		Log.d("fClick2","touch :x = "+x+", y = "+y);
		String title="";
		title = getTitle();
		if (isClickValid_radar(x, y,w,h,r,barheight)) {
			evtHandled = state.handleEvent(ctx, URL, title);
			//Log.d("MixView","fClick2");
		}
		return evtHandled;
	}
	
	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	@Override
	public int compareTo(Marker another) {

		Marker leftPm = this;
		Marker rightPm = another;

		return Double.compare(leftPm.getDistance(), rightPm.getDistance());

	}

	@Override
	public boolean equals (Object marker) {
		return this.ID.equals(((Marker) marker).getID());
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	abstract public int getMaxObjects();
 
}

class Label implements ScreenObj {
	private float x, y;
	private float width, height;
	private ScreenObj obj;

	public void prepare(ScreenObj drawObj) {
		obj = drawObj;
		float w = obj.getWidth();
		float h = obj.getHeight();

		x = w / 2;
		y = 0;

		width = w * 2;
		height = h * 2;
	}

	public void paint(PaintScreen dw) {
		dw.paintObj(obj, x, y, 0, 1);
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

}