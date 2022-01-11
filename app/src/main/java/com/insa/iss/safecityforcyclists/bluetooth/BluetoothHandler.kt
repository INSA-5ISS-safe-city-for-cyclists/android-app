package com.insa.iss.safecityforcyclists.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.location.Location
import com.insa.iss.safecityforcyclists.reports.DangerClassification
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.welie.blessed.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.ByteOrder
import java.util.*


internal class BluetoothHandler private constructor(
    private val activity: AppCompatActivity,
    private val bleButton: FloatingActionButton,
    private val dangerReportsViewModel: DangerReportsViewModel,
    private val startForResult: ActivityResultLauncher<Intent>,
    private val location: Location
) {

    companion object {

        private const val TAG = "BLE"
        private var instance: BluetoothHandler? = null

        // UUIDs for the Device Information service (DIS)
        private val DIS_SERVICE_UUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
        private val MODEL_NUMBER_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")

        // UUIDs for the Battery Service (BAS)
        private val BTS_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

        // Custom Service
        private val CUSTOM_SERVICE_UUID: UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        private val CUSTOM_CHARACTERISTIC_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        private val supportedServices = arrayOf(CUSTOM_SERVICE_UUID)

        @JvmStatic
        @Synchronized
        fun getInstance(
            activity: AppCompatActivity,
            bleButton: FloatingActionButton,
            dangerReportsViewModel: DangerReportsViewModel,
            startForResult: ActivityResultLauncher<Intent>,
            location: Location
        ): BluetoothHandler {
            if (instance == null) {
                instance = BluetoothHandler(
                    activity,
                    bleButton,
                    dangerReportsViewModel,
                    startForResult,
                    location
                )
            }
            return requireNotNull(instance)
        }
    }

    // Permissions

    private val packageManager = activity.packageManager
    private var bluetoothManager: BluetoothManager =
        activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val locationManager =
        activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Blessed android coroutines

    @JvmField
    var central: BluetoothCentralManager? = null


    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Drawable

    private val bleOffDrawable =
        ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_baseline_bluetooth_24, null)
    private val bleConnectedDrawable = ResourcesCompat.getDrawable(
        activity.resources,
        R.drawable.ic_baseline_bluetooth_connected_24,
        null
    )
    private val bleSearchingDrawable = ResourcesCompat.getDrawable(
        activity.resources,
        R.drawable.ic_baseline_bluetooth_searching_24,
        null
    )

    // General

    private var scanning = false
    private var connected = false
    private var connectedDevice: BluetoothPeripheral? = null

    // Only for debug

    var mapboxMap: MapboxMap? = null

    init {
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show()
        } else {
            central = BluetoothCentralManager(activity)
            central!!.observeConnectionState { peripheral, state ->
                Log.d(TAG, "Peripheral ${peripheral.name} has $state")
                when (state) {
                    ConnectionState.CONNECTED -> handlePeripheral(peripheral)
                    ConnectionState.DISCONNECTED -> scope.launch { onDeviceDisconnected() }
                    else -> {
                    }
                }
            }
        }

        bleButton.setOnClickListener {
            if (!connected) {
                if (!scanning) {
                    if (!PermissionsManager.areLocationPermissionsGranted(activity) ||
                        !locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER
                        ) || location.lastLocation == null
                    ) {
//                        val snackbar = Snackbar.make(
//                            bleButton.rootView,
//                            R.string.ble_localisation_disabled,
//                            Snackbar.LENGTH_LONG
//                        )
//                        snackbar.setAction(R.string.ble_localisation_disabled_validate) {
//                            onContinue()
//                        }
//                        snackbar.show()
                        MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.ble_localisation_disabled_title)
                            .setMessage(R.string.ble_localisation_disabled)
                            .setNeutralButton(R.string.ble_localisation_disabled_cancel) { _, _ ->
                                // Respond to neutral button press
                            }
                            .setPositiveButton(R.string.ble_localisation_disabled_validate) { _, _ ->
                                // Respond to positive button press
                                onContinue()
                            }
                            .show()
                    } else {
                        onContinue()
                    }
                } else if (scanning) {
                    stopScanning()
                    Toast.makeText(
                        activity,
                        R.string.ble_stop_scan,
                        Toast.LENGTH_SHORT
                    ).show()
                    bleButton.setImageDrawable(bleOffDrawable)
                }
            } else {
                // Disconnection
                scope.launch {
                    if (connectedDevice != null) {
                        central?.cancelConnection(connectedDevice!!)
                    }
                }
            }
        }
    }

    private fun onDeviceDisconnected() {
        connected = false
        connectedDevice = null
        bleButton.setImageDrawable(bleOffDrawable)
        activity.runOnUiThread {
            Toast.makeText(
                activity,
                R.string.ble_device_disconnected,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onContinue() {
        if (setup()) {
            startScanning()
        }
    }

    private fun setup(): Boolean {
        var ok = true
        // Check to see if the classic Bluetooth feature is available.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(activity, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            ok = false
        } else {
            // Check to see if the BLE feature is available.
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(activity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
                ok = false
            } else {
                if (bluetoothAdapter == null) {
                    ok = false
                } else {
                    if (!bluetoothAdapter!!.isEnabled) {
                        ok = false
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startForResult.launch(enableBtIntent)
                    }
                }
            }
        }

        return ok
    }

    fun startScanning() {
        scanning = true
        bleButton.setImageDrawable(bleSearchingDrawable)
        Toast.makeText(
            activity,
            R.string.ble_start_scan,
            Toast.LENGTH_SHORT
        ).show()
        central!!.scanForPeripheralsWithServices(supportedServices) { peripheral, scanResult ->
            Log.d(TAG, "Found peripheral '${peripheral.name}' with RSSI ${scanResult.rssi}")
            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    activity.resources.getString(R.string.ble_device_found, peripheral.name),
                    Toast.LENGTH_SHORT
                ).show()
            }
            stopScanning()
            connectPeripheral(peripheral)
        }
    }

    private fun stopScanning() {
        central!!.stopScan()
        scanning = false
    }

    private fun connectPeripheral(peripheral: BluetoothPeripheral) {
//        peripheral.observeBondState {
//            Log.d(TAG,"Bond state is $it")
//        }

        scope.launch {
            try {
                try {
                    central!!.cancelConnection(peripheral)
                } catch (exception: Exception) {
                }
                central!!.connectPeripheral(peripheral)
            } catch (connectionFailed: ConnectionFailedException) {
                Log.e(TAG, "connection failed")
            }
        }
    }

    private fun handlePeripheral(peripheral: BluetoothPeripheral) {
        scope.launch(Dispatchers.IO) {
            try {
                connected = true
                connectedDevice = peripheral
                bleButton.setImageDrawable(bleConnectedDrawable)

                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        activity.resources.getString(
                            R.string.ble_device_connected,
                            peripheral.name
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val mtu = peripheral.requestMtu(185)
                Log.d(TAG, "MTU is $mtu")

                peripheral.requestConnectionPriority(ConnectionPriority.HIGH)

                val rssi = peripheral.readRemoteRssi()
                Log.d(TAG, "RSSI is $rssi")

                val model = peripheral.readCharacteristic(
                    DIS_SERVICE_UUID,
                    MODEL_NUMBER_CHARACTERISTIC_UUID
                ).asString()
                Log.d(TAG, "Received: $model")

                val batteryLevel = peripheral.readCharacteristic(
                    BTS_SERVICE_UUID,
                    BATTERY_LEVEL_CHARACTERISTIC_UUID
                ).asUInt8()
                Log.d(TAG, "Battery level: $batteryLevel")

                setupSensorValueNotifications(peripheral)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, e.toString())
            } catch (b: GattException) {
                Log.e(TAG, b.toString())
            }
        }
    }

    private suspend fun setupSensorValueNotifications(peripheral: BluetoothPeripheral) {
        peripheral.getCharacteristic(CUSTOM_SERVICE_UUID, CUSTOM_CHARACTERISTIC_UUID)?.let { it ->
            peripheral.observe(it) { value ->
                val parser = BluetoothBytesParser(value, ByteOrder.LITTLE_ENDIAN)
                val sensorValue = parser.getStringValue(0)
//                Log.d(TAG, value.fold("", { acc, byte ->
//                    byte.toString() + acc
//                }))
                Log.d(TAG, sensorValue)

                try {
                    // Parse JSON
                    val json = JSONTokener(sensorValue).nextValue() as JSONObject
                    val timestamp = Date().time / 1000
                    // Add report
                    val report: LocalReport

                    val danger = dangerReportsViewModel.getDangerClassification()
                    var responseCode = "0"

                    if (location.lastLocation != null) {
                        report = LocalReport(
                            timestamp = timestamp,
                            distance = json.getDouble("distance"),
                            objectSpeed = json.getDouble("object_speed"),
                            bicycleSpeed = location.lastLocation!!.speed.toDouble(),
                            latitude = location.lastLocation!!.latitude,
                            longitude = location.lastLocation!!.longitude,
                            sync = false
                        )
                        responseCode = danger.getDangerCode(report)
                        if (responseCode === DangerClassification.dangerCode) {
                            dangerReportsViewModel.addLocalReports(
                                listOf(report)
                            )
                        }
                    } else {
                        // TODO remove these lines (BLE => local reports)

                        // Used to test and see the points on the map

                        var lastIndex =
                            dangerReportsViewModel.getFeatures().value?.features()?.lastIndex
                        if (lastIndex == null) {
                            lastIndex = 0
                        }

                        var latitude = 43.602 + lastIndex.toDouble() * 0.01
                        var longitude = mapboxMap?.cameraPosition?.target?.latitude
                            ?: 1.453 + lastIndex.toDouble() * 0.01

                        mapboxMap?.let {
                            latitude = it.cameraPosition.target.latitude
                            longitude = it.cameraPosition.target.longitude
                        }

                        report = LocalReport(
                            timestamp = timestamp,
                            distance = json.getDouble("distance"),
                            objectSpeed = json.getDouble("object_speed"),
                            bicycleSpeed = 0.0 + lastIndex.toDouble(),
                            latitude = latitude,
                            longitude = longitude,
                            sync = false
                        )
                        responseCode = danger.getDangerCode(report)
                        if (responseCode === DangerClassification.dangerCode) {
                            dangerReportsViewModel.addLocalReports(
                                listOf(report)
                            )
                        }

                        // Keep this line

                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                R.string.error_creating_report,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }

                // Write characteristic (to activate the buzzer, etc)
                peripheral.getCharacteristic(CUSTOM_SERVICE_UUID, CUSTOM_CHARACTERISTIC_UUID)
                    ?.let { it2 ->
                        // Verify it has the write with response property
                        if (it2.supportsWritingWithResponse()) {
                            scope.launch(Dispatchers.IO) {
                                peripheral.writeCharacteristic(
                                    it2,
                                    responseCode.toByteArray(),
                                    WriteType.WITH_RESPONSE
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
            Log.w(TAG, "Received wrong messages")
        }
        }
    }
}

}