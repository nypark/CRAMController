package kr.ac.khu.cram.cramcontroller;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

/**
 * Created by Na-yeon Park on 2015-09-18.
 */
public class BebopHelper implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    public static final String TAG = "KRAMController";

    private Context mContext;
    private ARDiscoveryDevice arDevice;
    private ARDeviceController arController;

    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;
    private BroadcastReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;

    public BebopHelper(Context ctx){
        mContext = ctx;
    }

    public void initDiscoveryService() {
        // create the service connection
        if (mArdiscoveryServiceConnection == null) {
            mArdiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(mContext, ARDiscoveryService.class);
            mContext.bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    private void startDiscovery() {
        if (mArdiscoveryService != null) {
            mArdiscoveryService.start();
        }
    }

    public void registerReceivers()
    {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mContext);
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    @Override
    public void onServicesDevicesListUpdated()
    {
        Log.d(TAG, "onServicesDevicesListUpdated ...");

        if (mArdiscoveryService != null)
        {
            List<ARDiscoveryDeviceService> deviceList = mArdiscoveryService.getDeviceServicesArray();

            // Do what you want with the device list
            /*for (ARDiscoveryDeviceService device:deviceList) {
                Toast.makeText(mContext, device.getName(), Toast.LENGTH_SHORT).show();
            }*/
            if(deviceList.size() > 0) {
                try {
                    mArdiscoveryService.stop();

                    arDevice = createDiscoveryDevice(deviceList.get(0));
                    arController = new ARDeviceController(arDevice);
                    arController.start();

                    Toast.makeText(mContext, deviceList.get(0).getName(), Toast.LENGTH_SHORT).show();

                    arController.addListener(new ARDeviceControllerListener() {
                        @Override
                        public void onStateChanged(ARDeviceController arDeviceController, ARCONTROLLER_DEVICE_STATE_ENUM arcontroller_device_state_enum, ARCONTROLLER_ERROR_ENUM arcontroller_error_enum) {
                            TextView deviceState = (TextView) ((MainActivity) mContext).findViewById(R.id.txt_deviceStatus);

                            switch (arcontroller_device_state_enum) {
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
                                        String batValue = (String)args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_CURRENTTIMECHANGED_TIME);


                                        // do what you want with the battery level
                                        Log.i(TAG, "Time : " + batValue);
                                    }
                                }
                            } else {
                                Log.e(TAG, "elementDictionary is null");
                            }
                        }
                    });

                    arController.addStreamListener(new ARDeviceControllerStreamListener() {
                        @Override
                        public void onFrameReceived(ARDeviceController arDeviceController, ARFrame arFrame) {
                            Log.i(TAG, "arFrame Size : " + arFrame.getDataSize());
                        }

                        @Override
                        public void onFrameTimeout(ARDeviceController arDeviceController) {

                        }
                    });

                    ARCONTROLLER_ERROR_ENUM error = arController.start();
                    if(error != null)
                        Log.e(TAG, error.toString());

                } catch (ARControllerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service)
    {
        ARDiscoveryDevice device = null;
        if ((service != null) &&
                (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(ARDiscoveryService.getProductFromProductID(service.getProductID()))))
        {
            try
            {
                device = new ARDiscoveryDevice();

                ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();

                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());

            }
            catch (ARDiscoveryException e)
            {
                e.printStackTrace();
                Log.e(TAG, "Error: " + e.getError());
            }
        }

        return device;
    }

    //Clean Everything
    private void unregisterReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mContext);

        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    private void closeServices()
    {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null)
        {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    mArdiscoveryService.stop();

                    mContext.unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }
}
