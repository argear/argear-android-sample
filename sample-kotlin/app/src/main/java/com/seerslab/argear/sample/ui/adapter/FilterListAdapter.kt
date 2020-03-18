package com.seerslab.argear.sample.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.model.ItemModel
import java.util.*

class FilterListAdapter(
    private val context: Context?,
    private val listListener: Listener?
) :
    RecyclerView.Adapter<FilterListAdapter.ViewHolder>() {

    companion object {
        private val TAG = FilterListAdapter::class.java.simpleName
    }

    private val mItems: MutableList<ItemModel> = ArrayList()

    interface Listener {
        fun onFilterSelected(position: Int, item: ItemModel?)
    }

    fun setData(items: List<ItemModel>?) {
        items?.let {
            mItems.clear()
            mItems.addAll(items)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int ): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_filter, parent, false)
        return ItemViewHolder(v)
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    abstract class ViewHolder(v: View) :
        RecyclerView.ViewHolder(v) {
        abstract fun bind(position: Int)
    }

    inner class ItemViewHolder(v: View) :
        ViewHolder(v), View.OnClickListener {

        private var imageViewItemThumbnail: ImageView = v.findViewById<View>(R.id.item_thumbnail_imageview) as ImageView
        private var textViewTitle: TextView = v.findViewById<View>(R.id.title_textview) as TextView
        private var itemModel: ItemModel? = null
        private var itemPosition = 0

        override fun bind(position: Int) {
            itemModel = mItems[position]
            itemPosition = position

            Log.d( TAG, "item_filter " + position + " " + itemModel?.thumbnailUrl + " " + itemModel)
            imageViewItemThumbnail.setOnClickListener(this)

            //필터의 섬네일과 이름을 표시합니다 .
            context?.let {
                Glide.with(context)
                    .load(itemModel?.thumbnailUrl)
                    .fitCenter()
                    .into(imageViewItemThumbnail)
            }

            textViewTitle.text = itemModel?.title
        }

        override fun onClick(v: View) {
            listListener?.onFilterSelected(itemPosition, itemModel)
        }
    }
}