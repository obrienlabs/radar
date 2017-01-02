package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.obrienscience.radar.PreProcessor;
import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.Reading;
import org.obrienscience.radar.model.Site;
import org.obrienscience.radar.model.Sweep;

public class HistoricalRadarService extends RadarService {
    //private int offsetDay;
    //private int offsetYear;
    //private int offsetMonth;
    public static final boolean IS_HISTORICAL = true;
    private Calendar universalTime;
    private int siteIndex = -1;

    public HistoricalRadarService(int year, int month, int day, String site) {
    	setHistorical(true);
    	universalTime = GregorianCalendar.getInstance();
    	if(year > 0 && month > 0 && day > 0) {
    		universalTime.set(Calendar.YEAR, year);
    		universalTime.set(Calendar.MONTH, month);
    		universalTime.set(Calendar.DAY_OF_MONTH, day);
    	}
    	if(null != site) {
    		siteIndex = Integer.parseInt(site);
    		//ApplicationService.RADAR_SITE_IDENTIFIERS[siteIndex];
    		
    	}
		System.out.println("_Time set to " + universalTime.toString());
    }
    
    /*public void setOffsetDate(int year, int month, int day) {
        offsetDay = day;
        offsetMonth = month;
        offsetYear = year;
    }*/

    //@Override
    public void persistImage(Site site, BufferedImage image, String name) {
    }
    
	
	/**
     * http://www.climate.weatheroffice.gc.ca/radar/index_e.html
     * ?RadarSite=XFT&sYear=2011&sMonth=8&sDay=7&sHour=19&sMin=40&sec=00&Duration=2&ImageType=Default
	 */
    @Override
	public void performCapture(Site site, boolean persist, long minDelayMS, long maxDelayMS) throws Exception {
    	
    }
    
    public void performCapture(List<Site> sites, boolean persist, long minDelayMS, long maxDelayMS, RadarView view) throws Exception {
        PreProcessor preProcessor = new PreProcessor();
        preProcessor.setApplicationServiceOnView(this);
        BufferedImage reducedImage = null;
        String outputPath = null;
        String filenameRoot = null;
	    // search for missing images from current date - down
	    //Calendar universalTime = GregorianCalendar.getInstance();
	    //universalTime.add(Calendar.HOUR, 4); // get UTC
	    //universalTime.add(Calendar.DAY_OF_MONTH, -day); // start with a day less
	    //universalTime.add(Calendar.DAY_OF_MONTH, -35);
	    //universalTime.add(Calendar.MONTH, -4);
		// start at 20110513
	    String imageName = null;
	    List<Site> siteFilterList = null;
	    if(siteIndex > -1) {
	    	siteFilterList = new ArrayList<Site>();
	    	siteFilterList.add(sites.get(siteIndex));
	    } else {
	    	siteFilterList = sites;
	    }
		for(;;) {
		    // remember to subtract 10 min. or sleep 10 min
		    universalTime.add(Calendar.MINUTE, -SWEEP_INTERVAL_MIN);
        	for(Site site : siteFilterList) {
        	    // look for image on hd, get it if missing, wait random time
        	    String filename = getResourceManager().getTimestampFileFormat(universalTime, "_");
        	    String filenamePath = getResourceManager().getFilename(site, filename, CURRENT_RADAR_URL_POSTFIX);
        	    BufferedImage image = loadImage(filenamePath);
        	    if(null == image) {
        	        // get image from net
        	        try {
        	            // as of 20110920 : we cannot directly get the image name - it is encoded - get the page first
        	            StringBuffer historicalURL  = new StringBuffer(HISTORICAL_RADAR_URL_SEARCH_PAGE_PREFIX);
        	            historicalURL.append(site.getName());
        	            historicalURL.append("&sYear=");
        	            historicalURL.append(universalTime.get(Calendar.YEAR));//"2011");
        	            historicalURL.append("&sMonth=");
        	            historicalURL.append(1 + universalTime.get(Calendar.MONTH));
        	            historicalURL.append("&sDay=");
        	            historicalURL.append(universalTime.get(Calendar.DAY_OF_MONTH));
        	            historicalURL.append("&sHour=");
        	            if(universalTime.get(Calendar.AM_PM) > 0) {
        	                historicalURL.append((universalTime.get(Calendar.HOUR) + 12));
        	            } else {
        	                historicalURL.append((universalTime.get(Calendar.HOUR)));
        	            }
        	            historicalURL.append("&sMin=");
        	            historicalURL.append((((int)(universalTime.get(Calendar.MINUTE)) / 10)));
        	            historicalURL.append("0");
        	            historicalURL.append("&sec=");
        	            historicalURL.append("00");
        	            historicalURL.append("&Duration=2&ImageType=Default");
        	            String historicalHTML = getResourceManager().captureURL(
                                this,
                                site, 
                                historicalURL.toString()//getURL(site, universalTime, 0),
                                );
        	            // parse html file
        	            //var NumImages =13;
        	            // http://www.climate.weatheroffice.gc.ca/radar/image.php?time=07-AUG-08%2007.47.23.721307%20PM&site=WHK
        	            //var blobArray = new Array("<img src='./image.php?time=07-AUG-11%2007.57.46.914512%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />",
        	            //"<img src='./image.php?time=07-AUG-11%2007.57.46.140544%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />",
        	            //"<img src='./image.php?time=07-AUG-11%2008.07.52.603743%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2008.28.04.148385%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2008.28.01.505846%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2008.37.49.863105%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2008.47.47.521850%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2008.57.51.041539%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2009.07.56.197892%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2009.17.49.274688%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2009.27.51.785989%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2009.37.52.981699%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","<img src='./image.php?time=07-AUG-11%2009.47.52.122318%20PM&amp;site=XFT' class='noBorder' alt='Radar Image' />","");
        	            //System.out.println(historicalHTML);
        	            // get first image
        	            String searchMatch = "image.php?time=";
        	            int searchPosition1 = historicalHTML.indexOf(searchMatch);
        	            if(searchPosition1 < 1) {
                            //System.out.println(historicalHTML);
        	            	imageName = null;
        	            	System.out.println(filename + " not available - skipping");
        	            } else {
        	            	int searchPosition2 = historicalHTML.indexOf("&amp;site=", searchPosition1);
        	            	String codedFragment = historicalHTML.substring(searchPosition1 + searchMatch.length(), searchPosition2); 
        	            	//System.out.println(codedFragment);
        	            	imageName = getResourceManager().captureImage(
        	            			this,
        	            			site, 
        	            			filename, 
        	            			getURL(site, universalTime, 0, codedFragment),
        	            			CURRENT_RADAR_URL_POSTFIX);				
        	            }
        	        } catch (Exception e) {
        	            e.printStackTrace();
        	            imageName = null;
        	        }
        	        if(null == imageName) {
        	        	// delete 0-length image
        	        	//remove0lengthImage();
        	        	
        	        } else {
        	            image = loadImage(filenamePath);
        	            if(null != image) { // server 500/3xx
        	                setCurrentImage(image);//reducedImage);
        	                reducedImage = preProcessor.doFilter(0, image, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);        	            
        	                filenameRoot = filename.substring(0, filename.length());
        	                outputPath = PreProcessor.FILTERED_DATA_DIR + site.getName() + "/" + site.getName() + "_" + filenameRoot + "_f";
        	                preProcessor.writeImage(reducedImage, outputPath, "gif");
        	                if(null != view) {
        	                	view.setBufferedImage(reducedImage);
        	                }
        	            }
        	        }
        	        getResourceManager().delayRandom(minDelayMS, maxDelayMS);
        	    } else {
        	        System.out.println(filenamePath + " already exists");
        	        setCurrentImage(image);
        	        getResourceManager().delayRandom(5,40);
        	    }
        	    if(isValidImage(image)) {
        	        // persistImage(site, image, buffer.toString());
        	    }
        	}
	    }
	}

	/**
	 * 
	 * @param siteName
	 * @param universalTime
	 * @return
	 */
    @Override
    public String getURL(Site site, Calendar universalTimeStart, int minuteOffset, String codedFragment) {//,  long minDelayMS, long maxDelayMS) {
        //http://www.climate.weatheroffice.gc.ca/radar/index_e.html
        //?RadarSite=XFT&sYear=2011&sMonth=8&sDay=7&sHour=19&sMin=40&sec=00&Duration=2&ImageType=Default
        //http://www.climate.weatheroffice.gc.ca/radar/get_image.cfm?img=201108071940~XFT_PRECIP_RAIN_WEATHEROFFICE_ARC~PRECIP,125,18,MPRATE:URP:XFT:RADAR:GIF
    	// 201108071940~WMN_CAPPI_RAIN_WEATHEROFFICE_ARC~CAPPI,1.5,AGL,MPRATE:URP:WMN:RADAR:GIF
        // as of 20110920
        StringBuilder historicalAppend = new StringBuilder(HISTORICAL_RADAR_URL_PREFIX);
        universalTimeStart.add(Calendar.MINUTE, minuteOffset);
        /**
        historicalAppend.append(getResourceManager().getTimestampFileFormat(universalTimeStart, ""));
        historicalAppend.append("~");
        historicalAppend.append(site.getName());
        historicalAppend.append(site.getParameterizedHistoricalDataUrl());
        historicalAppend.append(":URP:");
        historicalAppend.append(site.getName());
        historicalAppend.append(HISTORICAL_URL_POSTFIX);
        */
        //http://www.climate.weatheroffice.gc.ca/radar/image.php?time=07-AUG-08%2007.47.23.721307%20PM&site=WHK
        //historicalAppend.append(getResourceManager().getTimestampFileFormatHistorical(universalTimeStart, ""));
        //historicalAppend.append("%20");
        historicalAppend.append(codedFragment);
        historicalAppend.append("&site=");        
        historicalAppend.append(site.getName());
        //historicalAppend.append(site.getParameterizedHistoricalDataUrl());
        //historicalAppend.append(":URP:");
        //historicalAppend.append(site.getName());
        //historicalAppend.append(HISTORICAL_URL_POSTFIX);
        return historicalAppend.toString();
    }

    // Inner class implements Runnable instead of extending Thread directly
    class HistoricalRadarServiceRunnable implements Runnable {
    	protected int id;
    	
    	public HistoricalRadarServiceRunnable(int anId) {    		id = anId;    	}
    	
        public void run() {
        	//connect(id);
            // We loop an arbitrary number of iterations inside each thread
            //processUnitOfWork(id);
        	performCapture(null);
        }
    }
    
	public static void main(String[] args) {
        String param = null;
        String site = null;
        String day = "0";;
        String month = "0";
        String year = "0";        
        // 2013 02 01 30 for xft feb
        if(null != args && args.length > 1) {
        	param = args[0];
        	if(null != param) {
        		year = param;
        	}
            param = args[1];
            if(null != param) {
                month = param;
            }
            param = args[2];
            if(null != param) {
                day = param;
            }
            if(args.length > 3) {
            	param = args[3];
            	if(null != param) {
            		site = param;
            	}
            }

        }
        
		ApplicationService aService = null; 
        try {
        	//if(null != year && null != month && null != day) {
        		aService = new HistoricalRadarService(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), site);
        	//}
            RadarView aRadarView = new RadarView();
            aRadarView.applicationInit();
            //aRadarView.setApplicationService(aService);
            aRadarView.setFlash(true);
        	//aService.set
        	aService.performCapture(aRadarView);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
	} // main

}
