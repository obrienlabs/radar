package org.obrienscience.radar.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

public class ImageCapture {

	
	public void get(String prefix, String postfix) {
		String timestamp = "202107310420";
		BufferedImage image = null;
		try {
			URL url = new URL(prefix + timestamp + postfix);
			image = ImageIO.read(url);
			ImageIO.write(image, "png", new File("/Users/michaelobrien/_capture/ONT_" + timestamp + postfix));
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		ImageCapture cap = new ImageCapture();
		cap.get("https://weather.gc.ca/data/lightning_images/ONT_", ".png");

	}

}
