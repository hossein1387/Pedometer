package android.assignment3;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Prefs extends PreferenceActivity implements OnSharedPreferenceChangeListener{
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	    //get XML preferences
		addPreferencesFromResource(R.xml.prefs);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
		String str;
		int samples=0;
		int goal=0;
		float input=0;
		str=arg0.getString("Goal", "400");
		try {
			goal = Integer.parseInt(str);			
		} 
		catch (NumberFormatException e)
		{
				Toast.makeText(getBaseContext(),str+ " Is not a number! Goal changed to it's default value(400)",Toast.LENGTH_LONG).show();
				arg0.edit().putString("Goal", "400");
				arg0.edit().apply();
				this.resetElementValue("Goal", "400");
				goal=400;
		}
		str=arg0.getString("NumberOfSamples", "2000");
		try 
		{
			samples = Integer.parseInt(str);
			
		} 
		catch (NumberFormatException e)
		{
				Toast.makeText(getBaseContext(),str+ " Is not a number! Number of samples changed to it's default value (2000)",Toast.LENGTH_LONG).show();
				arg0.edit().putString("NumberOfSamples", "2000");
				arg0.edit().apply();
				this.resetElementValue("NumberOfSamples", "2000");
		}
		finally
		{
			if (samples<500){
				arg0.edit().putString("NumberOfSamples", "2000");
				arg0.edit().apply();
				this.resetElementValue("NumberOfSamples", "2000");
				PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
				Toast.makeText(getBaseContext(), "The minimum sample rate is 500. Changed to its default value(2000)",Toast.LENGTH_LONG).show();
			}
		}
		str=arg0.getString("PeakThr", "2.00");
		try 
		{
			input = Float.parseFloat(str);
			
		} 
		catch (NumberFormatException e)
		{
				Toast.makeText(getBaseContext(),str+ " Is not a number! Threshold changed to it's default value(2.00)",Toast.LENGTH_LONG).show();
				arg0.edit().putString("PeakThr", "2.00");
				arg0.edit().apply();
				this.resetElementValue("PeakThr", "2.00");
				input=0;
		}
		finally
		{
			if (input>4||input<-4){
				arg0.edit().putString("PeakThr", "2.00");
				arg0.edit().apply();
				this.resetElementValue("PeakThr", "2.00");
				PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
				Toast.makeText(getBaseContext(), "Input number must be between +4 and -4. Threshold changed to it's default value(2.00)",Toast.LENGTH_LONG).show();
			}
		}

	}
    private void resetElementValue(String Key,String defaultVal) {
        // First get reference to edit-text view elements
        @SuppressWarnings("deprecation")
		EditTextPreference myPrefText = (EditTextPreference) super.findPreference(Key);
 
        myPrefText.setText(defaultVal); // Now, if you click on the item, you'll see the value you've just set here
    }
}
