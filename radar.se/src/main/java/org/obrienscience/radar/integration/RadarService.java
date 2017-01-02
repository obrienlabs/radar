package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.Site;
import org.obrienscience.radar.model.Sweep;

public abstract class RadarService extends ApplicationService {

	public RadarService() {
		super();
	}

	
	public RadarService(boolean persist, String sourceDrive, String targetDrive, int dstOffset) {
		super(persist, sourceDrive, targetDrive, dstOffset);
	}

	//public abstract void persistImage(RadarSite site, BufferedImage image, String name);
    //@Override
    public void persistImage(RadarSite site, BufferedImage image, String name) {
    	//this.getResourceManager().pushS3(filename)
    	if(!isPersisting()) return;
        //Sweep sweep = new Sweep();
        //sweep.setTimestamp(name);
        //sweep.setSite((RadarSite)site);
       //image.getRaster().getDataBuffer().
        //List<Reading> readings = new ArrayList<Reading>();
        
        //sweep.setReadings(readings);
      try {
            getEntityManager().getTransaction().begin();
            for(Sweep sweep : site.getSweeps())
            getEntityManager().persist(sweep);
            getEntityManager().persist(site);
            getEntityManager().getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            //getEntityManager().getTransaction().rollback();
        }
    }
	
	
	private List<Site> getSites(boolean isORM) {
        List<Site> sites = new ArrayList<Site>();
        if(isORM) {
    		initializePersistence();
    		//List<Site> sites = new ArrayList<Site>();
    		Site aSite = null;
    		for(int i=0;i<NUMBER_RADAR_SITES;i++) {
    			aSite = new RadarSite();
    			aSite.setName(RADAR_SITE_IDENTIFIERS[i]);
    			sites.add(aSite);
    		}
    		
    		// persist only if the database is up
    		if(null != getEntityManager()) {
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
    		
 	
        } else {
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
        }
       return sites;
	}
	
    @Override
    public void performCapture(RadarView view) {
        try {
            performCapture(getSites(true), view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
