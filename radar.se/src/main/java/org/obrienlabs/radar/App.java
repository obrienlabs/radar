package org.obrienlabs.radar;

import java.io.File;

import org.springframework.util.FileCopyUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

/**
 * Hello world!
 *
 */
public class App {
	
	public void upload(String dir, String key, String s3Bucket) {
        AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());

        try {
        File file = new File(dir + "/" + key);
        s3Client.putObject(new PutObjectRequest(s3Bucket, key, file));
        } catch (AmazonServiceException ase) {
        System.out.println("Caught an AmazonServiceException, which " +
        		"means your request made it " +
                "to Amazon S3, but was rejected with an error response" +
                " for some reason.");
        System.out.println("Error Message:    " + ase.getMessage());
        System.out.println("HTTP Status Code: " + ase.getStatusCode());
        System.out.println("AWS Error Code:   " + ase.getErrorCode());
        System.out.println("Error Type:       " + ase.getErrorType());
        System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
        System.out.println("Caught an AmazonClientException, which " +
        		"means the client encountered " +
                "an internal error while trying to " +
                "communicate with S3, " +
                "such as not being able to access the network.");
        System.out.println("Error Message: " + ace.getMessage());
        }

	}
	
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        App app = new App();
        String bucket = "os-radar";
        String dir = "/Users/michaelobrien/_radar_unprocessed_image_to_persist/_heavy_rain";
        String file = "XFT_2012_06_03_17_00.GIF";
        app.upload(dir, file, bucket);
    }
}
