package org.obrienscience.radar.business.concurrency;

import java.awt.image.BufferedImage;
import java.util.concurrent.RecursiveAction;

import org.obrienscience.radar.model.RadarSite;

public class ForkFilter extends RecursiveAction {
	private static final long serialVersionUID = 6154712948423703919L;
	
	private BufferedImage mSource;
	private BufferedImage mDestination;
    //private int[] mSource;
    private int mStart;
    private int mLength;
    //private int[] mDestination;
    protected static int sThreshold = 120;//30;
    private int height;
    private int width;
  
    public ForkFilter(BufferedImage src, int start, int length, BufferedImage dst, int ht, int wd) {
        mSource = src;
        mStart = start;
        mLength = length;
        mDestination = dst;
        height = ht;
        width = wd;
    }

    protected void computeDirectly() {
        //boolean match = false;
    	int pColor;
    	int mEnd = mStart + mLength;
    	for (int index = mStart; index < mEnd; index++) {
        //for(int y=0;y<mStart;y++) {
            for(int x=0;x<width;x++) {
                pColor = mSource.getRGB(x, index);
                doFilterType(mDestination, x, index, pColor);
                // filter based on subset of intensities
                //match = false;
            }
        }
        /*
        int sidePixels = (mBlurWidth - 1) / 2;
        for (int index = mStart; index < mStart + mLength; index++) {
            // Calculate average.
            float rt = 0, gt = 0, bt = 0;
            for (int mi = -sidePixels; mi <= sidePixels; mi++) {
                int mindex = Math.min(Math.max(mi + index, 0),
                                    mSource.length - 1);
                int pixel = mSource[mindex];
                rt += (float)((pixel & 0x00ff0000) >> 16)
                      / mBlurWidth;
                gt += (float)((pixel & 0x0000ff00) >>  8)
                      / mBlurWidth;
                bt += (float)((pixel & 0x000000ff) >>  0)
                      / mBlurWidth;
            }
          
            // Re-assemble destination pixel.
            int dpixel = (0xff000000     ) |
                   (((int)rt) << 16) |
                   (((int)gt) <<  8) |
                   (((int)bt) <<  0);
            mDestination[index] = dpixel;
        }*/
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
        	// TODO: USE NON-SYNCHRONIZED METHOD
            input.setRGB(x, y, pColor);
        }
    }    

    @Override
	protected void compute() {
    	// base case
	    if (mLength < sThreshold) {
	        computeDirectly();
	        return;
	    }
	    
	    int split = mLength / 2;
	    // recursive case
	    invokeAll(new ForkFilter(mSource, mStart, split, mDestination, height, width),
	              new ForkFilter(mSource, mStart + split, mLength - split,mDestination, height, width));
		
	}
}
