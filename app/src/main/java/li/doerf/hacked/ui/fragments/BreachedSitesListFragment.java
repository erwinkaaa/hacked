package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.Serializable;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.BreachedSite;
import li.doerf.hacked.remote.haveibeenpwned.HIBPGetBreachedSitesAsyncTask;
import li.doerf.hacked.ui.HibpInfo;
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter;

/**
 * Created by moo on 09/10/16.
 */
public class BreachedSitesListFragment extends Fragment {
    private static final String KEY_BREACH_LIST_TYPE = "BreachListType";
    private final String LOGTAG = getClass().getSimpleName();
    private SQLiteDatabase myReadbableDb;
    private BreachedSitesAdapter myBreachedSitesAdapter;
    private Cursor myCursor;
    private BreachListType myBreachListType;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    public static BreachedSitesListFragment create(BreachListType aType) {
        BreachedSitesListFragment fragment = new BreachedSitesListFragment();
        fragment.setMyBreachListType(aType);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myReadbableDb = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        myBreachedSitesAdapter = new BreachedSitesAdapter(getContext(), null);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breached_sites_list, container, false);

        RecyclerView breachedSites = (RecyclerView) view.findViewById(R.id.breached_sites_list);
        breachedSites.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        breachedSites.setLayoutManager(lm);
        breachedSites.setAdapter(myBreachedSitesAdapter);

        HibpInfo.prepare( getContext(), (TextView) view.findViewById(R.id.hibp_info), breachedSites);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(breachedSites.getContext(),
                lm.getOrientation());
        breachedSites.addItemDecoration(dividerItemDecoration);

        mySwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadBreachedSites();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if ( savedInstanceState != null ) {
            Serializable blt = savedInstanceState.getSerializable(KEY_BREACH_LIST_TYPE);
            if ( blt != null ) {
                myBreachListType = (BreachListType) blt;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        if ( myBreachedSitesAdapter.getCursor().getCount() == 0 || lastUpdateBeforeOneHour()) {
            reloadBreachedSites();
        }
        if ( myBreachListType == null ) {
            myBreachListType = BreachListType.MostRecent;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_BREACH_LIST_TYPE, myBreachListType);
    }

    private boolean lastUpdateBeforeOneHour() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        long lastsync = settings.getLong(getString(R.string.PREF_KEY_LAST_SYNC_HIBP_TOP20), 0);
        long now = System.currentTimeMillis();
        return lastsync < ( now - 60 * 60 * 1000 );
    }

    @Override
    public void onDetach() {
        if ( myCursor != null ) {
            myCursor.close();
        }
        myReadbableDb = null;

        super.onDetach();
    }

    public void refreshList() {
        Log.d(LOGTAG, "refreshing list");

        if ( myReadbableDb == null ) {
            Log.w(LOGTAG, "refreshList: readable db null, nothing to refresh");
            return;
        }

        switch ( myBreachListType){
            case Top20:
                myCursor = BreachedSite.listTop20(myReadbableDb);
            break;

            case MostRecent:
                myCursor = BreachedSite.listMostRecent(myReadbableDb);
            break;

            case All:
            default:
                myCursor = BreachedSite.listAll(myReadbableDb);
        }
        if ( ! myCursor.isClosed() ) {
            Cursor old = null;
            try {
                old = myBreachedSitesAdapter.swapCursor(myCursor);
            } finally {
                if ( old != null ) {
                    old.close();
                }
            }
        } else {
            Log.w(LOGTAG, "cursor closed");
            myBreachedSitesAdapter.swapCursor(null);
        }
    }

    public void setMyBreachListType(BreachListType myBreachListType) {
        this.myBreachListType = myBreachListType;
    }

    public void reloadBreachedSites() {
        if ( ! mySwipeRefreshLayout.isRefreshing() ) {
            mySwipeRefreshLayout.setRefreshing(true);
        }
        new HIBPGetBreachedSitesAsyncTask(this).execute();
    }

    /**
     * Indicate that the refresh is complete to stop refresh animation.
     */
    public void refreshComplete() {
        mySwipeRefreshLayout.setRefreshing(false);
    }
}
