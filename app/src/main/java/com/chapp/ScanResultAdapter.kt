package com.chapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.recyclerview.widget.RecyclerView
import com.chapp.services.ConnectionManager


var mExpandedPosition = -1

@SuppressLint("MissingPermission")
class ScanResultAdapter (
    val context: Context,
    val bluetoothManager: BluetoothManager,
    private val items: List<ScanResult>,
    private val onClickListener: ((device: ScanResult) -> Unit),

) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.row_scan_result,
            parent,
            false
        )
        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        val isExpanded = position == mExpandedPosition

        if (bluetoothManager.getConnectionState(item.device, GATT) == STATE_CONNECTED){
            if (holder.button.isEnabled){
                markButtonDisable(holder.button, "Connected")
            }

            holder.disconnect.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.itemView.isActivated = isExpanded
            holder.itemView.setOnClickListener {
                mExpandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(position)
            }

        } else{
            markButtonEnable(holder.button, "Connect")
            holder.disconnect.visibility = View.GONE
            holder.itemView.isActivated = false
            holder.itemView.isClickable = false
        }

    }

    fun markButtonDisable(button: Button, state: String) {
        button.isEnabled = false
        button.text = state
        button.setTextColor(ContextCompat.getColor(context, R.color.white))
        button.setBackgroundColor(ContextCompat.getColor(context, R.color.greyish))
    }
    fun markButtonEnable(button: Button, state: String) {
        button.isEnabled = true
        button.text=state
        button.setTextColor(ContextCompat.getColor(context, R.color.white))
        button.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_500))
    }

    inner class ViewHolder(
        private val view: View,
        private val onClickListener: ((device: ScanResult) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        val button = itemView.findViewById<Button>(R.id.connect_button)
        val device = itemView.findViewById<TextView>(R.id.device_name)
        val mac = itemView.findViewById<TextView>(R.id.mac_address)
        val rssi = itemView.findViewById<TextView>(R.id.signal_strength)
        val disconnect = itemView.findViewById<TextView>(R.id.disconnect)

        fun bind(result: ScanResult) {

            device.text = result.device.name ?: "Unnamed"
            mac.text = result.device.address
            rssi.text = "${result.rssi} dBm"
            button.setOnClickListener { onClickListener.invoke(result)}
            disconnect.setOnClickListener { ConnectionManager.teardownConnection(result.device)}

            return

        }

    }
}