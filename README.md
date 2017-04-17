## Virtual Bluetooth Joystick

This is a very simple and straight-forward implementation that emulates Joystick functionality via Bluetooth.

Virtual Bluetooth Joystick turns your smartphone into a remote controller for automation or robotic applications. The custom joystick view uses a previous [Virtual Joystick project](https://github.com/teocci/VirtualJoystick).

### Disclaimer

This repository contains sample code intended to demonstrate the capabilities a simple custom `View`. It is not intended to be used as-is in applications as a library dependency, and will not be maintained as such. Bug fix contributions are welcome, but issues and feature requests will not be addressed.

### Specifications

The Communication Protocol is based on the [AndroTest V2.0 demo sketch](http://forum.arduino.cc/index
.php?topic=173246.msg1766646#msg1766646). Make sure that the Bluetooth serial board is already paired with your phone.

By default the **refresh rate** to get the data is **20/sec (every 50ms)**. If you want more or less just set the listener with a parameters to set the refresh rate in milliseconds. I the next example the value for `LOOP_INTERVAL` is 200ms.

```java
joystick.setOnMoveListener(new JoystickView.OnMoveListener() { ... }, LOOP_INTERVAL); // around 5/sec
```

### Contributing
If you would like to contribute code, you can do so through GitHub by forking the repository and sending a pull request.
When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

### Pre-requisites

* Android SDK 25
* Android Build Tools v25.0.2
* Android Support Repository

## License and third party libraries

The code supplied here is covered under the MIT Open Source License..