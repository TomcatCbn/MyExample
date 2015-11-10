package puborsub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * 作为利率主题的一个订阅者，它同样也是一个异步消息侦听器。
 * @author boning
 *
 */
public class TBorrower implements MessageListener {
private TopicConnection connection = null;
private TopicSession session = null;
private Topic topic = null;
private double currentRate ;

public TBorrower(String topiccf, String topicName, String rate) {
	try {
		currentRate = Double.valueOf(rate);
		
		Context ctx = new InitialContext();
		TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup(topiccf);
		connection = (TopicConnection) factory.createTopicConnection();
		
		session=connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		topic = (Topic) ctx.lookup(topicName);
		
		TopicSubscriber sub = session.createSubscriber(topic);
		sub.setMessageListener(this);
		
		connection.start();
		
		System.out.println("等待贷款申请");
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		System.exit(1);
	} 
}

@Override
public void onMessage(Message msg) {
	// TODO Auto-generated method stub
	try {
		BytesMessage message = (BytesMessage) msg;
		double newRate = message.readDouble();
		
		//如果该利率比当前利率至少低一个百分点，然后推荐再贷款
		if((currentRate - newRate)>=1.0){
			System.out.println("新利率= "+newRate+" - 考虑再贷款");
		}else{
			System.out.println("新利率= "+newRate+" - 保持当前的贷款");
		}
		System.out.println("\n等待新的利率发布");
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
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	System.exit(0);
}
public static void main(String[] args) {
	String topiccf = null;
	String topicName = null;
	String rate = null;
	if(args.length==3){
		topiccf=args[0];
		topicName=args[1];
		rate = args[2];
	}else{
		System.out.println("无效的参数");
		System.exit(0);
	}
	TBorrower borrower= new TBorrower(topiccf, topicName, rate);
	
	try {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("TBorrower application started");
		System.out.println("Press entry to quit");
		in.readLine();
		borrower.exit();
	} catch (IOException e) {
		e.printStackTrace();
	}
}
}
