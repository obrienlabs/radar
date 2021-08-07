package org.obrienscience.radar.model;

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
	public static final String LIGHTNING_URL = "https://weather.gc.ca/data/lightning_images/";
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
	
	public void get(String prefix, String postfix, String folder) {
		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC"));
		String timestamp = new String(
				Integer.toString(zdt.getYear()));
		int month =  zdt.getMonth().getValue();
		if(month < 10) {
			timestamp += "0";
		}
		timestamp += Integer.toString(month);
		int day =  zdt.getDayOfMonth();
		if(day < 10) {
			timestamp += "0";
		}
		timestamp += Integer.toString(day);
		int hour =  zdt.getHour();
		if(hour < 10) {
			timestamp += "0";
		}
		timestamp += Integer.toString(hour);
		int minute = zdt.getMinute();
		String minuteString = Integer.toString(minute);
		if(minute < 10) {
			timestamp += "0";
		}
		timestamp += minuteString.substring(0, minuteString.length()-1);
		timestamp += "0";
		
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
        if(null != args && args.length > 0) {
            param = args[0];
            if(null != param) {
                cap.setTargetDirPrefix(param);
            } else {
            	cap.setTargetDirPrefix(FILE_URI);
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
