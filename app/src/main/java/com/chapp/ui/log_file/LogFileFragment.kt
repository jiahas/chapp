package com.chapp.ui.log_file
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chapp.databinding.FragmentLogFileBinding

class LogFileFragment : Fragment() {

    private var _binding: FragmentLogFileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val logFileViewModel =
            ViewModelProvider(this)[LogFileViewModel::class.java]

        _binding = FragmentLogFileBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textLogFile
//        logFileViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}