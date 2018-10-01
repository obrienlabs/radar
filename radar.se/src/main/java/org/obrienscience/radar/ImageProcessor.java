package org.obrienscience.radar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.imageio.ImageIO;

import org.obrienscience.radar.business.concurrency.ForkFilter;
import org.obrienscience.radar.integration.ApplicationService;
import org.obrienscience.radar.integration.LightningService;
import org.obrienscience.radar.integration.LiveRadarService;
import org.obrienscience.radar.integration.SatService;
import org.obrienscience.radar.model.RadarSite;
import org.obrienscience.radar.model.Site;
import org.obrienscience.radar.model.Sweep;

//import com.google.common.io.Files;

// http://java.sun.com/developer/technicalArticles/Media/imagestrategies/index.html

/**
 * speed 57748 at 11.4/sec on corei860 9.4 on corei920, 11.1 on corei7-3610 (asus g75w)
 * 87/sec without filtering math (read/write only)
 * 11 ms to read/write with 79 ms processing = 88% time is non-IO processing
 * Therefore we can read/write 7 files in the time it takes to process 1 - or if we split the processing
 * among 7 cores - we would achieve the highest disk throughput
 * 
 * size from 
 * @author mfobrien
 *
 */
public abstract class ImageProcessor implements Runnable {

	private ForkJoinPool pool = new ForkJoinPool();
    //private Map<String, Color> colorMap = new HashMap<String, Color>();
    public static String inputDrive = "i";
    public static String outputDrive = "i";
    public static BufferedImage RING_FILTER_IMAGE = null;
    //public static final String FILTERED_DATA_DIR = "/Volumes/gdrive2/_filtered_data/";
    private BufferedImage statImage;
    private RadarView statView;

    public void setApplicationServiceOnView(ApplicationService service) {
    	if(null != statView) {
    		statView.setApplicationService(service);
    	}
    }

    private void initialize(RadarView aView) {
    	// set static variables
    	RING_FILTER_IMAGE = loadImage("xft/XFT_ring_filter", "gif");
	    statImage = new BufferedImage (RING_FILTER_IMAGE.getWidth(), RING_FILTER_IMAGE.getHeight(), BufferedImage.TYPE_INT_RGB);
        statView = aView;
        statView.setBufferedImage(statImage);     
        statView.applicationInit();
        statView.setFlash(false);
    }
    
    public BufferedImage getOR(String prevImage, String nextImage, String format) {
        BufferedImage pImage = loadImage(prevImage, format);
        BufferedImage nImage = loadImage(nextImage, format);
        return getOR(pImage, nImage);
    }

    public BufferedImage getDifference(String prevImage, String nextImage, String format) {
        BufferedImage pImage = loadImage(prevImage, format);
        BufferedImage nImage = loadImage(nextImage, format);
        return getDifference(pImage, nImage);
    }

    private List<BufferedImage> doOperation(BufferedImage prevImage, BufferedImage nextImage) {
        int height = prevImage.getHeight();
        int width = prevImage.getWidth();
        // exit if the image size is 0
        //int pColor, nColor;
        //boolean diffFound = false;
        List<BufferedImage> images = new ArrayList<BufferedImage>(3);
        BufferedImage bufferedImage =
            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
        images.add(prevImage);
        images.add(nextImage);        
        images.add(bufferedImage);
        if(height < 1 || width < 1) {
            return images;
        }
        return images;
    }

    private void doFilterType(BufferedImage input, int x, int y, int pColor) {
        //BufferedImage output = input;//new BufferedImage (height, height, BufferedImage.TYPE_INT_RGB);
        boolean match = false;
        for(int i=0; i<RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 0;i++) {                   
            if(pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[i]) {
                match = true;
                i = RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE;
            }
        }
        if(match) {
            input.setRGB(x, y, pColor);
        }
    }

    public BufferedImage doFilter(int filter, BufferedImage input, int layer) {
    	return doFilter(new Sweep(), filter,input, layer);
    }
    public BufferedImage doFilter(Sweep sweep, int filter, BufferedImage input, int layer) {
        int height = input.getHeight();
        BufferedImage croppedImage = new BufferedImage (height, height, BufferedImage.TYPE_INT_RGB);

        // get the historical status of the image - whether it has destructive range and overlay data
        // Look for at least 2 white pixels in any direction from the center
        int h = height >> 1 - 10; // there is always a + in the center
        int pColor;
        int rangeRingCount = 0;
        for(int y=0;y<height;y++) {
        	pColor = input.getRGB(h, y);
        	if(RadarSite.COLOR_WHITE == pColor) {
        		rangeRingCount++;
        	}
        	if(rangeRingCount > 4) {
        		y = height;
        		sweep.setIsHistorical(true); 
        		System.out.println("Historical Image: " + sweep.getTimestamp());
        	}
        	//System.out.println(h + "," + y + " = " + pColor);        	
        }
        
        // height 480
        ForkFilter ff = new ForkFilter(input, 0, height, croppedImage, height, height);
        pool.invoke(ff);

/*        //boolean match = false;
        for(int y=0;y<height;y++) {
            for(int x=0;x<height;x++) {
                pColor = input.getRGB(x, y);
                doFilterType(croppedImage, x, y, pColor);
                // filter based on subset of intensities
                //match = false;
            }
        }
  */      
        // remove red range circle
        doNAND(croppedImage, RING_FILTER_IMAGE);
        // remove single pixels
        //croppedImage = doPixelFilter(croppedImage, 0,1);
        
        // erase red edge box last
        Graphics2D g = croppedImage.createGraphics(); 
        g.drawImage(croppedImage, 0,0,null);

        g.setColor(Color.BLACK);
        //g.drawLine(0,0,height-1, height-1);
        g.drawLine(0,0,0, height-1);
        g.drawLine(0,0,height-1, 0);
        g.drawLine(0,height-1, height-1, height -1);
        g.drawLine(height-1,0, height-1, height - 1);
        // set historical flag
        return croppedImage;
    }

    public void doNAND(BufferedImage target, BufferedImage filter) {
        int height = target.getHeight();
        int width = target.getWidth();
        int pColor, nColor;
        Graphics2D g = target.createGraphics(); 
        g.drawImage(target, 0,0,null);
        g.setColor(Color.BLACK);
        //System.out.println(width + "," + height);
        for(int y=0;y<height;y++) {
            for(int x=0;x<width;x++) {
                //System.out.println(x + "," + y);
                pColor = target.getRGB(x, y);
                nColor = filter.getRGB(x, y);
                if(pColor == nColor) {
                    target.setRGB(x, y, 0);//Color.BLACK);
                }
            }
        }
    }    

    /**
     * Add missing radar data based on surroundings  - do not use
     * @param input
     * @param layer
     * @param pixelWidth
     * @return
     */
    BufferedImage doPixelFilter(BufferedImage input, int layer, int pixelWidth) {
        int height = input.getHeight();
        int width = input.getWidth();
        BufferedImage filteredImage = new BufferedImage (height, height, BufferedImage.TYPE_INT_RGB);
        
        int pColor, nColor;
        Graphics2D g = filteredImage.createGraphics(); 
        g.drawImage(filteredImage, 0,0,null);
        g.setColor(Color.BLACK);
        //int[] surroundingColors = {0,0,0,0,0,0,0,0};
        for(int y=1;y<height - 1;y++) {
            for(int x=1;x<width - 1;x++) {                
                pColor = input.getRGB(x, y);
                // check only non-black pixels for erasure
                if(pColor != RadarSite.COLOR_BLACK) {
                    nColor = 0;
                    // get surrounding pixels - they need to be all black
                    nColor += input.getRGB(x-1 , y-1);
                    nColor += input.getRGB(x    , y-1);
                    nColor += input.getRGB(x+1, y-1);
                    nColor += input.getRGB(x-1 , y);
                    nColor += input.getRGB(x+1, y);
                    nColor += input.getRGB(x-1 , y+1);
                    nColor += input.getRGB(x    , y+1);
                    nColor += input.getRGB(x+1, y+1);
                    if(nColor != -134217728) {
                        filteredImage.setRGB(x, y, pColor);
                    } else {
                        filteredImage.setRGB(x, y, 0);
                    }
                } else {
/*                    // check black pixels for refill
                    //nColor = input.getRGB(x-1 , y-1);
                    nColor = 0;
                    // get surrounding pixels - 2 of 8 can be different - we are erasing lines
                    surroundingColors[0] = input.getRGB(x-1 , y-1);
                    surroundingColors[1] = input.getRGB(x    , y-1);
                    surroundingColors[2] = input.getRGB(x+1, y-1);
                    surroundingColors[3] = input.getRGB(x-1 , y);
                    surroundingColors[4] = input.getRGB(x+1, y);
                    surroundingColors[5] = input.getRGB(x-1 , y+1);
                    surroundingColors[6] = input.getRGB(x    , y+1);
                    surroundingColors[7] = input.getRGB(x+1, y+1);
                    // look for more than 2 different colors
                    int categories = 0;
                    for(int i=1;i<8;i++) {
                        if(surroundingColors[i-1] != surroundingColors[i]) {
                            categories++;
                            surroundingColors[i-1] = 0;
                        }
                    }
                    if(categories < 3) {
                        // get the first non-black leftover color
                        for(int i=0;i<8;i++) {
                            if(surroundingColors[i] != 0) {
                                filteredImage.setRGB(x, y, surroundingColors[i]);
                                //System.out.println(x + "," + y);
                                i = 8;
                            }
                        }
                    }*/                    
                }
            }
        }
        return filteredImage;
    }

    public int isDifferent(BufferedImage prevImage, BufferedImage nextImage) {
    	return isDifferent(prevImage, nextImage, 10);
    }
    public int isDifferent(BufferedImage prevImage, BufferedImage nextImage, int pixelDiffThreashold) {
    	int pixelsDifferent = 0;
    	if(prevImage.getHeight() != nextImage.getHeight() ||
    		prevImage.getWidth() != nextImage.getWidth()) {
    		return 1 << 15;
    	}
        for(int y=0;y<prevImage.getHeight();y++) {
            for(int x=0;x<prevImage.getWidth();x++) {
                if(prevImage.getRGB(x, y) != nextImage.getRGB(x, y)) {
                	pixelsDifferent += 1;
                	// perf: short circuit
                	if(pixelsDifferent > pixelDiffThreashold) {
                		return pixelsDifferent;
                	 }
                }
            }
        }        
        return pixelsDifferent;
    }
    
    public BufferedImage getDifference(BufferedImage prevImage, BufferedImage nextImage) {
        List<BufferedImage> images = doOperation(prevImage, nextImage);
        Graphics2D g = images.get(2).createGraphics(); 
        g.drawImage(images.get(2), 0,0,null);
        int height = images.get(0).getHeight();
        int width = images.get(0).getWidth();
        int pColor, nColor;
        boolean diffFound = false;
        for(int y=0;y<height;y++) {
            for(int x=0;x<width;x++) {
                pColor = images.get(0).getRGB(x, y);
                nColor = images.get(1).getRGB(x, y);
                if(pColor < nColor || pColor > nColor) {
                    diffFound = true;
                } else {
                    g.drawRect(x, y,0,0);
                }
            }
            if(diffFound) {
                diffFound = false;
            }
        }        
        return images.get(2);
    }

    public BufferedImage getOR(BufferedImage prevImage, BufferedImage nextImage) {
        int height = prevImage.getHeight();
        int width = prevImage.getWidth();
        int pColor, nColor;
        //boolean diffFound = false;
        BufferedImage bufferedImage =
            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
        Graphics2D g = bufferedImage.createGraphics(); 
        g.drawImage(bufferedImage, 0,0,null);
        for(int y=0;y<height;y++) {
            for(int x=0;x<width;x++) {
                pColor = prevImage.getRGB(x, y);
                nColor = nextImage.getRGB(x, y);
                if(nColor != -16777216) {
                    bufferedImage.setRGB(x, y, nColor);
                } else {
                    bufferedImage.setRGB(x, y, pColor);
                }
/*                if(pColor < nColor || pColor > nColor) {
                    diffFound = true;
                    //System.out.print(x + ":" + y + ", ");
                    // do not use getGraphics()
                	g.setColor(new Color(nColor));
                    g.drawRect(x, y,1,1);
                } else {
                	//g.getColor(new Color(0));
                    //g.drawRect(x, y,1,1);
                }
            }
            if(diffFound) {
                //System.out.println();
                diffFound = false;*/
            }
        }
        return bufferedImage;
    }
    
	public BufferedImage loadImage(String filename, String format) {
		// JDK 1.1
		//Image image = Toolkit.getDefaultToolkit().getImage(filename + "." + format);
		// JDK 1.4
		File file = new File(filename + "." + format);
		BufferedImage image = null;
		try {
			image = ImageIO.read(file);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println(image.toString());
		return image;
	}

	/**
	_index: 150 r:80 g:196 b:80 : -10092391
	_index: 164 r:0 g:133 b:0 : -6736948
	_index: 178 r:80 g:196 b:80 : -64871
	_index: 192 r:0 g:0 b:0 : -65536
	_index: 206 r:0 g:0 b:0 : -39424
	_index: 220 r:0 g:0 b:0 : -26368
	_index: 234 r:0 g:0 b:0 : -13312
	_index: 248 r:239 g:219 b:239 : -205
	_index: 262 r:0 g:0 b:0 : -16751104
	_index: 276 r:0 g:0 b:0 : -16738048
	_index: 290 r:0 g:0 b:0 : -16724992
	_index: 304 r:0 g:247 b:0 : -16711834
	_index: 318 r:255 g:92 b:255 : -16737793
	_index: 332 r:255 g:92 b:255 : -6697729
*/
	public void calibrateRadarImage(BufferedImage image) {
        //Graphics2D g = image.createGraphics(); 
        //g.drawImage(image, 0,0,null);
        int height = image.getHeight();
        int width = image.getWidth();
        
        // verify valid image
        if(height > 0 && width > 0) {
            System.out.println("...Calibrating using: " + image);
            // get color values for 14 precip ranges
            int x = 524;
            int y = 150;
            int pColor;
            int red, green, blue;
            for(int i=0;i<RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE;i++) {
                pColor = image.getRGB(x, y);
                red = image.getColorModel().getRed(pColor);
                blue = image.getColorModel().getRed(pColor);
                green = image.getColorModel().getGreen(pColor);
                System.out.println("_index: " + i + " y:" +
                    y + " r:" + red + " g:" + green + " b:" + blue + " : " + pColor  + " = " +
                    RadarSite.PRECIP_INTENSITY_COLOR_CODES[i]);
                y += 14;
            }
        } else {
            System.out.println("Invalid image: " + image);
        }
	}
	
	public void processImage() {
		
	}
	
	public Image createImage() {
		BufferedImage bufferedImage =
			new BufferedImage (16, 16, BufferedImage.TYPE_INT_RGB); 
		Graphics2D g = bufferedImage.createGraphics();//getGraphics (); 
		//paint (g);
		g.drawImage(bufferedImage, 0,0,null);
		g.drawLine(0,0,7,15);
		return bufferedImage;
	}
	
	public void convertImage(BufferedImage anImage, String filename, String format) {
		//BufferedImage image = nImage).getBufferedImage();
		try {
			File file = new File(filename + "." + format);
			ImageIO.write(anImage, format, file);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
	}
	
	public void deleteImage(String filename, String format) {
	//	try {
			File file = new File(filename + "." + format);
			file.delete();
		//} catch (IOException ioe) {
			//ioe.printStackTrace();
		//}
	}
	
	public void writeImage(Image anImage, String filename, String format) {
		try {
		File file = new File(filename + "." + format);
		ImageIO.write((BufferedImage)anImage, format, file);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

    public void reduceRadarImages(String site, String anInputDir, String anOutputDir) {
        reduceRadarImages(site, anInputDir, anOutputDir, true, true, true);
    }
    
    protected int updateCounterPrivate(String site, int counter, int count) {
        // file exists
        //System.out.print("+");
        if(counter++ > 98) {
            System.out.print(site + "\n" + (count + 1));
            counter = 0;
        }
        return counter;        
    }
    
    public void reduceRadarImages(String site, String anInputDir, String anOutputDir, boolean display, boolean persist, boolean file) {
        // Setup View with a dummy image
        String filename = null;
        BufferedImage image = null;
        RadarView aRadarView = null;
        if(display) {
            filename = "xft/XFT_ring_filter";
            image = loadImage(filename, "gif");
            aRadarView = new RadarView();
            aRadarView.setBufferedImage(image);     
            aRadarView.applicationInit();
            aRadarView.setFlash(false);
        }
        
        String inputDir = inputDrive +":" + anInputDir + site +"/";
        String outputDir = outputDrive +":" + anOutputDir;
        LiveRadarService service = new LiveRadarService(); 
        // read everything in the directory
        File dir = new File(inputDir);
        String filenameRoot = null;
        String outputPath = null;
        BufferedImage input = null;
        BufferedImage verify = null;
        BufferedImage reducedImage = null;
        long filesize = 0;
        System.out.println("_retrieving files from:... " + inputDir);
        RadarSite radarSite = null;
        try {
            File[] files = dir.listFiles();
            int counter = 0;
            if(null != files && files.length > 0) {
                System.out.println("_files: " + files.length + "\n");
                for(int i=0; i<files.length; i++) {
                    filename = files[i].getName();
                    filesize = files[i].length();
                    filenameRoot = filename.substring(0, filename.length() - 4);
                    // check if a filtered image already exists - skip then
                    outputPath = outputDir + site +"/" + filenameRoot + "_f";
                    verify = service.loadImage(outputPath + ".gif", true);
                    if(null == verify) {              
                        input = service.loadImage(inputDir + filename);
                        if(null != input && filesize > 0) {
                            reducedImage = doFilter(0, input, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);
                            writeImage(reducedImage, outputPath, "gif");
                            // check performance
                            //writeImage(input, outputPath, "gif");
                            if(display) {
                                aRadarView.setBufferedImage(reducedImage);
                            }
                            // add to stat image
                            if(display) {
                                statImage = getOR(reducedImage, statImage);
                                statView.setBufferedImage(statImage);
                            }
                            System.out.print(".");
                            counter = updateCounterPrivate(site, counter, i);
                        } else {
                            System.out.println("\n_Invalid filesize: " + filesize + " :" + filename);
                        }
                    } else {
                        // file exists
                        System.out.print("+");
                        counter = updateCounterPrivate(site, counter, i);
                    }
            } // for
        } else {
            System.out.println("_Directory is missing: " + inputDir);
        }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(display) {
                aRadarView.tearDown();
                aRadarView = null;
            }
        }
    }

    
    public ForkJoinPool getPool() {
		return pool;
	}

	public void setPool(ForkJoinPool pool) {
		this.pool = pool;
	}
	
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
        String param = null;
        String drive = "gdrive2";
        String site = "wgj";//_te";
        if(null != args && args.length > 1) {
            param = args[0];
            if(null != param) {
                //inputDrive = param;
            	System.out.println("_site: " + param);
                site = param;
            }
            param = args[1];
            if(null != param) {
            	System.out.println("_drive: " + param);
                drive = param;
            }
        }
        
        //PreProcessor processor = new PreProcessor(false);
        //processor.convertAll("e:\\_in\\", "e:\\_out\\");
        
        boolean view = false;
		PreProcessor aProcessor = new PreProcessor(view);
		//String filename = "xft/XFT_2011_09_04_12_40";
/*		//BufferedImage radarImage = aProcessor.loadImage(filename, "gif");
		//aProcessor.convertImage(radarImage, filename, "bmp");
		//Image anImage = aProcessor.createImage();
		//aProcessor.writeImage(anImage);
		BufferedImage image01 = aProcessor.getOR( "2011_06_07_12_50", "2011_06_07_13_00", "gif");
		aProcessor.writeImage(image01, "2011_06_07_12_50_2011_06_07_13_00", "bmp" );
		BufferedImage image01or = aProcessor.getOR( "2011_06_07_12_50", "2011_06_07_13_00", "gif");
		aProcessor.writeImage(image01or, "2011_06_07_12_50_2011_06_07_13_00_or", "bmp" );
		BufferedImage image12 = aProcessor.getOR("2011_06_07_13_00", "2011_06_07_13_10", "gif");
		aProcessor.writeImage(image12,  "2011_06_07_13_00_2011_06_07_13_10", "bmp" );
		BufferedImage image012 = aProcessor.getOR("2011_06_07_12_50_2011_06_07_13_00",  "2011_06_07_13_00_2011_06_07_13_10", "bmp");
		aProcessor.writeImage(image012,  "diff", "bmp" );
		*/
		//BufferedImage image = aProcessor.loadImage(filename, "gif");
        //RadarView aRadarView = new RadarView();
		//aRadarView.setBufferedImage(image);		
        //aRadarView.applicationInit();
        //aRadarView.setFlash(false);
		//for(int i=ApplicationService.RADAR_SITE_IDENTIFIERS.length - 3;i>-1;i--) {
		       //aProcessor.reduceRadarImages("/_radar_unprocessed_image/xbi/", FILTERED_DATA_DIR, true, true, true);//, aRadarView);
//		        aProcessor.reduceRadarImages(ApplicationService.RADAR_SITE_IDENTIFIERS[i], "/_radar_unprocessed_image_to_persist/", PreProcessor.FILTERED_DATA_DIR, view, true, true);//, aRadarView);
//		        aProcessor.reduceRadarImages(0, 3, "/Volumes/evo512d/_radar_unprocessed_image_to_persist/", "/Volumes/evo512bfat" + PreProcessor.FILTERED_DATA_DIR, view, true, true);//, aRadarView);
		        //aProcessor.reduceRadarImages(site, "/Volumes/" + drive + "/_radar_unprocessed_image_to_persist/", "/Volumes/" + drive + PreProcessor.FILTERED_DATA_DIR, view, true, true);//, aRadarView);
		       //aProcessor.reduceRadarImages(site, "/_radar_unprocessed_image_to_persist/", "" + PreProcessor.FILTERED_DATA_DIR, view, true, true);//, aRadarView);
		//} 	
	}

/*
// 20151107
protected void threadSafetyPrivate(int siteStart, int siteEnd, String anInputDir, String anOutputDir, boolean display, boolean persist, boolean file) {
    List<Thread> threadList = new ArrayList<Thread>();
    ApplicationService aService = null;
    for(int i=siteStart, i<= siteEnd, i++ ) {
     PreProcessor aProcessor = new PreProcessor(false);
      aProcessor = aProcessor.reduceRadarImages(ApplicationService.RADAR_SITE_IDENTIFIERS[i], "/Volumes/evo512d/_radar_unprocessed_image_to_persist/", "/Volumes/evo512bfat" + PreProcessor.FILTERED_DATA_DIR, view, true, true);//, aRadarView);
      threadList.add(new Thread(aService));
    }
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
}*/
}


/**
 * 20110801: 001: 
 * BufferedImage@71f6f0bf: type = 13 IndexColorModel: #pixelBits = 8 numComponents = 3 color space = java.awt.color.ICC_ColorSpace@b37c60d transparency = 1 transIndex   = -1 has alpha = false isAlphaPre = false ByteInterleavedRaster: width = 580 height = 480 #numDataElements 1 dataOff[0] = 0

 */ 