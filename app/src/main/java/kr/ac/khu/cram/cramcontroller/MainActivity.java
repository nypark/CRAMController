package kr.ac.khu.cram.cramcontroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;

public class MainActivity extends Activity {

    public static final String TAG = "KRAMController";

    // video vars
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private SurfaceView sfView;
    private MediaCodec mediaCodec;
    private Lock readyLock;
    private boolean isCodecConfigured = false;
    private ByteBuffer csdBuffer;
    private boolean waitForIFrame = true;
    private ByteBuffer [] buffers;

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

    public BebopHelper bHelper;
    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Button btn_this = (Button) v;

            switch (v.getId()) {
                case R.id.btn_ctrl_landing:
                    if ((btn_this).getText() == getString(R.string.btn_landing))
                        (btn_this).setText(R.string.btn_takeOff);
                    else if ((btn_this).getText() == getString(R.string.btn_takeOff))
                        (btn_this).setText(R.string.btn_landing);
                    break;
                case R.id.btn_server:
                   /* MyProgressDialog myProgressDialog;

                    myProgressDialog = new MyProgressDialog();
                    myProgressDialog.execute();*/
                    Toast.makeText(MainActivity.this, "Start server", Toast.LENGTH_SHORT).show();
                    new AsyncControllerGetter().execute();
                    break;
            }
        }
    };
    private ARDeviceController arController;
    private SurfaceView mSurfaceView;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_landing = (Button) findViewById(R.id.btn_ctrl_landing);
        btn_landing.setOnClickListener(btnClickListener);

        Button btn_server = (Button) findViewById(R.id.btn_server);
        btn_server.setOnClickListener(btnClickListener);

        bHelper = new BebopHelper(this);
        bHelper.registerReceivers();
        bHelper.initDiscoveryService();

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (bHelper.mArdiscoveryServiceConnection != null) {
            bHelper.unregisterReceivers();
            bHelper.closeServices();
        }
    }

    class AsyncControllerGetter extends AsyncTask<Void, Integer, Void> {
        boolean running;
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            running = true;

            progressDialog = ProgressDialog.show(MainActivity.this, "ProgressDialog", "Wait!");

            progressDialog.setCanceledOnTouchOutside(false);
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
            int runningNum = 5;

            while (runningNum != 0 || arController == null) {
                arController = bHelper.getArController();
                runningNum--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(MainActivity.this, "Progress Ended", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();

            arController.addListener(new ARDeviceControllerListener() {
                @Override
                public void onStateChanged(ARDeviceController arDeviceController, ARCONTROLLER_DEVICE_STATE_ENUM arcontroller_device_state_enum, ARCONTROLLER_ERROR_ENUM arcontroller_error_enum) {
                    final TextView deviceState = (TextView) findViewById(R.id.txt_deviceStatus);

                    switch (arcontroller_device_state_enum)

                    {
                        case ARCONTROLLER_DEVICE_STATE_RUNNING:
                            deviceState.setText("RUNNING");
                            break;
                        case ARCONTROLLER_DEVICE_STATE_STOPPED:
                            deviceState.setText("STOP");
                            break;
                        case ARCONTROLLER_DEVICE_STATE_STARTING:
                            deviceState.setText("STARTING");
                            break;
                        case ARCONTROLLER_DEVICE_STATE_STOPPING:
                            deviceState.setText("STOPPING");
                            break;

                        default:
                            deviceState.setText("STOP");
                            break;
                    }
                }

                @Override
                public void onCommandReceived(ARDeviceController arDeviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM arcontroller_dictionary_key_enum, ARControllerDictionary arControllerDictionary) {
                    if (arControllerDictionary != null) {
                        Log.i(TAG, "Enum : " + arcontroller_dictionary_key_enum);
                        // if the command received is a battery state changed
                        if (arcontroller_dictionary_key_enum == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) {
                            ARControllerArgumentDictionary<Object> args = arControllerDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                            if (args != null) {
                                Integer batValue = (Integer) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);


                                // do what you want with the battery level
                                Log.i(TAG, "Battery : " + batValue);
                            }
                        } else if (arcontroller_dictionary_key_enum == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_CURRENTTIMECHANGED) {
                            ARControllerArgumentDictionary<Object> args = arControllerDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                            if (args != null) {
                                String batValue = (String) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_CURRENTTIMECHANGED_TIME);


                                // do what you want with the battery level
                                Log.i(TAG, "Time : " + batValue);
                            }
                        }
                    } else {
                        Log.e(TAG, "elementDictionary is null");
                    }
                }
            });

            arController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte) 1);
            arController.addStreamListener(new ARDeviceControllerStreamListener() {
                @Override
                public void onFrameReceived(ARDeviceController arDeviceController, final ARFrame frame) {
                    if(frame != null && frame.isIFrame()){
                        byte[] tmpByteArray = frame.getByteData();
                        if(tmpByteArray != null && tmpByteArray.length > 0){
                            Bitmap bmp;
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inMutable = true;
                            bmp = BitmapFactory.decodeByteArray(tmpByteArray, 0, tmpByteArray.length, options);
                            mCanvas = new Canvas(bmp);
                            mSurfaceView.draw(mCanvas);
                        }
                    }
                }

                @Override
                public void onFrameTimeout(ARDeviceController arDeviceController) {

                }

            });
        }
    }
}
