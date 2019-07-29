package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.SatSite;
import org.obrienscience.radar.model.Site;

public class SatService extends ApplicationService {
    //public static final String CURRENT_URL_SAT_PREFIX = "https://www.weatheroffice.gc.ca/data/satellite/goes_";
    // 20190720
	public static final String CURRENT_URL_SAT_PREFIX = "https://weather.gc.ca/data/satellite/goes_";
    public static final String CURRENT_URL_SAT_POSTFIX = "_1070_100.jpg";

    /*
http://www.weatheroffice.gc.ca/data/satellite/geos_ecan_1070_100.jpg
http://www.weatheroffice.gc.ca/data/satellite/geos_ecan_1070_100.jpg
http://www.weatheroffice.gc.ca/data/satellite/geos_wcan_1070_100.jpg
http://www.weatheroffice.gc.ca/data/satellite/geos_nam_1070_100.jpg
     */

	public SatService() {
		super();
	}

	public SatService(boolean persist, String sourceDrive, String targetDrive, int dstOffset) {
		super(persist, sourceDrive, targetDrive, dstOffset);
	}
    
    //@Override
    public void persistImage(Site site, BufferedImage image, String name) {
    }
    
    @Override
    public void performCapture(Site site, boolean persist, long minDelayMS, long maxDelayMS) throws Exception {        
    }

    public void performCaptureNIO(RadarView view) {
    	URL url;
    	String FILE_NAME = "/Users/michaelobrien/geos_ecan_1070_100.jpg";
    	FileOutputStream fileOutputStream = null;
    	FileChannel fileChannel = null;

    	try {
    	url = new URL("https://weather.gc.ca/data/satellite/goes_ecan_1070_100.jpg");
    	//url = new URL("http://wiki.obrienlabs.cloud/download/attachments/983215/Screenshot%202019-07-02%2016.21.27.png?version=1&modificationDate=1562098913258&api=v2");
    	ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
    	fileOutputStream = new FileOutputStream(FILE_NAME);
    	fileChannel = fileOutputStream.getChannel();
    	fileOutputStream.getChannel()
    	  .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    	
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		try {
    		fileOutputStream.close();
    		} catch (IOException fe) {
    			fe.printStackTrace();
    		}
    	}
    }
    
    public void performCapture(RadarView view) {
        List<Site> sites = new ArrayList<>();
        for(int i=0;i<NUMBER_SAT_SITES;i++) {
            SatSite aSite = new SatSite();
            aSite.setName(SAT_SITE_IDENTIFIERS[i]);
            aSite.setInterval(SAT_SITE_INTERVAL[i]);
            sites.add(aSite);
        }
        try {
            performCapture(sites, view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    @Override
    public void performCapture(List<Site> sites, boolean persist, long minDelayMS,
            long maxDelayMS, RadarView view) throws Exception {
        if(null != sites) {
            getResourceManager().captureSatelliteIndefinitely(this, sites, minDelayMS, maxDelayMS);
        }

    }

    @Override
    public String getURL(Site site, Calendar universalTime, int minuteOffset, String codedFragment) {
        universalTime.add(Calendar.MINUTE, -minuteOffset);

        //http://www.weatheroffice.gc.ca/data/satellite/goes_ecan_1070_100.jpg
       StringBuilder historicalAppend = new StringBuilder(CURRENT_URL_SAT_PREFIX);
       historicalAppend.append(site.getName());
       //historicalAppend.append("/");
       //historicalAppend.append(site.getName());
       //historicalAppend.append(site.getParameterizedUrl()); // midfix
       //historicalAppend.append(getResourceManager().getTimestampFileFormat(universalTime, "_"));
       historicalAppend.append(CURRENT_URL_SAT_POSTFIX);
       return historicalAppend.toString();
    }

    // Inner class implements Runnable instead of extending Thread directly
    class SatServiceRunnable implements Runnable {
    	protected int id;
    	
    	public SatServiceRunnable(int anId) {    		id = anId;    	}
    	
        public void run() {
        	//connect(id);
            // We loop an arbitrary number of iterations inside each thread
            //processUnitOfWork(id);
        	performCapture(null);
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        SatService aService = new SatService();
        String param1 = null;
        String param = null;
        if(null != args && args.length > 0) {
            param = args[0];
            if(null != param) {
                param1 = param;
            }
        }
        try {
            //RadarView aRadarView = new RadarView();
            //aRadarView.applicationInit();
            //aRadarView.setApplicationService(aService);
            
            aService.performCapture(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
