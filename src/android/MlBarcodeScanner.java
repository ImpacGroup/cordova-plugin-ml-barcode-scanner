package de.impacgroup.mlbarcodescanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import de.impacgroup.mlbarcodescanner.module.CameraActivity;
import de.impacgroup.mlbarcodescanner.module.ScannerInfo;

public class MlBarcodeScanner extends CordovaPlugin {

    private CallbackContext presentCallbackContext;
    private ScannerInfo scannerInfo;
    private String title;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // LocalBroadcastManager.getInstance(cordova.getContext()).registerReceiver(receiver, new IntentFilter(ZoomImageActivity.ZOOM_IMAGE_STATE_ACTION));
    }

    @Override
    public void onDestroy() {
        if (cordova.getActivity().isFinishing()) {
            LocalBroadcastManager.getInstance(cordova.getContext()).unregisterReceiver(receiver);
        }
        super.onDestroy();

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        switch (action) {
            case "openScanner":
                presentCallbackContext = callbackContext;
                title = args.getString(0);
                openScanner();
                break;
            case "setInfoScreen":
                scannerInfo = new Gson().fromJson(args.getString(0), ScannerInfo.class);
                openScanner();
                break;
            default:
                break;
        }
        return super.execute(action, args, callbackContext);
    }

    private void openScanner() {
        if (canOpenScanner()) {
            AppCompatActivity activity = this.cordova.getActivity();
            activity.runOnUiThread(() -> {
                CameraActivity.Companion.startScanner(activity, title, scannerInfo);
            });
        }
    }

    private boolean canOpenScanner() {
        return title != null && scannerInfo != null;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }

        public void willClose() {
            if (presentCallbackContext != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, "willClose");
                result.setKeepCallback(true);
                presentCallbackContext.sendPluginResult(result);
            }
        }

        public void didClose() {
            if (presentCallbackContext != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, "didClose");
                presentCallbackContext.sendPluginResult(result);
                presentCallbackContext = null;
            }
        }
    };
}
