package puborsub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * TLender类主要负责向一个主题发布新的抵押利率
 * 
 * @author boning
 *
 */
public class TLender {
	private TopicConnection connection = null;
	private TopicSession tSession = null;
	private Topic topic = null;

	public TLender(String topiccf, String topicName) {
		try {
			Context ctx = new InitialContext();
			TopicConnectionFactory qFactory = (TopicConnectionFactory) ctx.lookup(topiccf);
			connection = qFactory.createTopicConnection();

			tSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			topic = (Topic) ctx.lookup(topicName);

			connection.start();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void publishRate(double newRate){
		try {
			BytesMessage msg = tSession.createBytesMessage();
			msg.writeDouble(newRate);
			
			TopicPublisher publisher = tSession.createPublisher(topic);
			publisher.publish(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void exit(){
		try {
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void main(String[] args) {
		String topicCF = null;
		String topicName = null;
		if(args.length==2){
			topicCF=args[0];
			topicName=args[1];
		}else{
			System.out.println("无效的参数");
			System.exit(0);
		}
		
		TLender lender = new TLender(topicCF, topicName);
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("TLender application started");
			System.out.println("请输入利率，输入enter退出");
			System.out.println("\ne.g. 6.8");
			
			while(true){
				System.out.print("> ");
				String rate =reader.readLine();
				if(rate==null||rate.trim().length()<=0)
				{
					lender.exit();
				}
				
				double newRate = Double.valueOf(rate);
				lender.publishRate(newRate);
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
