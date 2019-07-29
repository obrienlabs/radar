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

 */
package org.obrienscience.radar.integration;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.obrienscience.radar.RadarView;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.Site;
import org.obrienscience.radar.model.Sweep;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

import javax.imageio.*;
import java.nio.channels.*;

/**
 * Images are stored in the file system and referenced from the database
 * 
 * 20140306: Implement thread pool of timed Callables to handle stuck sockets to EC satellite servers
 * 
 * @author F. Michael O'Brien
 *
 */
public class ResourceManager {

	public static final int INPUT_BUFFER_SIZE = 256_161; // check indexOutOfBoundsException
	public static final int MIN_SLEEP_MS = 1_000 * 10;
	private static final String DEFAULT_UNPROCESSED_IMAGE_DIR = "_radar_unprocessed_image_to_persist";
	//private static final String DEFAULT_UNPROCESSED_IMAGE_DIR = "_radar_unprocessed_image_dev";
	private static final String DEFAULT_PROCESSED_IMAGE_DIR = "_radar_processed_image_to_persist";
	//public static final String DEFAULT_RESOURCE_DRIVE = "/Volumes/gdrive1";//"f";
	public static final String DEFAULT_RESOURCE_DRIVE = "/Users/michaelobrien/";//"f";
	public static final int MAX_DOWNLOAD_RETRIES = 3;
	public static final String EMPTY_STRING = "";
	public static final String DIR_DELIMITER = "/";
	public static final String DEFAULT_SUBDIR = EMPTY_STRING;

	/** Compute the daylight savings time offset. 
	 * For post 2007 it is 4 hours from Mar xx to first sunday in Nov (20111106 at 3am)
	 * Outside of DST it is 5 hours
	 */
	
	private static int UTCOffset;
	//public static final int DAYLIGHT_SAVINGS_TIME_OFFSET = 0; // amazon virginia
	//public static final int DAYLIGHT_SAVINGS_TIME_OFFSET = 4;
	//private static final int NON_DAYLIGHT_SAVINGS_TIME_OFFSET = 5;
	
	private static String unprocessedImagePath;
	private String processedImagePath;
	private String sourceDrive;
    private String targetDrive;
    private String overrideImagePath;
    private int dstOffset;

	private boolean isProxied = false;
	private AmazonS3Client s3Client;

	private static final ExecutorService executorService = Executors.newCachedThreadPool();

	public ResourceManager() {
		// initialize logging
		if(isProxied) { setProxied(true); }
	        // initialize variables from defaults or properties file
		initialize();
	}

	public boolean uploadS3(String localDir, String s3Key, String s3Bucket, String s3FileName) {
        //AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
		boolean success = false;
        try {
        	File file = new File(localDir);// + "/" + key);
        	s3Client.putObject(new PutObjectRequest("os-radar/" + s3Bucket, s3FileName, file));
        	success = true;
        } catch (AmazonServiceException ase) {
        	System.out.println("Error Message:    " + ase.getMessage());
        	System.out.println("HTTP Status Code: " + ase.getStatusCode());
        	System.out.println("AWS Error Code:   " + ase.getErrorCode());
        	System.out.println("Error Type:       " + ase.getErrorType());
        	System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
        	System.out.println("Error Message: " + ace.getMessage());
        } catch (IllegalArgumentException iae) {
        	iae.printStackTrace();
        }
        return success;
	}

	public void deleteImage(String filename, boolean verifyIsZeroLength) {
		Path path = null;//FileSystems.getDefault().get, Path("", filename);
		try {
			// verify that the file is actually 0-length
		    Files.delete(path);
		} catch (NoSuchFileException x) {
		    System.err.format("%s: no such" + " file or directory%n", filename);
		} catch (DirectoryNotEmptyException x) {
		    System.err.format("%s not empty%n", filename);
		} catch (IOException x) {
		    // File permission problems are caught here.
		    System.err.println(x);
		}
	}
	
	// for Historical, Lightning services
	public String captureImage(ApplicationService service, Site site, 
	    		String urlAppend, String fullURL, String postfix) throws Exception {
		return captureImage(unprocessedImagePath, service, site, urlAppend, fullURL, postfix);
	}
	public String captureImage(ApplicationService service, Site site, 
    		String urlAppend, String fullURL, String postfix, String subdir) throws Exception {
		return captureImage(unprocessedImagePath, service, site, urlAppend, fullURL, postfix, subdir);
	}

    public String captureImage(String unprocessedImagePath, ApplicationService service, Site site, 
    		String urlAppend, String fullURL, String postfix) throws Exception {
    	return captureImage(unprocessedImagePath, service, site, urlAppend, fullURL, postfix, DEFAULT_SUBDIR);
    }
    public String captureImage(String unprocessedImagePath, ApplicationService service, Site site, 
    		String urlAppend, String fullURL, String postfix, String subdir) throws Exception {
        // use a timed callable
    	//return blockingCallable(unprocessedImagePath, service, site, urlAppend, fullURL, postfix, subdir);
	    // 20190324
	    return blockingCallable0(unprocessedImagePath, service, site, urlAppend, fullURL, postfix, subdir);
    }
	
    
    private String blockingCallable(final String unprocessedImagePath, final ApplicationService service, final Site site, 
    		final String urlAppend, final String fullURL, final String postfix, final String subdir) {
    	boolean _found = false;
    	String fileName = null;
    	//BlockingCallable aBlockingCallable;
    	FutureTask<String> aFutureTask = null;
    	
    
        //try {
    	try {
        	//Future<String> aFutureTask = executorService.submit(new BlockingCallable<String>(
        	//		service, site, urlAppend, fullURL, postfix, subdir, unprocessedImagePath));
        	aFutureTask = new FutureTask<String>(new BlockingCallable<String>(
        			service, site, urlAppend, fullURL, postfix, subdir, unprocessedImagePath));
    		/*aFutureTask = new FutureTask<String>(new Callable<String>() {        	
        		public String call() throws Exception {
        		String imageName = null;
        		InputStream abstractInputStream = null;
        		BufferedInputStream aBufferedInputStream = null;
        		String filenamePath = ResourceManager.getFilename(unprocessedImagePath, site, urlAppend, postfix, subdir);
        		FileOutputStream aFileWriter = new FileOutputStream(filenamePath);
        		System.out.println("_Writing to: " + filenamePath);
        		HttpURLConnection aURLConnection = null;
        		URL 	aURL = null;
        		long byteCount;			
        		int bytesRead;
        		long tsstart = System.currentTimeMillis();
        		try {
        			aURL  = new URL(fullURL);
        			aURLConnection = (HttpURLConnection)aURL.openConnection();
        			// fake the agent
        			HttpURLConnection.setDefaultAllowUserInteraction(true);
        			aURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
        			abstractInputStream = aURLConnection.getInputStream();
        			aBufferedInputStream = new BufferedInputStream(abstractInputStream);
        			byteCount = 0;
        			System.out.println("Downloading from: " + fullURL);
        			byte b[] = new byte[ResourceManager.INPUT_BUFFER_SIZE];
        			while ((bytesRead = aBufferedInputStream.read(
        			        b, 				// name of buffer
        			        0, 				// start of buffer to start reading into
        			        b.length		// save actual bytes read, not default max buffer size
        			    )) >= 0 && ((System.currentTimeMillis() - tsstart) < 30000)) {
        				byteCount += bytesRead;
        				aFileWriter.write(b, 0, bytesRead);//b.length);
        				Thread.sleep(10);
        				if(Thread.currentThread().isInterrupted() || Thread.currentThread().isDaemon()) {//aFutureTask.isCancelled()) {
        					throw new InterruptedException();
        				}
        			} // while
        			System.out.println((System.currentTimeMillis() - tsstart) + "ms:HTML capture/processing complete: bytes: " + byteCount);
        			aBufferedInputStream.close();
        			if(byteCount > 6144) {
        				imageName = urlAppend;
        				service.setCurrentImage(filenamePath);
        			} else {
        				System.out.println("Truncated download of " + byteCount + " bytes: " + urlAppend);
        			}
        			if(null != aFileWriter) {
        			    aFileWriter.flush();
        			    aFileWriter.close();
        			}
        		} catch (InterruptedException ie) {
        			System.out.println("_Interrupted: " + fullURL);
        		} catch (IllegalStateException e) {
        			System.out.println(e.getMessage());
        		} catch (UnknownServiceException e) {
        			System.out.println(e.getMessage());
        		} catch (MalformedURLException e) {
        			System.out.println(e.getMessage());
        		} catch (IOException e) {
        			e.printStackTrace();
        			System.out.println(e.getMessage());
        		} catch (Exception e) {
        			System.out.println(e.getMessage());
        		} finally {
        			if(aBufferedInputStream != null) {
        				aBufferedInputStream.close();
        			} // if
        			if(aFileWriter != null) {
        				//aFileWriter.flush(); 
        				aFileWriter.close();
        			}
        		} // finally
        		
        		// only return a non-null image name if the image is invalid
        		return imageName;
        		}});*/
        	executorService.submit(aFutureTask);
        	fileName = aFutureTask.get(120, TimeUnit.SECONDS); // returns an immediate result or throws TimeoutException
        		_found = true;
        	} catch (TimeoutException te) {
        		System.out.println("Timeout on: " + fullURL);
        		te.printStackTrace();
        		_found = false;
        		aFutureTask.cancel(true); 
        	} catch (InterruptedException ie) {
        		ie.printStackTrace();
        		_found = false;
        	} catch (ExecutionException ee) {
        		ee.printStackTrace();
        		_found = false;
        	} finally {

        	}
        	// verify that we returned normally or had a TimeoutException
        	if(_found) {
        	}
        //} catch ( Exception e) {
        //    e.printStackTrace();
        //} finally {
        //}
        return fileName;
    }
	/*private String blockingCallable1(final String unprocessedImagePath, final ApplicationService service, final Site site,
	                                 final String urlAppend, final String fullURL, final String postfix, final String subdir) {
		boolean _found = false;
		String fileName = null;
		//Image image = null;
		String filenamePath = ResourceManager.getFilename(unprocessedImagePath, site, urlAppend, postfix, subdir);
		System.out.println("_Writing to: " + filenamePath);


		//image = ImageIO.read(url);
		ReadableByteChannel readableByteChannel = Channels.newChannel(fullURL.openStream());
		URL aURL = null;
		try {
			aURL = new URL(fullURL);
			FileOutputStream fileOutputStream = new FileOutputStream(FILE_NAME);
			FileChannel fileChannel = fileOutputStream.getChannel();
			fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		} catch () {

		}

		return fileName;
	}*/



    
    private String blockingCallable0(final String unprocessedImagePath, final ApplicationService service, final Site site, 
    		final String urlAppend, final String fullURL, final String postfix, final String subdir) {
    	boolean _found = false;
    	String fileName = null;
    	FutureTask<String> aFutureTask = null;
    	
    	try {
    		aFutureTask = new FutureTask<String>(new Callable<String>() {        	
        		public String call() throws Exception {
        		   	FileOutputStream fileOutputStream = null;
        	    	FileChannel fileChannel = null;

        		String imageName = null;
        		
        		/** this stream is used to get the BufferedInputStream below */
 //       		InputStream abstractInputStream = null;
 //       		/** stream to read from the FTP server */
 //       		BufferedInputStream aBufferedInputStream = null;
        		/** stream to file system */
        		String filenamePath = ResourceManager.getFilename(unprocessedImagePath, site, urlAppend, postfix, subdir);
//        		FileOutputStream aFileWriter = new FileOutputStream(filenamePath);
        		System.out.println("_Writing to: " + filenamePath);
//        		HttpURLConnection aURLConnection = null;
        		URL 	aURL = null;
        		long byteCount = 0;			
//        		int bytesRead;
        		long tsstart = System.currentTimeMillis();
        		try {
        			aURL  = new URL(fullURL);
        	    	ReadableByteChannel readableByteChannel = Channels.newChannel(aURL.openStream());
        	    	fileOutputStream = new FileOutputStream(filenamePath);
        	    	fileChannel = fileOutputStream.getChannel();
        	    	fileOutputStream.getChannel()
        	    	  .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        	    	
//        			aURLConnection = (HttpURLConnection)aURL.openConnection();
        			// fake the agent
//        			HttpURLConnection.setDefaultAllowUserInteraction(true);
//        			aURLConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
//        			aURLConnection.setRequestProperty("User-Agent", //"Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
//        					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
        			//aURLConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
//        			abstractInputStream = aURLConnection.getInputStream();
//        			aBufferedInputStream = new BufferedInputStream(abstractInputStream);
//        			byteCount = 0;
        			System.out.println("Downloading from: " + fullURL);
//        			byte b[] = new byte[ResourceManager.INPUT_BUFFER_SIZE];
//        			while ((bytesRead = aBufferedInputStream.read(
//        			        b, 				// name of buffer
//        			        0, 				// start of buffer to start reading into
//        			        b.length		// save actual bytes read, not default max buffer size
//        			    )) >= 0 && ((System.currentTimeMillis() - tsstart) < 30000)) {
        				byteCount = fileChannel.size();//+= bytesRead;
//        				aFileWriter.write(b, 0, b.length);
				        //aFileWriter.write(b, 0, bytesRead);//b.length);

        				Thread.sleep(1);
        				if(Thread.currentThread().isInterrupted() || Thread.currentThread().isDaemon()) {//aFutureTask.isCancelled()) {
        					throw new InterruptedException();
        				}
//        			} // while
        			System.out.println((System.currentTimeMillis() - tsstart) + "ms:HTML capture/processing complete: bytes: " + byteCount);
//        			aBufferedInputStream.close();
//        			if(byteCount > 6144) {
        				imageName = urlAppend;
//        				service.setCurrentImage(filenamePath);
//        			} else {
//        				System.out.println("Truncated download of " + byteCount + " bytes: " + urlAppend);
//        			}
//        			if(null != aFileWriter) {
//        			    aFileWriter.flush();
//        			    aFileWriter.close();
//        			}
        		} catch (InterruptedException ie) {
        			System.out.println("_Interrupted: " + fullURL);
        		} catch (IllegalStateException e) {
        			System.out.println(e.getMessage());
        		} catch (UnknownServiceException e) {
        			System.out.println(e.getMessage());
        		} catch (MalformedURLException e) {
        			System.out.println(e.getMessage());
        		} catch (IOException e) {
        			e.printStackTrace();
        			System.out.println(e.getMessage());
        		} catch (Exception e) {
        			System.out.println(e.getMessage());
        		} finally {
        	  		try {
        	  			if(fileOutputStream != null) {
        	  				fileOutputStream.close();
        	  			}
        	    		} catch (IOException fe) {
        	    			fe.printStackTrace();
        	    		}
        	  		
/*
        			if(aBufferedInputStream != null) {
        				aBufferedInputStream.close();
        			} // if
        			if(aFileWriter != null) {
        				//aFileWriter.flush(); 
        				aFileWriter.close();
        			}*/
        		} // finally
        		
        		// only return a non-null image name if the image is invalid
        		return imageName;
        		}});
        	executorService.submit(aFutureTask);
        	fileName = aFutureTask.get(120, TimeUnit.SECONDS); // returns an immediate result or throws TimeoutException
        		_found = true;
        	} catch (TimeoutException te) {
        		System.out.println("Timeout on: " + fullURL);
        		te.printStackTrace();
        		_found = false;
        		aFutureTask.cancel(true); 
        	} catch (InterruptedException ie) {
        		ie.printStackTrace();
        		_found = false;
        	} catch (ExecutionException ee) {
        		ee.printStackTrace();
        		_found = false;
        	} finally {

        	}
        	// verify that we returned normally or had a TimeoutException
        	if(_found) {
        	}
        //} catch ( Exception e) {
        //    e.printStackTrace();
        //} finally {
        //}
        return fileName;
    }

    /* data corruption saving under http pre 20190720 */
    private String blockingCallable0old(final String unprocessedImagePath, final ApplicationService service, final Site site, 
    		final String urlAppend, final String fullURL, final String postfix, final String subdir) {
    	boolean _found = false;
    	String fileName = null;
    	//BlockingCallable aBlockingCallable;
    	FutureTask<String> aFutureTask = null;
    	
    
        //try {
    	try {
        	//Future<String> aFutureTask = executorService.submit(new BlockingCallable<String>(
        	//		service, site, urlAppend, fullURL, postfix, subdir, unprocessedImagePath));
        	//aFutureTask = new FutureTask<String>(new BlockingCallable<String>(
        	//		aFutureTask, service, site, urlAppend, fullURL, postfix, subdir, unprocessedImagePath));
    		aFutureTask = new FutureTask<String>(new Callable<String>() {        	
        		public String call() throws Exception {
        		String imageName = null;
        		
        		/** 
        		 * Local Variables
        		 * Note: Since there are separate calls to this function for each
        		 * thread, these are thread-safe, but don't put generic properties here
        		 */			
        		/** this stream is used to get the BufferedInputStream below */
        		InputStream abstractInputStream = null;
        		/** stream to read from the FTP server */
        		BufferedInputStream aBufferedInputStream = null;
        		/** stream to file system */
        		String filenamePath = ResourceManager.getFilename(unprocessedImagePath, site, urlAppend, postfix, subdir);
        		FileOutputStream aFileWriter = new FileOutputStream(filenamePath);
        		System.out.println("_Writing to: " + filenamePath);
        		HttpURLConnection aURLConnection = null;
        		URL 	aURL = null;
        		long byteCount;			
        		int bytesRead;
        		long tsstart = System.currentTimeMillis();
        		try {
        			aURL  = new URL(fullURL);
        			aURLConnection = (HttpURLConnection)aURL.openConnection();
        			// fake the agent
        			HttpURLConnection.setDefaultAllowUserInteraction(true);
        			aURLConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        			aURLConnection.setRequestProperty("User-Agent", //"Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
        					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
        			//aURLConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        			abstractInputStream = aURLConnection.getInputStream();
        			aBufferedInputStream = new BufferedInputStream(abstractInputStream);
        			byteCount = 0;
        			System.out.println("Downloading from: " + fullURL);
        			byte b[] = new byte[ResourceManager.INPUT_BUFFER_SIZE];
        			while ((bytesRead = aBufferedInputStream.read(
        			        b, 				// name of buffer
        			        0, 				// start of buffer to start reading into
        			        b.length		// save actual bytes read, not default max buffer size
        			    )) >= 0 && ((System.currentTimeMillis() - tsstart) < 30000)) {
        				byteCount += bytesRead;
        				aFileWriter.write(b, 0, b.length);
				        //aFileWriter.write(b, 0, bytesRead);//b.length);

        				Thread.sleep(1);
        				if(Thread.currentThread().isInterrupted() || Thread.currentThread().isDaemon()) {//aFutureTask.isCancelled()) {
        					throw new InterruptedException();
        				}
        			} // while
        			System.out.println((System.currentTimeMillis() - tsstart) + "ms:HTML capture/processing complete: bytes: " + byteCount);
        			aBufferedInputStream.close();
        			if(byteCount > 6144) {
        				imageName = urlAppend;
        				service.setCurrentImage(filenamePath);
        			} else {
        				System.out.println("Truncated download of " + byteCount + " bytes: " + urlAppend);
        			}
        			if(null != aFileWriter) {
        			    aFileWriter.flush();
        			    aFileWriter.close();
        			}
        		} catch (InterruptedException ie) {
        			System.out.println("_Interrupted: " + fullURL);
        		} catch (IllegalStateException e) {
        			System.out.println(e.getMessage());
        		} catch (UnknownServiceException e) {
        			System.out.println(e.getMessage());
        		} catch (MalformedURLException e) {
        			System.out.println(e.getMessage());
        		} catch (IOException e) {
        			e.printStackTrace();
        			System.out.println(e.getMessage());
        		} catch (Exception e) {
        			System.out.println(e.getMessage());
        		} finally {
        			if(aBufferedInputStream != null) {
        				aBufferedInputStream.close();
        			} // if
        			if(aFileWriter != null) {
        				//aFileWriter.flush(); 
        				aFileWriter.close();
        			}
        		} // finally
        		
        		// only return a non-null image name if the image is invalid
        		return imageName;
        		}});
        	executorService.submit(aFutureTask);
        	fileName = aFutureTask.get(120, TimeUnit.SECONDS); // returns an immediate result or throws TimeoutException
        		_found = true;
        	} catch (TimeoutException te) {
        		System.out.println("Timeout on: " + fullURL);
        		te.printStackTrace();
        		_found = false;
        		aFutureTask.cancel(true); 
        	} catch (InterruptedException ie) {
        		ie.printStackTrace();
        		_found = false;
        	} catch (ExecutionException ee) {
        		ee.printStackTrace();
        		_found = false;
        	} finally {

        	}
        	// verify that we returned normally or had a TimeoutException
        	if(_found) {
        	}
        //} catch ( Exception e) {
        //    e.printStackTrace();
        //} finally {
        //}
        return fileName;
    }

    public static String getFilename(Site site, String timestamp, String postfix) {
        return getFilename(unprocessedImagePath, site, timestamp, postfix, EMPTY_STRING);
    }

    public static String getFilename(Site site, String timestamp, String postfix, String subdir) {
        return getFilename(unprocessedImagePath, site, timestamp, postfix, subdir);
    }

    public static String getFilename(String unprocessedImagePath, Site site, String timestamp, String postfix) {
        return getFilename(unprocessedImagePath, site, timestamp, postfix, EMPTY_STRING);
    }
    
	public static String getFilename(String unprocessedImagePath, Site site, String timestamp, String postfix, String subdir) {
		StringBuffer buffer = new StringBuffer(unprocessedImagePath);
		if(null != subdir && !EMPTY_STRING.equals(subdir)) {
		    buffer.append(DIR_DELIMITER);
		    buffer.append(subdir);
		}
		buffer.append(DIR_DELIMITER);
		
		buffer.append(site.getName().toLowerCase());
		buffer.append(DIR_DELIMITER);
		buffer.append(site.getName());
		buffer.append("_");
		buffer.append(timestamp);
		buffer.append(postfix);
		return buffer.toString();
	}

    public String captureURL(ApplicationService service, Site site, String fullURL) throws Exception {
        //String imageName = null;
        StringBuffer buffer = new StringBuffer();
        
        /** 
         * Local Variables
         * Note: Since there are separate calls to this function for each
         * thread, these are thread-safe, but don't put generic properties here
         */         
        /** this stream is used to get the BufferedInputStream below */
        InputStream abstractInputStream = null;
        /** stream to read from the FTP server */
        BufferedInputStream aBufferedInputStream = null;
        /** stream to file system */
        //String filenamePath = getFilename(site, urlAppend, postfix);
        //FileOutputStream aFileWriter = new FileOutputStream(filenamePath);
        //System.out.println("_Writing to: " + filenamePath);
        //FileWriter aFileWriter = new FileWriter(PROD_DIR_PATH + "image.gif");
        /** connection based on the aURL */
        HttpURLConnection aURLConnection = null;
        /** URL object that we can pass to the URLConnection abstract factory */
        URL     aURL = null;
        /** create a date formatter for time tracking - not thread-safe */
        //SimpleDateFormat aFileStamp = new SimpleDateFormat("yyMMdd_kkmm");        
        long byteCount;         
        // mark the actual bytes read into the buffer, and write only those bytes
        int bytesRead;
        try {
            /*
             * Clear output content buffer, leave header and status codes
             * throws IllegalStateException
             */
            aURL  = new URL(fullURL);//urlString);
                            
            /*
             * get a connection based on the URL
             * throws IOException
             */
            aURLConnection = (HttpURLConnection)aURL.openConnection();
            // fake the agent
            HttpURLConnection.setDefaultAllowUserInteraction(true);
            aURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
            //Map<String, List<String>> map = aURLConnection.getRequestProperties();
    
            /*
             * get the abstract InputStream from the URLConnection
             * throws IOException, UnknownServiceException
             */             
            abstractInputStream = aURLConnection.getInputStream();
            aBufferedInputStream = new BufferedInputStream(abstractInputStream);
            // signed byte counter for file sizes up to 2^63 = 4GB * 2GB
            byteCount = 0;
                
            System.out.println("Downloading from: " + fullURL);//urlString);
            /*
             * buffer the input
             * Note: the implementation of OutputStream.write(,,)
             * may not allow the buffer size to affect download speed
             * Also: the byte array is preinitialized to 0-bytes
             * Range is -128 to 127
             */         
            byte b[] = new byte[INPUT_BUFFER_SIZE];
            
            /*
             * Read a specific amount of bytes from the input stream at a time
             * and redirect the buffer to the servlet output stream.
             * A -1 will signify an EOF on the input.
             * Start writing to the buffer at position 0
             * throws IOException - if an I/O error occurs.
             */
            while ((bytesRead = aBufferedInputStream.read(
                    b,              // name of buffer
                    0,              // start of buffer to start reading into
                    b.length        // save actual bytes read, not default max buffer size
                )) >= 0) {
                /**
                 * We will use the write() function of the abstract superclass
                 * OutputStream not the print() function which is used for html
                 * output and appends cr/lf chars.
                 * Only write out the actual bytes read starting at offset 0
                 * throws IOException 
                 * - if an I/O error occurs. 
                 * In particular, an IOException is thrown if the output stream is closed.
                 * IE: The client closing the browser will invoke the exception
                 * [Connection reset by peer: socket write error]
                 * IOException: Software caused connection abort: socket write error
                 * 
                 * If b is null, a NullPointerException is thrown.
                 * 
                 * Note: The default implementation of write(,,) writes one byte a time
                 * consequently performance may be unaffected by array size
                 */
                // keep track of total bytes read from array
                byteCount += bytesRead;
                for(int i=0;i<b.length;i++) {
                    buffer.append((char)b[i]);
                }
                // write to file
                
                //aFileWriter.write(b, 0, bytesRead);//b.length);
            } // while
            System.out.println("HTML capture/processing complete: bytes: " + byteCount);
            //imageName = urlAppend;
            // we successfully streamed the file                    
            aBufferedInputStream.close();
            // now write the file if it is valid
            /*if(byteCount > 6144) {
                service.setCurrentImage(filenamePath);
            }*/
                
                //aFileWriter = new FileOutputStream(filenamePath);
                //System.out.println("_Writing to: " + filenamePath);
                // write to file
                //aFileWriter.write(b, 0, b.length);

            /*if(null != aFileWriter) {
                aFileWriter.flush();
                aFileWriter.close();
            }*/
//          } else {
//              System.out.println("_skipping:    " + filenamePath);

//          }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw e;                            
        } catch (UnknownServiceException e) {
            // testcase: remove ftp prefix from the URL
            e.printStackTrace();
            throw e;                            
        } catch (MalformedURLException e) {
            // testcase: remove ftp prefix from the URL
            e.printStackTrace();
            throw e;            
        } catch (IOException e) {
            // 403 testcase: add text after ftp://
            e.printStackTrace();
            throw e;                            
        } catch (Exception e) {
            e.printStackTrace();
            //throw e;
        /*
         * Finalization block executed by all code paths
         */
        } finally {
            // close input stream
            if(aBufferedInputStream != null) {
                aBufferedInputStream.close();
            } // if
            
            // close file stream
            /*if(aFileWriter != null) {
                //aFileWriter.flush(); 
                aFileWriter.close();
            }*/
            // dereference objects
        } // finally
        
        // only return a non-null image name if the image is invalid
        //return imageName;
        return buffer.toString();
    }
	
    //public String get
    
	public String captureImage(Calendar initialTime, ApplicationService service, Site site, 
			boolean mostRecent, String postfix, int extraOffset, int timeOffset) throws Exception {
	    Calendar universalTime = initialTime;//GregorianCalendar.getInstance();
	    String fullURL = service.getURL(site, universalTime, timeOffset, EMPTY_STRING);//.site.getUrl();
	    if(fullURL == null) return null;
	    /*if(mostRecent) {
	    	fullURL +=  urlAppend.toString();
	    } else {
	    	fullURL += urlPostfix;
	    }*/
	    StringBuffer urlAppend = new StringBuffer();
	    urlAppend.append(getTimestampFileFormat(universalTime, "_"));
	    return captureImage(unprocessedImagePath, service, site, urlAppend.toString(), fullURL, postfix);
	} // captureURL

	// 201108071940
    public String getTimestampFileFormat(Calendar universalTime, String delimeter) {
        return getTimestampFileFormat(universalTime, delimeter, EMPTY_STRING, EMPTY_STRING, false);
    }
    
    // 20110928_000000
	public String getTimestampFileFormat(Calendar universalTime, String delimeter, String midfix, String postfix, boolean dropMin) {//, int extraMinOffset) {
		// subtract - likely 20 min first  (for satellite)
		// 20190720 subtract 10 for some sites like CASBV
        universalTime.add(Calendar.MINUTE, -10);//-extraMinOffset);

        StringBuilder buffer = new StringBuilder();
        buffer.append(universalTime.get(Calendar.YEAR));
        buffer.append(delimeter);
        if(universalTime.get(Calendar.MONTH) < 9) {
        	buffer.append("0");
        }
        buffer.append(1 + universalTime.get(Calendar.MONTH));
        buffer.append(delimeter);
        if(universalTime.get(Calendar.DAY_OF_MONTH) < 10) {
            buffer.append("0");
        }
        
        buffer.append(universalTime.get(Calendar.DAY_OF_MONTH));
        buffer.append(delimeter);
        buffer.append(midfix);        
        
        if(universalTime.get(Calendar.AM_PM) > 0) {
            buffer.append((universalTime.get(Calendar.HOUR) + 12));
        } else {
            if(universalTime.get(Calendar.HOUR) < 10) {
                buffer.append("0");
            }
            buffer.append((universalTime.get(Calendar.HOUR)));
        }
        buffer.append(delimeter);
        if(dropMin) {
            buffer.append("0");
        } else {
            buffer.append((((int)(universalTime.get(Calendar.MINUTE)) / 10)));
        }
        buffer.append("0");
        buffer.append(postfix);        
        return buffer.toString();
	}

    // 07-AUG-08%2007.47.23.721307%20PM
    public String getTimestampFileFormatHistoryUnfinished(Calendar universalTime, String delimeter) {//, int extraMinOffset) {
        // subtract - likely 20 min first  (for satellite)
        //universalTime.add(Calendar.MINUTE);//, -extraMinOffset);

        StringBuilder buffer = new StringBuilder();
        buffer.append(universalTime.get(Calendar.YEAR));
        buffer.append(delimeter);
        if(universalTime.get(Calendar.MONTH) < 9) {
            buffer.append("0");
        }
        buffer.append(1 + universalTime.get(Calendar.MONTH));
        buffer.append(delimeter);
        if(universalTime.get(Calendar.DAY_OF_MONTH) < 10) {
            buffer.append("0");
        }
        buffer.append(universalTime.get(Calendar.DAY_OF_MONTH));
        buffer.append(delimeter);
        if(universalTime.get(Calendar.AM_PM) > 0) {
            buffer.append((universalTime.get(Calendar.HOUR) + 12));
        } else {
            if(universalTime.get(Calendar.HOUR) < 10) {
                buffer.append("0");
            }
            buffer.append((universalTime.get(Calendar.HOUR)));
        }
        buffer.append(delimeter);
        buffer.append((((int)(universalTime.get(Calendar.MINUTE)) / 10)));
        buffer.append("0");
        return buffer.toString();
    }
	
	/**
	 * Return whether the next 10 min interval has started
	 * @return
	 */
    public void delayUntilNextTimeInterval() {
        delayUntilNextTimeInterval(ApplicationService.DELAY_INTERVAL_FOR_RADAR_SWEEP_WAIT, 1);
    }

    public void delayUntilNextTimeInterval(long interval, int iterations) {
        delayUntilNextTimeInterval(interval, iterations, null);
    }
    
	public void delayUntilNextTimeInterval(long interval, int iterations, Integer overrideMin) {
        String nextTimestamp;
        Calendar currentTime = null;
        //if(iterations > 0) iterations+=1;
	    for(int i=0;i<iterations;i++) {
	        Calendar prevTime = GregorianCalendar.getInstance();
	        //prevTime.add(Calendar.MINUTE, -ApplicationService.SWEEP_INTERVAL_MIN);
	        // if the next truncated timestamp is different - then we are ready for a new sweep
	        String prevTimestamp = getTimestampFileFormat(prevTime, "_");
	        System.out.println("waiting for next interval to: " + prevTimestamp);       
	        do {
	            try {
	                Thread.sleep(interval);            
	            } catch (Exception e2) {
	                //e2.printStackTrace();
	            }
	            // check for override first
	            currentTime = GregorianCalendar.getInstance();
	            if(null != overrideMin && currentTime.get(Calendar.MINUTE) == overrideMin.intValue()) {
	                i = iterations;
	                // wait 50 sec so we dont repeat during the whole minute
	                try { Thread.sleep(40000); } catch(Exception e3) {};
	                break;
	            }
	            nextTimestamp = getTimestampFileFormat(currentTime, "_");   
                System.out.print(".");
	        } while (prevTimestamp.equalsIgnoreCase(nextTimestamp));
	    }
	    //System.out.println(nextTimestamp);
	}
	
    public  void delayRandom(long minDelayMS, long maxDelayMS) {
        try {
            long randomTime = minDelayMS + Math.round((Math.random() * maxDelayMS));
            //System.out.println("wait " + randomTime );
            Thread.sleep(randomTime);
        } catch (Exception e2) {
        	e2.printStackTrace();
        }
    }
    
    public void captureSatelliteIndefinitely(ApplicationService service, List<Site> sites,long  minDelayMS, long maxDelayMS) {
        String imageName = null;
        int numberRetries = 0;

        for(;;) {
            Calendar startTime = GregorianCalendar.getInstance();
            startTime.add(Calendar.HOUR, this.dstOffset);//DAYLIGHT_SAVINGS_TIME_OFFSET); // get UTC
            startTime.add(Calendar.MINUTE, -10);
            //for(Site site : sites) {
            try {
                // don't get into an infinite loop on error
                System.out.println();      
                for(Site site : sites) {
                    // recalculate time
                    do {
                        try {
                            delayRandom(minDelayMS, maxDelayMS);
                            numberRetries++;
                         // TODO: REQUIRE TIMED THREAD
                            
                            imageName = captureImage(startTime, service, site, true, ApplicationService.CURRENT_SAT_URL_POSTFIX, 20,0);                        
                            
                        } catch (Exception e) {
                            e.printStackTrace(); // remove 0-length filename	//deleteImage(imageName, true);
                            imageName = null;
                        }
                        if(null != imageName) {
                        	
                   			// push images to Amazon S3
                        	
                        	// persist image
           		        	String timeStamp = getTimestampFileFormat(startTime, "_");
                   			String filenamePath = getFilename(unprocessedImagePath, site, 
                   					timeStamp,//service.getURL(site, startTime, 0, EMPTY_STRING), 
                   					ApplicationService.CURRENT_SAT_URL_POSTFIX, DEFAULT_SUBDIR);
                        	
                   			File aFile = new File(filenamePath);
                   			if(null != aFile && imageName != null && aFile.exists() && aFile.length() > 0) {
                   				String s3FileName = site.getName() + "_" + imageName + ApplicationService.CURRENT_SAT_URL_POSTFIX;
                   				uploadS3(filenamePath, imageName, site.getName(), s3FileName);
                   			}
                        	
                            imageName = null;
                            numberRetries = MAX_DOWNLOAD_RETRIES;
                        }
                    } while (null == imageName && numberRetries < MAX_DOWNLOAD_RETRIES);
                    numberRetries = 0;
                }
                System.out.print("Sat Sleeping for ~20 min until next time interval: ");
                this.delayUntilNextTimeInterval(ApplicationService.DELAY_INTERVAL_FOR_SAT_SWEEP_WAIT, 2);
            } catch (Exception e) {
               e.printStackTrace();
               try {
                   Thread.sleep(MIN_SLEEP_MS);
               } catch (Exception e2) {
            	   e2.printStackTrace();
               }
            }
        //}
        }
    }    

    /*public void captureLightningIndefinitely(ApplicationService service, List<Site> sites, int utcOffset, String postfix, int extraTimeOffset, long detayTime, long delayIntervals, long  minDelayMS, long maxDelayMS) {
        String imageName = null;
        int numberRetries = 0;

        for(;;) {
            Calendar startTime = GregorianCalendar.getInstance();
            startTime.add(Calendar.HOUR, DAYLIGHT_SAVINGS_TIME_OFFSET); // get UTC
            startTime.add(Calendar.MINUTE, -10);
            //for(Site site : sites) {
            try {
                // don't get into an infinite loop on error
                System.out.println();      
                for(Site site : sites) {
                    // recalculate time
                    do {
                        try {
                            delayRandom(minDelayMS, maxDelayMS);
                            numberRetries++;
                            imageName = captureImage(startTime, service, site, true, ApplicationService.CURRENT_LIGHTNING_URL_POSTFIX, 60,0);                        
                        } catch (Exception e) {
                            e.printStackTrace();
                            imageName = null;
                        }
                        if(null != imageName) {
                        	
                        	// persist image
           		        	String timeStamp = getTimestampFileFormat(startTime, "_");
                   			String filenamePath = getFilename(unprocessedImagePath, site, 
                   					timeStamp,//service.getURL(site, startTime, 0, EMPTY_STRING), 
                   					ApplicationService.CURRENT_LIGHTNING_URL_POSTFIX, DEFAULT_SUBDIR);
                        	
                   			File aFile = new File(filenamePath);
                   			if(null != aFile && imageName != null && aFile.exists() && aFile.length() > 0) {
                   				String s3FileName = site.getName() + "_" + imageName + ApplicationService.CURRENT_LIGHTNING_URL_POSTFIX;
                   				uploadS3(filenamePath, imageName, site.getName(), s3FileName);
                   			}
                            imageName = null;
                            numberRetries = MAX_DOWNLOAD_RETRIES;
                        }
                    } while (null == imageName && numberRetries < MAX_DOWNLOAD_RETRIES);
                    numberRetries = 0;
                }
                System.out.print("Sleeping for ~60 min until next time interval: ");
                this.delayUntilNextTimeInterval(ApplicationService.DELAY_INTERVAL_FOR_LIGHTNING_SWEEP_WAIT, 6);
            } catch (Exception e) {
               e.printStackTrace();
               try {
                   Thread.sleep(MIN_SLEEP_MS);
               } catch (Exception e2) {}
            }
        //}
        }
    }    */
    
    /**
     * Capture live radar data (10 min old) in an endless loop for all sites.
     * @param service
     * @param sites
     * @param minDelayMS
     * @param maxDelayMS
     */
	public void captureRadarIndefinitely(RadarService service, List<Site> sites, long  minDelayMS, long maxDelayMS) {
	    String imageName = null;
	    BufferedImage reducedImage = null;
	    BufferedImage image = null;
	    int numberRetries = 0;
    	String timeStamp = null;
		String filenamePath = null; 
		RadarSite radarSite;
	    Sweep sweep = null;
	    Calendar startTime = null;
	    startTime = GregorianCalendar.getInstance();
	    //startTime.add(Calendar.MINUTE, -10); // some radar is behind by 13 min (use 20)
	    
	    for(;;) {
		    startTime = GregorianCalendar.getInstance();
		    startTime.add(Calendar.MINUTE, -10); // some radar is behind by 13 min (use 20)
		    //if(startTime.get)
		    //TimeZone.getDefault().inDaylightTime(new Date())
		    startTime.add(Calendar.HOUR, this.dstOffset); // get UTC
		    startTime.add(Calendar.MINUTE, -10);

            try {
                // don't get into an infinite loop on error
            	System.out.println();
            	
            	for(Site site : sites) {
            		radarSite = (RadarSite) site;
            		// recalculate time
            		do {
            		    try {
            		        delayRandom(minDelayMS, maxDelayMS);
            		        imageName = captureImage(startTime, service, site, true, ApplicationService.CURRENT_RADAR_URL_POSTFIX, 0,0);
            		    } catch (Exception e) {
            		        //e.printStackTrace();
            		    	System.out.println(e.getMessage());
            		    	// erase zero length image
                       		//deleteImage(imageName, true); // throws npe            		        
            		        imageName = null;
            		    } finally {
            		    	numberRetries++;            		        
            		    }
            		    if(null != imageName) {
            		        //imageName = null;
            		        numberRetries = MAX_DOWNLOAD_RETRIES;
                    		// persist image
           		        	timeStamp = getTimestampFileFormat(startTime, "_");
                   			filenamePath = getFilename(unprocessedImagePath, site, 
                   					timeStamp,//service.getURL(site, startTime, 0, EMPTY_STRING), 
                   					ApplicationService.CURRENT_RADAR_URL_POSTFIX, DEFAULT_SUBDIR);
                   			sweep = new Sweep();
                   			sweep.setSite(radarSite);
                   			if(null != service.getPreProcessor()) {
                       			image = service.loadImage(filenamePath);
                       			
                       			// push images to Amazon S3
                       			File aFile = new File(filenamePath);
                       			if(null != aFile && imageName != null && aFile.exists() && aFile.length() > 0) {
                       				String s3FileName = site.getName() + "_" + imageName + ApplicationService.CURRENT_RADAR_URL_POSTFIX;
                       				uploadS3(filenamePath, imageName, site.getName(), s3FileName);
                       			}
                       			
                   				// filter only HistoricalRadarService - to remove range rings
                   				if(service.isHistorical()) {
                   					reducedImage = service.getPreProcessor().doFilter(sweep, 0, image, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);
                   				} else {
                   					reducedImage = image;
                   				}
                   				//writeImage(reducedImage, outputPath, "gif");
                   				sweep.setImage(reducedImage); // this converts image to byte array
                   				// original image is on HD, filtered image is in DB
                   				sweep.setTimestamp(timeStamp);// we dont store the site prefix or directory with each image
                   				service.setCurrentImage(reducedImage);//filenamePath);
                   				radarSite.addSweep(sweep);
                   				service.persistImage(radarSite, reducedImage, filenamePath);// timeStamp);
                   				// remove sweep - after saving it - or we will get an eventual OOME after a couple days
                   				radarSite.removeSweep(sweep);
                   				sweep.clear();                    				
                   			} else {
                   				System.out.println("_service.preProcessor is not set");
                   			}
                   			imageName = null;
            		    }
            		} while (null == imageName && numberRetries < MAX_DOWNLOAD_RETRIES);
            		numberRetries = 0;
            	}
                System.out.print("Sleeping for 3-10 min until next time interval: ");
                this.delayUntilNextTimeInterval();
            } catch (Exception e) {
               //e.printStackTrace();
            	System.out.println(e.getMessage());
            	try {
                   Thread.sleep(MIN_SLEEP_MS);
            	} catch (Exception e2) {}
            }
	    }
	}
	
	public void initialize() {
		initialize(DEFAULT_RESOURCE_DRIVE);
	}
	
	private void setDirectories() {
		if(null == this.overrideImagePath) {
			setProcessedImagePath(targetDrive + "/" + DEFAULT_PROCESSED_IMAGE_DIR);
			setUnprocessedImagePath(sourceDrive + DEFAULT_UNPROCESSED_IMAGE_DIR);
		} else {
			setProcessedImagePath(targetDrive + "/" + overrideImagePath);
			setUnprocessedImagePath(sourceDrive + "/" + overrideImagePath);			
		}
	}
	
	public void initialize(String drive) {
	    this.sourceDrive = drive;
	    this.targetDrive = drive;
	    setDirectories();
		// set DST offset
	    //GregorianCalendar startTime = (GregorianCalendar)GregorianCalendar.getInstance();
	    //startTime.
	    //NON_DAYLIGHT_SAVINGS_TIME_OFFSET
	    this.dstOffset=4;
	    s3Client = new AmazonS3Client(new ProfileCredentialsProvider());

	}
	
	public boolean isProxied() {		return isProxied;	}
	public void setProxied(boolean isProxied) {	
		this.isProxied = isProxied;	
		if(isProxied) {
			// inside a firewall only
		System.getProperties().put("proxySet","true"); 
		System.getProperties().put("proxyHost", "proxy.lbs.alcatel-lucent.com");//"webproxystatic-on.tsl.telus.com");//"www-proxy.us.oracle.com"); 
		System.getProperties().put("proxyPort",  "8000");//"8080");//"80");
		}
	}
	public String getUnprocessedImagePath() {		return unprocessedImagePath;	}
	public void setUnprocessedImagePath(String unprocessedImagePath) {		this.unprocessedImagePath = unprocessedImagePath;	}
	public String getProcessedImagePath() {		return processedImagePath;	}
	public void setProcessedImagePath(String processedImagePath) {		this.processedImagePath = processedImagePath;	}
    public String getSourceDrive() {
        return sourceDrive;
    }

    public void setSourceDrive(String sourceDrive) {
        this.sourceDrive = sourceDrive;
        setDirectories();
    }

    public String getTargetDrive() {
        return targetDrive;
    }

    public void setTargetDrive(String targetDrive) {
        this.targetDrive = targetDrive;
        setDirectories();
    }

    public String getOverrideImagePath() {
		return overrideImagePath;
	}

	public void setOverrideImagePath(String overrideImagePath) {
		this.overrideImagePath = overrideImagePath;
        setDirectories();
	}

	public int getDstOffset() {
		return dstOffset;
	}

	public void setDstOffset(int dstOffset) {
		this.dstOffset = dstOffset;
	}

	public AmazonS3Client getS3Client() {
		return s3Client;
	}

	public void setS3Client(AmazonS3Client s3Client) {
		this.s3Client = s3Client;
	}
    
} // ApplicationService
