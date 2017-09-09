package android.assignment3;
import android.app.Application;

import com.box.boxandroidlibv2.BoxAndroidClient;

public class BoxClient extends Application {

    public static final String CLIENT_ID = "fe82ew6hlhw82nh271j6cs5j7yoypabc";
    public static final String CLIENT_SECRET = "lbrv0Ct5wpWYhXig1QXahshiKRsa4Ldg";
    public static final String REDIRECT_URL = "http://localhost";
    
    private BoxAndroidClient mClient;

    public void setClient(BoxAndroidClient client) {
        this.mClient = client;
    }
    public BoxAndroidClient getClient() {
        return mClient;
    }
}