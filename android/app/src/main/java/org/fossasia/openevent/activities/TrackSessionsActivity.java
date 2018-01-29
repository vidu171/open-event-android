package org.fossasia.openevent.activities;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.R;
import org.fossasia.openevent.adapters.SessionsListAdapter;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.data.Track;
import org.fossasia.openevent.dbutils.RealmDataRepository;
import org.fossasia.openevent.events.BookmarkChangedEvent;
import org.fossasia.openevent.utils.ConstantStrings;
import org.fossasia.openevent.utils.Views;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.realm.RealmObjectChangeListener;
import timber.log.Timber;


/**
 * User: MananWason
 * Date: 14-06-2015
 */
public class TrackSessionsActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    final private String SEARCH = "org.fossasia.openevent.searchText";

    private SessionsListAdapter sessionsListAdapter;

    private GridLayoutManager gridLayoutManager;

    private List<Session> mSessions = new ArrayList<>();

    private String searchText;

    private SearchView searchView;

    private static final int trackWiseSessionList = 4;
    private int trackId;

    private RealmDataRepository realmRepo = RealmDataRepository.getDefaultInstance();
    private Track track;

    private ActionBar actionBar;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.recyclerView) RecyclerView sessionsRecyclerView;
    @BindView(R.id.txt_no_sessions) TextView noSessionsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(null);

        setSupportActionBar(toolbar);
        String track = getIntent().getStringExtra(ConstantStrings.TRACK);

        actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (!TextUtils.isEmpty(track))
                actionBar.setTitle(track);
        }
        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }

        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);

        sessionsRecyclerView.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(this, spanCount);
        sessionsRecyclerView.setLayoutManager(gridLayoutManager);
        sessionsListAdapter = new SessionsListAdapter(this, mSessions, trackWiseSessionList);
        if(searchText!=null){
            sessionsListAdapter.getFilter().filter(searchText);
        }
        sessionsRecyclerView.setAdapter(sessionsListAdapter);
        sessionsRecyclerView.scrollToPosition(SessionsListAdapter.listPosition);
        sessionsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        trackId = getIntent().getIntExtra(ConstantStrings.TRACK_ID, -1);

        sessionsListAdapter.setTrackId(trackId);

        loadData();

        handleVisibility();
    }

    private void loadData() {
        track = realmRepo.getTrack(trackId);
        track.removeAllChangeListeners();
        track.addChangeListener((RealmObjectChangeListener<Track>) (track, objectChangeSet) -> {
            int color = Color.parseColor(track.getColor());
            setUiColor(color);

            actionBar.setTitle(track.getName());

            mSessions.clear();
            mSessions.addAll(track.getSessions().sort("startTime"));
            sessionsListAdapter.notifyDataSetChanged();

            handleVisibility();
        });
    }

    private void handleVisibility() {
        if (!mSessions.isEmpty()) {
            noSessionsView.setVisibility(View.GONE);
            sessionsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            noSessionsView.setVisibility(View.VISIBLE);
            sessionsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void setUiColor(int color) {
        toolbar.setBackgroundColor(color);

        sessionsListAdapter.setColor(color);

        if (Views.isCompatible(Build.VERSION_CODES.LOLLIPOP)) {
            getWindow().setStatusBarColor(Views.getDarkColor(color));
            sessionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    Views.setEdgeGlowColorRecyclerView(sessionsRecyclerView, color);
                }
            });

        }
    }

    @Subscribe
    public void onBookmarksChanged(BookmarkChangedEvent bookmarkChangedEvent) {
        Timber.d("Bookmarks Changed");
        loadData();
    }

    @Override
    public void onStart() {
        super.onStart();
        OpenEventApp.getEventBus().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        OpenEventApp.getEventBus().unregister(this);
        if(track != null) track.removeAllChangeListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionsListAdapter.notifyDataSetChanged();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_tracks;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (searchView != null) {
            bundle.putString(SEARCH, searchText);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search_sessions:
                return true;
            default:
                //Do nothing
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search_tracks).getActionView();
        searchView.setOnQueryTextListener(this);
        if (searchText != null) {
            searchView.setQuery(searchText, false);
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);
        gridLayoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        sessionsListAdapter.getFilter().filter(query);

        searchText = query;
        return true;
    }
}