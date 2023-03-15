package com.chapp.ui.log_file

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chapp.MainActivity
import com.chapp.R
import com.chapp.databinding.FragmentLogFileBinding
import com.chapp.ui.chat.ChatAdapter
import com.chapp.ui.chat.Message
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.sql.Date
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LogFileFragment : Fragment() {

    private var logAdapter: ChatAdapter? = null
    private lateinit var recyclerviewLog: RecyclerView
    private var messageList = listOf<Message>()
    private var _binding: FragmentLogFileBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainActivity = activity as MainActivity
        _binding = FragmentLogFileBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val startDateButton = root.findViewById<Button>(R.id.startDate)
        val endDateButton = root.findViewById<Button>(R.id.endDate)
        val refreshButton = root.findViewById<Button>(R.id.refresh)
        val exportButton = root.findViewById<Button>(R.id.export)
        val start = HourDate("Start")
        val end = HourDate("End")

        initViews(root, mainActivity)

        startDateButton.setOnClickListener {
            pickDateTime(startDateButton, start)
        }

        endDateButton.setOnClickListener {
            pickDateTime(endDateButton, end)
        }

        refreshButton.setOnClickListener {
            start.hourDate?.let {it1 ->
                end.hourDate?.let {it2 ->
                    showLog(mainActivity.messageDatabase.getMessages(it1, it2))
                } ?: run{
                    Snackbar.make(it,"Set ending date", 5).show()
                }
            } ?: run {
                Snackbar.make(it,"Set starting date", 5).show()
            }
        }

        exportButton.setOnClickListener{

            if (messageList.isNotEmpty()){
                val path = context?.getExternalFilesDir(null)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val current = LocalDateTime.now().format(formatter)
                val letDirectory = File(path,"Log")
                if (!letDirectory.exists()){
                    letDirectory.mkdirs()
                }
                Log.i("path", letDirectory.toString())
                val file = File(letDirectory, "Chapp_Log_File_$current.csv")
                file.createNewFile()
                FileOutputStream(file).apply { writeCsv(messageList) }
            } else {
                Snackbar.make(it,"Set log period to export data", 5).show()
            }


        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun OutputStream.writeCsv(messages: List<Message>) {
        val writer = bufferedWriter()
        writer.write(""""Date", "Device", "Message"""")
        writer.newLine()
        messages.forEach {
            writer.write("${Date(it.time)}, ${it.user},\"${it.message}\"")
            writer.newLine()
        }
        writer.flush()
    }

    private fun pickDateTime(view: Button, hourDate: HourDate) {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val pickedDateTime = Calendar.getInstance()
                    pickedDateTime.set(year, month, day, hour, minute)
                    "${"%02d".format(hour)}:${"%02d".format(minute)} ${"%02d".format(day)}/${"%02d".format(month+1)}/${"%02d".format(year)}".also { view.text = it }
                    hourDate.hourDate = pickedDateTime.timeInMillis
                },
                startHour,
                startMinute,
                true
            ).show()
        }, startYear, startMonth, startDay).show()
    }

    private fun showLog(flow: Flow<List<Message>>){
        lifecycleScope.launch {
                    flow.collect { value ->
                        messageList = value
                        logAdapter = ChatAdapter(messageList,requireContext())
                        recyclerviewLog.adapter = logAdapter
                        recyclerviewLog.scrollToPosition(0)
                    }
                }
            }


    private fun initViews(mView: View, context: Context) {

        recyclerviewLog = mView.findViewById(R.id.logRecyclerView)
        val llm = LinearLayoutManager(context)
        llm.reverseLayout = true
        recyclerviewLog.layoutManager = llm
        logAdapter = ChatAdapter(messageList, context)
        recyclerviewLog.adapter = logAdapter

    }

}