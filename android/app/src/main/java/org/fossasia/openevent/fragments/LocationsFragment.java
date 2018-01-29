package org.fossasia.openevent.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.R;
import org.fossasia.openevent.adapters.LocationsListAdapter;
import org.fossasia.openevent.api.DataDownloadManager;
import org.fossasia.openevent.data.Microlocation;
import org.fossasia.openevent.events.MicrolocationDownloadEvent;
import org.fossasia.openevent.utils.NetworkUtils;
import org.fossasia.openevent.utils.Utils;
import org.fossasia.openevent.utils.Views;
import org.fossasia.openevent.viewmodels.LocationsFragmentViewModel;
import org.fossasia.openevent.views.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import timber.log.Timber;

/**
 * User: MananWason
 * Date: 8/18/2015
 */
public class LocationsFragment extends BaseFragment implements SearchView.OnQueryTextListener {

    final private String SEARCH = "searchText";

    @BindView(R.id.locations_swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.list_locations)
    protected RecyclerView locationsRecyclerView;
    @BindView(R.id.txt_no_microlocations)
    protected TextView noMicrolocationsView;
    @BindView(R.id.txt_no_result_locations)
    protected TextView noResultsView;

    private List<Microlocation> locations = new ArrayList<>();
    private LocationsListAdapter locationsListAdapter;

    private String searchText = "";
    private SearchView searchView;

    private LocationsFragmentViewModel locationsFragmentViewModel;
    private RecyclerView.AdapterDataObserver adapterDataObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = super.onCreateView(inflater, container, savedInstanceState);

        Utils.registerIfUrlValid(swipeRefreshLayout, this, this::refresh);
        setUpRecyclerView();

        //set up view model
        locationsFragmentViewModel = ViewModelProviders.of(this).get(LocationsFragmentViewModel.class);
        searchText = locationsFragmentViewModel.getSearchText();
        loadLocations();
        handleVisibility();
        return view;
    }

    private void loadLocations() {
        locationsFragmentViewModel.getLocations(searchText).observe(LocationsFragment.this, microlocations ->  {
            locations.clear();
            locations.addAll(microlocations);
            locationsListAdapter.setCopyOfTracks(microlocations);
            locationsListAdapter.notifyDataSetChanged();
            handleVisibility();
        });
    }

    private void setUpRecyclerView() {
        locationsListAdapter = new LocationsListAdapter(getContext(), locations);

        locationsRecyclerView.setHasFixedSize(true);
        locationsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        locationsRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        locationsRecyclerView.setAdapter(locationsListAdapter);

        final StickyRecyclerHeadersDecoration headersDecoration = new StickyRecyclerHeadersDecoration(locationsListAdapter);
        locationsRecyclerView.addItemDecoration(headersDecoration);
        adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                headersDecoration.invalidateHeaders();
            }
        };
        locationsListAdapter.registerAdapterDataObserver(adapterDataObserver);
    }

    private void handleVisibility() {
        if (locationsListAdapter.getItemCount() != 0) {
            noMicrolocationsView.setVisibility(View.GONE);
            locationsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            noMicrolocationsView.setVisibility(View.VISIBLE);
            locationsRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.list_locations;
    }

    public void setVisibility(Boolean isDownloadDone) {
        if (isDownloadDone) {
            noMicrolocationsView.setVisibility(View.GONE);
            locationsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            noMicrolocationsView.setVisibility(View.VISIBLE);
            locationsRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (isAdded() && searchView != null && locationsFragmentViewModel != null) {
            locationsFragmentViewModel.setSearchText(searchText);
        }
        super.onSaveInstanceState(bundle);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_locations_fragment, menu);
        MenuItem item = menu.findItem(R.id.action_search_locations);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        DrawableCompat.setTint(menu.findItem(R.id.action_search_locations).getIcon(), Color.WHITE);
        searchView.setOnQueryTextListener(this);
        searchView.setQuery(searchText, false);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searchText = query;
        loadLocations();
        locationsListAdapter.animateTo(locations);

        Utils.displayNoResults(noResultsView, locationsRecyclerView, noMicrolocationsView, locationsListAdapter.getItemCount());

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Utils.unregisterIfUrlValid(this);
        locationsListAdapter.unregisterAdapterDataObserver(adapterDataObserver);

        // Remove listeners to fix memory leak
        if (swipeRefreshLayout != null) swipeRefreshLayout.setOnRefreshListener(null);
        if (searchView != null) searchView.setOnQueryTextListener(null);
    }

    @Subscribe
    public void onLocationsDownloadDone(MicrolocationDownloadEvent event) {
        Views.setSwipeRefreshLayout(swipeRefreshLayout, false);

        if (event.isState()) {
            Timber.d("Locations download completed");
        } else {
            Timber.d("Locations download failed");
            if (getActivity() != null && swipeRefreshLayout != null) {
                Snackbar.make(swipeRefreshLayout, getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, view -> refresh()).show();
            }
        }
    }

    private void refresh() {
        NetworkUtils.checkConnection(new WeakReference<>(getContext()), new NetworkUtils.NetworkStateReceiverListener() {

            @Override
            public void networkAvailable() {
                // Network is available
                DataDownloadManager.getInstance().downloadMicrolocations();
            }

            @Override
            public void networkUnavailable() {
                OpenEventApp.getEventBus().post(new MicrolocationDownloadEvent(false));
            }
        });
    }
}
