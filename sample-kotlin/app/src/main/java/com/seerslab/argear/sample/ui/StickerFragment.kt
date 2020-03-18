package com.seerslab.argear.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.api.ContentsResponse
import com.seerslab.argear.sample.databinding.FragmentStickerBinding
import com.seerslab.argear.sample.model.CategoryModel
import com.seerslab.argear.sample.model.ItemModel
import com.seerslab.argear.sample.ui.adapter.StickerCategoryListAdapter
import com.seerslab.argear.sample.ui.adapter.StickerListAdapter
import com.seerslab.argear.sample.viewmodel.ContentsViewModel

class StickerFragment : Fragment(),
    View.OnClickListener,
    StickerCategoryListAdapter.Listener, StickerListAdapter.Listener {

    private lateinit var stickerCategoryListAdapter: StickerCategoryListAdapter
    private lateinit var stickerListAdapter: StickerListAdapter
    private lateinit var contentsViewModel: ContentsViewModel

    private lateinit var rootView: FragmentStickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_sticker, container, false)

        rootView.closeStickerButton.setOnClickListener(this)
        rootView.clearStickerButton.setOnClickListener(this)

        // init category_sticker list
        val recyclerViewStickerCategory: RecyclerView = rootView.stickerCategoryRecyclerview
        recyclerViewStickerCategory.setHasFixedSize(true)

        val categoryLayoutManager = LinearLayoutManager(context)
        categoryLayoutManager.orientation = LinearLayoutManager.HORIZONTAL

        recyclerViewStickerCategory.layoutManager = categoryLayoutManager
        stickerCategoryListAdapter = StickerCategoryListAdapter(this)
        recyclerViewStickerCategory.adapter = stickerCategoryListAdapter

        // init item_sticker list
        val recyclerViewSticker: RecyclerView = rootView.stickerRecyclerview
        recyclerViewSticker.setHasFixedSize(true)

        val itemsLayoutManager = LinearLayoutManager(context)
        itemsLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerViewSticker.layoutManager = itemsLayoutManager

        stickerListAdapter = StickerListAdapter(context, this)
        recyclerViewSticker.adapter = stickerListAdapter

        return rootView.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            contentsViewModel = ViewModelProvider(it).get(ContentsViewModel::class.java)
            contentsViewModel.contents.observe(
                viewLifecycleOwner,
                Observer<ContentsResponse?> { contentsResponse ->
                    contentsResponse?.categories?.let {
                        stickerCategoryListAdapter.setData(contentsResponse.categories)
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.close_sticker_button -> activity?.onBackPressed()
            R.id.clear_sticker_button -> {
                (activity as CameraActivity).clearStickers()
            }
        }
    }

    override fun onCategorySelected(category: CategoryModel?) {
        category?.let {
            stickerListAdapter.setData(category.items)
        }
    }

    override fun onStickerSelected(position: Int, item: ItemModel?) {
        item?.let {
            (activity as CameraActivity).setSticker(item)
        }
    }

    companion object {
        private val TAG = StickerFragment::class.java.simpleName
    }
}