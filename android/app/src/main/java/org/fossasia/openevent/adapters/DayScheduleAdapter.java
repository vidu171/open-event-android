package org.fossasia.openevent.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.fossasia.openevent.R;
import org.fossasia.openevent.adapters.viewholders.DayScheduleViewHolder;
import org.fossasia.openevent.adapters.viewholders.HeaderViewHolder;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.dbutils.RealmDataRepository;
import org.fossasia.openevent.listeners.OnBookmarkSelectedListener;
import org.fossasia.openevent.utils.DateConverter;
import org.fossasia.openevent.utils.SortOrder;
import org.fossasia.openevent.utils.Utils;
import org.fossasia.openevent.views.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by Manan Wason on 17/06/16.
 */
public class DayScheduleAdapter extends BaseRVAdapter<Session, DayScheduleViewHolder> implements StickyRecyclerHeadersAdapter<HeaderViewHolder> {

    private Context context;
    private String eventDate;
    private OnBookmarkSelectedListener onBookmarkSelectedListener;

    private RealmDataRepository realmRepo = RealmDataRepository.getDefaultInstance();

    private ArrayList<String> tracks = new ArrayList<>();
    private List<Session> copyOfSessions = new ArrayList<>();

    public DayScheduleAdapter(List<Session> sessions, Context context) {
        super(sessions);
        this.copyOfSessions = new ArrayList<>(sessions);
        this.context = context;
    }

    public void setCopy(List<Session> sessions) {
        copyOfSessions = sessions;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    @Override
    public DayScheduleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_schedule, parent, false);
        return new DayScheduleViewHolder(view,context, onBookmarkSelectedListener);
    }

    @Override
    public void onBindViewHolder(DayScheduleViewHolder holder, int position) {
        Session currentSession = getItem(position);
        holder.setSession(currentSession);
        holder.bindSession(realmRepo);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    public void filter(String constraint) {
        final String query = constraint.toLowerCase(Locale.getDefault());

        ((RealmResults<Session>) copyOfSessions).sort(SortOrder.sortTypeSchedule());

        List<Session> filteredSessions = Observable.fromIterable(copyOfSessions)
                .filter(session -> session.getTitle().toLowerCase().contains(query))
                .toList().blockingGet();

        Timber.d("Filtering done total results %d", filteredSessions.size());

        if (filteredSessions.isEmpty()) {
            Timber.e("No results published. There is an error in query. Check " + getClass().getName() + " filter!");
        }

        animateTo(filteredSessions);
    }

    @Override
    public long getHeaderId(int position) {
        String id = "";
        if (SortOrder.sortTypeSchedule().equals(Session.TITLE)) {
            return getItem(position).getTitle().toUpperCase().charAt(0);
        } else if (SortOrder.sortTypeSchedule().equals(Session.TRACK)){
            if (tracks != null && !tracks.contains(getItem(position).getTrack().getName())) {
                tracks.add(getItem(position).getTrack().getName());
            }
            return tracks.indexOf(getItem(position).getTrack().getName());
        } else if (SortOrder.sortTypeSchedule().equals(Session.START_TIME)) {
            id = DateConverter.formatDateWithDefault(DateConverter.FORMAT_24H, getItem(position).getStartsAt(), "")
                    .replace(":", "")
                    .replace(" ", "");
        }
        return Long.valueOf(id);
    }

    @Override
    public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_header, parent, false);
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(HeaderViewHolder holder, int position) {
        String sortTitle = Utils.checkStringEmpty(getItem(position).getTitle());
        String sortName = Utils.checkStringEmpty(getItem(position).getTrack().getName());

        if (SortOrder.sortTypeSchedule().equals(Session.TITLE) && (!Utils.isEmpty(sortTitle))) {
            holder.header.setText(String.valueOf(sortTitle.toUpperCase().charAt(0)));
        } else if (SortOrder.sortTypeSchedule().equals(Session.TRACK)){
            holder.header.setText(String.valueOf(sortName));
        } else if (SortOrder.sortTypeSchedule().equals(Session.START_TIME)) {
            holder.header.setText(DateConverter.formatDateWithDefault(DateConverter.FORMAT_24H, getItem(position).getStartsAt()));
        }
    }

    public void setOnBookmarkSelectedListener(OnBookmarkSelectedListener onBookmarkSelectedListener) {
        this.onBookmarkSelectedListener = onBookmarkSelectedListener;
    }
}