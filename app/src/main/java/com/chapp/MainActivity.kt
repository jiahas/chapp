package com.chapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.chapp.databinding.ActivityMainBinding
import com.chapp.services.*
import com.chapp.services.ConnectionManager.deviceGattMap
import com.chapp.services.ConnectionManager.registerListener
import com.chapp.services.ConnectionManager.teardownConnection
import com.chapp.ui.chat.ChatFragment
import com.chapp.ui.chat.Constants
import com.chapp.ui.chat.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*


private const val RUNTIME_PERMISSION_REQUEST_CODE = 2
@SuppressLint("MissingPermission")

class MainActivity : AppCompatActivity(),
    ChatFragment.CommunicationListener{

    companion object {
        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"

    }
    lateinit var chatFragment: ChatFragment
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    /*******************************************
     * Properties
     *******************************************/

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }

    val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .build()

    var isScanning = false

    private val scanResults: MutableList<ScanResult> = mutableListOf()

    val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(this, bluetoothManager, scanResults) { result ->
            if (isScanning) {
                stopBleScan()
                }
            with(result.device){
                Log.w("ScanResultAdapter", "Connecting to $address")
                ConnectionManager.connect(this, this@MainActivity)
                ConnectionManager.listenToBondStateChanges( this@MainActivity)
                }
            }
        }

    /*******************************************
     * Activity function overrides
     *******************************************/

    override fun onResume() {
        super.onResume()
        registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        ConnectionManager.application = this.applicationContext as Application
        appBarConfiguration = AppBarConfiguration(navController.graph)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_bt, R.id.navigation_chat, R.id.navigation_log_file
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        registerListener(connectionEventListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.bottom_nav_menu, menu)
        return true
    }

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            promptEnableBluetooth()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                when {
                    containsPermanentDenial -> {
                        startForResult.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                    }
                    containsDenial -> {
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() -> {
                        startBleScan()
                    }
                    else -> {
                        // Unexpected scenario encountered when handling permissions
                        recreate()
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        deviceGattMap.keys.forEach{ teardownConnection(it) }
        super.onDestroy()
    }

    /*******************************************
     * Private functions
     *******************************************/

    fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startForResult.launch(enableBtIntent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun startBleScan() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }

    }

    fun stopBleScan() {
        val scan = binding.root.findViewById<FloatingActionButton>(R.id.scanButton)
        scan.setImageResource(android.R.drawable.ic_menu_search)
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }


    private fun requestLocationPermission() {
        runOnUiThread {
            val dialogBuilder = AlertDialog.Builder(this)

            // set message of alert dialog
            dialogBuilder.setMessage(
                "Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices."
            )
                // if the dialog is cancelable
                .setCancelable(false)
                // positive button text and action
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                    finish()
                }
            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle("Location permission required")
            // show alert dialog
            alert.show()
        }
    }


    /*******************************************
     * Callback bodies
     *******************************************/

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                if (result.device.name != null){
                    with(result.device) {
                        Log.i(
                            "ScanCallback",
                            "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                        )
                    }
                    scanResults.add(result)
                    scanResultAdapter.notifyItemInserted(scanResults.size - 1)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }


    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {

            onCharacteristicChanged = { gatt, char ->
                Log.i("Received", "$gatt:${AllGattCharacteristics.lookup(char.uuid)}:${char.value.toString(Charsets.UTF_8)}")
                val writeMessage = char.value.toString(Charsets.UTF_8)
                val milliSecondsTime = System.currentTimeMillis()
                runOnUiThread {
                    chatFragment.communicate(
                        Message(
                            writeMessage,
                            milliSecondsTime,
                            Constants.MESSAGE_TYPE_RECEIVED
                        )
                    )
                }

            }

            onConnectionSetupComplete = {

                it.findCharacteristic(UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616"))
                    ?.let { it1 -> ConnectionManager.enableNotifications(it.device, it1) }

            }
        }
    }



    /*******************************************
     * Extension functions
     *******************************************/

    private fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) {
            return
        }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    private fun requestBluetoothPermissions() {
        runOnUiThread {
            val dialogBuilder = AlertDialog.Builder(this)

            // set message of alert dialog
            dialogBuilder.setMessage(
                "Starting from Android 12, the system requires apps to be granted " +
                        "Bluetooth access in order to scan for and connect to BLE devices."
            )
                // if the dialog is cancelable
                .setCancelable(false)
                // positive button text and action
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                    finish()
                }
            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle("Bluetooth permissions required")
            // show alert dialog
            alert.show()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }



    override fun onCommunication(message: String) {
        sendMessage(message)
    }

    private fun sendMessage(
        message: String) {
        // Check that there's actually something to send
        if (message.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            val send = message.toByteArray(Charsets.UTF_8)
            Log.i("Message Sent","${send.toHexString()} ${send.toString(Charsets.UTF_8)} ${send.size}")
            deviceGattMap.forEach { (device, gatt) ->
                    gatt.findCharacteristic(UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3"))
                        ?.let {
                            ConnectionManager.writeCharacteristic(
                                device,
                                it,
                                "0x9a".toByteArray() + byteArrayOf(0x0d,0x0a)
                            )
                            ConnectionManager.writeCharacteristic(
                                device,
                                it,
                                send + byteArrayOf(0x0d,0x0a)
                            )
                        }
                }
            val writeMessage = String(send)
            val milliSecondsTime = System.currentTimeMillis()
            chatFragment.communicate(Message(writeMessage,milliSecondsTime, Constants.MESSAGE_TYPE_SENT))
            }
            // Reset out string buffer to zero and clear the edit text field
            //mOutStringBuffer.setLength(0)
            //mOutEditText.setText(mOutStringBuffer)
        }
    }

