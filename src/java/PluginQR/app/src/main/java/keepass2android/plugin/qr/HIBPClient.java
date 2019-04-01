package keepass2android.plugin.qr;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

class HIBPClient {

    private final String hashedPassword;
    private final Context context;

    HIBPClient(String password, Context context) {
        this.hashedPassword = HIBPClient.getHashedPassword(password);
        this.context = context;
    }

    private static String getHashedPassword(String password) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return String.format("%040X", new BigInteger(1, digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void loadPasswordHashes() {
        RequestQueue queue = Volley.newRequestQueue(this.context);
        String url = "https://api.pwnedpasswords.com/range/" + this.hashedPassword.substring(0, 5);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                HIBPClient.this::checkPassword, error -> HIBPClient.this.checkPassword(null));
        queue.add(stringRequest);
    }

    void startCheckingPassword() {
        this.loadPasswordHashes();
    }

    private void checkPassword(String response) {
        if (response != null) {
            for (String line: response.split("\r\n")) {
                String[] passwordInfo = line.split(":");
                if (passwordInfo.length >= 2) {
                    if (this.hashedPassword.endsWith(passwordInfo[0])) {
                        Logger.getLogger(this.toString()).log(INFO, line);
                    }
                }
            }
        }
    }


}
