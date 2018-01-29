package org.fossasia.openevent.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.leakcanary.RefWatcher;

import org.fossasia.openevent.OpenEventApp;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseFragment extends Fragment {

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().getWindow().setBackgroundDrawable(null);
        View view = inflater.inflate(getLayoutResource(), container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    protected abstract int getLayoutResource();

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();

        RefWatcher refWatcher = OpenEventApp.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }



}
