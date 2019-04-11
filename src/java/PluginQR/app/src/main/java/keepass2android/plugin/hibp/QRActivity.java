package keepass2android.plugin.hibp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import keepass2android.pluginsdk.KeepassDefs;
import keepass2android.pluginsdk.Kp2aControl;
import keepass2android.pluginsdk.Strings;

import android.animation.Animator;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.preference.PreferenceManager;

public class QRActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent() != null) && (getIntent().getStringExtra(Strings.EXTRA_ENTRY_OUTPUT_DATA)!= null))
			setContentView(R.layout.activity_qr);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.qr, menu);
		return true;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		
		// Hold a reference to the current animator,
	    // so that it can be canceled mid-way.
	    private Animator mCurrentAnimator;


		private int mShortAnimationDuration;
		
		Bitmap mBitmap;
		ImageView mImageView;
		TextView mErrorView;
		HashMap<String, String> mEntryOutput;
		
		//JSON-Array with field keys of the protected strings.
		//We don't need that list (so don't deserialize) other than for 
		//forwarding to KP2A
		String mProtectedFieldsList;
		
		ArrayList<String> mFieldList = new ArrayList<String>();
		Spinner mSpinner;
		String mHostname;

		private CheckBox mCbIncludeLabel;


		private Resources kp2aRes;

		public PlaceholderFragment() {
		}
		
		
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_qr, container,
					false);
			
			mSpinner = (Spinner) rootView.findViewById(R.id.spinner);
			
			mEntryOutput = Kp2aControl.getEntryFieldsFromIntent(getActivity().getIntent());

			HIBPClient client = new HIBPClient(mEntryOutput.get(KeepassDefs.PasswordField), this.getContext(), null);
			client.startCheckingPassword();


			ArrayList<String> spinnerItems = new ArrayList<String>();
			spinnerItems.add(getActivity().getString(R.string.all_fields));
			mFieldList.add(null); //all fields
			
			try {
				mHostname = getActivity().getIntent().getStringExtra(Strings.EXTRA_SENDER);
				kp2aRes = getActivity().getPackageManager().getResourcesForApplication(mHostname);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			addIfExists(KeepassDefs.UserNameField, "entry_user_name", spinnerItems);
			addIfExists(KeepassDefs.UrlField, "entry_url", spinnerItems);
			addIfExists(KeepassDefs.PasswordField, "entry_password", spinnerItems);
			addIfExists(KeepassDefs.TitleField, "entry_title", spinnerItems);
			addIfExists(KeepassDefs.NotesField, "entry_comment", spinnerItems);
			
			//add non-standard fields:
			ArrayList<String> allKeys = new ArrayList<String>(mEntryOutput.keySet());
			Collections.sort(allKeys);
			
			for (String k: allKeys)
			{
				if (!KeepassDefs.IsStandardField(k))
				{
					if (!TextUtils.isEmpty(mEntryOutput.get(k)))
					mFieldList.add(k);
					spinnerItems.add(k);
				}
			}
			
			mCbIncludeLabel = (CheckBox)rootView.findViewById(R.id.cbIncludeLabel);
			
			boolean includeLabel = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("includeLabels", false);
			mCbIncludeLabel.setChecked(includeLabel);
			mCbIncludeLabel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("includeLabels", isChecked);
					//updateQrCode(buildQrData(mFieldList.get( mSpinner.getSelectedItemPosition() )));
				}
			});
			
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, spinnerItems);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpinner.setAdapter(adapter);
			
			mImageView = ((ImageView)rootView.findViewById(R.id.qrView));
			mErrorView = ((TextView)rootView.findViewById(R.id.tvError));
			String fieldId = null;
			
			if (getActivity().getIntent() != null)
			{
				fieldId = getActivity().getIntent().getStringExtra(Strings.EXTRA_FIELD_ID);
				if (fieldId != null)
				{
					fieldId = fieldId.substring(Strings.PREFIX_STRING.length());
				}
			}

			mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					if  (arg2 != 0)
						mCbIncludeLabel.setVisibility(View.VISIBLE);
					else
						mCbIncludeLabel.setVisibility(View.GONE);
					//updateQrCode(buildQrData(mFieldList.get(arg2)));
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					
				}
			});
			
			mSpinner.setSelection(mFieldList.indexOf(fieldId));
			
			mShortAnimationDuration = getResources().getInteger(
	                android.R.integer.config_shortAnimTime);
			
		

			return rootView;
		}

		private void addIfExists(String fieldKey, String resKey,
				ArrayList<String> spinnerItems) {
			if (!TextUtils.isEmpty(mEntryOutput.get(fieldKey)))
			{
				mFieldList.add(fieldKey);
				String displayString = fieldKey;
				try
				{
					displayString = kp2aRes.getString(kp2aRes.getIdentifier(resKey, "string", mHostname));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				spinnerItems.add(displayString);
			}
			

		}

	}

}
