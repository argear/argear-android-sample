package com.seerslab.argear.sample.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seerslab.argear.sample.AppConfig
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.data.BeautyItemData
import com.seerslab.argear.sample.databinding.FragmentBeautyBinding
import com.seerslab.argear.sample.ui.adapter.BeautyListAdapter
import com.seerslab.argear.session.ARGContents
import com.seerslab.argear.session.ARGContents.BeautyType
import com.seerslab.argear.session.ARGFrame
import java.util.*

class BeautyFragment : Fragment(),
    View.OnClickListener,
    BeautyListAdapter.Listener {

    companion object {
        private val TAG = BeautyFragment::class.java.simpleName
        const val BEAUTY_PARAM1 = "bearuty_param1"
    }

    private lateinit var beautyListAdapter: BeautyListAdapter
    private var beautyItemData: BeautyItemData? = null
    private var currentBeautyType = BeautyType.VLINE
    private var screenRatio: ARGFrame.Ratio = ARGFrame.Ratio.RATIO_4_3

    private lateinit var rootView: FragmentBeautyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenRatio = arguments?.getSerializable(BEAUTY_PARAM1) as ARGFrame.Ratio? ?: ARGFrame.Ratio.RATIO_4_3
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_beauty, container, false)

        rootView.beautySeekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateBeautyInfoPosition(rootView.beautyLevelInfo, progress)
                    beautyItemData?.setBeautyValue(currentBeautyType, progress.toFloat())
                    (activity as CameraActivity).setBeauty(beautyItemData?.getBeautyValues())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                rootView.beautyLevelInfo.visibility = View.VISIBLE
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                rootView.beautyLevelInfo.visibility = View.GONE
            }
        })

        rootView.beautyInitButton.setOnClickListener(this)
        rootView.beautyCloseButton.setOnClickListener(this)
        rootView.beautyComparisonButton.setOnTouchListener { _, event ->
            if (MotionEvent.ACTION_DOWN == event.action) {
                zeroBeautyParam()
            } else if (MotionEvent.ACTION_UP == event.action) {
                reloadBeauty()
            }
            true
        }

        val recyclerViewBeauty: RecyclerView = rootView.beautyItemsLayout
        recyclerViewBeauty.setHasFixedSize(true)

        val beautyLayoutManager = LinearLayoutManager(context)
        beautyLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerViewBeauty.layoutManager = beautyLayoutManager

        beautyListAdapter = BeautyListAdapter(this)
        recyclerViewBeauty.adapter = beautyListAdapter

        return rootView.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        beautyItemData = (activity as CameraActivity).beautyItemData
        beautyListAdapter.setData(beautyItemData?.getItemInfoData())
        beautyListAdapter.selectItem(BeautyType.VLINE)

        updateUIStyle(screenRatio)
        onBeautyItemSelected(BeautyType.VLINE)
        reloadBeauty()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.beauty_init_button -> {
                (activity as CameraActivity).setBeauty(AppConfig.BEAUTY_TYPE_INIT_VALUE)
                beautyItemData?.initBeautyValue()
                rootView.beautySeekbar.progress = beautyItemData?.getBeautyValue(currentBeautyType)?.toInt() ?: 0
            }
            R.id.beauty_close_button -> activity?.onBackPressed()
        }
    }

    override fun onBeautyItemSelected(beautyType: BeautyType?) {
        val values = ARGContents.BEAUTY_RANGE[beautyType] ?: return
        if (beautyType == null) {
            return
        }
        currentBeautyType = beautyType
        rootView.beautySeekbar.minValue = values[0]
        rootView.beautySeekbar.maxValue = values[1]
        rootView.beautySeekbar.progress = beautyItemData?.getBeautyValue(beautyType)?.toInt() ?: 0
    }

    override fun onGLViewRatio(): ARGFrame.Ratio {
        return screenRatio
    }

    private fun updateBeautyInfoPosition(view: TextView?, progress: Int) {
        view?.let {
            val max: Int = rootView.beautySeekbar.maxValue - rootView.beautySeekbar.minValue
            view.text = String.format(Locale.getDefault(), "%d", progress)

            val paddingLeft = 0
            val paddingRight = 0
            val offset = -5
            val viewWidth = view.width
            val x: Int =
                (((rootView.beautySeekbar.right - rootView.beautySeekbar.left - paddingLeft - paddingRight - viewWidth - 2 * offset).toFloat()
                        * (progress - rootView.beautySeekbar.minValue) / max).toInt()
                        + rootView.beautySeekbar.left + paddingLeft + offset)
            view.x = x.toFloat()
        }
    }

    private fun zeroBeautyParam() {
        (activity as CameraActivity).setBeauty(FloatArray(ARGContents.BEAUTY_TYPE_NUM))
    }

    private fun reloadBeauty() {
        (activity as CameraActivity).setBeauty(beautyItemData?.getBeautyValues())
    }

    fun updateUIStyle(ratio: ARGFrame.Ratio) {
        screenRatio = ratio
        if (ratio == ARGFrame.Ratio.RATIO_FULL) {
            rootView.beautySeekbar.isActivated = false
            rootView.beautyLevelInfo.isActivated = false
            rootView.beautyLevelInfo.setTextColor(Color.BLACK)
        } else {
            rootView.beautySeekbar.isActivated = true
            rootView.beautyLevelInfo.isActivated = true
            rootView.beautyLevelInfo.setTextColor(Color.WHITE)
        }
        beautyListAdapter.notifyDataSetChanged()
    }
}