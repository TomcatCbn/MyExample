package lendorborrow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
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

/**
 * 异步消息侦听器
 * @author boning
 *
 */
public class QLender implements MessageListener {
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue requestQ = null;

	/**
	 * @param queuecf 主题连接工厂的名称
	 * @param requestQueue 队列的名称
	 */
	public QLender(String queuecf, String requestQueue) {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(queuecf);
			qConnect = qFactory.createQueueConnection();

			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			requestQ = (Queue) ctx.lookup(requestQueue);

			qConnect.start();

			QueueReceiver qReceiver = qSession.createReceiver(requestQ);
			qReceiver.setMessageListener(this);
			System.out.println("等待贷款申请！");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		try {
			boolean accepted = false;

			MapMessage msg = (MapMessage) message;
			double salary = msg.getDouble("Salary");
			double loanAmt = msg.getDouble("LoanAmount");

			//
			if (loanAmt < 200000) {
				accepted = (salary / loanAmt) > .25;
			} else {
				accepted = (salary / loanAmt) > .33;
			}
			System.out.println("" + "% = " + (salary / loanAmt) + ", loan is " + (accepted ? "Accepted!" : "Declined"));

			// J
			TextMessage tmsg = qSession.createTextMessage();
			tmsg.setText(accepted ? "Accepted!" : "Declined");
			//当消息消费者准备发送应答消息时，它将来自JMSCorrelationID消息属性设置为原始消息的消息ID
			tmsg.setJMSCorrelationID(message.getJMSMessageID());

			QueueSender qSender = qSession.createSender((Queue) message.getJMSReplyTo());
			qSender.send(tmsg);

			System.out.println("\nWaiting for loan requests ... ");
		} catch (JMSException e) {
			// TODO Auto-generated catch block
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
		String requestq = null;
		if (args.length == 2) {
			queuecf = args[0];
			requestq = args[1];
		} else {
			System.out.println("无效的参数");
			System.out.println("java QLender factory request_queue");
			System.exit(0);
		}
		QLender lender = new QLender(queuecf, requestq);

		try {
			BufferedReader st = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QLender application started");
			System.out.println("Press enter to quit application");
			st.readLine();
			lender.exit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
