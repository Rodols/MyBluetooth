package com.example.mybluetoothapp

import android.R.attr.x
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var bluetoothAdapter: BluetoothAdapter
    private val REQUEST_ENABLE_BT = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setup()

    }

    private fun setup() {

        //init bluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        //Check if bluetooth is on/off
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "No soporta bluetooth tu dispositivo", Toast.LENGTH_SHORT).show()
        } else {
            //Device support Bluetooth
            enabledBluetooth()
        }

        getPairedDevicesButton.setOnClickListener {
            pairedDevices()
        }

        sendButton.setOnClickListener {
            sendCommand("1")
        }

        bluetoothIn = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == handlerState) {
                    val readMessage = msg.obj as String
                    DataStringIN.append(readMessage)
                    val endOfLineIndex = DataStringIN.indexOf("#")
                    if (endOfLineIndex > 0) {
                        val dataInPrint = DataStringIN.substring(0, endOfLineIndex)
                        reciveTextView.text=dataInPrint
                        Toast.makeText(baseContext, "" + dataInPrint, Toast.LENGTH_SHORT).show()
                        DataStringIN.delete(0, DataStringIN.length)
                    }
                }
            }
        }

    }

    private fun enabledBluetooth() {
        //Verificamos si el bluetooth esta habilitado con el metodo isEnabled
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "El usuario rechazo activar el bluetooth!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    companion object{
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var  m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter : BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        lateinit var bluetoothIn: Handler
        val handlerState = 0;
        private val DataStringIN = StringBuilder()
        lateinit var MyConexionBT: ConnectedThread
    }

    private fun pairedDevices() {
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Primero active su bluetooth", Toast.LENGTH_SHORT).show()
        } else {
            val btDeviceArray = ArrayList<BluetoothDevice>()
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                arrayAdapter.add(deviceName + "\n" + deviceHardwareAddress)
                btDeviceArray.add(device)
            }
            pairedDevicesListView.adapter = arrayAdapter
            pairedDevicesListView.setOnItemClickListener { parent, view, position, id ->
                Toast.makeText(this, "${btDeviceArray[position]}", Toast.LENGTH_SHORT).show()
                val device: BluetoothDevice = btDeviceArray[position]
                m_address  = device.address
                ConnectToDevice(this).execute()
            }
        }
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess:Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                if(m_bluetoothSocket == null || !m_isConnected){
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                    MyConexionBT=ConnectedThread(m_bluetoothSocket!!)
                    MyConexionBT.start()
                }
            }catch (e: Exception){
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectSuccess){
                Log.i("data", "couldn´t connect")
            }else{
                m_isConnected = true
            }
            m_progress.dismiss()

        }
    }

    private fun sendCommand(input: String) {
        if(m_bluetoothSocket != null){
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    //create new class for connect thread
    class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    val readMessage = String(buffer, 0, bytes)
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget()
                } catch (e: IOException) {
                    break
                }
            }
        }

        //creation of the connect thread
        init {
            var tmpIn: InputStream? = null
            try {
                //Create I/O streams for connection
                tmpIn = socket.inputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
        }
    }

    }