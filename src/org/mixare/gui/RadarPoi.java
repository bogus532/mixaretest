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
package org.mixare.gui;

import org.mixare.DataView;
import org.mixare.Marker;
import org.mixare.data.DataHandler;
import org.mixare.data.DataSource;

import android.graphics.Color;
import android.util.Log;

/** Takes care of the small radar in the top left corner and of its points
 * @author daniele
 *
 */
public class RadarPoi implements ScreenObj {
	/** The screen */
	public DataView view;
	/** The radar's range */
	float range;
	/** Radius in pixel on screen */
	public static float RADIUS = 200;
	/** Position on screen */
	static float originX = 0 , originY = 0;
	/** Color */
	static int radarColor = Color.argb(100, 200, 0, 0);
	
	float draw_lot = 0;
	
	public void paint(PaintScreen dw) {
		/** radius is in KM. */
		range = view.getRadius() * 1000;
		
		draw_lot = view.getbearing();
		
		/** put the markers in it */
		float scale = range / RADIUS;

		DataHandler jLayer = view.getDataHandler();
		//Log.d("TextBlock","------------------------------------------------------------");
		for (int i = 0; i < jLayer.getMarkerCount(); i++) {
			Marker pm = jLayer.getMarker(i);
			float x = pm.getLocationVector().x / scale;
			float y = pm.getLocationVector().z / scale;
			
			if (pm.isActive() && (x * x + y * y < RADIUS * RADIUS)) {
				dw.setFill(true);
				
				dw.setColor(DataSource.getColor(pm.getDatasource()));
				dw.paintRect(x + RADIUS - 1, y + RADIUS - 1, 10, 10);
				
				//bogus
				pm.setOriginVector(x + RADIUS - 1,0,y + RADIUS - 1);
				//pm.draw_radar_point(dw, (float) ((x + RADIUS - 1)*Math.sin(draw_lot)), (float) ((y + RADIUS - 1)*Math.cos(draw_lot)));
				pm.draw_radar_point(dw, x + RADIUS - 1, y + RADIUS - 1,scale,RADIUS-1);
				
				//Log.d("Marker",i+", marker x= "+(x + RADIUS - 1+39)+", y= "+(y + RADIUS - 1+199));
			}
		}
	}
	
	/** Width on screen */
	public float getWidth() {
		return RADIUS * 2;
	}

	/** Height on screen */
	public float getHeight() {
		return RADIUS * 2;
	}
	
}

