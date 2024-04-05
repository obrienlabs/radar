/**
 * Purpose:
 *   The following application gathers markup from an external internet source
 *   and uploads it to a device attached to this server for display.
 * 
     Proxy Access:
        To get outside the proxy - you also need to do all 3 of the following

        Set the proxy in IE to set it in windows
            "HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyServer')
            "HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyEnable')

        Set the proxy in your client
        System.getProperties().put("proxySet", defaultProperties.getProperty("proxySet","true")); 
        System.getProperties().put("proxyHost", defaultProperties.getProperty("proxyHost", "www-proxy.*.com")); 
        System.getProperties().put("proxyPort", defaultProperties.getProperty("proxyPort", "80"));

        Set the proxy for your JDK
        \jre\lib\net.properties
        http.proxyHost=www-proxy.us.*.com
        http.proxyPort=80
        http.nonProxyHosts=localhost|127.0.0.1
        
    History:
       20081014 - Modify for realtime quote from Google finance
       20081016 - Add timeout retries
       20071102 - Add hi/log capability
       20110808 - historical capture
       20110809 - JPA persistence

 */
package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.obrienscience.radar.PreProcessor;
import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.Reading;
import org.obrienscience.radar.model.SatSite;
import org.obrienscience.radar.model.Site;
import org.obrienscience.radar.model.Sweep;

public abstract class ApplicationService implements Runnable {
	private ResourceManager resourceManager;
	private BufferedImage currentImage;
	private PreProcessor preProcessor = null;//new PreProcessor();
	private EntityManagerFactory emf;
    private EntityManager entityManager;
    private boolean isPersisting = false;
    public static final boolean IS_HISTORICAL = false;
    private boolean isHistorical = false;

	public boolean isHistorical() {
		return isHistorical;
	}

	public void setHistorical(boolean isHistorical) {
		this.isHistorical = isHistorical;
	}

	public boolean isPersisting() {
		return isPersisting;
	}

	public void setPersisting(boolean isPersisting) {
		this.isPersisting = isPersisting;
		initializePersistence();
	}

	public BufferedImage getCurrentImage() {
		return currentImage;
	}

	public void setCurrentImage(String path) {
		setCurrentImage(this.loadImage(path));
	}

	public void setCurrentImage(BufferedImage currentImage) {
		this.currentImage = currentImage;
	}
	public static final long DELAY_INTERVAL_FOR_RADAR_SWEEP_WAIT = 10000;
	public static final long DELAY_INTERVAL_FOR_SAT_SWEEP_WAIT = 30000;
    public static final long DELAY_INTERVAL_FOR_LIGHTNING_SWEEP_WAIT = 10000;
	/** wait 10 min between sweeps */
	public static final int DEC_MIN_INTERVAL_ITERATIONS_FOR_RADAR = 1;
	/** wait 30 min between sat downloads */
	public static final int DEC_MIN_INTERVAL_ITERATIONS_FOR_SAT = 3;
    public static final int DEC_MIN_INTERVAL_ITERATIONS_FOR_LIGHTNING = 6;
    public static final int LIGHTNING_REFRESH_MINUTE = 31;//27;
	public static final long MIN_DELAY_BETWEEN_URL_CAPTURE_MS = 4500;//800;//9667 - 258; // 5 min / 31 sites ( 8 sec or .258 processing time)
    public static final long MAX_DELAY_BETWEEN_URL_CAPTURE_MS = 8900;//2900;
    public static final int SWEEP_INTERVAL_MIN = 10;
    public static final int NUMBER_RADAR_SITES = 32;//31;
    public static final int NUMBER_SAT_SITES = 9;
	public static final String[] RADAR_SITE_IDENTIFIERS = {
		//"WMN",
			"CASBV", //18_06
		"XLA",
		"WMB",
		"XAM",
		"WVY",
		"XNC", // fredricton
		"XGO", // halifax
		"XMB", // pei
		"XME", // w nfld
		"WTP", // e nfld
		"XNI", // superior		
		//"XTI", // timmins
			"CASRF",
		"WGJ", // sault
		"WKR",
		"WSO",
		"XDR",
		"WBI",
		"XWL",
		//"XFW", // brandon
			"CASFW",
		"XBE", // regina
		//"XRA", // saskatoon
			"CASRA",
		"XBU", // medicine hat
		"WHN", // edmonton
		"XSM", // calgary
		"WHK", // edmonton
		//"WWW", // grande prarie
			"CASSR",
		"XSS", // kelowna
		"XPG",
		"WUJ", // vancouver
		"XSI", // victoria
		"XFT",
		"CASFT"
	};
	
	//get_image.cfm?img=201109191940~NATIONAL_PRECIP_RAIN_WEATHEROFFICE_ARC~PRECIP,125,18,MPRATE:URP:NATIONAL:RADAR:GIF
	public static final String[] RADAR_SITE_FULL_NAMES = {
		"McGill",
		"Landrienne",
		"Lac Castor",
		"Val d'Irene",
		"Villeroy",
		"Chipman", // nb fredrickton
		"Halifax",
		"Marion Bridge", // pei
		"Marble Mountain", // w nfld
		"Holyrood", // e nfld
		"Superior West",
		"Northeast Ontario",
		"Montreal River",
		"King City", // TORONTO
		"Exeter",
		"Britt",
		"Dryden",
		"Woodlands", // winnipeg
		"Foxwarren", // brandon
		"Bethune", // regina
		"Radisson", // saskatoon
		"Schuler", // medicine hat
		"Jimmy Lake", // edmonton
		"Strathmore", // calgary
		"Carvel", // edmonton
		"Spirit River", // grande prarie
		"Silver Star Mountain", // kelowna
		"Prince George",
		"Aldergrove", // vancouver
		"Victoria",
		"Franktown",
		"Franktown"
		
	};

	/**http://www.weatheroffice.gc.ca/data/satellite/goes_ecan_1070_100.jpg
	    http://www.weatheroffice.gc.ca/data/satellite/goes_wcan_1070_100.jpg
	    http://www.weatheroffice.gc.ca/data/satellite/goes_nam_1070_100.jpg
	    */	
    public static final String[] SAT_SITE_IDENTIFIERS = {
        "ecan",
        "wcan",
        "nam",
        "enam",
        "eusa",       
        "sigwx",
        "wnam",
        "gedisk11",
        "gwdisk11"
    };
    
    public static final String[] SAT_SITE_FULL_NAMES = {
        "Eastern Canada",
        "Western Canada",
        "North America",
        "Eastern North America",
        "Eastern United States",
        "North Atlantic",
        "Western North America",
        "North and South America",
        "Pacific and North America"
    };

    public static final int[] SAT_SITE_INTERVAL = {
        3,
        3,
        3, // 215
        3,
        3, //245
        3, //315
        3,
        18,
        18
    };

	//public static final String CURRENT_URL_PREFIX = "http://www.weatheroffice.gc.ca/data/radar/temp_image/";
	public static final String CURRENT_URL_PREFIX = "https://weather.gc.ca/data/radar/temp_image/";
	public static final String CURRENT_RADAR_URL_POSTFIX = ".GIF";
	public static final String CURRENT_SAT_URL_POSTFIX = "_1070_100.jpg";	
    public static final String CURRENT_LIGHTNING_URL_POSTFIX = ".png";   
	//public static final String HISTORICAL_RADAR_URL_PREFIX_PRE_20110920 = "http://www.climate.weatheroffice.gc.ca/radar/get_image.cfm?img=";
    public static final String HISTORICAL_RADAR_URL_PREFIX = "http://www.climate.weatheroffice.gc.ca/radar/image.php?time=";
    public static final String HISTORICAL_RADAR_URL_SEARCH_PAGE_PREFIX = "http://climate.weather.gc.ca/radar/index_e.html?site=";//http://www.climate.weatheroffice.gc.ca/radar/index_e.html?RadarSite=";
	//public static final String HISTORICAL_RADAR_URL_POSTFIX_PRE_20110920 = ":RADAR:GIF";
	public static final String[] HISTORICAL_RADAR_URL_FRAGMENTS = {
		"_PRECIP_RAIN_WEATHEROFFICE_ARC~PRECIP,125,18,MPRATE",
		"_CAPPI_RAIN_WEATHEROFFICE_ARC~CAPPI,1.5,AGL,MPRATE"	};
	public static final int[] HISTORICAL_RADAR_URL_FRAGMENTS_INDEX = { 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };
	//public static final String[] CURRENT_RADAR_URL_MIDFIX = { "_PRECIP_RAIN_", 	"_CAPPI_RAIN_" }; 	
	public static final String[] CURRENT_RADAR_URL_MIDFIX = { "_PRECIP_RAIN_", 	"_CAPPI_RAIN_", "_PRECIP_SNOW_", "_COMP_PRECIP_RAIN", "_COMP_PRECIP_SNOW_"};
    //public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };	
    // post 201320709 XFT TO PRECIP,XSI,XBE,XTI TO CAPPI, - all down
    //public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1,0,0,1,0,0,0,0,0,0,0,0,0,1,0 };// 20130709 prior	
    // post 20120501 WMN, WBI, XFT, 
    //public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 };// 20130709 prior
    // 20140102 (wmb=cappi)
	//public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 };// 20130709 prior
	// 20140112 xss=cappi
	//public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,1 };// 20130709 prior
	// 20150131 PRECIP_SNOW
	//public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 };// 201340102 prior
	// 20190323 (wmb=cappi)
	//public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 2,2,2,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 };// 20130709 prior
	// 20190720 
	//public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 };// 20130709 prior
	// 20240404 adding casft for historical 
	public static final int[] CURRENT_RADAR_URL_MIDFIX_INDEX = { 0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1 };//

	public static final String[] SITE_GPSS = {
		"",
		""	};
	
	/**
	 * format until 20140528
	http://www.weatheroffice.gc.ca/data/lightning_images/Prairies_20110920_0000.png
		BC-AB_20110920_0000.png
		YK-NWT_20110920_0000.png
		ON-GL_20110920_0000.png
		QU-AC_20110920_0000.png
		AC_20110920_0000.png
		CLDN_20110920_000000.png
		
		format 20140528
		//ONT_2015060032330.png
		NAT
		*/
    public static final int NUMBER_LIGHTNING_SITES = 7;
	//public static final String LIGHTNING_DETECTION_URL_PREFIX = "http://www.weatheroffice.gc.ca/data/lightning_images/";
	public static final String LIGHTNING_DETECTION_URL_PREFIX = "http://weather.gc.ca/data/lightning_images/";
	public static final String LIGHTNING_DETECTION_URL_POSTFIX = ".png";
	public static final String LIGHTNING_DIR_ROOT = "lightning";
    //public static final int DEC_MIN_INTERVAL_ITERATIONS_FOR_LIGHTNING = 6;
	// to 20130505
	/*public static final String[] LIGHTNING_DETECTION_IDENTIFIERS = {
		"BC-AB",
		"YK-NWT",
		"Prairies",
		"ON-GL",
		"QU-AC",
		"AC",
		"CLDN"		
	};*/
	public static final String[] LIGHTNING_DETECTION_IDENTIFIERS = {
		"PAC",
		"ARC",
		"WRN",
		"ONT",
		"QUE",
		"ATL",
		"NAT"		
	};
	
	public static final String[] LIGHTNING_DETECTION_NAMES = {
		"BC-AB",
		"YK-NWT",
		"Prairies",
		"ON-GL",
		"QU-AC",
		"AC",
		"CLDN"		
	};

    public static final int[] LIGHTNING_SITE_INTERVAL = {
        6,
        6,
        6,
        6,
        6,
        6,
        6
    };
	
    // 20140528 code is back at EC
	public static final String[] CURRENT_LIGHTNING_URL_MIDFIX = { "_",  "_" };
    public static final String[] CURRENT_LIGHTNING_URL_PREPOSTFIX = { "",  "00" };
    public static final int[] CURRENT_LIGHTNING_URL_MIDFIX_INDEX = { 0,0,0,0,0,0,1};   
    public static final int[] CURRENT_LIGHTNING_URL_PREPOSTFIX_INDEX = { 0,0,0,0,0,0,1};   
	
    public ApplicationService() {
    	this(false);
    }
    
	public ApplicationService(boolean persist) {
		this(persist, ResourceManager.DEFAULT_RESOURCE_DRIVE, ResourceManager.DEFAULT_RESOURCE_DRIVE, 4);
	}

	public ApplicationService(boolean persist, String sourceDrive, String targetDrive, int dstOffset) {
		resourceManager = new ResourceManager();
		resourceManager.setDstOffset(dstOffset);
		isHistorical = IS_HISTORICAL;
		//resourceManager.setProxied(true);
		if(persist) {
			setPersisting(true);
			initializePersistence();			
		}
		this.getResourceManager().setSourceDrive(sourceDrive);
		this.getResourceManager().setTargetDrive(targetDrive);
	}
	
	public void finalizePersistence() {
		if(isPersisting()) {
			entityManager.close();
			emf.close();
		}
	}
	
	public void initializePersistence() {
		if(isPersisting()) {
			emf = Persistence.createEntityManagerFactory("radar");
			entityManager = emf.createEntityManager();
		}
	}
	
	public boolean isValidImage(BufferedImage image) {
		return null != image && image.getHeight() > 0 && image.getWidth() > 0;
	}
	
	public BufferedImage getFilteredImage(BufferedImage input, int layer) {
		return preProcessor.doFilter(0,input, layer);
	}

	
	public void saveImage(BufferedImage image) {
		
	}
	
	public BufferedImage loadImage(String filename) {
	    return loadImage(filename, false);
	}
	
	public BufferedImage loadImage(String filename, boolean verifyOnlyFlag) {
		// JDK 1.1
		//Image image = Toolkit.getDefaultToolkit().getImage(filename + "." + format);
		// JDK 1.4
		File file = new File(filename);
		BufferedImage image = null;
		try {
			image = ImageIO.read(file);
		} catch (Exception ioe) {
		    if(!verifyOnlyFlag) {
		        System.out.println("missing: " + filename);
		        //ioe.printStackTrace();
		    }
		}
		if(null != image) {
			//System.out.println(image.toString());
		}
		return image;
	}
	
	public abstract void performCapture(RadarView view);
	
	public void performCapture(Site site, int val) throws Exception {
		performCapture(site, true);
	}
	
	public void performCapture(List<Site> sites, RadarView view) throws Exception {
		performCapture(sites, true, view);
	}
	
	public void performCapture(Site site, boolean persist) throws Exception {
	    performCapture(site, persist, MIN_DELAY_BETWEEN_URL_CAPTURE_MS, MAX_DELAY_BETWEEN_URL_CAPTURE_MS);
	}
	
    public abstract void performCapture(Site site, boolean persist, long minDelayMS, long maxDelayMS) throws Exception;
    
    public void performCapture(List<Site> sites, boolean persist, RadarView view) throws Exception {
        performCapture(sites, persist, MIN_DELAY_BETWEEN_URL_CAPTURE_MS, MAX_DELAY_BETWEEN_URL_CAPTURE_MS, view);        
    }
    
    public void performCapture(List<Site> sites, boolean persist, long minDelayMS, long maxDelayMS, RadarView view) throws Exception {
        if(null != sites) {
            for(Site aSite : sites) {
                // call the implementing subclass
                performCapture(aSite, persist, minDelayMS, maxDelayMS);
            }
        }
    }

    // subclasses implement this
    public abstract String getURL(Site site, Calendar universalTime, int minuteOffset,String codedFragment);//,  long minDelayMS, long maxDelayMS);
    //public abstract void performCapture(List<Site> sites, boolean persist, long minDelayMS, long maxDelayMS) throws Exception;
	
    public ResourceManager getResourceManager() {        return resourceManager;    }
    public void setResourceManager(ResourceManager resourceManager) {        this.resourceManager = resourceManager;    }
    public EntityManagerFactory getEmf() {        return emf;    }
    public void setEmf(EntityManagerFactory emf) {        this.emf = emf;    }
    public EntityManager getEntityManager() {        return entityManager;    }

	public PreProcessor getPreProcessor() {		return preProcessor;	}
	public void setPreProcessor(PreProcessor preProcessor) {		this.preProcessor = preProcessor;	}
    
	public void prePopulateDatabase() {
		initializePersistence();
		List<Site> sites = new ArrayList<Site>();
		Site aSite = null;
		for(int i=0;i<NUMBER_RADAR_SITES;i++) {
			aSite = new RadarSite();
			aSite.setName(RADAR_SITE_IDENTIFIERS[i]);
			aSite.setLongName(RADAR_SITE_FULL_NAMES[i]);
			sites.add(aSite);
		}
		
		try {
			getEntityManager().getTransaction().begin();
			for(Site site : sites) {
				getEntityManager().persist(site);
			}
			getEntityManager().getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	// Runnable interface
    public void run() {
    	//connect(id);
        // We loop an arbitrary number of iterations inside each thread
        //processUnitOfWork(id);
    	performCapture(null);
    	

    }
	
} // ApplicationService
