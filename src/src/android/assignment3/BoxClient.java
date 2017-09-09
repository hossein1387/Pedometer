package android.assignment3;
import android.app.Application;

import com.box.boxandroidlibv2.BoxAndroidClient;

public class BoxClient extends Application {

    public static final String CLIENT_ID = "CLIENT_ID";
    public static final String CLIENT_SECRET = "CLIENT_SECRET";
    public static final String REDIRECT_URL = "http://localhost";
    
    private BoxAndroidClient mClient;

    public void setClient(BoxAndroidClient client) {
        this.mClient = client;
    }
    public BoxAndroidClient getClient() {
        return mClient;
    }
}