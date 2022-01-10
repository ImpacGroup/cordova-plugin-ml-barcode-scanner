

// Empty constructor
function MlBarcodeScanner() {}

/**
Methode to start the Barcode Scanner. With the successCallback you get messages and requests of the scanner. Each requets and messages contains a action string to identify.
The following actions are available for messages: didSelectCode, preferredManuelInput, willClose, didClose
The following actions are available for requets: resultScreen, isValid
*/
MlBarcodeScanner.prototype.openScanner = function(title, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'MlBarcodeScanner', 'openScanner', [title]);
}

/**
The methode should be used to answer on a request if a given barcode is valid. You need to call this method at least 3 seconds after the you received the isValid requst.
*/
MlBarcodeScanner.prototype.validationResult = function(answer, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'validationResult', [answer]);
}

/**
Set the layout for the info screen at the bottom of the scanner.
*/
MlBarcodeScanner.prototype.setInfoScreen = function(info, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'setInfoScreen', [info]);
}

/**
The methode should be used to answer on a resultScreen request. With this method it can be defined how to display the result screen. You need to call this method at least 3 seconds after the you received the requst.
*/
MlBarcodeScanner.prototype.setResultScreen = function(screen, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'setResultScreen', [screen]);
}

/**
The method can be used to customize the message display to the customer if the camera permissions are missing.
*/
MlBarcodeScanner.prototype.setPermissionInfo = function(info, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'setPermissionInfo', [info]);
}

MlBarcodeScanner.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.barcodeScanner = new MlBarcodeScanner();
    return window.plugins.barcodeScanner;
}
cordova.addConstructor(MlBarcodeScanner.install);
