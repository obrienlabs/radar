package org.obrienscience.radar;

//import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
//import java.io.IOException;

//import javax.imageio.ImageIO;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.obrienscience.radar.integration.ApplicationService;
import org.obrienscience.radar.integration.SatService;

public class DuplicationProcessor extends ImageProcessor {

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

	private void copyFile(String src, String dest) {
		try {
		Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public void merge(String _site, String _aDir, String _bDir){//, String _cDir) {
        String aDir = _aDir + "/" + _site + "/";
        String bDir = _bDir + "/" + _site + "/";
        //String cDir = _cDir + "/" + _site + "/";
        String filename;
        long filesize;
        boolean overwriteIfLarger = false;
        // only overwrite radar images with the timestamp in the name
        if(_site.length()<4) {
        	overwriteIfLarger=true;
        }
        
        //verifyDirectory(outputDir);
        // copy all valid b to c based on a
        File bFileDir = new File(bDir);
        File[] files = bFileDir.listFiles();
        BufferedImage bFile = null;
        BufferedImage aFile = null;
        int count = 0;
        int countTotal = 0;
        ApplicationService service = new SatService();
        if(null != files && files.length > 0) {
            System.out.println("\n_files b: " + _site + " : " + files.length + "\n");
            for(int i=0; i<files.length; i++) {
            	count++; countTotal++;
            	if(count > 99) {
            		count = 0;
            		
            		System.out.println(" " +_site + ":" + countTotal);
            	}
            	// b file must be valid
                filename = files[i].getName();
                filesize = files[i].length();
                bFile = service.loadImage(bDir + filename, true);
                if(null != bFile && filesize > 0) {
            	// if a file exists - check a is valid, a is larger than b - then use b otherwise keep a
                	aFile = service.loadImage(aDir + filename, true);
                	if(null != aFile) {
                		File aFileFile = new File(aDir + filename);
                		// if a file is !0-length, and greater size - use it
                		if(null == aFileFile || aFileFile.length() == 0) {
                			System.out.print("B");
                			copyFile(bDir + filename, aDir + filename);
                		} else if(aFileFile.length() >= filesize) {
                			System.out.print("s");
                		} else {
                			if(overwriteIfLarger) {
                				System.out.print("o");
                				copyFile(bDir + filename, aDir + filename);	
                			} else {
                				System.out.print("D");
                				//copyFile(bDir + filename, aDir + filename);
                			}
                		}
                	} else {
                		System.out.print("M");
                		copyFile(bDir + filename, aDir + filename);
                	}
                } else {
                	System.out.print("-");
                }
            }
        }
	}
	
	/**
	 * Sort and delete all duplicate images
	 * @param site
	 */
	public void process(String site, String anInputDir) {//, String anOutputDir) {
        String inputDir = anInputDir + "/" + site + "/";//inputDrive + ":/" + anInputDir + "/" + site + "/";
        //String outputDir = anOutputDir + "/" + site + "/";//outputDrive +":/" + anOutputDir + "/" + site +"/";
        //verifyDirectory(outputDir);
        String filename = null;
        //BufferedImage image = null;
        String filenameRoot = null;
        //String outputPath = null;
        long filesize = 0;
        //BufferedImage verify = null;
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
                System.out.println("_files: " + site + " : " + files.length + "\n");
                for(int i=0; i<files.length; i++) {
                    filename = files[i].getName();
                    filesize = files[i].length();
                    filenameRoot = filename.substring(0, filename.length() - 4);
                    // check if a filtered image already exists - skip then
                    //outputPath = outputDir + filenameRoot;// + "_f";
                    //verify = service.loadImage(outputPath + ".jpg", true);
                    //if(null == verify) {              
                        input = service.loadImage(inputDir + filename);
                        if(null != input && filesize > 0) {
                        	// save and check for duplicates
                        	if(null == prev) {
                        		// set comparing image
                        		//writeImage(input, outputPath, "jpg");
                        		prev = input;
                        	} else {
                        		// need to solve interleaved out of order in ecan_2012_02_16_19_10_1070_100
                        		int diff = isDifferent(prev, input,10);
                        		if(diff < 1) {
                        			//System.out.println("Removing: " + filename);
                                    System.out.print("x");
                                    //deleteImage(outputPath,"jpg");
                                    files[i].delete();
                        		} else {
                        			if(diff < 10) {
                        				System.out.print("Z");
                        			} else {
                        				//writeImage(input, outputPath, "jpg");
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
                    /*} else {
                        // file exists
                        System.out.print(".");
                        counter = updateCounterPrivate(counter, i);
                    }*/
                }
            } else {
                System.out.println("_Directory is missing: " + inputDir);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }		
	}

	public static final String[] sitesSat = {"eusa","sigwx","wnam",//"xft","xsi",
		"gedisk11",//"wbi","wso","xfw","xsm",
		"gwdisk11",//"wbi_precip","wtp","xgo","xss",
		//"lightning/ON-GL",
		//"lightning/AC","lightning/ONT",
		//"lightning/ARC","lightning/PAC",
		//"lightning/ATL","lightning/Prairies",
		//"lightning/BC-AB","lightning/QU-AC",
		//"lightning/CLDN","lightning/QUE",
		//"lightning/WRN",
		//"lightning/NAT","lightning/YK-NWT",
		"wcan",//"wuj","xla","xti",
		"nam",//"wgj","wvy","xmb","xwl",
		"nat",//"whk","www","xme",
		//"atl","ont","whn","xam","xnc",
		"ecan",//"pnr","wkr","xbe","xni",
		//"pyr","wmb","xbu","xpg",
		"enam"/*"que","wmn","xdr","xra"*/};
	public static final String[] sites = {"eusa","sigwx","wnam","xft","xsi",
		"gedisk11","wbi","wso","xfw","xsm",
		"gwdisk11","wbi_precip","wtp","xgo","xss",
		"lightning/ON-GL",
		"lightning/AC","lightning/ONT",
		"lightning/ARC","lightning/PAC",
		"lightning/ATL","lightning/Prairies",
		"lightning/BC-AB","lightning/QU-AC",
		"lightning/CLDN","lightning/QUE",
		"lightning/WRN",
		"lightning/NAT","lightning/YK-NWT",
		"wcan","wuj","xla","xti",
		"nam","wgj","wvy","xmb","xwl",
		"nat","whk","www","xme",
		"atl","ont","whn","xam","xnc",
		"ecan","pnr","wkr","xbe","xni",
		"pyr","wmb","xbu","xpg",
		"enam","que","wmn","xdr","xra"};
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        String param = null;
        String site = "atl";
        if(null != args && args.length > 0) {
            param = args[0];
            if(null != param) {
                //inputDrive = param;
                site = param;
            }
        }
	
        DuplicationProcessor processor = new DuplicationProcessor();
        //site = "xwl";
        for(int i=0;i<sites.length;i++) {
        	processor.merge(sites[i], 
        		"/Volumes/8tb/_radar_unprocessed_image_to_persist", // to
//        		"/Volumes/ppro2/_radar_unprocessed_image_to_persist" // from
//        		"/Users/michaelobrien/_radar_unprocessed_image_to_persist", // new
//        		"/_radar_unprocessed_image_to_persist", // new
        		"/Volumes/Untitled/_to_process/biomos14/_radar_unprocessed_image_to_persist"
        	);
        }
        for(int i=0;i<sitesSat.length;i++) {
        ////processor.process(site,"/_radar_unprocessed_image_to_persist", "/Users/michaelobrien/_filtered_data/");
        //processor.process(sitesSat[i],"F:/_radar_unprocessed_image_to_persist");//, "/Volumes/gdrive2/_filtered_data/");
 //       processor.process(sitesSat[i],"/Users/michaelobrien/_radar_unprocessed_image_to_persist");//, "/Volumes/gdrive2/_filtered_data_mbp/");
        //processor.process(site,"/Volumes/Untitled/_to_process/biomos14/_radar_unprocessed_image_to_persist/", "/Volumes/8tb/_radar_unprocessed_image_to_persist/");

        }

	}


}
