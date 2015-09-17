package kr.ac.khu.cram.cramcontroller;

import android.app.Activity;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

public class MainActivity extends Activity {

    public static final String TAG = "KRAMController";

    static {
        try {
            System.loadLibrary("arsal");
            System.loadLibrary("arsal_android");
            System.loadLibrary("arnetworkal");
            System.loadLibrary("arnetworkal_android");
            System.loadLibrary("arnetwork");
            System.loadLibrary("arnetwork_android");
            System.loadLibrary("arcommands");
            System.loadLibrary("arcommands_android");
            System.loadLibrary("arstream");
            System.loadLibrary("arstream_android");
            System.loadLibrary("json");
            System.loadLibrary("ardiscovery");
            System.loadLibrary("ardiscovery_android");
            System.loadLibrary("arutils");
            System.loadLibrary("arutils_android");
            System.loadLibrary("ardatatransfer");
            System.loadLibrary("ardatatransfer_android");
            System.loadLibrary("armedia");
            System.loadLibrary("armedia_android");
            System.loadLibrary("arcontroller");
            System.loadLibrary("arcontroller_android");
        } catch (Exception e) {
            Log.e(TAG, "Problem occured during native library loading", e);
        }
    }

    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;

    View.OnClickListener landingCtrlClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (R.id.btn_ctrl_landing == v.getId()) {
                if (((Button) v).getText() == getString(R.string.btn_landing))
                    ((Button) v).setText(R.string.btn_takeOff);
                else if (((Button) v).getText() == getString(R.string.btn_takeOff))
                    ((Button) v).setText(R.string.btn_landing);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.btn_ctrl_landing);
        btn.setOnClickListener(landingCtrlClickListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
