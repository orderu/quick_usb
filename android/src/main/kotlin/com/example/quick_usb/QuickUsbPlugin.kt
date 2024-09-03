package com.example.quick_usb

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.*
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

// private const val ACTION_USB_PERMISSION = "com.example.quick_usb.USB_PERMISSION"

class QuickUsbPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.quick_usb.USB_PERMISSION"
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "quick_usb")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            context.registerReceiver(
//                usbPermissionReceiver,
//                IntentFilter(ACTION_USB_PERMISSION),
//                Context.RECEIVER_NOT_EXPORTED,
//            )
//        } else {
//            context.registerReceiver(usbPermissionReceiver, IntentFilter(ACTION_USB_PERMISSION))
//        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getDeviceList" -> {
                val usbDeviceList = usbManager.deviceList.entries.map {
                    mapOf(
                        "identifier" to it.key,
                        "vendorId" to it.value.vendorId,
                        "productId" to it.value.productId,
                        "configurationCount" to it.value.configurationCount,
                        "manufacturer" to it.value.manufacturerName,
                        "product" to it.value.productName,
                        "serialNumber" to if (usbManager.hasPermission(it.value)) it.value.serialNumber else null,
                    )
                }
                result.success(usbDeviceList)
            }

            "getDeviceDescription" -> {
                val identifier = call.argument<Map<String, Any>>("device")!!["identifier"]!!;
                val device = usbManager.deviceList[identifier] ?: return result.error(
                    "IllegalState",
                    "usbDevice null",
                    null
                )
                result.success(
                    mapOf(
                        "identifier" to identifier,
                        "vendorId" to device.vendorId,
                        "productId" to device.productId,
                        "configurationCount" to device.configurationCount,
                        "manufacturer" to device.manufacturerName,
                        "product" to device.productName,
                        "serialNumber" to if (usbManager.hasPermission(device)) device.serialNumber else null,
                    )
                )
            }

            "hasPermission" -> {
                val identifier = call.argument<String>("identifier")
                val device = usbManager.deviceList[identifier]
                result.success(usbManager.hasPermission(device))
            }

            "requestPermission" -> {
                val identifier = call.argument<String>("identifier")
                val device = usbManager.deviceList[identifier] ?: return result.error(
                    "IllegalState",
                    "usbDevice null",
                    null
                )
                if (usbManager.hasPermission(device)) {
                    result.success(true)
                } else {
                    val intent = PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE
                    );
                    usbManager.requestPermission(device, intent)
                    result.success(false)
                }
            }

            "openDevice" -> {
                val identifier = call.argument<String>("identifier")
                usbDevice = usbManager.deviceList[identifier]
                usbDeviceConnection = usbManager.openDevice(usbDevice)
                result.success(true)
            }

            "closeDevice" -> {
                usbDeviceConnection?.close()
                usbDeviceConnection = null
                usbDevice = null
                result.success(null)
            }

            "getConfiguration" -> {
                val device =
                    usbDevice ?: return result.error("IllegalState", "usbDevice null", null)
                val index = call.argument<Int>("index")!!
                val configuration = device.getConfiguration(index)
                val map = configuration.toMap() + ("index" to index)
                result.success(map)
            }

            "setConfiguration" -> {
                val device =
                    usbDevice ?: return result.error("IllegalState", "usbDevice null", null)
                val connection = usbDeviceConnection ?: return result.error(
                    "IllegalState",
                    "usbDeviceConnection null",
                    null
                )
                val index = call.argument<Int>("index")!!
                val configuration = device.getConfiguration(index)
                result.success(connection.setConfiguration(configuration))
            }

            "claimInterface" -> {
                val device =
                    usbDevice ?: return result.error("IllegalState", "usbDevice null", null)
                val connection = usbDeviceConnection ?: return result.error(
                    "IllegalState",
                    "usbDeviceConnection null",
                    null
                )
                val id = call.argument<Int>("id")!!
                val alternateSetting = call.argument<Int>("alternateSetting")!!
                val usbInterface = device.findInterface(id, alternateSetting)
                result.success(connection.claimInterface(usbInterface, true))
            }

            "releaseInterface" -> {
                val device =
                    usbDevice ?: return result.error("IllegalState", "usbDevice null", null)
                val connection = usbDeviceConnection ?: return result.error(
                    "IllegalState",
                    "usbDeviceConnection null",
                    null
                )
                val id = call.argument<Int>("id")!!
                val alternateSetting = call.argument<Int>("alternateSetting")!!
                val usbInterface = device.findInterface(id, alternateSetting)
                result.success(connection.releaseInterface(usbInterface))
            }

            "bulkTransferIn" -> {
                val device =
                    usbDevice ?: return result.error("IllegalState", "usbDevice null", null)
                val connection = usbDeviceConnection ?: return result.error(
                    "IllegalState",
                    "usbDeviceConnection null",
                    null
                )
                val endpointMap = call.argument<Map<String, Any>>("endpoint")!!
                val maxLength = call.argument<Int>("maxLength")!!
                val endpoint =
                    device.findEndpoint(
                        endpointMap["endpointNumber"] as Int,
                        endpointMap["direction"] as Int
                    )
                val timeout = call.argument<Int>("timeout")!!

                // TODO Check [UsbDeviceConnection.bulkTransfer] API >= 28
                require(maxLength <= UsbRequest__MAX_USBFS_BUFFER_SIZE) { "Before 28, a value larger than 16384 bytes would be truncated down to 16384" }
                val buffer = ByteArray(maxLength)
                val actualLength =
                    connection.bulkTransfer(endpoint, buffer, buffer.count(), timeout)
                if (actualLength < 0) {
                    result.error("unknown", "bulkTransferIn error", null)
                } else {
                    result.success(buffer.take(actualLength))
                }
            }

            "bulkTransferOut" -> {
                val device =
                    usbDevice ?: return result.error("IllegalState", "usbDevice null", null)
                val connection = usbDeviceConnection ?: return result.error(
                    "IllegalState",
                    "usbDeviceConnection null",
                    null
                )
                val endpointMap = call.argument<Map<String, Any>>("endpoint")!!
                val data = call.argument<ByteArray>("data")!!
                val timeout = call.argument<Int>("timeout")!!
                val endpoint =
                    device.findEndpoint(
                        endpointMap["endpointNumber"] as Int,
                        endpointMap["direction"] as Int
                    )

                // TODO Check [UsbDeviceConnection.bulkTransfer] API >= 28
                val dataSplit = data.asList()
                    .windowed(
                        UsbRequest__MAX_USBFS_BUFFER_SIZE,
                        UsbRequest__MAX_USBFS_BUFFER_SIZE,
                        true
                    )
                    .map { it.toByteArray() }
                var sum: Int? = null
                for (bytes in dataSplit) {
                    val actualLength =
                        connection.bulkTransfer(endpoint, bytes, bytes.count(), timeout)
                    if (actualLength < 0) break
                    sum = (sum ?: 0) + actualLength
                }
                if (sum == null) {
                    result.error("unknown", "bulkTransferOut error", null)
                } else {
                    result.success(sum)
                }
            }

            else -> result.notImplemented()
        }
    }

//    private val usbPermissionReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            println("onReceive: " + intent.action)
//            if (intent.action == ACTION_USB_PERMISSION) {
//                synchronized(this) {
//                    val result = permissionRequestCallback.pop(requestCode) ?: return false
//                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
//                    val device: UsbDevice? =
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                            intent.getParcelableExtra(
//                                UsbManager.EXTRA_DEVICE,
//                                UsbDevice::class.java
//                            )
//                        } else {
//                            @Suppress("DEPRECATION")
//                            intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE);
//                        }
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        if (device != null) {
//                            // Permission granted, now you can access device info
//                            val devices = getUsbDevices()
//                            // You may want to pass this result back to Flutter, or handle it as needed
//                        }
//                    } else {
//                        // Permission denied, handle accordingly
//                    }
//                }
//            }
//        }
//    }
}

fun UsbDevice.findInterface(id: Int, alternateSetting: Int): UsbInterface? {
    for (i in 0..interfaceCount) {
        val usbInterface = getInterface(i)
        if (usbInterface.id == id && usbInterface.alternateSetting == alternateSetting) {
            return usbInterface
        }
    }
    return null
}

fun UsbDevice.findEndpoint(endpointNumber: Int, direction: Int): UsbEndpoint? {
    for (i in 0..interfaceCount) {
        val usbInterface = getInterface(i)
        for (j in 0..usbInterface.endpointCount) {
            val endpoint = usbInterface.getEndpoint(j)
            if (endpoint.endpointNumber == endpointNumber && endpoint.direction == direction) {
                return endpoint
            }
        }
    }
    return null
}

/** [UsbRequest.MAX_USBFS_BUFFER_SIZE] */
const val UsbRequest__MAX_USBFS_BUFFER_SIZE = 16384

fun UsbConfiguration.toMap() = mapOf(
    "id" to id,
    "interfaces" to List(interfaceCount) { getInterface(it).toMap() }
)

fun UsbInterface.toMap() = mapOf(
    "id" to id,
    "alternateSetting" to alternateSetting,
    "endpoints" to List(endpointCount) { getEndpoint(it).toMap() }
)

fun UsbEndpoint.toMap() = mapOf(
    "endpointNumber" to endpointNumber,
    "direction" to direction
)
