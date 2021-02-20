import 'dart:io';
import 'dart:typed_data';

import 'src/common.dart';
import 'src/quick_usb_android.dart';
import 'src/quick_usb_desktop.dart';
import 'src/quick_usb_platform_interface.dart';

export 'src/common.dart';

bool _manualDartRegistrationNeeded = true;

QuickUsbPlatform get _platform {
  // This is to manually endorse Dart implementations until automatic
  // registration of Dart plugins is implemented. For details see
  // https://github.com/flutter/flutter/issues/52267.
  if (_manualDartRegistrationNeeded) {
    // Only do the initial registration if it hasn't already been overridden
    // with a non-default instance.
    if (Platform.isAndroid) {
      QuickUsbPlatform.instance = QuickUsbAndroid();
    } else if (Platform.isWindows) {
      QuickUsbPlatform.instance = QuickUsbWindows();
    } else if (Platform.isMacOS) {
      QuickUsbPlatform.instance = QuickUsbMacos();
    } else if (Platform.isLinux) {
      QuickUsbPlatform.instance = QuickUsbLinux();
    }
    _manualDartRegistrationNeeded = false;
  }

  return QuickUsbPlatform.instance;
}

class QuickUsb {
  static Future<bool> init() => _platform.init();

  static Future<void> exit() => _platform.exit();

  static Future<List<UsbDevice>> getDeviceList() => _platform.getDeviceList();

  static Future<bool> hasPermission(UsbDevice usbDevice) =>
      _platform.hasPermission(usbDevice);

  static Future<void> requestPermission(UsbDevice usbDevice) =>
      _platform.requestPermission(usbDevice);

  static Future<bool> openDevice(UsbDevice usbDevice) =>
      _platform.openDevice(usbDevice);

  static Future<void> closeDevice() => _platform.closeDevice();

  static Future<UsbConfiguration> getConfiguration(int index) =>
      _platform.getConfiguration(index);

  static Future<bool> setConfiguration(UsbConfiguration config) =>
      _platform.setConfiguration(config);

  static Future<bool> claimInterface(UsbInterface intf) =>
      _platform.claimInterface(intf);

  static Future<bool> releaseInterface(UsbInterface intf) =>
      _platform.releaseInterface(intf);

  static Future<Uint8List> bulkTransferIn(
          UsbEndpoint endpoint, int maxLength) =>
      _platform.bulkTransferIn(endpoint, maxLength);

  static Future<int> bulkTransferOut(UsbEndpoint endpoint, Uint8List data) =>
      _platform.bulkTransferOut(endpoint, data);
}
