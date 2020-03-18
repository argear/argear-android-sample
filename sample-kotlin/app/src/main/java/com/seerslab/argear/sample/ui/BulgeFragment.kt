package com.seerslab.argear.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.databinding.FragmentBulgeBinding

class BulgeFragment : Fragment(),
    View.OnClickListener {

    companion object {
        private val TAG = BulgeFragment::class.java.simpleName
    }

    private lateinit var rootView: FragmentBulgeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView  = DataBindingUtil.inflate(inflater, R.layout.fragment_bulge, container, false)

        rootView.closeBulgeButton.setOnClickListener(this)
        rootView.clearBulgeButton.setOnClickListener(this)
        rootView.bulgeFun1Button.setOnClickListener(this)
        rootView.bulgeFun2Button.setOnClickListener(this)
        rootView.bulgeFun3Button.setOnClickListener(this)
        rootView.bulgeFun4Button.setOnClickListener(this)
        rootView.bulgeFun5Button.setOnClickListener(this)
        rootView.bulgeFun6Button.setOnClickListener(this)

        return rootView.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.close_bulge_button -> activity?.onBackPressed()
            R.id.clear_bulge_button -> (activity as CameraActivity).closeBulge()
            R.id.bulge_fun1_button -> (activity as CameraActivity).setBulgeFunType(1)
            R.id.bulge_fun2_button -> (activity as CameraActivity).setBulgeFunType(2)
            R.id.bulge_fun3_button -> (activity as CameraActivity).setBulgeFunType(3)
            R.id.bulge_fun4_button -> (activity as CameraActivity).setBulgeFunType(4)
            R.id.bulge_fun5_button -> (activity as CameraActivity).setBulgeFunType(5)
            R.id.bulge_fun6_button -> (activity as CameraActivity).setBulgeFunType(6)
        }
    }
}