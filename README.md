# GPS Tie

This app can share a gps location to multiple smartphones in the same (wifi-)network who will be able to mock the received location a theirs using the location mocking feature (in the android developer settings).


## Tinkering

This app sends and receives the location as a stream of json objects. Since this app is under the MIT-License, you can easily receive the location or send custom ones using a simple client/server script/program.

More about this topic and a python example in [this gist](https://gist.github.com/LinusCDE/05b08d4b6246e89ebd4ffe1931b2b888).


## Libaries:
Used libraries:
- [Markwon-Library](https://github.com/noties/Markwon): Parsing the privacy policy of this app (needed for Google Play Store)
- [QRGen](https://github.com/kenglxn/QRGen): Generating QR Code to share address more easily

## FAQ:

**App X doesn't show the expected location**:
GPS mocking doesn't work with all apps because some are using Google's location services (e.g. Google Maps and Earth) which can't get mocked.

**App X demands that I turn my gps on**:
The app doesn't know that the location is beeing mocked. When turning on your gps the app will still use the location of the sending smartphone as long as your smartphone doesn't display the gps icon.

**App X started using my gps (gps icon shows up)**:
Sometimes the smartphone doesn't recognize the mocked location correct. First try restarting this and the problematic app. If this doesn't do the trick restart your smartphone.


## Screenshots
<img src="https://github.com/LinusCDE/GPSTie/blob/master/screenshots/App_Main.png" height=400>

<img src="https://github.com/LinusCDE/GPSTie/blob/master/screenshots/App_Receive.png" height=400>

<img src="https://github.com/LinusCDE/GPSTie/blob/master/screenshots/App_Send.png" height=400>
