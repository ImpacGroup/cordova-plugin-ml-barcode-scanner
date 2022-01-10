# README #

### An Cordova plugin for iOS and Android that allows to open a barcode scanner. ###

### Note ###
These plugin uses Google MLKit for detection of the barcode or QR-Code.

### Supported platforms ###
- Android 7+
- iOS 13+
- Cordova iOS 6+
- Cordova Android 10+

### Install ###

```
cordova plugin add https://github.com/ImpacGroup/cordova-plugin-ml-barcode-scanner

```

### Immplementation ###

For iOS you need to add this to your cordova config.xml

```
<edit-config file="*-Info.plist" mode="merge" target="NSCameraUsageDescription">
    <string>ENTER YOUR MESSAGE HERE</string>
</edit-config>

```

For android you need to make sure that koltin is acitvated in your cordova config.xml.
To these add the following:

```
    <preference name="GradlePluginKotlinEnabled" value="true" />
    <preference name="GradlePluginKotlinCodeStyle" value="official" />
    <preference name="GradlePluginKotlinVersion" value="1.6.0-RC" />
```

Before you can open the scanner you should setup the scanner. First you should setup the info screen. Therefor you can set a ScannerInfo object for default or if the user declined the camera permissions (iOS only).

```    
    // object definition for info. 
    export interface ScannerInfo {
        title: string;
        infoText: string;
        button: ScannerButton;
    }

    export interface ScannerButton {
        title: string;
        tintColor: string;
        backgroundColor: string;
        roundedCorners: boolean;
    }
    
    const button: ScannerButton = {
        title: translation.scanner.manuelImei,
        tintColor: "#FFFFFF",
        backgroundColor: "#FF0000",
        roundedCorners: true
    };
    const scannerInfo: ScannerInfo = {
        title: "Title,
        infoText: "Info text",
        button: button
    };
    
    // set info object
    window.plugins.barcodeScanner.setInfoScreen(JSON.stringify(scannerInfo), (error) => {
        console.log(error);
    });
```

