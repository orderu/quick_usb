import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:quick_usb/src/common.dart';

abstract class QuickUsbPlatform extends PlatformInterface {
  QuickUsbPlatform() : super(token: _token);

  static final Object _token = Object();

  static QuickUsbPlatform _instance;

  /// The default instance of [PathProviderPlatform] to use.
  ///
  /// Defaults to [MethodChannelPathProvider].
  static QuickUsbPlatform get instance => _instance;

  /// Platform-specific plugins should set this with their own platform-specific
  /// class that extends [PathProviderPlatform] when they register themselves.
  static set instance(QuickUsbPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool> init();

  Future<void> exit();

  Future<List<UsbDevice>> getDeviceList();

  Future<bool> hasPermission(UsbDevice usbDevice);

  Future<void> requestPermission(UsbDevice usbDevice);

  Future<bool> openDevice(UsbDevice usbDevice);

  Future<void> closeDevice(UsbDevice usbDevice);

  Future<UsbConfiguration> getConfiguration(int index);

  Future<bool> setConfiguration(UsbConfiguration config);
}
