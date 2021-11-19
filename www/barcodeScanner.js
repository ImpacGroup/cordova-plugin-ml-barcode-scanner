

// Empty constructor
function MlBarcodeScanner() {}

MlBarcodeScanner.prototype.open = function(codes, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'MlBarcodeScanner', 'openScanner', [codes]);
}

MlBarcodeScanner.prototype.open = function(answer, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'validationResult', [answer]);
}

MlBarcodeScanner.prototype.setInfoScreen = function(info, errorCallback) {
    cordova.exec(null, errorCallback, 'MlBarcodeScanner', 'setInfoScreen', [info]);
}

MlBarcodeScanner.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.barcodeScanner = new MlBarcodeScanner();
    return window.plugins.barcodeScanner;
}
cordova.addConstructor(MlBarcodeScanner.install);
