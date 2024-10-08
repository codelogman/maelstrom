package net.code.btalk;

import java.io.DataInputStream;

import net.code.btalk.ui.MessageScreen;
import net.rim.device.api.i18n.DateFormat;

public class Buddy {
	private static final long MAX_TIME_INTERVAL = 180000;

	public static BTalk btalk;

	private static DateFormat dateGen = DateFormat.getInstance(DateFormat.TIME_DEFAULT);
	public long lastTimeStampBuddy;

	public String jid;
	public String name;
	public int 	status;
	public String custom_str;
	public boolean unread;

	// is the last message from this buddy?
	public boolean lastFrom;
	public long lastTimeStampMe;

	//public MessageListField msgList;
	//public MessageRichTextField msgField;

	private MessageScreen msgScreen;

	public Buddy(String id, String n, int s) {
		if (n == null)
			this.name = id;
		else 
			this.name = n;
		this.jid = id;		
		this.status = s;
		this.lastFrom = false;
		this.lastTimeStampBuddy = 0;
		this.lastTimeStampMe = 0;
	}

	/*public MessageListField getMsgList() {
		if (msgList == null)
			msgList = new MessageListField();
		return msgList;
	}*/

	public void sendMessage(String msg) {
		if (lastFrom) {
			this.lastTimeStampMe = System.currentTimeMillis();
			msgScreen.sendMessage(msg, true, dateGen.formatLocal(this.lastTimeStampMe));
		} else {
			long curtime = System.currentTimeMillis();
			if ((curtime - this.lastTimeStampMe) > MAX_TIME_INTERVAL)
				msgScreen.sendMessage(msg, true, dateGen.formatLocal(curtime));
			else
				msgScreen.sendMessage(msg, false, null);

			this.lastTimeStampMe = curtime;

		}
		lastFrom = false;
	}

	private boolean isStream(String msg)
	{
		if(msg.startsWith("$_sf"))
			return true;
		else
			return false;
	}


	private boolean isFile(String msg)
	{
		if(msg.startsWith("$_st"))
			return true;
		else
			return false;
	}

	public void receiveMessage(String msg, boolean current, boolean isFile, String fileUrl) {		
		if(!isFile(msg) && !isStream(msg) && !isNotification(msg)){
			if (!lastFrom) {
				this.lastTimeStampBuddy = System.currentTimeMillis();
				this.getMsgScreen().receiveMessage(msg, current, true, dateGen.formatLocal(this.lastTimeStampBuddy));
			} else {
				long curTime = System.currentTimeMillis();
				if ((curTime - this.lastTimeStampBuddy) > MAX_TIME_INTERVAL) 
					msgScreen.receiveMessage(msg, current, true, dateGen.formatLocal(curTime));
				else
					msgScreen.receiveMessage(msg, current, false, null);

				this.lastTimeStampBuddy = curTime;
			}
		}else
		{
			if (!lastFrom) {
				if(!isNotification(msg)){
					this.getMsgScreen().receiveMessageFile(msg, fileUrl);
				}else
				{
					msgScreen.receiveMessage(msg, current, true, dateGen.formatLocal(System.currentTimeMillis()));
				}
			} else {
				if(!isNotification(msg))
				{
					msgScreen.receiveMessageFile(msg, fileUrl);
				}
				else
				{
					msgScreen.receiveMessage(msg, current, true, dateGen.formatLocal(System.currentTimeMillis()));
				}
			}		    
		}

		if(!isNotification(msg))
		{
			lastFrom= true;
		}
	}

	private boolean isNotification(String msg)
	{
		if(msg.startsWith("$_NT"))
			return true;
		else
			return false;
	}

	public MessageScreen getMsgScreen() {
		if (msgScreen == null)
			msgScreen = new MessageScreen(this);
		return msgScreen;
	}

	// status value
	public static final int STATUS_OFFLINE 	= 0x00000000;
	public static final int STATUS_AWAY 	= 0x00000001;
	public static final int STATUS_BUSY 	= 0x00000002;
	public static final int STATUS_ONLINE 	= 0x00000003;
	public static final int STATUS_INVOICE 	= 0x00000004;
}
