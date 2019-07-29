
package org.obrienscience.radar.integration;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.obrienscience.radar.model.Site;

/**
 * <p><i>Notes:</i><br>
 * <br>
 * 20140306: Implement thread pool of timed Callables to handle stuck sockets to EC satellite servers
 * </p>
 */
public class BlockingCallable<T> implements Callable<T> {
	
	private static final ExecutorService executorService = Executors.newCachedThreadPool();

	private ApplicationService service;
	private Site site;
	private String urlAppend;
	private String fullURL;
	private String postfix;
	private String subdir;
	private String unprocessedImagePath;
	//private FutureTask<T> task;
	
	
	public BlockingCallable(//FutureTask<T> task, 
			ApplicationService service,
			Site site,
			String urlAppend,
			String fullURL,
			String postfix,
			String subdir,
			String unprocessedImagePath) {
		//this.task = task;
		this.service = service;
		this.site = site;
		this.urlAppend = urlAppend;
		this.fullURL = fullURL;
		this.postfix = postfix;
		this.subdir = subdir;
		this.unprocessedImagePath = unprocessedImagePath;
	}    

	public T call() {
		T filenamePath = null;
    	try {
            //synchronized (site) { 
            	filenamePath = captureImage(unprocessedImagePath, service, site,
       					urlAppend, fullURL, postfix, subdir);
            //}
    	} catch (Exception e) {
    		e.printStackTrace();
     	}
    	return filenamePath;
    }

	public T captureImage(String unprocessedImagePath,
			ApplicationService service, Site site, String urlAppend, String fullURL, String postfix, String subdir) throws Exception {
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
		// 2018 historical add GIF
		//filenamePath += ".GIF";
		FileOutputStream aFileWriter = new FileOutputStream(filenamePath);
		System.out.println("_Writing to: " + filenamePath);
		/** connection based on the aURL */
		HttpURLConnection aURLConnection = null;
		/** URL object that we can pass to the URLConnection abstract factory */
		URL 	aURL = null;
		/** create a date formatter for time tracking - not thread-safe */
		long byteCount;			
		// mark the actual bytes read into the buffer, and write only those bytes
		int bytesRead = 0;
		long tsstart = System.currentTimeMillis();
		try {
			/*
			 * Clear output content buffer, leave header and status codes
			 * throws IllegalStateException
			 */
			aURL  = new URL(fullURL);
							
			/*
			 * get a connection based on the URL
			 * throws IOException
			 */
			aURLConnection = (HttpURLConnection)aURL.openConnection();
			// fake the agent
			HttpURLConnection.setDefaultAllowUserInteraction(true);
			aURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
	
			/*
			 * get the abstract InputStream from the URLConnection
			 * throws IOException, UnknownServiceException
			 */				
			abstractInputStream = aURLConnection.getInputStream();
			aBufferedInputStream = new BufferedInputStream(abstractInputStream);
			// signed byte counter for file sizes up to 2^63 = 4GB * 2GB
			byteCount = 0;
				
			System.out.println("Downloading from: " + fullURL);
			/*
			 * buffer the input
			 * Note: the implementation of OutputStream.write(,,)
			 * may not allow the buffer size to affect download speed
			 * Also: the byte array is preinitialized to 0-bytes
			 * Range is -128 to 127
			 */			
			final byte b[] = new byte[ResourceManager.INPUT_BUFFER_SIZE];
			
			FutureTask<Integer> aFutureTask = null;
			while((bytesRead >= 0) && ((System.currentTimeMillis() - tsstart) < 30000)) {
				final BufferedInputStream bufferedInputStream = aBufferedInputStream;
				aFutureTask = new FutureTask<Integer>(new Callable<Integer>() {        	
	        		public Integer call() throws Exception {
	        			int bytesReadAnon = -1;
	        			try {
				bytesReadAnon = bufferedInputStream.read(
				        b, 				// name of buffer
				        0, 				// start of buffer to start reading into
				        b.length		// save actual bytes read, not default max buffer size
				    );
	        			} catch (Exception e) {
	        				e.printStackTrace();
	        			}
	        			return bytesReadAnon;
	        		}});
				executorService.submit(aFutureTask);
	        	bytesRead = aFutureTask.get(120, TimeUnit.SECONDS); // returns an immediate result or throws TimeoutException
				if(bytesRead > 0) {
					byteCount += bytesRead;
					// write to file
					aFileWriter.write(b, 0, bytesRead);//b.length);
					// sleep to allow for thread interruption or shutdown
					Thread.sleep(10);
					if(Thread.currentThread().isInterrupted() || Thread.currentThread().isDaemon()) {//aFutureTask.isCancelled()) {
						throw new InterruptedException();
					}
				}
			} // while
			System.out.println((System.currentTimeMillis() - tsstart) + "ms:HTML capture/processing complete: bytes: " + byteCount);
			// we successfully streamed the file					
			aBufferedInputStream.close();
			// now write the file if it is valid
			if(byteCount > 6144) {
				// TODO: IF the filesize
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
			System.out.println("_Interrupted: " + this.getFullURL());
		} catch (IllegalStateException e) {
			System.out.println(e.getMessage());
		} catch (UnknownServiceException e) {
			// testcase: remove ftp prefix from the URL
			System.out.println(e.getMessage());
		} catch (MalformedURLException e) {
			// testcase: remove ftp prefix from the URL
			System.out.println(e.getMessage());
		} catch (IOException e) {
			// 403 testcase: add text after ftp://
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage()); // check IndexOutOfBoundsException on buffer
		} finally {
			// close input stream
			if(aBufferedInputStream != null) {
				aBufferedInputStream.close();
			} // if
			
			// close file stream
			if(aFileWriter != null) {
				//aFileWriter.flush(); 
				aFileWriter.close();
			}
		} // finally
		
		// only return a non-null image name if the image is invalid
		return (T)imageName;
	}

	
	
 
	public ApplicationService getService() {
		return service;
	}

	public void setService(ApplicationService service) {
		this.service = service;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public String getUrlAppend() {
		return urlAppend;
	}

	public void setUrlAppend(String urlAppend) {
		this.urlAppend = urlAppend;
	}

	public String getFullURL() {
		return fullURL;
	}

	public void setFullURL(String fullURL) {
		this.fullURL = fullURL;
	}

	public String getPostfix() {
		return postfix;
	}

	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	public String getSubdir() {
		return subdir;
	}

	public void setSubdir(String subdir) {
		this.subdir = subdir;
	}
	
/*
 * 
"pool-4-thread-15" prio=5 tid=0x00007f905d0ec000 nid=0xe333 waiting for monitor entry [0x00000001246c5000]
   java.lang.Thread.State: BLOCKED (on object monitor)
	at org.obrienscience.radar.integration.BlockingCallable.call(BlockingCallable.java:52)
	- waiting to lock <0x00000007000d4c50> (a org.obrienscience.radar.model.SatSite)
	at java.util.concurrent.FutureTask.run(FutureTask.java:262)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
	at java.lang.Thread.run(Thread.java:744)

   Locked ownable synchronizers:
	- <0x0000000700671ea0> (a java.util.concurrent.ThreadPoolExecutor$Worker)
	// was lock on Site
 */
/**
 * 
"pool-4-thread-5" prio=5 tid=0x00007fa75b85f800 nid=0xdc37 runnable [0x00000001241bf000]
   java.lang.Thread.State: RUNNABLE
	at java.net.SocketInputStream.socketRead0(Native Method)
	at java.net.SocketInputStream.read(SocketInputStream.java:152)
	at java.net.SocketInputStream.read(SocketInputStream.java:122)
	at java.io.BufferedInputStream.read1(BufferedInputStream.java:273)
	at java.io.BufferedInputStream.read(BufferedInputStream.java:334)
	- locked <0x00000007004814d8> (a java.io.BufferedInputStream)
	at sun.net.www.MeteredStream.read(MeteredStream.java:134)
	- locked <0x0000000700481500> (a sun.net.www.http.KeepAliveStream)
	at java.io.FilterInputStream.read(FilterInputStream.java:133)
	at sun.net.www.protocol.http.HttpURLConnection$HttpInputStream.read(HttpURLConnection.java:3053)
	at java.io.BufferedInputStream.read1(BufferedInputStream.java:273)
	at java.io.BufferedInputStream.read(BufferedInputStream.java:334)
	- locked <0x0000000700481570> (a java.io.BufferedInputStream)
	at org.obrienscience.radar.integration.BlockingCallable.captureImage(BlockingCallable.java:129)
	at org.obrienscience.radar.integration.BlockingCallable.call(BlockingCallable.java:52)
	at java.util.concurrent.FutureTask.run(FutureTask.java:262)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
	at java.lang.Thread.run(Thread.java:744)

   Locked ownable synchronizers:
	- <0x0000000700481768> (a java.util.concurrent.ThreadPoolExecutor$Worker)
// was bis.read
 */
	
	
}
