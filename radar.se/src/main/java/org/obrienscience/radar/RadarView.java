
package org.obrienscience.radar;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.obrienscience.radar.integration.ApplicationService;
import org.obrienscience.radar.integration.LiveRadarService;
import org.obrienscience.radar.integration.ResourceManager;
import org.obrienscience.radar.model.RadarSite;

// TODO: zoom sequence entity
public class RadarView extends AnimApplet2 {
    // There is only the need for one of the following statics
    private static final long serialVersionUID = 6502859074457924892L;
    /** Get number of (hyperthreaded + real) cores.  IE: p630 with HT=2, Core2 E8400=2 and Core i7-920 = 8 */
    public static final int CORES = 4;//Runtime.getRuntime().availableProcessors() << 0;
    public static List<Color> rgbColors;
    public static List<Color> currentColors;
    private int imageWidth =  478;
    private int imageHeight = 478;
    private BufferedImage bufferedImage;
    private ResourceManager resourceManager;
    private ApplicationService applicationService;
    
    public ApplicationService getApplicationService() {
		return applicationService;
	}


	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}
	private static List<Color> radarColors = new ArrayList<Color>(14);

	static {
        rgbColors = new ArrayList<Color>();
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(0,0,i));        }
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(0,i,0));        }
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(i,0,0));        }
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(0,i,i));        }
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(i,0,i));        }
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(i,i,0));        }
        for(int i=0;i<256;i++) {            rgbColors.add(new Color(i,i,i));        }
        currentColors = rgbColors;
        
        for(int i=0;i<14;i++) {       
            radarColors.add(new Color(RadarSite.PRECIP_INTENSITY_COLOR_CODES[RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1 - i])); 
            }
    }
	    
    private boolean flash = true;
    private long startTimestamp;
    
    
    private void displayImage(BufferedImage image, Graphics gContext, int filter) {//, boolean flash) {
        int pColor;
        boolean paint = true;
        if(null != image) {
            imageHeight = image.getHeight();
            imageWidth = image.getWidth();
            for(int y=0;y<imageHeight;y+=1) {            
                for(int x=0;x<imageWidth;x++) {
                    pColor = image.getRGB(x, y);
                    paint = true;
/*                    if(flash) {
                        // is a color within a range
                        if(pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[7] ||
                                pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[12] ||
                                pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[11] ||
                                pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[10] ||
                                pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[9] ||
                                pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[8] ) {
                            paint = false;
                        }
                    }*/
                    if(paint) {
                        gContext.setColor(new Color(pColor));
                        gContext.fillRect(x, y,1,1);
                    }
                    }
            }

            /*            int x = 524; 
            //int red, green, blue;
            for(int y=150;y<340;y+=14) {
                    pColor = image.getRGB(x, y);
                    gContext.setColor(new Color(pColor));
                    gContext.fillRect(x, y, 3,3);
                    //red = image.getColorModel().getRed(pColor);
                    //blue = image.getColorModel().getRed(pColor);
                    //green = image.getColorModel().getGreen(pColor);
                    //System.out.println("_index: " + y 
                     //       + " r:" + red + " g:" + green + " b:" + blue + " : " + pColor );
            } */           
        }
    }
    
        
    /**
     * This function is called by the Java2d framework
     */
    @Override
    public void paint(Graphics g) {
        if(flash) {
            flash = false;
        } else {
            flash = true;
        }
        g.drawImage(buffer, 0, 0, this);
        // clear previous image from buffer
        if (null != gContext) { // image may not be ready
            gContext.setColor(Color.white);
            gContext.fillRect(0, 0, imageWidth, imageHeight);
            BufferedImage image = applicationService.getCurrentImage();
            if(null != image) {
            	bufferedImage = image;
            }
            displayImage(bufferedImage, gContext, 1);//, flash);            
            // Let the Java2d display catch up and render the frame completely
            try { Thread.sleep(100); } catch (InterruptedException e) { // this will avoid running the host thread at 50% cpu
                showStatus(e.toString());
            }
            
        }

        repaint(); // display buffered image
    }

    @Override
    public void genericInit() {
        // setup images
    	//BufferedImage filteredImage;
        resourceManager = new ResourceManager();
        applicationService = new LiveRadarService();
        //String filename = "xft/XFT_2011_09_04_12_40";
        //bufferedImage = applicationService.loadImage(filename + ".gif");
        //for(int layer = 0; layer < 12; layer++) {
/*        	filteredImage = applicationService.getFilteredImage(bufferedImage, 12);//layer);
        	bufferedImage = filteredImage;
        	applicationService.getPreProcessor().writeImage(bufferedImage, filename+"_f", "gif");
        	try {
        		Thread.sleep(100);
        	} catch (Exception e) {}
        //}
  */      
        // start the timer
        startTimestamp = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        RadarView aRadarView = new RadarView();
        aRadarView.applicationInit();
    }
    
    public int getImageWidth() {        return imageWidth;    }
    public void setImageWidth(int imageWidth) {        this.imageWidth = imageWidth;    }
    public int getImageHeight() {        return imageHeight;    }
    public void setImageHeight(int imageHeight) {        this.imageHeight = imageHeight;    }
    public BufferedImage getBufferedImage() {        return bufferedImage;    }
    public void setBufferedImage(BufferedImage bufferedImage) {        this.bufferedImage = bufferedImage;    }
    public static List<Color> getCurrentColors() {      return currentColors;   }
    public static void setCurrentColors(List<Color> currentColors) {        RadarView.currentColors = currentColors;   }
    public boolean isFlash() {        return flash;    }
    public void setFlash(boolean flash) {        this.flash = flash;    }
}
