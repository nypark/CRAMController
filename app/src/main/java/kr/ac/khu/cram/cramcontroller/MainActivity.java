package kr.ac.khu.cram.cramcontroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Button btn_this = (Button)v;

            switch (v.getId()) {
                case R.id.btn_ctrl_landing :
                    if ((btn_this).getText() == getString(R.string.btn_landing))
                        (btn_this).setText(R.string.btn_takeOff);
                    else if ((btn_this).getText() == getString(R.string.btn_takeOff))
                        (btn_this).setText(R.string.btn_landing);
                    break;
                case R.id.btn_server :
                    MyProgressDialog myProgressDialog;

                    Toast.makeText(MainActivity.this, "Start server", Toast.LENGTH_SHORT).show();

                    myProgressDialog = new MyProgressDialog();
                    myProgressDialog.execute();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_landing = (Button) findViewById(R.id.btn_ctrl_landing);
        btn_landing.setOnClickListener(btnClickListener);

        Button btn_server = (Button) findViewById(R.id.btn_server);
        btn_server.setOnClickListener(btnClickListener);

        BebopHelper bHelper = new BebopHelper(this);

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

    class MyProgressDialog extends AsyncTask<Void, Integer, Void> {
        boolean running;
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            running = true;

            progressDialog = ProgressDialog.show(MainActivity.this, "ProgressDialog", "Wait!");

            progressDialog.setCanceledOnTouchOutside(true);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    running = false;
                }
            });

            Toast.makeText(MainActivity.this, "Progress Start", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int i=5;
            while(running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(i-- == 0)
                    running = false;

                publishProgress(i);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(String.valueOf(values[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(MainActivity.this, "Progress Ended", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }
}
