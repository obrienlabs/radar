package org.obrienscience.radar;

//import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
//import java.io.IOException;

//import javax.imageio.ImageIO;

import org.obrienscience.radar.integration.ApplicationService;
import org.obrienscience.radar.integration.SatService;

public class MergeProcessor extends ImageProcessor {

	@Override
	public void run() {
	}
	
	private void verifyDirectory(String aDir) {
		if(!(new File(aDir)).isDirectory()) {
			System.out.println("Missing report directory: " + aDir);
			File dir = new File(aDir);
			dir.mkdir();	        
			if(!(new File(aDir)).isDirectory()) {
				System.out.println("Cannot create report directory: " + aDir);
			}
		}
	}
	
	/**
	 * Sort and delete all duplicate images
	 * @param site
	 */
	public void process(String site, String anInputDir, String anOutputDir) {
        String inputDir = anInputDir + "/" + site + "/";//inputDrive + ":/" + anInputDir + "/" + site + "/";
        String outputDir = anOutputDir + "/" + site + "/";//outputDrive +":/" + anOutputDir + "/" + site +"/";
        verifyDirectory(outputDir);
        String filename = null;
        //BufferedImage image = null;
        String filenameRoot = null;
        String outputPath = null;
        long filesize = 0;
        BufferedImage verify = null;
        BufferedImage input = null;
        ApplicationService service = new SatService();
        // read everything in the directory
        File dir = new File(inputDir);
        BufferedImage prev = null;
        //BufferedImage next = null;
        try {
            File[] files = dir.listFiles();
            int counter = 0;
            //int lastMatch = -1;
            if(null != files && files.length > 0) {
                System.out.println("_files: " + files.length + "\n");
                for(int i=0; i<files.length; i++) {
                    filename = files[i].getName();
                    filesize = files[i].length();
                    filenameRoot = filename.substring(0, filename.length() - 4);
                    // check if a filtered image already exists - skip then
                    outputPath = outputDir + filenameRoot;// + "_f";
                    //outputPath = outputDir + site +"/" + filename;
                    verify = service.loadImage(outputPath + ".jpg", true);
                    if(null == verify) {              
                        input = service.loadImage(inputDir + filename);
                        if(null != input && filesize > 0) {
                        	// save and check for duplicates
                        	if(null == prev) {
                        		// set comparing image
                        		writeImage(input, outputPath, "jpg");
                        		prev = input;
                        		System.out.print("w");
                        	} else {
                        		// need to solve interleaved out of order in ecan_2012_02_16_19_10_1070_100
                        		int diff = isDifferent(prev, input,9000);
                        		if(diff < 1) {
                        			//System.out.println("Removing: " + filename);
                                    System.out.print("x");
                        		} else {
                        			if(diff < 9000) {
                        				System.out.print("X");
                        			} else {
                        				writeImage(input, outputPath, "jpg");
                        				prev = input;
                        				System.out.print("+");//diff + ",");
                        			}
                        		}
                                counter = updateCounterPrivate(site, counter, i);
                        		//
                        	}                       	
                        } else {
                            System.out.println("\n_Invalid filesize: " + filesize + " :" + filename);
                        }
                    } else {
                        // file exists
                        System.out.print(".");
                        counter = updateCounterPrivate(site, counter, i);
                    }
                }
            } else {
                System.out.println("_Directory is missing: " + inputDir);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        String param = null;
        String site = "ecan";
        if(null != args && args.length > 0) {
            param = args[0];
            if(null != param) {
                //inputDrive = param;
                site = param;
            }
        }
	
        MergeProcessor processor = new MergeProcessor();
        
        // don't use use DuplicationProcessor
        //processor.process(site,"/_radar_unprocessed_image_to_persist", "/Users/michaelobrien/_filtered_data/");
        processor.process(site,"/Volumes/Untitled/_to_process/biomos14/_radar_unprocessed_image_to_persist/", "/Volumes/8tb/_radar_unprocessed_image_to_persist/");
	}


}
