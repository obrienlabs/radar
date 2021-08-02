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
		String timestamp = "202107310420";

		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC"));
		
		timestamp = new String(
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
		
		
		BufferedImage image = null;
		try {
			URL url = new URL(LIGHTNING_URL + "/" + folder + "_" + timestamp + ".png");
			image = ImageIO.read(url);
			ImageIO.write(image, "png", new File(FILE_URI + 
			folder + "/" + folder +"_" + timestamp + postfix));
			System.out.println("captured: " + folder + ":" + timestamp);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("skipping: " + folder + ":" + timestamp);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("skipping: " + folder + ":"  + timestamp);
		}
		delayRandom(3000,4000);
	}
	
	public static void main(String[] args) {
		ImageCapture cap = new ImageCapture();
		for(;;) {
			cap.get("https://weather.gc.ca/data/lightning_images/ONT_", ".png", "ONT");
			cap.get("https://weather.gc.ca/data/lightning_images/ARC_", ".png", "ARC");
			cap.get("https://weather.gc.ca/data/lightning_images/ATL_", ".png", "ATL");
			cap.get("https://weather.gc.ca/data/lightning_images/NAT_", ".png", "NAT");
			cap.get("https://weather.gc.ca/data/lightning_images/PAC_", ".png", "PAC");
			cap.get("https://weather.gc.ca/data/lightning_images/QUE_", ".png", "QUE");
			cap.get("https://weather.gc.ca/data/lightning_images/WRN_", ".png", "WRN");

			try {
				Thread.sleep(300000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
