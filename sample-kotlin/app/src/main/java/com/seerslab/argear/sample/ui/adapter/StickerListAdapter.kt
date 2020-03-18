package com.seerslab.argear.sample.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.model.ItemModel
import java.util.*

class StickerListAdapter(
    private val context: Context?,
    private val listListener: Listener?
) :
    RecyclerView.Adapter<StickerListAdapter.ViewHolder>() {

    companion object {
        private val TAG = StickerListAdapter::class.java.simpleName
    }

    private val itemModels: MutableList<ItemModel> = ArrayList()

    interface Listener {
        fun onStickerSelected(position: Int, item: ItemModel?)
    }

    fun setData(items: List<ItemModel>?) {
        items?.let {
            itemModels.clear()
            itemModels.addAll(items)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return itemModels.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
        return StickerViewHolder(v)
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    abstract class ViewHolder(v: View) :
        RecyclerView.ViewHolder(v) {
        abstract fun bind(position: Int)
    }

    inner class StickerViewHolder(v: View) :
        ViewHolder(v), View.OnClickListener {

        private var imageViewItemThumbnail: ImageView = v.findViewById(R.id.item_thumbnail_imageview)
        private var itemModel: ItemModel? = null
        private var itemPosition = 0

        override fun bind(position: Int) {
            itemModel = itemModels[position]
            itemPosition = position

            Log.d(TAG, "item_sticker " + position + " " + itemModel?.thumbnailUrl + " " + itemModel)
            imageViewItemThumbnail.setOnClickListener(this)

            //스티커의 섬네일을 보여줍니다
            context?.let {
                Glide.with(context)
                    .load(itemModel?.thumbnailUrl)
                    .fitCenter()
                    .into(imageViewItemThumbnail)
            }
        }

        override fun onClick(v: View) {
            listListener?.onStickerSelected(itemPosition, itemModel)
        }
    }
}