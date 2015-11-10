package chatroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;

public class MyChat implements MessageListener {
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String username;

	public MyChat(String topicFactory, String topicName, String username) throws Exception {
		//获得JNDI连接使用jndi.properties
		InitialContext ctx = new InitialContext();

		//在消息传送服务器的命名服务中查找TopicConnectionFactory
		TopicConnectionFactory confactory = (TopicConnectionFactory) ctx.lookup(topicFactory);
		//表示和消息服务器的一个连接，代表一个TCP连接，
		TopicConnection connection = confactory.createTopicConnection();

		//TopicSession对象是用于创建Message、TopicPublisher和TopicSubscriber对象的工厂
		TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);//非事务，确认模式，自动确认
		TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		//查找jms主题
		Topic ChatTopic = (Topic) ctx.lookup(topicName);

		//jms发布者和订阅者
		TopicPublisher publisher = pubSession.createPublisher(ChatTopic);//确定了从TopicPublisher接收消息的主题
		TopicSubscriber subscriber = subSession.createSubscriber(ChatTopic, null, true);//确定了TopicSubscriber将从哪个主题中接收消息，消息选择器，自己不消费消息

		subscriber.setMessageListener(this);

		this.connection = connection;
		this.pubSession = pubSession;
		this.publisher = publisher;
		this.username = username;

		//建立完订阅者关系之后再启动连接
		connection.start();
	}

	@Override
	public void onMessage(Message message) {
		//消息头、属性和有效负载
		TextMessage textMessage = (TextMessage) message;
		System.out.println(textMessage.toString());
	}

	protected void writeMessage(String text) throws JMSException {
		TextMessage message = pubSession.createTextMessage();
		message.setText(username + ": " + text);
		publisher.publish(message);
	}

	public void close() throws JMSException {
		//关闭TpicConnection将关闭和该连接有关的所有对象，包括TopicSeesion、TopicPublisher和TopicSubscriber
		connection.close();
	}

	public static void main(String[] args) {
		try {
			if (args.length != 3)
				System.out.println("Factory, Topic, or username missing");
			MyChat chat = new MyChat(args[0], args[1], args[2]);

			BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				String s = commandLine.readLine();
				if (s.equalsIgnoreCase("exit")) {
					chat.close();
					System.exit(0);
				} else {
					chat.writeMessage(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
