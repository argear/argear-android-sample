package com.seerslab.argear.sample.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.seerslab.argear.sample.model.CategoryModel;
import com.seerslab.argear.sample.viewmodel.ContentsViewModel;
import com.seerslab.argear.sample.R;
import com.seerslab.argear.sample.api.ContentsResponse;
import com.seerslab.argear.sample.model.ItemModel;
import com.seerslab.argear.sample.ui.adapter.FilterListAdapter;

import java.util.List;

public class FilterFragment
        extends Fragment
        implements View.OnClickListener, FilterListAdapter.Listener {

    private static final String TAG = FilterFragment.class.getSimpleName();

    private FilterListAdapter mFilterListAdapter;
    private ContentsViewModel mContentsViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_filter, container, false);

        rootView.findViewById(R.id.close_filter_button).setOnClickListener(this);
        rootView.findViewById(R.id.clear_filter_button).setOnClickListener(this);
        rootView.findViewById(R.id.filter_plus_button).setOnClickListener(this);
        rootView.findViewById(R.id.filter_minus_button).setOnClickListener(this);
        rootView.findViewById(R.id.vignett_button).setOnClickListener(this);
        rootView.findViewById(R.id.blur_button).setOnClickListener(this);

        RecyclerView recyclerViewFilter = rootView.findViewById(R.id.filter_recyclerview);

        recyclerViewFilter.setHasFixedSize(true);
        LinearLayoutManager filterLayoutManager = new LinearLayoutManager(getContext());
        filterLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewFilter.setLayoutManager(filterLayoutManager);

        mFilterListAdapter = new FilterListAdapter(getContext(), this);
        recyclerViewFilter.setAdapter(mFilterListAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            mContentsViewModel = new ViewModelProvider(getActivity()).get(ContentsViewModel.class);
            mContentsViewModel.getContents().observe(getViewLifecycleOwner(), new Observer<ContentsResponse>() {
                @Override
                public void onChanged(ContentsResponse contentsResponse) {

                    if (contentsResponse == null) return;

                    for (CategoryModel model : contentsResponse.categories) {
                        if (TextUtils.equals(model.title, "filters")) {
                            mFilterListAdapter.setData(model.items);
                            return;
                        }
                    }

                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_filter_button:
                getActivity().onBackPressed();
                break;
            case R.id.clear_filter_button: {
                ((CameraActivity)getActivity()).clearFilter();
                break;
            }
            case R.id.filter_plus_button:
                ((CameraActivity)getActivity()).setFilterStrength(10);
                break;
            case R.id.filter_minus_button:
                ((CameraActivity)getActivity()).setFilterStrength(-10);
                break;
            case R.id.vignett_button:
                ((CameraActivity)getActivity()).setVignette();
                break;
            case R.id.blur_button:
                ((CameraActivity)getActivity()).setBlurVignette();
                break;
        }
    }

    @Override
    public void onFilterSelected(int position, ItemModel item) {
        ((CameraActivity)getActivity()).setFilter(item);
    }
}
