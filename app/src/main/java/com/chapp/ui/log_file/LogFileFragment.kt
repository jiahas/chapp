package com.chapp.ui.log_file

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chapp.MainActivity
import com.chapp.R
import com.chapp.databinding.FragmentLogFileBinding
import com.chapp.ui.chat.LogAdapter
import com.chapp.ui.chat.Message
import com.chapp.ui.chat.MessageDao
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class LogFileFragment : Fragment(){

    private var logAdapter: LogAdapter? = null
    private lateinit var recyclerviewLog: RecyclerView
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
                    show(observePagingSource(mainActivity.messageDatabase,it1, it2))
                } ?: run{
                    Snackbar.make(it,"Set ending date", 5).show()
                }
            } ?: run {
                Snackbar.make(it,"Set starting date", 5).show()
            }
        }

        exportButton.setOnClickListener{

                start.hourDate?.let { it1 ->
                    end.hourDate?.let { it2 ->

                        lifecycleScope.launch(Dispatchers.IO) {
                            exportLog(mainActivity.messageDatabase.exportLog(it1, it2))
                        }

                    } ?: run{
                        Snackbar.make(it,"Set ending date", 5).show()
                    }
                } ?: run {
                    Snackbar.make(it,"Set starting date", 5).show()
                }

            }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatDate(milliSeconds: Long, dateFormat: String): String {
        val formatter = SimpleDateFormat(dateFormat)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }
    private fun observePagingSource(dao : MessageDao, start: Long, end: Long): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = 50, initialLoadSize = 50),
            pagingSourceFactory = { dao.getMessages(start,end) }
        ).flow
    }
    private fun createFile(context: Context): File? {
        val path = context.getExternalFilesDir(null)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm")
        val current = LocalDateTime.now().format(formatter)
        val letDirectory = File(path,"Log")
        if (!letDirectory.exists()){
            letDirectory.mkdirs()
        }
        Log.i("path", letDirectory.toString())
        val file = File(letDirectory, "Chapp_Log_File_$current.csv")
        file.createNewFile()
        return if (file.exists()){
            file
        } else {
            null
        }
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

    private fun show(page: Flow<PagingData<Message>>){
        lifecycleScope.launch {
                    page.collect { value ->
                        logAdapter?.submitData(value)
                        recyclerviewLog.adapter = logAdapter
                        recyclerviewLog.scrollToPosition(0)
                    }
                }
            }


    private fun exportLog(crs: Cursor) {
        val csvFile = context?.let { it1 -> createFile(it1) }
        if (csvFile != null) {
            csvWriter().open(csvFile, append = false) {
                writeRow(listOf("Date", "Device", "Message"))
                        if (crs.moveToFirst()) {
                            while (!crs.isAfterLast) {
                                writeRow(
                                    listOf(
                                        formatDate(
                                            crs.getLong(0),
                                            "dd-MM-yyyy HH:MM:ss.SSSS"
                                        ),
                                        crs.getString(1),
                                        crs.getString(2).replace("\n", "").replace("\r", "")
                                    )
                                )
                                crs.moveToNext()
                            }
                            view?.let { Snackbar.make(it,"Finished", 1000).show() }
                        crs.close()
                        }
                }
            }
    }


    private fun initViews(mView: View, context: Context) {

        recyclerviewLog = mView.findViewById(R.id.logRecyclerView)
        val llm = LinearLayoutManager(context)
        llm.reverseLayout = true
        recyclerviewLog.layoutManager = llm
        logAdapter = LogAdapter(context)
        recyclerviewLog.adapter = logAdapter

    }

}