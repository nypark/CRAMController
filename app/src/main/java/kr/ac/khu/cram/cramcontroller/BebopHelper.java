package kr.ac.khu.cram.cramcontroller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
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
import com.parrot.arsdk.arsal.ARSALPrint;

import java.util.List;

/**
 * Created by Na-yeon Park on 2015-09-18.
 */
public class BebopHelper implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    public static final String TAG = "KRAMController";
    public ServiceConnection mArdiscoveryServiceConnection = null;
    private Context mContext;
    private ARDiscoveryDevice arDevice;
    private ARDeviceController arController;
    private ARDiscoveryService mArdiscoveryService;
    private BroadcastReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;

    public BebopHelper(Context ctx) {
        mContext = ctx;
    }

    public ARDeviceController getArController() {
        return arController;
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

    public void registerReceivers() {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mContext);
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    @Override
    public void onServicesDevicesListUpdated() {
        Log.d(TAG, "onServicesDevicesListUpdated ...");

        if (mArdiscoveryService != null) {
            List<ARDiscoveryDeviceService> deviceList = mArdiscoveryService.getDeviceServicesArray();

            // Do what you want with the device list
            /*for (ARDiscoveryDeviceService device:deviceList) {
                Toast.makeText(mContext, device.getName(), Toast.LENGTH_SHORT).show();
            }*/
            if (deviceList.size() > 0) {
                try {
                    mArdiscoveryService.stop();

                    arDevice = createDiscoveryDevice(deviceList.get(0));
                    arController = new ARDeviceController(arDevice);
                    arController.start();

                    Toast.makeText(mContext, deviceList.get(0).getName(), Toast.LENGTH_SHORT).show();

                    ARCONTROLLER_ERROR_ENUM error = arController.start();
                    if (error != null)
                        Log.e(TAG, error.toString());

                } catch (ARControllerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service) {
        ARDiscoveryDevice device = null;
        if ((service != null) &&
                (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(ARDiscoveryService.getProductFromProductID(service.getProductID())))) {
            try {
                device = new ARDiscoveryDevice();

                ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();

                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());

            } catch (ARDiscoveryException e) {
                e.printStackTrace();
                Log.e(TAG, "Error: " + e.getError());
            }
        }

        return device;
    }

    //Clean Everything
    public void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mContext);

        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    public void closeServices() {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mArdiscoveryService.stop();

                    mContext.unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }

    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getPilotingState()
    {
        ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.eARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_UNKNOWN_ENUM_VALUE;
        if (arController != null)
        {
            try
            {
                ARControllerDictionary dict = arController.getCommandElements(ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED);
                if (dict != null)
                {
                    ARControllerArgumentDictionary<Object> args = dict.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                    if (args != null)
                    {
                        Integer flyingStateInt = (Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE);
                        flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue(flyingStateInt);
                    }
                }
            }
            catch (ARControllerException e)
            {
                e.printStackTrace();
            }

            return flyingState;
        }
        return null;
    }

    public ARCONTROLLER_ERROR_ENUM takeoff()
    {
        if (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED.equals(getPilotingState()))
            return arController.getFeatureARDrone3().sendPilotingTakeOff();

        return null;
    }

    public ARCONTROLLER_ERROR_ENUM land()
    {
        ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = getPilotingState();
        if (!ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDING.equals(flyingState) &&
                !ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED.equals(flyingState))
            return arController.getFeatureARDrone3().sendPilotingLanding();

        return arController.getFeatureARDrone3().sendPilotingLanding();
    }
}
