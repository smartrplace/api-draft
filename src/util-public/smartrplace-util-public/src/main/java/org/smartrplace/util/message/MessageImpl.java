package org.smartrplace.util.message;

import de.iwes.widgets.api.messaging.Message;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class MessageImpl implements Message {
	
	private final String title;
	private final String msg;
	private final String link = null;
	private final MessagePriority prio;
	
	public MessageImpl(String title,String msg,MessagePriority prio) {
		this.title = title;
		this.msg = msg;
		this.prio = prio;
	}

	@Override
	public String title(OgemaLocale locale) {
		return title;
	}

	@Override
	public String message(OgemaLocale locale) {
		return msg;
	}

	@Override
	public String link() {
		return link;
	}

	@Override
	public MessagePriority priority() {
		return prio;
	}

	
	
}
