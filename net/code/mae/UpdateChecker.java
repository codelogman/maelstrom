package net.code.btalk;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import net.code.btalk.ui.BTalkLocale;
import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.DialogClosedListener;

public class UpdateChecker extends Thread {
	
	final static String LATEST_URL = "aHR0cDovL3NreXdlc3R4Lm5vaXAubWUvTEFURVNU";
	final static String CROSS_TL = "aHR0cDovL3NreXdlc3R4Lm5vaXAubWUvdXB0ZC9rZXlzdG9uZS5qYWQ=";
	public static Dialog popupDialog = null;
	
	String httpMask;
	
	public UpdateChecker(boolean use_wifi) {
		if (use_wifi) {
			this.httpMask = ";deviceside=true;interface=wifi";
		} else {
			this.httpMask = ";deviceside=true";
		}
	}
	
	public void run() {
		try {
			byte[] decoded2 = Base64InputStream.decode(LATEST_URL);
			HttpConnection conn = (HttpConnection) Connector.open((new String(decoded2))+this.httpMask);
			conn.setRequestMethod(HttpConnection.GET);
			InputStream in = conn.openInputStream();
			int len = (int)conn.getLength();
			byte[] result = new byte[len];
			in.read(result, 0, len);
			final String retver = new String(result);
			int newver = Integer.parseInt(retver);
			
			if (newver > BTalk.REVISION) {
				BTalk.getApplication().invokeLater(new Runnable() {
					public void run() {
						
					    popupDialog = new Dialog("Nueva version "+retver+" desea actualizar? \n",
					            new String[] {"Open", "Cancel"}, new int[] {1, 2}, 1, null);
					    popupDialog.setDialogClosedListener(new DialogClosedListener() {
					        public void dialogClosed(Dialog dialog, int choice) {
					            switch (choice) {
					            case 1:
					            	BrowserSession visita=Browser.getDefaultSession();
					            	try {
										final byte[] decoded = Base64InputStream.decode(CROSS_TL);
						            	visita.displayPage(new String(decoded));
						            	visita.showBrowser();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
					                break;
					            case 2:
					                break;
					            default:
					                break;
					            }
					        }
					    });
					    popupDialog.setEscapeEnabled(true);
					    popupDialog.show();
						
						
					}
				});
			} else {
				BTalk.getApplication().invokeLater(new Runnable() {
					public void run() {
						//Dialog.inform(BTalkLocale.locale[BTalkLocale.ITEM_NO_UPDATED][SavedData.lang]);
						Dialog.inform(BTalkLocale.getString(BTalkResource.WHOLE_POP_NO_UPDATE));
					}
				});
			}
			
		} catch (Exception e) {
			BTalk.getApplication().invokeLater(new Runnable() {
				public void run() {
					//Dialog.alert(BTalkLocale.locale[BTalkLocale.ITEM_UPDATE_FAILED][SavedData.lang]);
					Dialog.alert(BTalkLocale.getString(BTalkResource.WHOLE_POP_CHECK_UPDATE_FAIL));
				}
			});
		}
		
		
	}

}
