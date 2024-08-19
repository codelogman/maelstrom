package net.code.btalk.ui;

import net.code.btalk.BTalkResource;
import net.code.btalk.SavedData;
import net.rim.device.api.i18n.Locale;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.i18n.ResourceBundleFamily;

public class BTalkLocale {
	
	private final static int [] LOCALE_MAP = {Locale.LOCALE_ROOT, Locale.LOCALE_ROOT};
	
	static ResourceBundleFamily _resBundleFamily = 
		ResourceBundle.getBundle(BTalkResource.BUNDLE_ID, BTalkResource.BUNDLE_NAME);
	static ResourceBundle _resource = _resBundleFamily.getBundle(Locale.get(LOCALE_MAP[0]));
	
	public static void setLocale(int localeCode) {
		_resource = _resBundleFamily.getBundle(Locale.get(LOCALE_MAP[localeCode]));
	}
	
	public static String getString(int key) {
		return _resource.getString(key);
	}	
}
