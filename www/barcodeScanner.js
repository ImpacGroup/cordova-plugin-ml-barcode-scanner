

// Empty constructor
function MlBarcodeScanner() {}

MlBarcodeScanner.prototype.openScanner = function(title, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'MlBarcodeScanner', 'openScanner', [title]);
}

MlBarcodeScanner.prototype.validationResult = function(answer, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'validationResult', [answer]);
}

MlBarcodeScanner.prototype.setInfoScreen = function(info, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'setInfoScreen', [info]);
}

MlBarcodeScanner.prototype.setResultScreen = function(screen, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'setResultScreen', [info]);
}

MlBarcodeScanner.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.barcodeScanner = new MlBarcodeScanner();
    return window.plugins.barcodeScanner;
}
cordova.addConstructor(MlBarcodeScanner.install);
