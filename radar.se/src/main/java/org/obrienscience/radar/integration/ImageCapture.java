package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.imageio.ImageIO;


public class ImageCapture {
	
	private String targetDirPrefix;
	
	public static final int SLEEP_MS =300000;
	public static final String LIGHTNING_URL = "https://weather.gc.ca/data/lightning_images";
	public static final String FILE_URI = "/Users/michaelobrien/_capture/";
	
    public  void delayRandom(long minDelayMS, long maxDelayMS) {
        try {
            long randomTime = minDelayMS + Math.round((Math.random() * maxDelayMS));
            //System.out.println("wait " + randomTime );
            Thread.sleep(randomTime);
        } catch (Exception e2) {
        	e2.printStackTrace();
        }
    }
	
    private void appendZero(int index, StringBuilder buffer) {
    	if(index < 10) {
    		buffer.append("0");
    	}
    	buffer.append(Integer.toString(index));
    }
    
	public void get(String prefix, String postfix, String folder) {
		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC"));
		StringBuilder timestampB = new StringBuilder(Integer.toString(zdt.getYear()));
		appendZero(zdt.getMonth().getValue(), timestampB);
		appendZero(zdt.getDayOfMonth(), timestampB);
		appendZero(zdt.getHour(), timestampB);
		int minute = zdt.getMinute();
		//appendZero(minute, timestampB);
		String minuteString = Integer.toString(minute);
		timestampB.append(minuteString.substring(0, minuteString.length() - 1));
		timestampB.append("0");
		String timestamp = timestampB.toString();
		
		String targetFile;
		BufferedImage image = null;
		try {
			URL url = new URL(LIGHTNING_URL + "/" + folder + "_" + timestamp + ".png");
			image = ImageIO.read(url);
			targetFile = new StringBuffer(getTargetDirPrefix()).append(folder)
					.append("/")
					.append(folder)
					.append("_")
					.append(timestamp)
					.append(postfix).toString();
			ImageIO.write(image, "png", new File(targetFile));
			System.out.println("captured: " + folder + ":" + timestamp + " to: " + targetFile);
		} catch (MalformedURLException e) {
			System.out.println("skipping: " + folder + ":" + timestamp);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("skipping: " + folder + ":"  + timestamp);
		}
		delayRandom(3000,4000);
	}

	
	public String getTargetDirPrefix() {
		return targetDirPrefix;
	}

	public void setTargetDirPrefix(String targetDirPrefix) {
		this.targetDirPrefix = targetDirPrefix;
	}

	public static void main(String[] args) {
		ImageCapture cap = new ImageCapture();
		String param = null;
		cap.setTargetDirPrefix(FILE_URI);
        if(null != args && args.length > 0) {
            param = args[0];
            if(null != param) {
                cap.setTargetDirPrefix(param);
            }
        }

		for(;;) {
			cap.get("https://weather.gc.ca/data/lightning_images/ONT_", ".png", "ONT");
			cap.get("https://weather.gc.ca/data/lightning_images/ARC_", ".png", "ARC");
			cap.get("https://weather.gc.ca/data/lightning_images/ATL_", ".png", "ATL");
			cap.get("https://weather.gc.ca/data/lightning_images/NAT_", ".png", "NAT");
			cap.get("https://weather.gc.ca/data/lightning_images/PAC_", ".png", "PAC");
			cap.get("https://weather.gc.ca/data/lightning_images/QUE_", ".png", "QUE");
			cap.get("https://weather.gc.ca/data/lightning_images/WRN_", ".png", "WRN");

			try {
				System.out.println("...sleep ms " + SLEEP_MS);
				Thread.sleep(SLEEP_MS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
