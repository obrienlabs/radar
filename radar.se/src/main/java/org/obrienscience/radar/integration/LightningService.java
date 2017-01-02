package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.obrienscience.radar.PreProcessor;
import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.LightningSite;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.SatSite;
import org.obrienscience.radar.model.Site;

public class LightningService extends ApplicationService {

	public LightningService() {
		super();
	}

	
	public LightningService(boolean persist, String sourceDrive, String targetDrive, int dstOffset) {
		super(persist, sourceDrive, targetDrive, dstOffset);
	}
	
    //@Override
    public void persistImage(Site site, BufferedImage image, String name) {
    }

	@Override
    public void performCapture(Site site, boolean persist, long minDelayMS, long maxDelayMS) throws Exception {
    }

    public void performCapture(List<Site> sites, boolean persist, long minDelayMS, long maxDelayMS, RadarView view) throws Exception {
        PreProcessor preProcessor = new PreProcessor(view);
        preProcessor.setApplicationServiceOnView(this);
        //BufferedImage reducedImage = null;
        //String outputPath = null;
        Calendar universalTime = GregorianCalendar.getInstance();
        
        String imageName = null;
        String filename = null;
        String filenamePath = null;                
        BufferedImage image = null;
        boolean missedImage = true;
        for(;;) {
            universalTime = GregorianCalendar.getInstance();
            // timestamp is updated at 25 past the hour
            // need wait or check if we are right around the 25th min
            if(universalTime.get(Calendar.MINUTE) > 25) {
                //universalTime.add(Calendar.HOUR, 0);//4); // get UTC
                universalTime.add(Calendar.HOUR, getResourceManager().getDstOffset());//.0);//4); // get UTC
            } else {
                //universalTime.add(Calendar.HOUR, -2);//3); // get UTC  - 1
                universalTime.add(Calendar.HOUR, getResourceManager().getDstOffset()-0);//-2);//3); // get UTC  - 1
            }
            // remember to subtract 10 min. or sleep 10 min
            universalTime.add(Calendar.MINUTE, -SWEEP_INTERVAL_MIN);
            for(Site site : sites) {
            	// try each site across a range of times (as some lightning urls are updated late)
                // look for image on hd, get it if missing, wait random time
                //filename = getResourceManager().getTimestampFileFormat(universalTime, "", CURRENT_LIGHTNING_URL_MIDFIX[0], site.getParameterizedUrl(), true);
                filename = getResourceManager().getTimestampFileFormat(universalTime, "", "", "", true);
                filenamePath = ResourceManager.getFilename(site, filename, CURRENT_LIGHTNING_URL_POSTFIX, LIGHTNING_DIR_ROOT);                
                image = loadImage(filenamePath);
                if(null == image || (null != image && image.getHeight() < 1)) {
                    // get image from net
                    try {

                    	// TODO: REQUIRE TIMED THREAD
                        imageName = getResourceManager().captureImage(
                            this,
                            site, 
                             filename,//LIGHTNING_DIR_ROOT + "/" + filename, 
                            getURL(site, universalTime, 0, null),//codedFragment),
                            LIGHTNING_DETECTION_URL_POSTFIX,
                            LIGHTNING_DIR_ROOT);               
                    } catch (Exception e) {
                        e.printStackTrace();
                        imageName = null;
                        try {
                            Thread.sleep(ResourceManager.MIN_SLEEP_MS);
                        } catch (Exception e2) {}
                    }
                    if(null == imageName) {
                    	// remove 0-length filename
                    	//getResourceManager().deleteImage(filenamePath, true);
                    	
                    } else {
                        image = loadImage(filenamePath);
                        if(null != image) { // server 500/3xx
                        	
                           	// persist image
           		        	//String timeStamp = filename;//getTimestampFileFormat(universalTime, "_");
                   			//String filenamePath = getFilename(unprocessedImagePath, site, 
               				//	timeStamp,//service.getURL(site, startTime, 0, EMPTY_STRING), 
                  			//		ApplicationService.CURRENT_LIGHTNING_URL_POSTFIX, getResourceManager().DEFAULT_SUBDIR);
                            	
                   			File aFile = new File(filenamePath);
                   			if(null != aFile && imageName != null && aFile.exists() && aFile.length() > 0) {
                   				String s3FileName = site.getName() + "_" + imageName + ApplicationService.CURRENT_LIGHTNING_URL_POSTFIX;
                   				getResourceManager().uploadS3(filenamePath, imageName, "lightning/" + site.getName(), s3FileName);
                   			}

                        	
                            setCurrentImage(image);//reducedImage);
                            //reducedImage = preProcessor.doFilter(0, image, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);                        
                            //filenameRoot = filename.substring(0, filename.length());
                            //outputPath = PreProcessor.FILTERED_DATA_DIR + site.getName() + "_" + filenameRoot + "_f";
                            //preProcessor.writeImage(reducedImage, outputPath, "gif");
                            //view.setBufferedImage(reducedImage);
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
            // verify we got an image, try +1-1 time intervals
            
            System.out.print("Sleeping for ~60 min until next time interval: ");
            getResourceManager().delayUntilNextTimeInterval(DELAY_INTERVAL_FOR_LIGHTNING_SWEEP_WAIT, 
                    DEC_MIN_INTERVAL_ITERATIONS_FOR_LIGHTNING,
                    LIGHTNING_REFRESH_MINUTE);
            
        }
	}

    public void performCapture(RadarView view) {
        List<Site> sites = new ArrayList<Site>();
        for(int i=0;i<NUMBER_LIGHTNING_SITES;i++) {
            LightningSite aSite = new LightningSite();
            aSite.setName(LIGHTNING_DETECTION_IDENTIFIERS[i]);
            aSite.setInterval(LIGHTNING_SITE_INTERVAL[i]);
            //aSite.setParameterizedUrl(CURRENT_LIGHTNING_URL_PREPOSTFIX[CURRENT_LIGHTNING_URL_PREPOSTFIX_INDEX[i]]);
            sites.add(aSite);
        }
        try {
            performCapture(sites, view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	@Override
	public String getURL(Site site, Calendar universalTimeStart, int minuteOffset, String codedFragment) {
	    // CLDN_20110928_000000.png
        //http://www.weatheroffice.gc.ca/data/lightning_images/Prairies_20110920_0000.png
		// 20130505
		//http://weather.gc.ca/data/lightning_images/ONT.png
		// 20140528
		//ONT_201406032330.png
        StringBuilder urlAppend = new StringBuilder(LIGHTNING_DETECTION_URL_PREFIX);
        universalTimeStart.add(Calendar.MINUTE, minuteOffset);
        urlAppend.append(site.getName());
        urlAppend.append("_");        
        //urlAppend.append(getResourceManager().getTimestampFileFormat(universalTimeStart, "", CURRENT_LIGHTNING_URL_MIDFIX[0], "", true));//site.getParameterizedUrl(), true));
        urlAppend.append(getResourceManager().getTimestampFileFormat(universalTimeStart, "", "", "", true));//site.getParameterizedUrl(), true));
        urlAppend.append(LIGHTNING_DETECTION_URL_POSTFIX);
        return urlAppend.toString();
	}


    // Inner class implements Runnable instead of extending Thread directly
    class LightningServiceRunnable implements Runnable {
    	protected int id;
    	
    	public LightningServiceRunnable(int anId) {    		id = anId;    	}
    	
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
        String param = null;
        String site = "xft";
        if(null != args && args.length > 0) {
            param = args[0];
            if(null != param) {
                //inputDrive = param;
                site = param;
            }
        }
        
        boolean view = true;
        LightningService aService = new LightningService();
        RadarView aRadarView = new RadarView();
        aRadarView.applicationInit();
        aRadarView.setApplicationService(aService);
        aService.performCapture(aRadarView);
	}
}
