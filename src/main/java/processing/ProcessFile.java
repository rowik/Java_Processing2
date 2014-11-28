package processing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
		int max_thread=10;
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
				List<Thread> t = new ArrayList<Thread>();
				for(int i=0;i<max_thread;i++){
					if(t.size()>=msgs.size())
						break;
					
					Thread tmp=new Thread( new MyThread(i, message,sqs,url));
					tmp.start();
					t.add(tmp);
				}
				for(Thread tt:t){
					if(!tt.isAlive())
						t.remove(tt);
				}

				
			} else {
				System.out
						.println("  nothing found, trying again in 10 seconds");
				Thread.sleep(1000);
			}
		}

	}


}