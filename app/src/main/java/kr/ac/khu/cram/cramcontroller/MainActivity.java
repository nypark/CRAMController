package kr.ac.khu.cram.cramcontroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
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
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    public static final String TAG = "KRAMController";

    private ARDeviceController arController;
    private BebopHelper bHelper;

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
    private ByteBuffer[] buffers;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_landing = (Button) findViewById(R.id.btn_ctrl_landing);
        btn_landing.setOnClickListener(btnClickListener);

        Button btn_server = (Button) findViewById(R.id.btn_server);
        btn_server.setOnClickListener(btnClickListener);

        sfView = (SurfaceView) findViewById(R.id.surface_stream);

        bHelper = new BebopHelper(this);
        bHelper.registerReceivers();
        bHelper.initDiscoveryService();

        initVideoVars();
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
                    readyLock.lock();

                    if ((mediaCodec != null)) {
                        if (!isCodecConfigured && frame.isIFrame()) {
                            csdBuffer = getCSD(frame);
                            if (csdBuffer != null) {
                                configureMediaCodec();
                            }
                        }
                        if (isCodecConfigured && (!waitForIFrame || frame.isIFrame())) {
                            waitForIFrame = false;

                            // Here we have either a good PFrame, or an IFrame
                            int index = -1;

                            try {
                                index = mediaCodec.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
                            } catch (IllegalStateException e) {
                                Log.e(TAG, "Error while dequeue input buffer");
                            }
                            if (index >= 0) {
                                ByteBuffer b = buffers[index];
                                b.clear();
                                b.put(frame.getByteData(), 0, frame.getDataSize());
                                //ByteBufferDumper.dumpBufferStartEnd("PFRAME", b, 10, 4);
                                int flag = 0;
                                if (frame.isIFrame()) {
                                    //flag = MediaCodec.BUFFER_FLAG_SYNC_FRAME | MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
                                }

                                try {
                                    mediaCodec.queueInputBuffer(index, 0, frame.getDataSize(), 0, flag);
                                } catch (IllegalStateException e) {
                                    Log.e(TAG, "Error while queue input buffer");
                                }

                            } else {
                                waitForIFrame = true;
                            }
                        }

                        // Try to display previous frame
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex = -1;
                        try {
                            outIndex = mediaCodec.dequeueOutputBuffer(info, 0);

                            while (outIndex >= 0) {
                                mediaCodec.releaseOutputBuffer(outIndex, true);
                                outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Error while dequeue input buffer (outIndex)");
                        }
                    }


                    readyLock.unlock();
                }

                @Override
                public void onFrameTimeout(ARDeviceController arDeviceController) {

                }

            });
        }
    }


    //region video
    public void initVideoVars() {
        readyLock = new ReentrantLock();
        applySetupVideo();
    }

    private void applySetupVideo() {
        String deviceModel = Build.DEVICE;
        Log.d(TAG, "configuring HW video codec for device: [" + deviceModel + "]");
        sfView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        sfView.getHolder().addCallback(this);
    }

/*    public void reset() {
        *//* This will be run either before or after decoding a frame. *//*
        readyLock.lock();

        view.removeView(sfView);
        sfView = null;

        releaseMediaCodec();

        readyLock.unlock();
    }*/

    /**
     * Configure and start media codec
     *
     * @param type
     */
    private void initMediaCodec(String type) {
        mediaCodec = MediaCodec.createDecoderByType(type);

        if (csdBuffer != null) {
            configureMediaCodec();
        }
    }

    private void configureMediaCodec() {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setByteBuffer("csd-0", csdBuffer);

        mediaCodec.configure(format, sfView.getHolder().getSurface(), null, 0);
        mediaCodec.start();

        buffers = mediaCodec.getInputBuffers();

        isCodecConfigured = true;
    }

    private void releaseMediaCodec() {
        if ((mediaCodec != null) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            if (isCodecConfigured) {
                mediaCodec.stop();
                mediaCodec.release();
            }
            isCodecConfigured = false;
            mediaCodec = null;
        }
    }

    public ByteBuffer getCSD(ARFrame frame) {
        int spsSize = -1;
        if (frame.isIFrame()) {
            byte[] data = frame.getByteData();
            int searchIndex = 0;
            // we'll need to search the "00 00 00 01" pattern to find each header size
            // Search start at index 4 to avoid finding the SPS "00 00 00 01" tag
            for (searchIndex = 4; searchIndex <= frame.getDataSize() - 4; searchIndex++) {
                if (0 == data[searchIndex] &&
                        0 == data[searchIndex + 1] &&
                        0 == data[searchIndex + 2] &&
                        1 == data[searchIndex + 3]) {
                    break;  // PPS header found
                }
            }
            spsSize = searchIndex;

            // Search start at index 4 to avoid finding the PSS "00 00 00 01" tag
            for (searchIndex = spsSize + 4; searchIndex <= frame.getDataSize() - 4; searchIndex++) {
                if (0 == data[searchIndex] &&
                        0 == data[searchIndex + 1] &&
                        0 == data[searchIndex + 2] &&
                        1 == data[searchIndex + 3]) {
                    break;  // frame header found
                }
            }
            int csdSize = searchIndex;

            byte[] csdInfo = new byte[csdSize];
            System.arraycopy(data, 0, csdInfo, 0, csdSize);
            return ByteBuffer.wrap(csdInfo);
        }
        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        readyLock.lock();
        initMediaCodec(VIDEO_MIME_TYPE);
        readyLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        readyLock.lock();
        releaseMediaCodec();
        readyLock.unlock();
    }
    //endregion video

}
