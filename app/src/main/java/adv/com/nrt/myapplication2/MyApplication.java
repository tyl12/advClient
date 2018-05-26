package adv.com.nrt.myapplication2;

import android.app.Application;
import android.content.Context;

/**
 * Created by S4-SKL on 2018/5/26.
 */

public class MyApplication extends Application {
    private static Context mContext;

    public static MyApplication instace;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        instace = this;
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static MyApplication getIntance() {
        return instace;
    }

}
