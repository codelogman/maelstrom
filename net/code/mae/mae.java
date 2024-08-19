package net.code.btalk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

import net.code.btalk.ui.BTalkLocale;
import net.code.btalk.ui.BuddyListField;
import net.code.btalk.ui.BuddyScreen;
import net.code.btalk.ui.FileSaveDialog;
import net.code.btalk.ui.IconLabelField;
import net.code.btalk.ui.LicensePopup;
import net.code.btalk.ui.LoginScreen;
import net.code.btalk.ui.MessageScreen;
import net.code.btalk.ui.MessageTextField;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.media.control.AudioPathControl;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngine;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Status;
import net.sourceforge.jxa.Jxa;
import net.sourceforge.jxa.XmppListener;

public class BTalk extends UiApplication implements XmppListener {

	public static final String VERSION = "0.5.175";
	public static final int REVISION = 180;

	public static final int SERVER_GTALK = 0;
	public static final int SERVER_CUSTOM = 1;

	public static final String GTALK_SERVER = "talk.google.com";
	public static final String GTALK_PORT = "5223";

	public Jxa jxa;
	private LoginScreen loginscreen;
	private String _jid;
	private String _password;
	public int serverType = SERVER_GTALK;
	public String _server = GTALK_SERVER;
	public String _port = GTALK_PORT;
	public boolean use_ssl = true;
	public boolean use_wifi = false;
	public int retryCount;

	//private String last_idx = "";	
	public BuddyListField buddyList;
	public BuddyScreen buddyscreen;

	private Timer mTimer = null;
	private  TimerTask mTimerTask = null;

	public InputStream bufferIStream;
	/** An OutputStream of the buffer */
	public ByteArrayOutputStream bufferOStream = new ByteArrayOutputStream();

	public Buddy currentBuddy;
	private int unreadCount;
	private byte[] unreadLock = new byte[0];

	// status values
	public Object stateLock;
	public int state;
	public int LastState = 15;

	private boolean isconnected = false;

	public boolean invalidate = false;

	public final static int STATE_STARTUP = 0x0;
	public final static int STATE_LOGINING = 0x1;
	public final static int STATE_ONLINE = 0x2;
	public final static int STATE_FAILED = 0x3;
	public final static int STATE_WAITING = 0x4;
	public final static int STATE_RETRYING = 0x5;
	public final static int STATE_BUSY = 0x6;
	public final static int STATE_AWAY = 0x7;
	public final static int STATE_RELOAD = 0x9;
	public final static int STATE_EXITING = 0x100;
	public String[] filenames = {"","",""};
	public int[] filesize = {0,0,0};
	public int filecount = 0;

	public volatile Player myPlayer;	

	public static void main(String[] args) {
		BTalk app = new BTalk();
		Notification.midStream = app.getClass().getResourceAsStream("/mid/Ringer_BBpro_2.mp3");
		Notification.midStream.mark(550);

		Notification.midStream_current = app.getClass().getResourceAsStream("/mid/Notifier_BBpro_6.mp3");
		Notification.midStream_current.mark(550);

		app.enterEventDispatcher();
	}

	public BTalk() {
		BuddyScreen.btalk = this;
		Buddy.btalk = this;
		MessageTextField.btalk = this;
		MessageScreen.btalk = this;
		LoginScreen.btalk = this;
		Notification.btalk = this;
		SavedData.btalk = this;

		if (SavedData.needReset()) {
			SavedData.resetData();
		} else {
			SavedData.readOptions();
		}	

		this.stateLock = new Object();
		this.state = BTalk.STATE_STARTUP;
		Vector up = SavedData.getUserInfo();
		if (up != null) {
			ServerDef serverDef = (ServerDef) up.elementAt(2);
			loginscreen = new LoginScreen(true, (String)up.elementAt(0), (String)up.elementAt(1),
					serverDef.serverType, serverDef.server, serverDef.usessl);
		} else {
			loginscreen = new LoginScreen(false, null, null, BTalk.SERVER_CUSTOM, null, true);
		}

		this.pushScreen(loginscreen);
		unreadCount = 0;
		this.retryCount = 0;
	}

	/*
	 * Only called by MessageTextField and currentBuddy mustn't be null
	 */
	public void sendMessage(final String msg) {

		if(this.isconnected)
		{
			(new Thread() {
				public void run() {
					jxa.sendMessage(currentBuddy.jid + "@" + _server, msg);
				}
			}).start();
		}else
		{
			invokeAndWait(new Runnable(){
				public void run()
				{
					try
					{
						jxa.logoff();
					}catch (NullPointerException e)
					{
						isconnected = false;
					}			

					state = BTalk.STATE_RETRYING;
					retryCount = 0;
					retryBtalk();
				}				
			});

			(new Thread() {
				public void run() {
					jxa.sendMessage(currentBuddy.jid + "@" + _server, msg);
				}
			}).start();
		}
		if((!isFile(msg)) && (!isStream(msg)))
			currentBuddy.sendMessage(msg);
	}	

	public void sendMessageFile(final String msg, final Buddy intended) {
		(new Thread() {
			public void run() {
				jxa.sendMessage(intended.jid + "@" + _server, msg);
			}
		}).start();
	}

	public void sendMessageVoice(final String msg, final Buddy intended) {
		(new Thread() {
			public void run() {
				jxa.sendMessage(intended.jid + "@" + _server, msg);
			}
		}).start();
	}

	public void sendCallRequest(final String msg, final String intended) {
		(new Thread() {
			public void run() {
				jxa.sendMessage(intended + "@" + _server, msg);
			}
		}).start();
	}

	public void sendCallSound(final String msg, final String intended) {
		(new Thread() {
			public void run() {
				jxa.sendMessage(intended + "@" + _server, msg);
			}
		}).start();
	}

	public void sendReaded(final String msg, final String intended) {
		(new Thread() {
			public void run() {
				jxa.sendMessage(intended + "@" + _server, msg);
			}
		}).start();
	}

	public void sendVoiceAlert(final String msg, final String intended) {
		(new Thread() {
			public void run() {
				jxa.sendMessage(intended + "@" + _server, msg);
			}
		}).start();
	}

	public void openBuddy(Buddy b) {
		if (currentBuddy != null && currentBuddy.getMsgScreen().isDisplayed()) {
			this.popScreen(currentBuddy.getMsgScreen());
		}		
		if (b.unread) {
			synchronized (unreadLock) {
				if(unreadCount > 0)
					--unreadCount;
				if (unreadCount == 0) {
					Notification.clearNotification();
				}
			}
			b.unread = false;
			//send read notification
			sendReaded("$_NT;confirmacion de lectura: ",b.name);
		}
		this.currentBuddy = b;
		//lock position
		//buddyList.buddyReposition(b);
		this.pushScreen(b.getMsgScreen());
	}

	public void switchBuddy(Buddy b) {
		if (currentBuddy == b) {
			return;
		} else if (currentBuddy != null && currentBuddy.getMsgScreen().isDisplayed()) {
			this.popScreen(currentBuddy.getMsgScreen());
			currentBuddy = null;
		}

		this.openBuddy(b);
	}

	// Only called by LoginScreen
	public void loginJxa(final String username, final String password, ServerDef serverDef) {
		if (this.state != BTalk.STATE_LOGINING) {
			this.state = BTalk.STATE_LOGINING;
			loginscreen.logginState.setText(BTalkLocale.getString(BTalkResource.LOGIN_SCREEN_LA_LOGIN_STATUS)+
					BTalkLocale.getString(BTalkResource.LOGIN_SCREEN_STATUS_LOGGING_IN));
			new String(username);

			this._password = password;
			this.serverType = serverDef.serverType;
			this._server = new String(serverDef.server);
			this._port = new String (serverDef.port);
			this.use_ssl = serverDef.usessl;
			this.use_wifi = serverDef.useWifi;

			if (username.indexOf('@') == -1) {
				this._jid = username + "@" + this._server;
			} else {
				this._jid = username;
			}									
			jxa = new Jxa(this._jid, this._password, "master", 10, this._server, this._port, this.use_ssl, this.use_wifi);
			jxa.addListener(this);
			jxa.start();
		}
	}

	public void setGenStatus(int s)
	{
		switch (s) {
		case 0:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.onlineIconSmall, "  Canal Asegurado");
			buddyscreen.statusBanner.setMargin(5, 0, 5, 0);
			buddyscreen.setTitle(buddyscreen.statusBanner);
			jxa.sendPresence(null, "available", null, null, 0);
			state = BTalk.STATE_ONLINE;
			LastState = 15;
			break;

		case 1:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.awayIconSmall, "  Ausente");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			jxa.setStatus("away", "in other things", 10);
			state = BTalk.STATE_AWAY;;
			LastState = 15;
			break;

		case 2:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.busyIconSmall, "  Ocupado");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			jxa.setStatus("dnd", "busy at the moment", 10);
			state = BTalk.STATE_BUSY;
			LastState = 15;
			break;

		default:
			break;
		}	    
	}

	public void setMyStatus(int s, boolean customText, String text) {
		switch (s) {
		case BTalk.STATE_ONLINE:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.onlineIconSmall, "  Canal Asegurado");
			buddyscreen.statusBanner.setMargin(5, 0, 5, 0);
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;

		case BTalk.STATE_WAITING:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.offlineIconSmall, " " + BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_STATUS_WAIT_RETRY_1) + 
					String.valueOf(SavedData.retryDelay) + 
					BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_STATUS_WAIT_RETRY_2));
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;

		case BTalk.STATE_RETRYING:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.offlineIconSmall, " " + BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_STATUS_RETRYING));
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;

		case BTalk.STATE_RELOAD:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.reloading, " " + BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_STATUS_RETRYING));
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;

		case BTalk.STATE_FAILED:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.offlineIconSmall, " " + BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_STATUS_OFFLINE));
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;

		case BTalk.STATE_BUSY:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.busyIconSmall, "  Ocupado");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			jxa.setStatus("dnd", "busy at the moment", 10);
			break;

		case BTalk.STATE_AWAY:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.awayIconSmall, "  Ausente");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			jxa.setStatus("away", "in other things", 10);
			break;

		default:
			break;
		}
		if (customText)
			buddyscreen.statusBanner.setText(text);		
	}

	public void subscribe(final String jid) {
		(new Thread() {
			public void run() {
				jxa.subscribe(jid + "@" + _server);
			}
		}).start();
	}

	public void unsubscribe(final String jid) {
		(new Thread() {
			public void run() {
				jxa.unsubscribe(jid + "@" + _server);
			}
		}).start();
	}

	public void logoffJxa() {
		(new Thread() {
			public void run() {
		//		jxa.logoff();
			//	synchronized (stateLock) {
					state = BTalk.STATE_EXITING;
				//}
				//Notification.clearNotification();
				System.exit(0);
			}
		}).start();
	}

	
	public void exitBtalk() {
		//if ((this.state == BTalk.STATE_ONLINE) || (this.state == BTalk.STATE_BUSY) ||
			//	(this.state == BTalk.STATE_AWAY) || (this.state == BTalk.STATE_FAILED)) {
			this.logoffJxa();
		//}		
	}

	private void authBtalkHandler() {
		if (this.state == BTalk.STATE_LOGINING) {
			if (loginscreen.saveField.getOnState()) { 
				SavedData.setUserInfo(this._jid.substring(0, this._jid.indexOf("@")), this._password, this.serverType, this._server, this.use_ssl);
			} else {
				SavedData.destroyUserInfo();
			}

			buddyList = new BuddyListField(this);
			buddyscreen = new BuddyScreen(buddyList);

			this.popScreen(loginscreen);
			this.pushScreen(buddyscreen);
			loginscreen = null;
		} else if (this.state == BTalk.STATE_RETRYING) {
			buddyscreen.longer = 0;

		}

		if(LastState != 15 && LastState != BTalk.STATE_FAILED)
		{		    
			UiApplication.getUiApplication().invokeLater(new Runnable() 
			{
				public void run()
				{
					state = LastState;
					setMyStatus(state, false, null);
				}
			}, 1000,false);
			setMyStatus(BTalk.STATE_RELOAD, true, "  Reiniciando ...");

			if (mTimerTask!=null){
				mTimerTask.cancel();
				mTimerTask = null;
			}
			if (mTimer!=null) {
				mTimer.cancel();
				mTimer = null;
			}

		}else
		{		    
			this.state = BTalk.STATE_ONLINE;   
			this.setMyStatus(state, false, null);		    
		}
		isconnected = true;
	}

	/*
	 * Xmpp event handlers
	 * @see net.sourceforge.jxa.XmppListener#onAuth(java.lang.String)
	 */
	public void onAuth(String resource) {
		this.invokeAndWait(new Runnable() {
			public void run() {
				authBtalkHandler();
			}
		});

		try {
			jxa.getRoster();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (SavedData.autoRetry) {
			if(mTimer == null)
			{
				//infinite loop after manual retry ...
				mTimer = new Timer();
				mTimer.scheduleAtFixedRate(mTimerTask = new TimerTask() {
					public void run() {
						invokeAndWait(new Runnable() {
							public void run() {
								if (!isconnected)
								{
									Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
									setMyStatus(BTalk.STATE_RETRYING, true, " Reiniciando canal ...");
									retryBtalk();
									buddyscreen.longer++;
								}
							}
						});
					}
				}, (SavedData.retryDelay + 60) * 1000, SavedData.retryDelay * 1000);
			}
		}
	}

	private void authFailedBtalkHandler(final String msg) {
		this.state = BTalk.STATE_FAILED;
		if(loginscreen.isDisplayed()){
			loginscreen.logginState.setText("fallo en conexion");
		}else
		{
			setMyStatus(BTalk.STATE_FAILED, true, " Fallo en autenticacion");
			isconnected = false;
		}
	}

	public void onAuthFailed(final String message) {
		this.jxa.removeListener(this);
		this.invokeAndWait(new Runnable() {
			public void run() {
				authFailedBtalkHandler(message);
			}
		});
		//this.invokeAndWait(new NotifyDialog(BTalkLocale.getString(BTalkResource.LOGIN_SCREEN_ALERT_UP_ERROR)));
	}

	public void retryBtalk() {
		jxa.removeListener(this);
		jxa = new Jxa(_jid, _password, "master", 10, this._server, this._port, this.use_ssl, this.use_wifi);
		jxa.addListener(this);
		jxa.setPriority(Thread.MIN_PRIORITY);
		jxa.start();		
	}

	private void connFailedBtalkHandler(final String msg) {

		if (this.state == BTalk.STATE_LOGINING) {
			this.state = BTalk.STATE_FAILED;
			loginscreen.logginState.setText(BTalkLocale.getString(BTalkResource.LOGIN_SCREEN_LA_LOGIN_STATUS) +
					BTalkLocale.getString(BTalkResource.LOGIN_SCREEN_ALERT_CONN_ERROR));
			return;
		}
		try
		{
			//buddyList.invalBuddies();
			jxa.removeListener(this);
			jxa.logoff();
		}catch (NullPointerException e)
		{
			isconnected = false;
			this.setMyStatus(BTalk.STATE_WAITING, true, " Problema con la conexion !");
		}			
		isconnected = false;
		invokeLater(new Runnable(){
			public void run()
			{
				try
				{
					jxa.logoff();
				}catch (NullPointerException e)
				{
					isconnected = false;
				}			

				state = BTalk.STATE_RETRYING;
				retryCount = 0;
				retryBtalk();
			}
		});
	}

	public void onConnFailed(final String msg) {

		try
		{
			this.jxa.removeListener(this);            
		}catch (NullPointerException e)
		{
		}
		this.invokeAndWait(new Runnable() {
			public void run() {
				connFailedBtalkHandler(msg);
			}
		});
	}

	//add buddy alias here UNLOCK
	private void contactBtalkHandler(final String jid, final String name, final String group, final String subscription) {
		if (subscription.equals("both") && buddyList.findBuddyIndex(jid.substring(0, jid.indexOf("@"))) == -1) {
			buddyList.addBuddy(new Buddy(jid.substring(0, jid.indexOf("@")), name, Buddy.STATUS_OFFLINE));
		}
	}

	public void onContactEvent(final String jid, final String name, final String group,
			final String subscription) {
		this.invokeLater(new Runnable() {
			public void run() {
				contactBtalkHandler(jid, name, group, subscription);
			}
		});
	}

	public void onContactOverEvent() {
		System.out.println("over");	
	}

	private boolean isFile(String msg)
	{
		if(msg.startsWith("$_st"))
			return true;
		else
			return false;
	}

	private boolean isVoice(String msg)
	{
		if(msg.startsWith("&_MK"))
			return true;
		else
			return false;
	}

	private boolean isStream(String msg)
	{
		if(msg.startsWith("$_sf"))
			return true;
		else
			return false;
	}

	private boolean isNotification(String msg)
	{
		if(msg.startsWith("$_NT"))
			return true;
		else
			return false;
	}

	public void sendVoice(final byte[] voice, final Buddy intended)
	{
		(new Thread() {
			public void run() {

				InputStream is = new ByteArrayInputStream(voice);

				byte[] buffer = new byte[512];
				int bytesRead = 0;
				int amount = 0;                           

				try {
					while ((bytesRead = is.read(buffer)) != -1)
					{
						amount += bytesRead;
						String msg = "";
						msg = encodeBase64(buffer,0,bytesRead);
						sendMessageVoice("&_MK[:" + msg + ":]", intended);
						//adjust the speed of transmission
						Thread.sleep(300);
					}
					sendMessageVoice("&_MK;MK_END", intended);

					is.close();
					is = null;

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}                           
			}
		}).start();
	}    

	private  void playTone(String path, boolean repeat)
	{
		try {
			InputStream is;
			is = UiApplication.getUiApplication().getClass().getResourceAsStream(path);
			myPlayer = javax.microedition.media.Manager.createPlayer(is, "audio/mpeg");
			myPlayer.realize();
			AudioPathControl  lPathCtr = (AudioPathControl) myPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
			lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
			if(repeat)
			{
				myPlayer.setLoopCount(-1);
			}
			VolumeControl volumen = (VolumeControl) myPlayer.getControl("VolumeControl");
			volumen.setLevel(100);
			myPlayer.prefetch();            
			myPlayer.start();   

		} catch (MediaException me) {
			System.out.println("Media/Tone Media error: " + me.toString());
		} catch (IOException ioe) {
			System.out.println("Media/Tone IO error: " + ioe.toString());
		} catch (Exception e) {
			System.out.println("Media/Tone error: " + e.toString());
		}
	}

	private  void playToneMP3(String path, boolean repeat)
	{
		try {
			InputStream is;
			is = UiApplication.getUiApplication().getClass().getResourceAsStream(path);
			myPlayer = javax.microedition.media.Manager.createPlayer(is, "audio/mp3");
			myPlayer.realize();
			AudioPathControl  lPathCtr = (AudioPathControl) myPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
			lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
			if(repeat)
			{
				myPlayer.setLoopCount(-1);
			}
			VolumeControl volumen = (VolumeControl) myPlayer.getControl("VolumeControl");
			volumen.setLevel(100);            
			myPlayer.prefetch();            
			myPlayer.start();            
		} catch (MediaException me) {
			System.out.println("Media/Tone Media error: " + me.toString());
		} catch (IOException ioe) {
			System.out.println("Media/Tone IO error: " + ioe.toString());
		} catch (Exception e) {
			System.out.println("Media/Tone error: " + e.toString());
		}
	}

	private  void playVoice(byte[] voice)
	{
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(voice);
			myPlayer = Manager.createPlayer(is, "audio/amr");

			myPlayer.realize();
			AudioPathControl  lPathCtr = (AudioPathControl) myPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
			//lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
			lPathCtr.setAudioPath(lPathCtr.getAudioPath());
			myPlayer.prefetch();            

			((VolumeControl)myPlayer.getControl("VolumeControl")).setLevel(100);

			myPlayer.start();
		} catch (MediaException me) {
			System.out.println("Media/Tone Media error: " + me.toString());
		} catch (IOException ioe) {
			System.out.println("Media/Tone IO error: " + ioe.toString());
		} catch (Exception e) {
			System.out.println("Media/Tone error: " + e.toString());
		}
	}

	private void messageBtalkHandler(final String from, final String body) {
		boolean isCurrentBuddy;
		try{
			if(!isVoice(body))
			{
				if(!isFile(body) && !isStream(body))
				{                   
					if (currentBuddy != null && currentBuddy.jid.equalsIgnoreCase(from.substring(0, from.indexOf("@")))) {
						isCurrentBuddy = true;
						currentBuddy.receiveMessage(body, true, false, "");
						requestForeground();
						Notification.newMessage(currentBuddy, body, isCurrentBuddy);
						//send receive notification
						//if(!isNotification(body) && !isVoice(body) && !isStream(body) && !isFile(body))
						//{
						//sendReaded("$_NT;confirmacion de lectura: ",currentBuddy.name);	        			
						//}
						// from other buddy
					} else {
						isCurrentBuddy = false;
						final int idx = buddyList.findBuddyIndex(from.substring(0, from.indexOf("@")));
						if (idx != -1) {
							final Buddy b = buddyList.getBuddyAt(idx);

							if (!b.unread) {
								synchronized (unreadLock) {
									++unreadCount;
								}
								b.unread = true;
							}
							if((!(body.indexOf("PING!") != -1)) && !isNotification(body))
							{
								UiApplication.getUiApplication().invokeLater(new Runnable() 
								{
									public void run()
									{
										playToneMP3("/mid/Notifier_LightSpeed.mp3", false);
									}
								}, 2000,false);
								b.receiveMessage(body, false, false, "");
								//lock
								//buddyList.buddyReposition(idx);		                        
								requestForeground();		                        
								Notification.newMessage(b, body, isCurrentBuddy);
							}else
							{
								b.unread = false;

								b.receiveMessage(body, false, false, "");

								this.invokeLater(new Runnable() {
									public void run() {
										statusBtalkHandler(from, "voice", "");
									}
								});
							}	                        	                        
						} else {
							//message from administrator
							Status.show(body);
							//System.out.println("[warning] Message from unkown buddy");
						}
					}
				}else
				{           
					if (currentBuddy != null && currentBuddy.jid.equalsIgnoreCase(from.substring(0, from.indexOf("@")))) {
						isCurrentBuddy = true;

						if(isStream(body))
						{
							//filesave dialog
							currentBuddy.receiveMessage(body, true, false, "");   
						}

						if(isFile(body))
						{
							String name = body.substring(body.indexOf("<:")+2, body.indexOf(":>"));
							name = name.substring(name.lastIndexOf('/') + 1);

							playTone("/mid/Notifier_Eager.mp3", false);

							String[] test = new String[] {from + " envia: [" + name + "] aceptar?"};
							LicensePopup popupDialog = new LicensePopup(test, "si", "no", 6, true);
							net.rim.device.api.ui.UiApplication.getUiApplication().pushGlobalScreen(popupDialog, 1, UiEngine.GLOBAL_MODAL);                                       

							if(popupDialog.isLicenseAccepted()) {
								if(filecount < 3)
								{
									FileSaveDialog dialog = new FileSaveDialog(name);
									if(dialog.doModal() != Dialog.CANCEL) {
										filecount++;                                
										currentBuddy.receiveMessage(body, true, true, dialog.getFileUrl());
										sendMessageFile("$_sf;<:" + body.substring(body.indexOf("<:")+2, body.indexOf(":>")) + ":>;ID_OK", currentBuddy);
										requestForeground();
									}else
									{
										sendMessageFile("$_sf;<:" + body.substring(body.indexOf("<:")+2, body.indexOf(":>")) + ":>;ID_CANCEL", currentBuddy);  
									}
								}else
								{
									try
									{
										Status.show("Maxima capacidad usada !");
									}catch(RuntimeException e)
									{
										// do nothing
									}
								}
							}else
							{
								sendMessageFile("$_sf;<:" + body.substring(body.indexOf("<:")+2, body.indexOf(":>")) + ":>;ID_CANCEL", currentBuddy);  
							}
						}
					}else
					{
						isCurrentBuddy = false;
						final int idx = buddyList.findBuddyIndex(from.substring(0, from.indexOf("@")));
						if (idx != -1) {
							final Buddy b = buddyList.getBuddyAt(idx);
							//init file
							if(isStream(body))
							{
								//filesave dialog
								b.receiveMessage(body, false, false, "");
								//lock
								//buddyList.buddyReposition(idx);
							}

							if(isFile(body))
							{
								String name = body.substring(body.indexOf("<:")+2, body.indexOf(":>"));
								name = name.substring(name.lastIndexOf('/') + 1);

								playTone("/mid/Notifier_Eager.mp3", false);
								String[] test = new String[] {from + " envia: [" + name + "] aceptar?"};
								LicensePopup popupDialog = new LicensePopup(test, "si", "no", 6, true);
								net.rim.device.api.ui.UiApplication.getUiApplication().pushGlobalScreen(popupDialog, 1, UiEngine.GLOBAL_MODAL);                                       

								if(popupDialog.isLicenseAccepted()) {
									if(filecount < 3)
									{
										FileSaveDialog dialog = new FileSaveDialog(name);
										if(dialog.doModal() != Dialog.CANCEL) {
											filecount++;                                                                        
											switchBuddy(b);
											sendMessageFile("$_sf;<:" + body.substring(body.indexOf("<:")+2, body.indexOf(":>")) + ":>;ID_OK", currentBuddy);
											b.receiveMessage(body, false, true, dialog.getFileUrl());
											//lock
											//buddyList.buddyReposition(idx);
											requestForeground();
										}else
										{
											sendMessageFile("$_sf;<:" + body.substring(body.indexOf("<:")+2, body.indexOf(":>")) + ":>;ID_CANCEL", currentBuddy);  
										}
									}else
									{
										try
										{
											Status.show("Maxima capacidad usada !");
										}catch(RuntimeException e)
										{
											// do nothing
										}
									}
								}else
								{
									sendMessageFile("$_sf;<:" + body.substring(body.indexOf("<:")+2, body.indexOf(":>")) + ":>;ID_CANCEL", currentBuddy);  
								}
							}
							//end file
						}
					}
				}//file else
			}// Voice
			else
			{
				if(!(body.indexOf("MK_END") != -1))
				{
					if(body.indexOf("MK_LRT") != -1)
					{
						requestForeground();
						playTone("/mid/alert.wav",false);
						Status.show("Alerta de " + from.substring(0, from.indexOf("@")));
					}else
					{
						String temp = body.substring(body.indexOf("[:") + 2, body.indexOf(":]"));

						try {
							bufferOStream.write(Base64InputStream.decode(temp));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}else
				{
					requestForeground();
					playTone("/mid/incoming.wav",false);
					this.invokeLater(new Runnable() {
						public void run() {
							playVoice(bufferOStream.toByteArray());
							Status.show("Mensaje de " + from.substring(0, from.indexOf("@")));
							bufferOStream.reset();
						}
					},1000,false);

					this.invokeLater(new Runnable() {
						public void run() {
							statusBtalkHandler(from, "voice", "");
						}
					});

				}
			}

		}catch(NullPointerException e)
		{
		}
	}

	public void onMessageEvent(final String from, final String body) {
		if (body.length() == 0)
			return;
		this.invokeLater(new Runnable() {
			public void run() {
				messageBtalkHandler(from, body);
			}
		});
	}

	private void setStatus()
	{
		for(int i=1; i <= buddyList.buddyVector.size();i++)
		{
			Buddy b;
			b = (Buddy)buddyList.buddyVector.elementAt(i);
		}
	}

	private void statusBtalkHandler(final String jid, final String show, final String status) {
		int idx = buddyList.findBuddyIndex(jid.substring(0, jid.indexOf("@")));
		Buddy b;

		if (idx != -1) {
			b = (Buddy)buddyList.buddyVector.elementAt(idx);
			int state = 0;
			if (show.equals(""))
				state = Buddy.STATUS_ONLINE;
			else if (show.equals("chat"))
				state = Buddy.STATUS_ONLINE;
			else if (show.equals("away"))
				state = Buddy.STATUS_AWAY;
			else if (show.equals("xa"))
				state = Buddy.STATUS_AWAY;
			else if (show.equals("dnd"))
				state = Buddy.STATUS_BUSY;
			else if (show.equals("na"))
				state = Buddy.STATUS_OFFLINE;
			else if (show.equals("voice"))
				state = Buddy.STATUS_INVOICE;

			b.custom_str = status;
			if (b.status == state)
				return;
			else {
				//lock buddies
				//state = Buddy.STATUS_ONLINE;
				b.status = state;
				buddyList.buddyReposition(idx);
			}
		} else {
		}
	}

	public void onStatusEvent(final String jid, final String show, final String status) {

		int idx = jid.indexOf('/');
		final String id;
		// in some instance, jid contains no '/', fix this
		if (idx == -1) {
			id = new String(jid);
		} else {
			id = jid.substring(0, jid.indexOf('/'));
		}

		this.invokeLater(new Runnable() {
			public void run() {
				statusBtalkHandler(id, show, status);
			}
		});
	}

	private void subscribeBtalkHandler(final String jid) {
		int rst = Dialog.ask("\""+jid+"\""+BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_REQUEST),
				new String[] {BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_REQUEST_AC), 
			BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_REQUEST_DENY), 
			BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_REQUEST_LATER)},
			new int[] {1, 2, 3}, 1);

		switch (rst) {
		case 1:
			(new Thread() {
				public void run() {
					jxa.subscribed(jid);
					jxa.subscribe(jid);					
				}
			}).start();

			invokeLater(new Runnable() 
			{
				public void run()
				{
					jxa.sendPresence(null, "available", null, null, 0);
				}
			}, 3000,false);
			return;
		case 2:
			(new Thread() {
				public void run() {
					jxa.unsubscribed(jid);
				}
			}).start();
			return;
		case 3:
			return;
		}
	}


	public void onSubscribeEvent(final String jid) {
		this.invokeLater(new Runnable() {
			public void run() {
				subscribeBtalkHandler(jid);
			}
		});	
	}

	public void onUnsubscribeEvent(final String jid) {
		this.invokeLater(new Runnable() {
			public void run() {
				Dialog.inform(jid+BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_REMOVED));
				buddyList.deleteBuddy(jid);
			}
		});
		jxa.unsubscribe(jid);		
	}

	//inicia el encoderBase64
	private String encodeBase64( byte[] toEncode, int offset, int length ) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(length);
		Base64OutputStream base64OutputStream = new Base64OutputStream( byteArrayOutputStream );
		try{
			base64OutputStream.write( toEncode, offset, length );
			base64OutputStream.flush();
			base64OutputStream.close();
		}
		catch (IOException ioe){
			System.out.println("Error in encodeBase64() : "+ioe.toString());
			return null;
		}
		return byteArrayOutputStream.toString();
	}//fn encodeBase64

}
