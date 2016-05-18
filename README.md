# Experience Tracker for Android and Android Wear

Android Studio project for the Experience Tracker Android app.
Project is forked from: https://github.com/google/climb-tracker
and still has some unnecessary code from original to be cleaned and some refactoring to be done.

![climbtracker-architecture](https://cloud.githubusercontent.com/assets/360895/10863474/23b1d060-7fcf-11e5-9365-82b64b19ec14.png)

Uses Firebase as storage (with offline enabled) and authentication mechanism. The DataApi from the Wear SDK is used to send data from the wearable to the phone. Change Firebase URL on mobile modules string.xml as necessary.

Uses some Google services (at he moment too many, should be changed to use only necessary ones), so google-services.json should be obtained from Google and put in the mobile module folder.
