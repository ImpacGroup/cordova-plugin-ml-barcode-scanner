package de.impacgroup.mlbarcodescanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

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
import de.impacgroup.mlbarcodescanner.module.Code;
import de.impacgroup.mlbarcodescanner.module.ScannerInfo;

public class MlBarcodeScanner extends CordovaPlugin {

    private CallbackContext presentCallbackContext;
    private ScannerInfo scannerInfo;
    private String title;
    private Boolean isScannerOpen = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        LocalBroadcastManager.getInstance(cordova.getContext()).registerReceiver(receiver, new IntentFilter(CordovaScanner.CORDOVA_SCANNER_STATE_ACTION));
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
            case "setResult":
                Intent intent = new Intent(CordovaScanner.CORDOVA_SCANNER_VALIDATION_ANSWER_INTENT_KEY);
                intent.putExtra(CordovaScanner.CORDOVA_SCANNER_ANSWER_EXTRA_KEY, args.getString(0));
                LocalBroadcastManager.getInstance(cordova.getContext()).sendBroadcast(intent);
                break;
            default:
                break;
        }
        return super.execute(action, args, callbackContext);
    }

    private void openScanner() {
        if (canOpenScanner() && !isScannerOpen) {
            AppCompatActivity activity = this.cordova.getActivity();
            activity.runOnUiThread(() -> {
                Intent detailIntent = new Intent(activity, CordovaScanner.class);
                detailIntent.putExtra(CameraActivity.CAMERA_VIEW_TITLE, title);
                detailIntent.putExtra(CameraActivity.CAMERA_SCANNER_INFO, scannerInfo);
                activity.startActivity(detailIntent);
                isScannerOpen = true;
            });
        }
    }

    private boolean canOpenScanner() {
        return title != null && scannerInfo != null;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(CordovaScanner.CORDOVA_SCANNER_STATE_ACTION_KEY)) {
                String key = intent.getStringExtra(CordovaScanner.CORDOVA_SCANNER_STATE_ACTION_KEY);
                ScannerAction state = ScannerAction.Companion.getFor(key);
                switch (state) {
                    case IS_VALID:
                        String validationRequest = intent.getStringExtra(CordovaScanner.CORDOVA_SCANNER_VALIDATION_INTENT_KEY);
                        sendMessage(validationRequest);
                        break;
                    case RESULT_SCREEN:
                        String screenRequest = intent.getStringExtra(CordovaScanner.CORDOVA_SCANNER_RESULT_INTENT_KEY);
                        sendMessage(screenRequest);
                        break;
                    case DID_SELECT_CODE:
                        Code code = new Gson().fromJson(intent.getStringExtra(CordovaScanner.CORDOVA_SCANNER_PARAM_ACTION_KEY), Code.class);
                        ScannerMessage<Code> message = new ScannerMessage<>(ScannerAction.DID_SELECT_CODE.getKey(), code);
                        String jsonMsg = new Gson().toJson(message);
                        sendMessage(jsonMsg);
                        break;
                    case DID_CLOSE:
                        didClose();
                        break;
                    case WILL_CLOSE:
                        willClose();
                        break;
                    default: {
                        break;
                    }
                }
            }
        }

        public void willClose() {
            sendMessage(ScannerAction.WILL_CLOSE.getKey());
        }

        public void didClose() {
            sendMessage(ScannerAction.DID_CLOSE.getKey(), false);
        }
    };

    private void sendMessage(String msg) {
        sendMessage(msg, true);
    }

    private void sendMessage(String msg, Boolean keepCallback) {
        if (presentCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, msg);
            result.setKeepCallback(keepCallback);
            presentCallbackContext.sendPluginResult(result);
            if (!keepCallback) {
                presentCallbackContext = null;
            }
        }
    }
}
