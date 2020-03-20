package com.seerslab.argear.sample.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.seerslab.argear.sample.AppConfig
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.data.BeautyItemData
import com.seerslab.argear.session.ARGContents.BeautyType
import com.seerslab.argear.session.ARGFrame
import java.util.*

class BeautyListAdapter(private val listListener: Listener?) :
    RecyclerView.Adapter<BeautyListAdapter.ViewHolder?>() {

    private val adapterData: ArrayList<BeautyItemData.BeautyItemInfo> = ArrayList()
    private var selectedIndex = -1

    interface Listener {
        fun onBeautyItemSelected(beautyType: BeautyType?)
        fun onGLViewRatio(): ARGFrame.Ratio
    }

    override fun getItemCount(): Int {
        return adapterData.size
    }

    fun setData(data: List<BeautyItemData.BeautyItemInfo>?) {
        data?.let {
            adapterData.clear()
            adapterData.addAll(it)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_beauty, parent, false)
        return BeautyItemViewHolder(v)
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    fun selectItem(beautyType: BeautyType) {
        for (i in adapterData.indices) {
            val itemInfo: BeautyItemData.BeautyItemInfo = adapterData[i]
            if (beautyType == itemInfo.beautyType) {
                selectedIndex = i
                break
            }
        }
        if (selectedIndex != -1) {
            notifyDataSetChanged()
        }
    }

    abstract class ViewHolder(v: View) :
        RecyclerView.ViewHolder(v) {
        abstract fun bind(position: Int)
    }

    inner class BeautyItemViewHolder(v: View) :
        ViewHolder(v), View.OnClickListener {

        private var itemButton: Button = v.findViewById(R.id.beauty_item_button)
        private var itemInfo: BeautyItemData.BeautyItemInfo = BeautyItemData.BeautyItemInfo(
            BeautyType.VLINE,
            0,
            0
        )

        override fun bind(position: Int) {
            itemInfo = adapterData[position]

            if (listListener?.onGLViewRatio() == ARGFrame.Ratio.RATIO_FULL) {
                if (selectedIndex == position) {
                    itemButton.setBackgroundResource(itemInfo.resource2)
                } else {
                    itemButton.setBackgroundResource(itemInfo.resource1)
                }
            } else {
                if (selectedIndex == position) {
                    itemButton.setBackgroundResource(itemInfo.resource1)
                } else {
                    itemButton.setBackgroundResource(itemInfo.resource2)
                }
            }

            itemButton.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if (listListener?.onGLViewRatio() == ARGFrame.Ratio.RATIO_FULL) {
                itemButton.setBackgroundResource(itemInfo.resource2)
            } else {
                itemButton.setBackgroundResource(itemInfo.resource1)
            }

            notifyItemChanged(selectedIndex)
            selectedIndex = layoutPosition

            listListener?.onBeautyItemSelected(itemInfo.beautyType)
        }
    }
}