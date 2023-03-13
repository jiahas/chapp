package com.chapp.ui.chat


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.text.Editable
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chapp.MainActivity
import com.chapp.R
import com.chapp.databinding.FragmentChatBinding

class ChatFragment : Fragment() , View.OnClickListener {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatInput: EditText
    private lateinit var sendButton: FrameLayout
    private var sendListener: CommunicationListener? = null
    private var chatAdapter: ChatAdapter? = null
    private lateinit var recyclerviewChat: RecyclerView
    private val messageList = arrayListOf<Message>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val mainActivity = (activity as MainActivity)
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val mView: View = binding.root
        mainActivity.chatFragment = this
        initViews(mView, mainActivity)
        return mView
    }

    private fun initViews(mView: View, context: Context) {

        chatInput = mView.findViewById(R.id.chatInput)
        val chatIcon: ImageView = mView.findViewById(R.id.sendIcon)
        sendButton = mView.findViewById(R.id.sendButton)
        recyclerviewChat = mView.findViewById(R.id.chatRecyclerView)
        sendButton.isClickable = false
        sendButton.isEnabled = false

        val llm = LinearLayoutManager(context)
        llm.reverseLayout = true
        recyclerviewChat.layoutManager = llm

        chatInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun afterTextChanged(s: Editable) {

                if (s.isNotEmpty()) {
                    chatIcon.setImageDrawable(context.getDrawable(R.drawable.ic_send))
                    sendButton.isClickable = true
                    sendButton.isEnabled = true
                }else {
                    chatIcon.setImageDrawable(context.getDrawable(R.drawable.ic_send_depri))
                    sendButton.isClickable = false
                    sendButton.isEnabled = false
                }
            }
        })

        sendButton.setOnClickListener(this)
        chatAdapter = ChatAdapter(messageList.reversed(),context)
        recyclerviewChat.adapter = chatAdapter

    }

    override fun onClick(p0: View?) {

        if (chatInput.text.isNotEmpty()){
            sendListener?.onCommunication(chatInput.text.toString())
            chatInput.setText("")
        }

    }

    interface CommunicationListener{
        fun onCommunication(message: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sendListener = context as CommunicationListener
     }

    fun communicate(message: Message){
        messageList.add(message)
        if(activity != null) {
            chatAdapter = ChatAdapter(messageList.reversed(), activity as MainActivity)
            recyclerviewChat.adapter = chatAdapter
            recyclerviewChat.scrollToPosition(0)
        }
    }
}