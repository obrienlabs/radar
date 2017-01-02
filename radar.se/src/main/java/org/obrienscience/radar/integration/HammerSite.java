package org.obrienscience.radar.integration;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;

import org.obrienscience.radar.PreProcessor;
import org.obrienscience.radar.RadarView;

public class HammerSite {
	private boolean isProxied = false;
	private String site;
	private long duration;
	private long iterations;

	
	public void captureImage(String fullURL) {
		InputStream abstractInputStream = null;
		BufferedInputStream aBufferedInputStream = null;
		HttpURLConnection aURLConnection = null;
		URL 	aURL = null;
		long byteCount;			
		int bytesRead;
		try {
			aURL  = new URL(fullURL);
			aURLConnection = (HttpURLConnection)aURL.openConnection();
			HttpURLConnection.setDefaultAllowUserInteraction(true);
			aURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
			abstractInputStream = aURLConnection.getInputStream();
			aBufferedInputStream = new BufferedInputStream(abstractInputStream);
			byteCount = 0;
				
			//System.out.println("Downloading from: " + fullURL);
			byte b[] = new byte[1024];
			while ((bytesRead = aBufferedInputStream.read(
			        b, 				// name of buffer
			        0, 				// start of buffer to start reading into
			        b.length		// save actual bytes read, not default max buffer size
			    )) >= 0) {
				byteCount += bytesRead;
			}
			//System.out.println("HTML capture/processing complete: bytes: " + byteCount);
			System.out.print(".");
			// we successfully streamed the file					
			aBufferedInputStream.close();
		} catch (IllegalStateException e) {
			System.out.println(e.getMessage());
			throw e;							
		} catch (UnknownServiceException e) {
			System.out.println(e.getMessage());
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if(aBufferedInputStream != null) {
				try {
					aBufferedInputStream.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} // if
		} // finally		
	}
	
	public void hammer() {
		long starttime = System.nanoTime();
		for(long i=0; i<iterations;i++) {
			starttime = System.nanoTime();
			captureImage(this.getSite());
			//System.out.println("Time (ns): " + ((System.nanoTime() - starttime))/1000000);
			try {
				Thread.sleep(this.duration);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}
	
	public boolean isProxied() {
		return isProxied;
	}

	public void setProxied(boolean isProxied) {
		this.isProxied = isProxied;
		if(isProxied) {
			// inside a firewall only
			System.getProperties().put("proxySet","true"); 
			System.getProperties().put("proxyHost", "webproxystatic-on.tsl.telus.com");//"www-proxy.us.oracle.com"); 
			System.getProperties().put("proxyPort",  "8080");//"80");
		}
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getIterations() {
		return iterations;
	}

	public void setIterations(long iterations) {
		this.iterations = iterations;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HammerSite hammer = new HammerSite();
		String param = null;
        if(null != args && args.length > 3) {
        	hammer.setIterations(Long.parseLong(args[0]));        	
        	hammer.setDuration(Long.parseLong(args[1]));        	
        	hammer.setSite(args[2]);
        	param = args[3];
        	if(null != param && param.equalsIgnoreCase("proxy")) {
        			hammer.setProxied(true);
        	}
            hammer.hammer();
        } else {
        	System.out.println("Usage HammerSite <iterations> <spacing (ms)> <http://site <proxy_boolean>");
        }
	}

}
