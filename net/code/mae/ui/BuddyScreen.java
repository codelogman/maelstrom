package net.code.btalk.ui;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import net.code.btalk.BTalk;
import net.code.btalk.BTalkResource;
import net.code.btalk.Buddy;
import net.code.btalk.UpdateChecker;
import net.rim.device.api.media.control.AudioPathControl;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.GPRSInfo;
import net.rim.device.api.system.KeyListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngine;
import net.rim.device.api.ui.XYEdges;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.DialogClosedListener;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.Status;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;
//version 6
import net.rim.device.api.ui.menu.SubMenu;

import com.samples.toolkit.ui.container.FieldSet;

public class BuddyScreen extends MainScreen{

	private static String IMEI = "";

	public static BTalk btalk;
	public IconLabelField statusBanner;
	public BuddyListField buddyList;

	private CallTalk talker;

	public int longer = 0;

	private MenuItem _status1 = new MenuItem(" conectado", 100, 1)
	{
		public void run()
		{
			btalk.setGenStatus(0);
		}
	};

	private MenuItem _status2 = new MenuItem(" ocupado", 200, 2)
	{
		public void run()
		{
			btalk.setGenStatus(2);
		}
	};

	private MenuItem _status3 = new MenuItem(" ausente", 300, 3)
	{
		public void run()
		{
			btalk.setGenStatus(1);
		}
	};

	private MenuItem _status4 = new MenuItem(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_MI_CHAT), 0, 0)
	{
		public void run()
		{
			int idx = buddyList.getSelectedIndex();
			if (idx >= 0) {
				btalk.currentBuddy = (Buddy) buddyList.buddyVector.elementAt(idx);
				btalk.openBuddy(btalk.currentBuddy);
			}
		}
	};

	private MenuItem _status5 = new MenuItem(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_MI_RETRY), 2, 0)
	{
		public void run()
		{
			btalk.state = BTalk.STATE_RETRYING;
			btalk.setMyStatus(BTalk.STATE_RETRYING, false, null);
			btalk.retryCount = 0;
			btalk.retryBtalk();
		}
	};

	private MenuItem _status13 = new MenuItem("Enviar Alerta", 1, 0)
	{
		public void run()
		{
			int idx = buddyList.getSelectedIndex();
			if (idx >= 0) {
				btalk.invalidate = true;
				btalk.currentBuddy = (Buddy) buddyList.buddyVector.elementAt(idx);
				btalk.sendVoiceAlert("&_MK;MK_LRT", btalk.currentBuddy.name);
				btalk.sendVoiceAlert("PING!", btalk.currentBuddy.name);
			}
		}
	};

	private MenuItem _status6 = new MenuItem(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_MI_BUDDY_INFO), 3, 0)
	{
		final String[] STATUS_STR = new String[] {BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_INFO_OFFLINE), 
				BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_INFO_AWAY),
				BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_INFO_BUSY),
				BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_INFO_AVA)};

		public void run()
		{
			if (buddyList.buddyVector.size() <= 0)
				return;
			Buddy b = buddyList.getBuddyAt(buddyList.getSelectedIndex());

			if (b == null)
				return;
			if (b.name.equalsIgnoreCase(b.jid)) {
				Dialog buddyInfoDialog = new Dialog(Dialog.D_OK, "ID: "+ b.jid+"\n"+STATUS_STR[b.status]+"\n",
						0, null, Screen.LEFTMOST);
				buddyInfoDialog.setEscapeEnabled(true);
				buddyInfoDialog.show();
			} else {
				Dialog buddyInfoDialog = new Dialog(Dialog.D_OK, "Nombre: "+b.name+"\nID: "+b.jid+"\n"+STATUS_STR[b.status]+"\n",
						0, null, Screen.LEFTMOST);
				buddyInfoDialog.setEscapeEnabled(true);
				buddyInfoDialog.show();
			}
		}
	};

	private MenuItem _update = new MenuItem("check updates", 6, 0)
	{
		public void run() {
			(new UpdateChecker(btalk.use_wifi)).start();
		}
	};

	private MenuItem _status7 = new MenuItem(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_MI_NEW_BUDDY), 4, 0)
	{
		public void run()
		{
			final EditField jidField;
			Dialog addBuddyDialog = new Dialog(Dialog.D_OK_CANCEL, BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_ADD_BUDDY_ID),
					0, null, Manager.USE_ALL_WIDTH);
			jidField = new EditField(EditField.NO_COMPLEX_INPUT | EditField.NO_NEWLINE);
			addBuddyDialog.add(jidField);
			addBuddyDialog.setDialogClosedListener(new DialogClosedListener() {
				public void dialogClosed(Dialog dialog, int choice) {
					switch (choice) {
					case 0:
						final String jid = jidField.getText();
						(new Thread() {
							public void run() {
								btalk.subscribe(jid);
							}
						}).start();
						return;
					case -1:
						return;
					default:
						return;
					}
				}
			});
			addBuddyDialog.show();
		}
	};


	private MenuItem _status8 = new MenuItem(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_MI_DELETE_BUDDY), 5, 0)
	{
		public void run()
		{
			if (buddyList.buddyVector.size() <= 0)
				return;
			final Buddy b = buddyList.getBuddyAt(buddyList.getSelectedIndex());
			String str;
			if (!b.name.equals(b.jid))
				str = b.name+"("+b.jid+")";
			else
				str = b.jid;
			int rst = Dialog.ask(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_POP_DELETE_BUDDY)
					+" \""+str+"\"?", new String[] {"Si", "No"}, new int[] {1, 2}, 2);

			switch (rst) {
			case 1:
				(new Thread() {
					public void run() {
						btalk.unsubscribe(b.jid);
					}
				}).start();
				buddyList.deleteBuddy(b.jid);
				return;
			case 2:
				return;
			}
		}
	};


	private MenuItem _status9 = new MenuItem(BTalkLocale.getString(BTalkResource.WHOLE_MI_OPTION), 6, 0)
	{
		public void run()
		{
			btalk.pushScreen(new OptionScreen());
		}
	};


	private MenuItem _status10 = new MenuItem(BTalkLocale.getString(BTalkResource.BUDDY_SCREEN_MI_EXIT), 8, 0)
	{
		public void run()
		{
			btalk.exitBtalk();
		}
	};

	private MenuItem _status11 = new MenuItem(BTalkLocale.getString(BTalkResource.WHOLE_MI_ABOUT), 7, 0)
	{
		public void run()
		{
			IMEI = GPRSInfo.imeiToString(GPRSInfo.getIMEI());
			String[] test = new String[] {"Maelstrom, © 2012 Alex Strange",
					"El producto contiene algoritmos de cifrado que pudieran ser prohibidos " +
					"por algunas legislaciones en algunas partes del mundo, Con la licencia " +
					"debe asegurarse que cuenta con los permisos de importacion/exportacion de cifrado" +
					"cuando redistribuye copias del producto, la licencia permite la distribucion de copias " +
					"con el cifrado estandar AES 256, Twofish, PGP Key para SSL, especialmente " +
					"cuando son exportadas.", "\n Producto certificado para:" + IMEI};
			LicensePopup popupDialog = new LicensePopup(test, "Aceptar", "Rechazar", 6, false);
			UiApplication.getUiApplication().pushGlobalScreen(popupDialog, 1, UiEngine.GLOBAL_MODAL);                                }
	};                


	public BuddyScreen(BuddyListField l) {
		super(NO_VERTICAL_SCROLL);
		statusBanner = new IconLabelField(BuddyListField.onlineIconSmall, "  Conectado");
		this.setTitle(statusBanner);

		ForegroundManager foreground = new ForegroundManager();

		Border titleBorder   = BorderFactory.createBitmapBorder( new XYEdges( 4, 12, 4, 12 ), Bitmap.getBitmapResource( "fieldset2_title_border.png" ) );
		Border contentBorder = BorderFactory.createBitmapBorder( new XYEdges( 4, 12, 4, 12 ), Bitmap.getBitmapResource( "fieldset2_body_border.png" ) );

		FieldSet set = new FieldSet( "lista de contactos privada", titleBorder, contentBorder, USE_ALL_WIDTH | VERTICAL_SCROLL);
		set.setFont(Constants.FONT_BOLD_SMALL_STYLE);
		set.setMargin( 4, 2, 4, 2 );

		this.buddyList = l;
		this.buddyList.setFont(Constants.FONT_BUDDY_PLAIN_STYLE);

		set.add(l);
		foreground.add( set );
		this.add(foreground);        
	}

	protected void makeMenu( Menu menu, int instance )
	{
		//version 6
		SubMenu statusSubMenu = new SubMenu(null,"Estatus",3,0);

		menu.add(_status4);
		menu.add(_status13);
		menu.add(_status5);
		menu.add(_status6);
		//statusSubMenu.add(_status1);
		//statusSubMenu.add(_status2);
		//statusSubMenu.add(_status3);
		//menu.add(statusSubMenu);
		menu.add(_update);
		menu.add(_status7);
		menu.add(_status8);
		menu.add(_status9);
		menu.add(_status10);                
		menu.add(_status11);
		super.makeMenu(menu, instance);
	};

	public boolean onClose() {
		/*if (btalk.state == BTalk.STATE_ONLINE ||
				btalk.state == BTalk.STATE_RETRYING ||
				btalk.state == BTalk.STATE_WAITING ||
				btalk.state == BTalk.STATE_BUSY ||
				btalk.state == BTalk.STATE_AWAY ||
				btalk.state == BTalk.STATE_FAILED) {*/
			btalk.requestBackground();
		/*} else {
			btalk.exitBtalk();
		}*/
		return true;
	}


	protected void onDisplay() {

	}

	protected void onUndisplay() {

	}

	public boolean keyUp(int key, int up){
		if(Keypad.KEY_CONVENIENCE_1 == Keypad.key(key))
		{			
			talker = new CallTalk("Capturando Audio ...");
			UiApplication.getUiApplication().pushGlobalScreen(talker, 1, UiEngine.GLOBAL_MODAL);

			if (buddyList.buddyVector.size() <= 0)
				return false;
			final Buddy b = buddyList.getBuddyAt(buddyList.getSelectedIndex());

			if (b == null)
				return false;


			if(talker._data != null)
			{
				UiApplication.getUiApplication().invokeLater(new Runnable()
				{	            
					public void run()
					{
						btalk.sendVoice(talker._data, b);
					}
				},1000,false);          
			}

			return true;
		}		
		return false;

	}


	public boolean pageDown(int amount, int status, int time) {
		return this.trackwheelRoll(amount, status, time);
	}


	public boolean pageUp(int amount, int status, int time) {
		return this.trackwheelRoll(amount, status, time);
	}

	private  void playTone(String path, boolean repeat)
	{
		try {
			InputStream is;
			Player myPlayer;
			is = UiApplication.getUiApplication().getClass().getResourceAsStream(path);
			myPlayer = javax.microedition.media.Manager.createPlayer(is, "audio/x-wav");
			myPlayer.realize();
			AudioPathControl  lPathCtr = (AudioPathControl) myPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
			lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
			if(repeat)
			{
				myPlayer.setLoopCount(-1);
			}
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

}
