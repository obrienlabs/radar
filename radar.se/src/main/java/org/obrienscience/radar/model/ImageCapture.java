package org.obrienscience.radar.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.imageio.ImageIO;



public class ImageCapture {

	
	public void get(String prefix, String postfix, String folder) {
		String timestamp = "202107310420";
		
		// java8 time defaults to UTC
		//Instant instant = Instant.now();
		ZoneId zone = ZoneId.of("UTC");
		ZonedDateTime zdt = ZonedDateTime.now(zone);
		
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
			URL url = new URL(prefix + timestamp + postfix);
			image = ImageIO.read(url);
			ImageIO.write(image, "png", new File("/Users/michaelobrien/_capture/" + 
			folder + "/ONT_" + timestamp + postfix));
			System.out.println("captured: " + timestamp);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("skipping: " + timestamp);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("skipping: " + timestamp);
		}
	}
	
	public static void main(String[] args) {
		ImageCapture cap = new ImageCapture();
		for(;;) {
			cap.get("https://weather.gc.ca/data/lightning_images/ONT_", ".png", "ONT");
			try {
				Thread.sleep(300000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
