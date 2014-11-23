package processing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class ProcessFile {
	public static void main(String[] args) throws InterruptedException,
			IOException, JSONException {

		ProcessFile pr = new ProcessFile();
		pr.checkSQS("korycki");

	}

	PropertiesCredentials getCredentials() throws IOException {
		PropertiesCredentials awsCredentials = new PropertiesCredentials(Thread
				.currentThread().getContextClassLoader()
				.getResourceAsStream("AwsCredentials.properties"));
		if (awsCredentials.getAWSAccessKeyId().equals("")
				|| awsCredentials.getAWSSecretKey().equals(""))
			throw new AmazonClientException("Empty key");

		return awsCredentials;
	}

	public void checkSQS(String sqsName) throws InterruptedException, IOException, JSONException {

		AmazonSQS sqs = new AmazonSQSClient(getCredentials());
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		 sqs.setRegion(usWest2);
		String url = sqs.createQueue(
				new CreateQueueRequest(sqsName))
				.getQueueUrl();

		while (true) {
			List<com.amazonaws.services.sqs.model.Message> msgs = sqs
					.receiveMessage(
							new ReceiveMessageRequest(url)
									.withMaxNumberOfMessages(1)).getMessages();

			if (msgs.size() > 0) {
				com.amazonaws.services.sqs.model.Message message = msgs.get(0);
				System.out.println("new message: " + message.getBody());

				process(message.getBody(),sqsName);

				sqs.deleteMessage(new DeleteMessageRequest(url, message
						.getReceiptHandle()));
			} else {
				System.out
						.println("  nothing found, trying again in 10 seconds");
				Thread.sleep(10000);
			}
		}

	}

	BufferedImage getBufferedImage(String bucket, String key)
			throws IOException, InterruptedException {
		// init
		AmazonS3 s3;
		s3 = new AmazonS3Client(getCredentials());

		// get object
		S3Object s3object = s3.getObject(bucket, key);
		System.out.println("image downloaded from s3");

		// s3objecto to file
		File file2 = new File("tmpFile.jpg");
		if(!file2.exists()){
			file2.createNewFile();
		}
		InputStream in = s3object.getObjectContent();
		byte[] buf = new byte[1024];
		OutputStream out = new FileOutputStream(file2);
		int count;
		while ((count = in.read(buf)) != -1) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			out.write(buf, 0, count);
		}
		out.close();
		in.close();

		// file to img
		File img = file2;
		BufferedImage in2 = ImageIO.read(img);
		return in2;
	}

	void saveBufferedImageToS3(BufferedImage bufferedImage, String bucket,
			String key) throws IOException {

		// bufferedImage to file
		File file3 = new File("file.jpg");
		ImageIO.write(bufferedImage, "jpg", file3);

		// init
		AmazonS3 s3;
		s3 = new AmazonS3Client(getCredentials());
		PutObjectRequest putObj=new PutObjectRequest(bucket, key,file3);

        //making the object Public
        putObj.setCannedAcl(CannedAccessControlList.PublicRead);

        s3.putObject(putObj);
		// put to s3
		//s3.putObject(bucket, key, file3);

		System.out.println("image was upload to s3");

	}

	// download, edit, upload to s3
	private void process(String message,String sqsName) throws IOException,
			InterruptedException, JSONException {
		
//1404040648013|test|sepia#10
		String bucket = "kkpbucket";
		String array[] =message.split("\\|");
		if(array.length<2)
			return;
		String inputFile=array[0];
		String outputFile=array[1];
		BufferedImage image = getBufferedImage(bucket, inputFile);
		if( image==null)
			return;
		for (int i = 2;i<array.length;i++){
			String operation=array[i];
			String additionalParameters[]=operation.split("#");
			int additionalParameter=10;
			if(additionalParameters.length>1){
				operation=additionalParameters[0];
				additionalParameter=Integer.parseInt(additionalParameters[1]);
			}
				if(operation.equalsIgnoreCase("sepia")){
					image = ImageEditor.toSepia(image,additionalParameter);
				}
				if(operation.equalsIgnoreCase("negatyw")){
					image = ImageEditor.negateImage(image);
				}
				if(operation.equalsIgnoreCase("jasnosc")){
					//image = ImageEditor.brightnessImage(image,additionalParameter);
					image = ImageEditor.changeBrightness(image,additionalParameter);
				}
				if(operation.equalsIgnoreCase("kontrast")){
					//image = ImageEditor.brightnessImage(image,additionalParameter);
					image = ImageEditor.changeContrast(image,additionalParameter);
				}
			
		}
		if( image==null)
			return;
		saveBufferedImageToS3(image, bucket, outputFile);
		
		

	}

}