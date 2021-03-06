/*
 * Copyright (C) 2014 Eduardo Barrenechea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.barrenechea.ticker.ui;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ca.barrenechea.ticker.R;
import ca.barrenechea.ticker.data.Event;
import ca.barrenechea.ticker.event.OnEventEdit;
import ca.barrenechea.ticker.utils.ViewUtils;
import ca.barrenechea.ticker.widget.EventAdapter;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class EventListFragment extends BaseFragment implements RealmChangeListener {

    @InjectView(R.id.list)
    RecyclerView mRecyclerView;
    @InjectView(R.id.loading)
    View mLoadingView;
    @InjectView(R.id.empty)
    View mEmptyView;
    @InjectView(R.id.text_empty)
    TextView mTextEmpty;

    private String mSearchQuery;
    private boolean mSearchOpen = false;
    private boolean mSortOrder = RealmResults.SORT_ORDER_ASCENDING;

    private Realm mRealm;
    private EventAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_event, container, false);

        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new EventAdapter(this.getActivity(), mBus);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_list_event, menu);

        MenuItem search = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(search);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    search(s);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    search(s);
                    return true;
                }

                private void search(String s) {
                    mSearchQuery = s;
                    loadData();
                }
            });

            MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    mSearchOpen = true;
                    resetEmptyText();
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    mSearchOpen = false;
                    mSearchQuery = null;
                    resetEmptyText();
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add:
//                CreateEventDialog d = new CreateEventDialog();
//                d.show(this.getFragmentManager(), "EditEvent");
                mBus.post(new OnEventEdit(null));
                return true;

            case R.id.action_refresh:
                loadData();
                return true;

            case R.id.sort_start_asc:
                item.setChecked(true);
                mSortOrder = RealmResults.SORT_ORDER_ASCENDING;
                sort();
                return true;

            case R.id.sort_start_desc:
                item.setChecked(true);
                mSortOrder = RealmResults.SORT_ORDER_DECENDING;
                sort();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mRealm = Realm.getInstance(this.getActivity());
        mRealm.addChangeListener(this);
        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();

        mRealm.removeChangeListener(this);
    }

    private void loadData() {
        RealmQuery<Event> query = mRealm.where(Event.class);

        if (mSearchOpen) {
            query = query.contains(Event.COLUMN_NAME, mSearchQuery);
        }

        final RealmResults<Event> data = query.findAll().sort(Event.COLUMN_START, mSortOrder);
        mAdapter.setData(data);
        displayData();
    }

    private void sort() {
        mAdapter.sort(Event.COLUMN_START, mSortOrder);
    }

    private void displayData() {
        mAdapter.notifyDataSetChanged();

        if (mAdapter.getItemCount() > 0) {
            showList();
        } else {
            showEmpty();
        }
    }

    @Override
    public void onChange() {
        displayData();
    }

    private void showList() {
        if (mRecyclerView.getVisibility() == View.INVISIBLE) {
            ViewUtils.fadeIn(mRecyclerView);

            if (mLoadingView.getVisibility() == View.VISIBLE) {
                ViewUtils.fadeOut(mLoadingView);
            }

            if (mEmptyView.getVisibility() == View.VISIBLE) {
                ViewUtils.fadeOut(mEmptyView);
            }
        }
    }

    private void showEmpty() {
        resetEmptyText();

        if (mEmptyView.getVisibility() == View.INVISIBLE) {
            ViewUtils.fadeIn(mEmptyView);

            if (mLoadingView.getVisibility() == View.VISIBLE) {
                ViewUtils.fadeOut(mLoadingView);
            }

            if (mRecyclerView.getVisibility() == View.VISIBLE) {
                ViewUtils.fadeOut(mRecyclerView);
            }
        }
    }

    private void resetEmptyText() {
        if (!mSearchOpen) {
            mTextEmpty.setText(R.string.no_events);
        } else {
            if (TextUtils.isEmpty(mSearchQuery)) {
                mTextEmpty.setText(R.string.empty_search);
            } else {
                final String msg = String.format(Locale.getDefault(), this.getString(R.string.nothing_found), mSearchQuery);
                mTextEmpty.setText(msg);
            }
        }
    }
}
