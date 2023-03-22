package com.chapp.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chapp.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by ramankit on 25/7/17.
 */

class ChatAdapter(private val chatData: List<Message>, val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val SENT = 0
    private val RECEIVED = 1
    var df: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a",Locale.getDefault())

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when(holder.itemViewType){

            SENT -> {
                val holder: SentHolder = holder as SentHolder
                holder.user.text = chatData[position].user
                holder.sentTV.text = chatData[position].message
                val timeMilliSeconds = chatData[position].time
                val resultdate = Date(timeMilliSeconds)

                holder.timeStamp.text = df.format(resultdate)

            }
            RECEIVED -> {
                val holder: ReceivedHolder = holder as ReceivedHolder
                holder.user_other.text = chatData[position].user
                holder.receivedTV.text = chatData[position].message
                val timeMilliSeconds = chatData[position].time
                val resultDate = Date(timeMilliSeconds)
                holder.timeStamp.text = df.format(resultDate)
            }

        }
    }

    override fun getItemViewType(position: Int): Int {

        when(chatData[position].type){
            Constants.MESSAGE_TYPE_SENT -> return SENT
            Constants.MESSAGE_TYPE_RECEIVED -> return RECEIVED
        }

        return -1
    }

    override fun getItemCount(): Int {
        return chatData.size
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