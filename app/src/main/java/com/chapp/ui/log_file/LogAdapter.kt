package com.chapp.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chapp.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by ramankit on 25/7/17.
 */

class LogAdapter(val context: Context)  : PagingDataAdapter<Message, RecyclerView.ViewHolder>(
    diffCallback) {


    private val SENT = 0
    private val RECEIVED = 1

    var df: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss a",Locale.getDefault())

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val chatData = getItem(position)
        if (chatData != null){
            when(holder.itemViewType){

                SENT -> {
                    val holder: SentHolder = holder as SentHolder
                    holder.user.text = chatData.user
                    holder.sentTV.text = chatData.message
                    val timeMilliSeconds = chatData.time
                    val resultDate = Date(timeMilliSeconds)

                    holder.timeStamp.text = df.format(resultDate)

                }
                RECEIVED -> {
                    val holder: ReceivedHolder = holder as ReceivedHolder
                    holder.user_other.text = chatData.user
                    holder.receivedTV.text = chatData.message
                    val timeMilliSeconds = chatData.time
                    val resultDate = Date(timeMilliSeconds)
                    holder.timeStamp.text = df.format(resultDate)
                }

            }
        }

    }

    companion object {
        //This diff callback informs the PagedListAdapter how to compute list differences when new
        private val diffCallback = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem.time == newItem.time

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem.time == newItem.time
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chatData = getItem(position)
        if (chatData != null) {
            when (chatData.type) {
                Constants.MESSAGE_TYPE_SENT -> return SENT
                Constants.MESSAGE_TYPE_RECEIVED -> return RECEIVED
            }
        }
        return -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when(viewType){
            SENT -> {
                val view = LayoutInflater.from(context).inflate(R.layout.sent_layout,parent,false)
                return SentHolder(view)
            }
            RECEIVED -> {
                val view = LayoutInflater.from(context).inflate(R.layout.received_layout,parent,false)
                return ReceivedHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.sent_layout,parent,false)
                return SentHolder(view)
            }
        }
    }

    inner class SentHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var user = itemView.findViewById<TextView>(R.id.text_gchat_user)
        var sentTV = itemView.findViewById<TextView>(R.id.sentMessage)
        var timeStamp = itemView.findViewById<TextView>(R.id.timeStamp)
    }

    inner class ReceivedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var user_other = itemView.findViewById<TextView>(R.id.text_gchat_user_other)
        var receivedTV = itemView.findViewById<TextView>(R.id.receivedMessage)
        var timeStamp = itemView.findViewById<TextView>(R.id.timeStamp)
    }

}