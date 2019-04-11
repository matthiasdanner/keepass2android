package keepass2android.plugin.hibp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

import keepass2android.plugin.hibp.R;
import keepass2android.pluginsdk.KeepassDefs;
import keepass2android.pluginsdk.Kp2aControl;

/**
 * A placeholder fragment containing a simple view.
 */
public class HIBPActivityFragment extends Fragment implements HIBPClientDelegate {

    private ProgressBar progressBar;
    private View rootView;

    public HIBPActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        HashMap<String, String> entryOutput = Kp2aControl.getEntryFieldsFromIntent(getActivity().getIntent());
        String title = entryOutput.get(KeepassDefs.TitleField);
        String userName = entryOutput.get(KeepassDefs.UserNameField);
        View rootView = inflater.inflate(R.layout.fragment_hibp, container, false);
        TextView titleTextView = rootView.findViewById(R.id.titleTextView);
        titleTextView.setText(title);

        TextView userNameTitleTextView = rootView.findViewById(R.id.userNameTitleTextView);
        userNameTitleTextView.setText(R.string.user_name_title);

        TextView userNameTextView = rootView.findViewById(R.id.userNameTextView);
        userNameTextView.setText(userName);
        this.rootView = rootView;

        this.progressBar = rootView.findViewById(R.id.progressBar);

        HIBPClient client = new HIBPClient(entryOutput.get(KeepassDefs.PasswordField), this.getContext(), this);
        client.startCheckingPassword();
        return rootView;
    }


    @Override
    public void finishedPasswordCheck(boolean requestSuccess, boolean passwordSave) {
        if (this.progressBar != null) {
            this.progressBar.setVisibility(View.GONE);
        }
        if (this.rootView != null) {
            if (requestSuccess) {
                if (passwordSave) {
                    rootView.setBackgroundColor(getResources().getColor(R.color.green));
                } else {
                    rootView.setBackgroundColor(getResources().getColor(R.color.red));
                }
            } else {
                rootView.setBackgroundColor(getResources().getColor(R.color.yellow));
            }
        }
    }
}
