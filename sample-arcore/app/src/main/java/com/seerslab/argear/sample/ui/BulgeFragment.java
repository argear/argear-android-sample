package com.seerslab.argear.sample.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.seerslab.argear.sample.R;

public class BulgeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = BulgeFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bulge, container, false);

        rootView.findViewById(R.id.close_bulge_button).setOnClickListener(this);
        rootView.findViewById(R.id.clear_bulge_button).setOnClickListener(this);
        rootView.findViewById(R.id.bulge_fun1_button).setOnClickListener(this);
        rootView.findViewById(R.id.bulge_fun2_button).setOnClickListener(this);
        rootView.findViewById(R.id.bulge_fun3_button).setOnClickListener(this);
        rootView.findViewById(R.id.bulge_fun4_button).setOnClickListener(this);
        rootView.findViewById(R.id.bulge_fun5_button).setOnClickListener(this);
        rootView.findViewById(R.id.bulge_fun6_button).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
            case R.id.close_bulge_button:
                getActivity().onBackPressed();
                break;
            case R.id.clear_bulge_button:
                ((CameraActivity)getActivity()).clearBulge();
                break;
            case R.id.bulge_fun1_button :
                ((CameraActivity)getActivity()).setBulgeFunType(1);
                break;
            case R.id.bulge_fun2_button :
                ((CameraActivity)getActivity()).setBulgeFunType(2);
                break;
            case R.id.bulge_fun3_button :
                ((CameraActivity)getActivity()).setBulgeFunType(3);
                break;
            case R.id.bulge_fun4_button :
                ((CameraActivity)getActivity()).setBulgeFunType(4);
                break;
            case R.id.bulge_fun5_button :
                ((CameraActivity)getActivity()).setBulgeFunType(5);
                break;
            case R.id.bulge_fun6_button :
                ((CameraActivity)getActivity()).setBulgeFunType(6);
                break;
        }
    }
}
