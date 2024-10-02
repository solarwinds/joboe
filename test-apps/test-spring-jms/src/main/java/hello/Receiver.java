package hello;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {

	@JmsListener(destination = "mailbox", containerFactory = "myFactory")
	public void receiveMessage(Email email) {
		System.out.println("[mailbox] Received <" + email + ">");
	}

	@JmsListener(destination = "junk", containerFactory = "myFactory")
	public void receiveJunk(Email email) {
		System.out.println("[junk] Received <" + email + ">");
	}
}
