

// Empty constructor
function MlBarcodeScanner() {}


MlBarcodeScanner.prototype.open = function(codes, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'MlBarcodeScanner', 'open', [codes]);
}

MlBarcodeScanner.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.barcodeScanner = new MlBarcodeScanner();
    return window.plugins.barcodeScanner;
}
cordova.addConstructor(MlBarcodeScanner.install);
