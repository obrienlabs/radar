package org.obrienscience.radar.integration;

import org.obrienscience.radar.model.GPS;
import org.obrienscience.radar.model.RadarSite;

public class LoadConfiguration {

	public void reset() {
		RadarSite franktown = new RadarSite();
		GPS dishGPS = new GPS();
		franktown.setDishGPS(dishGPS);
        franktown.setName("XFT");
        franktown.setUrl("http://www.weatheroffice.gc.ca/data/radar/temp_image/XFT/XFT_PRECIP_RAIN_");
        franktown.setUrl("http://www.weatheroffice.gc.ca/data/radar/temp_image/%1/%2_%3_%4_%5_%6_%7_%8_%9.GIF");
        franktown.setParameterizedHistoricalDataUrl("http://www.climate.weatheroffice.gc.ca/radar/index_e.html?RadarSite=XFT&sYear=2011&sMonth=8&sDay=7&sHour=19&sMin=40&sec=00&Duration=2&ImageType=Default");


	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadConfiguration config = new LoadConfiguration();
		config.reset();

	}

}
