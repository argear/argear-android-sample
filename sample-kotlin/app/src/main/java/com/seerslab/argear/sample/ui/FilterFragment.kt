package com.seerslab.argear.sample.ui

import android.os.Bundle
import android.text.TextUtils
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
import com.seerslab.argear.sample.databinding.FragmentFilterBinding
import com.seerslab.argear.sample.model.ItemModel
import com.seerslab.argear.sample.ui.adapter.FilterListAdapter
import com.seerslab.argear.sample.viewmodel.ContentsViewModel

class FilterFragment : Fragment(),
    View.OnClickListener,
    FilterListAdapter.Listener {

    companion object {
        private val TAG = FilterFragment::class.java.simpleName
    }

    private lateinit var filterListAdapter: FilterListAdapter
    private lateinit var contentsViewModel: ContentsViewModel
    private lateinit var rootView: FragmentFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_filter, container, false)

        rootView.closeFilterButton.setOnClickListener(this)
        rootView.clearFilterButton.setOnClickListener(this)
        rootView.filterPlusButton.setOnClickListener(this)
        rootView.filterMinusButton.setOnClickListener(this)
        rootView.vignettButton.setOnClickListener(this)
        rootView.blurButton.setOnClickListener(this)

        val recyclerViewFilter: RecyclerView = rootView.filterRecyclerview
        recyclerViewFilter.setHasFixedSize(true)

        val filterLayoutManager = LinearLayoutManager(context)
        filterLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerViewFilter.layoutManager = filterLayoutManager

        filterListAdapter = FilterListAdapter(context, this)
        recyclerViewFilter.adapter = filterListAdapter

        return rootView.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let { it ->
            contentsViewModel = ViewModelProvider(it).get(ContentsViewModel::class.java)
            contentsViewModel.contents.observe(
                viewLifecycleOwner,
                Observer<ContentsResponse?> { contentsResponse ->
                    contentsResponse?.categories?.let {
                        for (model in it) {
                            if (TextUtils.equals(model.title, "filters")) {
                                filterListAdapter.setData(model.items)
                                return@Observer
                            }
                        }
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
            R.id.close_filter_button -> activity?.onBackPressed()
            R.id.clear_filter_button -> (activity as CameraActivity).clearFilter()
            R.id.filter_plus_button -> (activity as CameraActivity).setFilterStrength(10)
            R.id.filter_minus_button -> (activity as CameraActivity).setFilterStrength(-10)
            R.id.vignett_button -> (activity as CameraActivity).setVignette()
            R.id.blur_button -> (activity as CameraActivity).setBlurVignette()
        }
    }

    override fun onFilterSelected(position: Int, item: ItemModel?) {
        item?.let {
            (activity as CameraActivity).setFilter(item)
        }
    }
}