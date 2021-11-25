# README #

### An Cordova plugin for iOS and Android that allows to open a barcode scanner. For the detection the plugin uses Googles MLKit. ###

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


For android you need to make sure that koltin is acitvated in your cordova config.xml.
To these add the following:

```
    <preference name="GradlePluginKotlinEnabled" value="true" />
    <preference name="GradlePluginKotlinCodeStyle" value="official" />
    <preference name="GradlePluginKotlinVersion" value="1.6.0-RC" />
```

