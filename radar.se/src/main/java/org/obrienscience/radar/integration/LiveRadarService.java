package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.obrienscience.radar.PreProcessor;
import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.Reading;
import org.obrienscience.radar.model.Site;
import org.obrienscience.radar.model.Sweep;

/**
 * 20190322
 * from http://www.weatheroffice.gc.ca/data/radar/temp_image/WMN/WMN_PRECIP_RAIN_2019_03_23_12_20.GIF
 * to   https://weather.gc.ca/data/radar/temp_image/XFT/XFT_PRECIP_SNOW_2019_03_23_17_40.GIF
 */

public class LiveRadarService extends RadarService {

	public LiveRadarService() {
		super();
	}

	
	public LiveRadarService(boolean persist, String sourceDrive, String targetDrive, int dstOffset) {
		super(persist, sourceDrive, targetDrive, dstOffset);
	}

    @Override
    public void performCapture(Site site, boolean persist, long minDelayMS, long maxDelayMS) throws Exception {
    }

    @Override
    public void performCapture(RadarView view) {
        List<Site> sites = new ArrayList<Site>();
        for(int i=0;i<NUMBER_RADAR_SITES;i++) {
            RadarSite aSite = new RadarSite();
            aSite.setName(RADAR_SITE_IDENTIFIERS[i]);
            //aSite.setUrl(getURL(aSite, GregorianCalendar.getInstance(), 10));
            aSite.setParameterizedUrl(CURRENT_RADAR_URL_MIDFIX[CURRENT_RADAR_URL_MIDFIX_INDEX[i]]);
            aSite.setParameterizedHistoricalDataUrl(HISTORICAL_RADAR_URL_FRAGMENTS[HISTORICAL_RADAR_URL_FRAGMENTS_INDEX[i]]);
            
        //siteXFT.setUrl("http://www.weatheroffice.gc.ca/data/radar/temp_image/XFT/XFT_PRECIP_RAIN_");
        //siteXFT.setParameterizedUrl("http://www.weatheroffice.gc.ca/data/radar/temp_image/%1/%2_%3_%4_%5_%6_%7_%8_%9.GIF");
        //siteXFT.setParameterizedUrl("http://www.weatheroffice.gc.ca/data/radar/temp_image/%1/%2_%3_%4_%5_%6_%7_%8_%9.GIF");
        //siteXFT.setParameterizedHistoricalDataUrl("http://www.climate.weatheroffice.gc.ca/radar/index_e.html?RadarSite=XFT&sYear=2011&sMonth=8&sDay=7&sHour=19&sMin=40&sec=00&Duration=2&ImageType=Default");
            sites.add(aSite);
       // .setParameterizedHistoricalDataUrl("http://www.climate.weatheroffice.gc.ca/radar/index_e.html?RadarSite=XFT&sYear=2011&sMonth=8&sDay=7&sHour=19&sMin=40&sec=00&Duration=2&ImageType=Default");
        }
        try {
            performCapture(sites, view);
        } catch (Exception e) {
            System.out.println(e.getMessage());//e.printStackTrace();
        }
    }
    
    @Override
    public void performCapture(List<Site> sites, boolean persist, long minDelayMS, long maxDelayMS, RadarView view) throws Exception {
        if(null != sites) {
        	getResourceManager().captureRadarIndefinitely(this, sites, minDelayMS, maxDelayMS);
        }
    }

    @Override
    // also get the last 20 min photo as backup for time drift
	public String getURL(Site site, Calendar universalTime, int minuteOffset, String codedFragment) {//, long minDelayMS, long maxDelayMS) {
        universalTime.add(Calendar.MINUTE, -minuteOffset);

		 //http://www.weatheroffice.gc.ca/data/radar/temp_image/XFT/XFT_PRECIP_RAIN_2011_05_30_23_00.GIF
        StringBuilder historicalAppend = new StringBuilder(CURRENT_URL_PREFIX);
        historicalAppend.append(site.getName());
        historicalAppend.append("/");
        historicalAppend.append(site.getName());
        historicalAppend.append(site.getParameterizedUrl()); // midfix
        historicalAppend.append(getResourceManager().getTimestampFileFormat(universalTime, "_"));
        historicalAppend.append(CURRENT_RADAR_URL_POSTFIX);
        return historicalAppend.toString();
	}

    // Inner class implements Runnable instead of extending Thread directly
    class LiveRadarServiceRunnable implements Runnable {
    	protected int id;
    	
    	public LiveRadarServiceRunnable(int anId) {    		id = anId;    	}
    	
        public void run() {
        	//connect(id);
            // We loop an arbitrary number of iterations inside each thread
            //processUnitOfWork(id);
        	performCapture(null);
        }
    }

	public static void main(String[] args) {
		RadarService aService = new LiveRadarService();
        //String param1 = null;
        String param = null;
        Boolean swingDisplay = false;
        if(null != args && args.length > 0) {
        	param = args[0];
        	if(null != param && param.equalsIgnoreCase("-disp")) {
        			swingDisplay = true;
        	}
        }

        if(null != args && args.length > 1) {
        	param = args[1];
        	if(null != param && param.equalsIgnoreCase("-proxy")) {
        			aService.getResourceManager().setProxied(true);
        	}
        }

        if(null != args && args.length > 2) {
        	param = args[2];
        	if(null != param && param.equalsIgnoreCase("-persist")) {
        			aService.setPersisting(true);
        	}
        }

        // get drives
        if(null != args && args.length > 3) {
            param = args[3];
            if(null != param && param.length() > 0) {
                aService.getResourceManager().setSourceDrive(param);
            }
        }

        if(null != args && args.length > 4) {
            param = args[4];
            if(null != param && param.length() > 0) {
                aService.getResourceManager().setTargetDrive(param);
            }
        }
        
        // get path on hd
        if(null != args && args.length > 5) {
            param = args[5];
            if(null != param && param.length() > 0) {
                aService.getResourceManager().setOverrideImagePath(param);
            }
        }
        
        
        RadarView aRadarView = null;
        if(swingDisplay.booleanValue()) { 
        	aRadarView = new RadarView();        
        	aRadarView.applicationInit();
        	aRadarView.setApplicationService(aService);
        }
        aService.setPreProcessor(new PreProcessor(false));
        aService.performCapture(aRadarView);
        
	} // main

}
