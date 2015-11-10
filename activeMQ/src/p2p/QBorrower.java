package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author boning
 *
 */
public class QBorrower {
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue responseQ = null;
	private Queue requestQ = null;

	/**
	 * @param queuecf 主题连接工厂的名称
	 * @param requestQueue 队列的名称，将贷款申请发送给该队列
	 * @param responseQueue 队列名称，QBorrower使用该队列从QLender类中接受结果
	 */
	public QBorrower(String queuecf, String requestQueue, String responseQueue) {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(queuecf);
			qConnect = qFactory.createQueueConnection();//JMS提供者的连接

			// 创建JMS会话（事务，模式）
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			// 查找请求和响应队列
			requestQ = (Queue) ctx.lookup(requestQueue);
			responseQ = (Queue) ctx.lookup(responseQueue);

			qConnect.start();
			
			//可以获取有关于连接的元数据
			//qConnect.getMetaData();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void sendLoadRequest(double salary, double loanAmt) {
		// 创建JMS消息
		try {
			MapMessage msg = qSession.createMapMessage();
			msg.setDouble("Salary", salary);
			msg.setDouble("LoadAmount", loanAmt);
			msg.setJMSReplyTo(responseQ);

			// 创建发送者并发送消息
			QueueSender qSender = qSession.createSender(requestQ);//目的地，指定希望发送消息的队列
			qSender.send(msg);

			// 等待查看贷款申请被接受或拒绝
			/**
			 * 在创建QueueReceiver时，我们会指定过滤器，表明只有在JMSCorrealationID和原始的JMSMessageID相等时才会接受消息
			 */
			String filter = "JMSCorrelationID = '" + msg.getJMSMessageID() + "' ";
			QueueReceiver qReceiver = qSession.createReceiver(responseQ, filter);
			TextMessage tmsg = (TextMessage) qReceiver.receive(10000);
			if (tmsg == null) {
				System.out.println("贷款申请还未答复");
			} else {
				System.out.println("贷款申请 " + tmsg.getText());
			}
		} catch (JMSException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void exit() {
		try {
			qConnect.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void main(String[] args) {
		String queuecf = null;
		String requestq= null;
		String responseq = null;
		
		if(args.length == 3){
			queuecf=args[0];
			requestq=args[1];
			responseq=args[2];
		}else{
			System.out.println("无效的参数");
			System.exit(0);
		}
		
		QBorrower borrower = new QBorrower(queuecf, requestq, responseq);
		
		try {
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QBorrower Application Started");
			System.out.println("请输入：");
			System.out.println("Enter: Salary, Loadn_Amount");
			System.out.println("\ne.g. 50000, 120000");
			
			while(true){
				System.out.print("> ");
				String loanRequest =stdin.readLine();
				if(loanRequest == null || loanRequest.trim().length()<=0){
					borrower.exit();
				}
				
				//解析交易说明
				StringTokenizer st = new StringTokenizer(loanRequest, ",");
				double salary = Double.valueOf(st.nextToken().trim()).doubleValue();
				double loanAmt = Double.valueOf(st.nextToken().trim()).doubleValue();
				
				borrower.sendLoadRequest(salary, loanAmt);
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
