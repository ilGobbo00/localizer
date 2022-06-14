# Localizer

App developped for Embedded systems programming for the course hosted at University of Padua. The main aim is reading, storing and displaying locations read every T seconds (with T>=1s). Locations need to be read and stored even if the UI isn't displayed (so foreground service is required). The app doesn't need to read and store locations after reboot but it needs to be restarted in a consistent state. 

## Installation

To install the app download the [APK](https://github.com/ilGobbo00/localizer/blob/master/APK/Localizer.apk) and open it into eigther phisical or virtual device. You can also install the app downloading the source code and run it with Android Studio.

Android version target: API 29 (Android 10)

## Device requirements

1. Google play services 
2. Minimum Android version required: API 21 (Android 5.0) (**Suggested version target**)
3. 4.65'' minimum screen size (**Suggested 6.2''**)
4. A working MAPS_API_KEY into local.proprieties (***MAPS_API_KEY=<api_key>***)

## Flowchart

[App principle flowchart](https://github.com/ilGobbo00/localizer/blob/master/SimpleFlowchart.png)

## Devices used to test the app

- **Asus Zenfone 5z** (6.2'' 2240x1080) with **Android 10** (API 29)      <== ***Suggested***
- **Samsung Galaxy A5** (SMA500FU) (5'' 720x1280) with **Android 6.0** (API 23)  
- **Samsung Galaxy S Advance** (4'' 800x480) with **Android 5.1** (API 22) (screen too small)
- **Pixel 3** (virtual) (5.46'' 1080x2160) with **Android 10** (API 29)   <== ***Suggested***
- **Pixel** (virtual) (5'' 1080x1920) with **Android 5.1** (API 22)
- **Galaxy Nexsus** (virtual) (4.65'' 720x1280) with **Android 5.0** (API 21) 

## Logs

To display only the logs related to the app, in Logcat select *Verbose* and write in the search bar `Localizer/` and the letter after `/` indicates the fragment where log was generated.

## Screenshots

[Folder with examples](https://github.com/ilGobbo00/localizer/tree/master/Screenshots)
