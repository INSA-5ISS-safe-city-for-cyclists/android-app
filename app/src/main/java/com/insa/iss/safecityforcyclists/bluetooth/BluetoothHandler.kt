package com.insa.iss.safecityforcyclists.bluetooth

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import com.welie.blessed.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import java.lang.Exception


internal class BluetoothHandler private constructor(
    private val activity: AppCompatActivity,
    private val bleButton: FloatingActionButton,
    private val dangerReportsViewModel: DangerReportsViewModel,
    private val startForResult: ActivityResultLauncher<Intent>
) {

    companion object {

        private val TAG = "BLE"
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
            activity: AppCompatActivity?,
            bleButton: FloatingActionButton?,
            dangerReportsViewModel: DangerReportsViewModel?,
            startForResult: ActivityResultLauncher<Intent>?
        ): BluetoothHandler {
            if (instance == null) {
                if (activity == null || bleButton == null || dangerReportsViewModel == null || startForResult == null) {
                    requireNotNull(instance)
                } else {
                    instance = BluetoothHandler(
                        activity,
                        bleButton,
                        dangerReportsViewModel,
                        startForResult
                    )
                }
            }
            return requireNotNull(instance)
        }
    }

    // Permissions

    private val packageManager = activity.packageManager
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)
    private var bluetoothManager: BluetoothManager =
        activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    // Blessed android coroutines

    @JvmField
    var central: BluetoothCentralManager = BluetoothCentralManager(activity)


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

    init {
        central.observeConnectionState { peripheral, state ->
            Log.d(TAG, "Peripheral ${peripheral.name} has $state")
            when (state) {
                ConnectionState.CONNECTED -> handlePeripheral(peripheral)
                ConnectionState.DISCONNECTED -> scope.launch {
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
                else -> {
                }
            }
        }

        bleButton.setOnClickListener {
            if (!connected) {
                if (setup() && !scanning) {
                    startScanning()
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
                Toast.makeText(
                    activity,
                    activity.resources.getString(
                        R.string.ble_already_connected,
                        connectedDevice?.name
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setup(): Boolean {
        var ok = true
        // Check to see if the Bluetooth classic feature is available.
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH) }?.also {
            Toast.makeText(activity, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            ok = false
        }
        // Check to see if the BLE feature is available.
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.also {
                Toast.makeText(activity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
                ok = false
            }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startForResult.launch(enableBtIntent)
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
        central.scanForPeripheralsWithServices(supportedServices) { peripheral, scanResult ->
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
        central.stopScan()
        scanning = false
    }

    private fun connectPeripheral(peripheral: BluetoothPeripheral) {
//        peripheral.observeBondState {
//            Log.d(TAG,"Bond state is $it")
//        }

        scope.launch {
            try {
                try {
                    central.cancelConnection(peripheral)
                } catch (exception: Exception) {
                }
                central.connectPeripheral(peripheral)
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
        peripheral.getCharacteristic(CUSTOM_SERVICE_UUID, CUSTOM_CHARACTERISTIC_UUID)?.let {
            peripheral.observe(it) { value ->
                val parser = BluetoothBytesParser(value, ByteOrder.LITTLE_ENDIAN)
                val sensorValue = parser.getStringValue(0)
                Log.d(TAG, sensorValue)

                // Parse JSON

                val json = JSONTokener(sensorValue).nextValue() as JSONObject

                // TODO change date format to dd/MM/yyyy on the ESP
                val formatter = DateTimeFormatter.ofPattern("d/MM/yyyy", Locale.getDefault())
                val date = LocalDate.parse(json.getString("date"), formatter)

                val timestamp =
                    json.getLong("timestamp") / 1000 + date.atStartOfDay(ZoneId.systemDefault())
                        .toEpochSecond()

//                Log.d(TAG, timestamp.toString())
//                Log.d(TAG, DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(timestamp)))

                // Add report
                dangerReportsViewModel.addLocalReports(
                    listOf(
                        LocalReport(
                            timestamp = timestamp,
                            distance = json.getDouble("distance"),
                            objectSpeed = json.getDouble("distance"),
                            bicycleSpeed = json.getDouble("distance"),
                            latitude = json.getDouble("distance"),
                            longitude = json.getDouble("distance"),
                            sync = false
                        )
                    )
                )

            }
        }
    }

    suspend fun write(report: LocalReport): Boolean {
        return if (connected) {
            val json = report.toJSON()
            json.put("date", "4/12/2021")
            val jsonString = json.toString().replace("\\","")
            Log.d(TAG, jsonString)
            connectedDevice?.writeCharacteristic(
                CUSTOM_SERVICE_UUID,
                CUSTOM_CHARACTERISTIC_UUID,
                jsonString.toByteArray(),
                WriteType.WITH_RESPONSE
            )
            true
        } else {
            false
        }
    }

}