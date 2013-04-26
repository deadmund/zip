package net.ednovak.zip;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class zipPreferences  extends PreferenceActivity{
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
