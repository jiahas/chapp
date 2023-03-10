package com.chapp.ui.bt
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.chapp.MainActivity
import com.chapp.MainActivity.Companion.ACTION_GATT_CONNECTED
import com.chapp.MainActivity.Companion.ACTION_GATT_DISCONNECTED
import com.chapp.R
import com.chapp.ScanResultAdapter
import com.chapp.databinding.FragmentBluetoothBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BluetoothFragment: Fragment() {

    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!

    private fun setupRecyclerView(view: RecyclerView, adapter: ScanResultAdapter, context: Context) {
        view.adapter = adapter

        view.layoutManager = LinearLayoutManager(
                            context,
                            RecyclerView.VERTICAL,
                            false)

        view.isNestedScrollingEnabled = false

        val animator = view.itemAnimator

        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

    }


    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(ACTION_GATT_CONNECTED)
            addAction(ACTION_GATT_DISCONNECTED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val mainActivity = (activity as MainActivity)

        val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_GATT_CONNECTED -> {
                        mainActivity.scanResultAdapter.notifyDataSetChanged()
                    }
                    ACTION_GATT_DISCONNECTED -> {
                        mainActivity.scanResultAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val scan: FloatingActionButton = binding.scanButton

        scan.setOnClickListener {

            if (!mainActivity.bluetoothAdapter.isEnabled) {
                mainActivity.promptEnableBluetooth()
            } else {

                if (mainActivity.isScanning) {
                    scan.setImageResource(android.R.drawable.ic_menu_search)
                    mainActivity.stopBleScan()
                } else {
                    scan.setImageResource(android.R.drawable.ic_media_pause)
                    mainActivity.startBleScan()
                }
            }
        }

        val recycler = binding.root.findViewById<View>(R.id.scan_results_recycler_view) as RecyclerView
        // Initialize contacts
        // Create adapter passing in the sample user data
        val adapter = mainActivity.scanResultAdapter
        setupRecyclerView(recycler, adapter, mainActivity)
        mainActivity.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}