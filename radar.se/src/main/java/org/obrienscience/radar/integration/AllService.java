package org.obrienscience.radar.integration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.obrienscience.radar.PreProcessor;
import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.Site;

public class AllService extends ApplicationService {

	@Override
	public void performCapture(RadarView view) {
		// Spawn 4 threads for each Service
		startThreads(4);	

	}
	
	@Override
	public void performCapture(Site site, boolean persist, long minDelayMS,
			long maxDelayMS) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String getURL(Site site, Calendar universalTime, int minuteOffset,
			String codedFragment) {
		// TODO Auto-generated method stub
		return null;
	}
	
    // Use the fork-join or Executable framework
    protected void startThreads(int numberOfThreads) {
        threadSafetyPrivate(numberOfThreads);
    }
    
    protected void threadSafetyPrivate(int numberOfThreads) {
        List<Thread> threadList = new ArrayList<Thread>();
        ApplicationService aService = null;
        aService = new LiveRadarService(this.isPersisting(), this.getResourceManager().getSourceDrive(), this.getResourceManager().getTargetDrive(), getResourceManager().getDstOffset());
        aService.setPreProcessor(new PreProcessor(false));
        threadList.add(new Thread(aService));
        aService = new LightningService(this.isPersisting(), this.getResourceManager().getSourceDrive(), this.getResourceManager().getTargetDrive(), getResourceManager().getDstOffset());
        aService.setPreProcessor(new PreProcessor(false));
        threadList.add(new Thread(aService));
        aService = new SatService(this.isPersisting(), this.getResourceManager().getSourceDrive(), this.getResourceManager().getTargetDrive(), getResourceManager().getDstOffset());
        aService.setPreProcessor(new PreProcessor(false));
        threadList.add(new Thread(aService));
        
        
            // stagger the threads so they are not in lockstep
        for(Thread aThread : threadList) {
            try {
            	Thread.sleep(3000);
            } catch (Exception e) {           }
            aThread.start();
        }

        // Wait for [threadNumber] threads to complete before ending 
        for(Thread aThread : threadList) {
            try {
                synchronized (aThread) {
                	aThread.join();
                }
            } catch (InterruptedException ie_Ignored) {
            	ie_Ignored.printStackTrace();
            } // The InterruptedException can be ignored 
        }
    }
    
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AllService aService = new AllService();
        //String param1 = null;
        String param = null;
        Boolean swingDisplay = false;
        System.out.println("org.obrienscience.radar.integration.AllService dstOffset -disp -proxy -persist /Volumes/ppro1/ /Volumes/ppro2/  /Volumes");
        RadarView aRadarView = null;
        int dstOffset = 4;
        if(null != args && args.length > 0) {
        	param = args[0];
        	if(null != param && param.length() > 0) {
        			dstOffset = Integer.parseInt(param);
        			aService.getResourceManager().setDstOffset(dstOffset);
        			System.out.println("Setting dstOffset: " + aService.getResourceManager().getDstOffset());
        	}
        }
        if(null != args && args.length > 1) {
        	param = args[1];
        	if(null != param && param.equalsIgnoreCase("-disp")) {
        			swingDisplay = true;
        	}
        }

        if(null != args && args.length > 2) {
        	param = args[2];
        	if(null != param && param.equalsIgnoreCase("-proxy")) {
        			aService.getResourceManager().setProxied(true);
        	}
        }

        if(null != args && args.length > 3) {
        	param = args[3];
        	if(null != param && param.equalsIgnoreCase("-persist")) {
        			aService.setPersisting(true);
        	}
        }

        // get drives
        if(null != args && args.length > 4) {
            param = args[4];
            if(null != param && param.length() > 0) {
                aService.getResourceManager().setSourceDrive(param);
            }
        }

        if(null != args && args.length > 5) {
            param = args[5];
            if(null != param && param.length() > 0) {
                aService.getResourceManager().setTargetDrive(param);
            }
        }
        
        // get path on hd
        if(null != args && args.length > 6) {
            param = args[6];
            if(null != param && param.length() > 0) {
                aService.getResourceManager().setOverrideImagePath(param);
            }
        }
        
        if(swingDisplay.booleanValue()) { 
        	aRadarView = new RadarView();        
        	aRadarView.applicationInit();
        	aRadarView.setApplicationService(aService);
        }
	    aService.setPreProcessor(new PreProcessor(false));
	    aService.performCapture(null);
	}
}
