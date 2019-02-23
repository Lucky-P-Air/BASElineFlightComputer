package com.platypii.baseline.views.laser;

import com.platypii.baseline.Intents;
import com.platypii.baseline.R;
import com.platypii.baseline.cloud.CloudData;
import com.platypii.baseline.events.ProfileLayerEvent;
import com.platypii.baseline.laser.LaserLayers;
import com.platypii.baseline.tracks.TrackFile;
import com.platypii.baseline.util.Exceptions;
import com.platypii.baseline.views.charts.layers.TrackProfileLayerLocal;
import com.platypii.baseline.views.charts.layers.TrackProfileLayerRemote;
import com.platypii.baseline.views.charts.layers.LaserProfileLayer;
import com.platypii.baseline.views.tracks.TrackListFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.google.firebase.analytics.FirebaseAnalytics;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LaserPanelFragment extends ListFragment {
    private final FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

    @Nullable
    private ProfileAdapter listAdapter;
    private final LaserLayers layers = LaserLayers.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.laser_panel, container, false);
        view.findViewById(R.id.chooseTrack).setOnClickListener(this::chooseTrack);
        view.findViewById(R.id.chooseLaser).setOnClickListener(this::chooseLaser);
        view.findViewById(R.id.addLaser).setOnClickListener(this::clickAdd);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Initialize the ListAdapter
        final LaserActivity laserActivity = (LaserActivity) getActivity();
        if (laserActivity != null) {
            listAdapter = new ProfileAdapter(laserActivity);
            setListAdapter(listAdapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (listAdapter != null) {
            listAdapter.setLayers(layers.layers);
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        super.onListItemClick(parent, view, position, id);
        final Object item = parent.getItemAtPosition(position);
        if (item instanceof LaserProfileLayer) {
            // TODO: Open editor / view mode
            final LaserProfileLayer layer = (LaserProfileLayer) item;
            firebaseAnalytics.logEvent("click_laser_profile", null);
            final Bundle bundle = new Bundle();
            bundle.putString("laser_id", layer.laserProfile.laser_id);
            final Fragment frag = new LaserEditFragment();
            frag.setArguments(bundle);
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.laserPanel, frag)
                    .addToBackStack(null)
                    .commit();
        } else if (item instanceof TrackProfileLayerLocal) {
            // Open local track file charts
            final TrackFile track = ((TrackProfileLayerLocal) item).track;
            Intents.openCharts(getContext(), track.file);
        } else if (item instanceof TrackProfileLayerRemote) {
            // Open cloud track charts
            final CloudData track = ((TrackProfileLayerRemote) item).track;
            Intents.openCharts(getContext(), track.abbrvFile(getContext()));
        } else {
            Exceptions.report(new IllegalStateException("Unexpected list item type " + item));
        }
    }

    private void chooseTrack(View view) {
        firebaseAnalytics.logEvent("click_laser_track", null);
        final Fragment frag = new TrackPickerFragment();
        final Bundle args = new Bundle();
        args.putString(TrackListFragment.SEARCH_KEY, "Wingsuit BASE");
        frag.setArguments(args);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.laserPanel, frag)
                .addToBackStack(null)
                .commit();
    }

    private void chooseLaser(View view) {
        firebaseAnalytics.logEvent("click_laser_list", null);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.laserPanel, new LaserListFragment())
                .addToBackStack(null)
                .commit();
    }

    private void clickAdd(View view) {
        firebaseAnalytics.logEvent("click_laser_add", null);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.laserPanel, new LaserEditFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    void updateLayers(ProfileLayerEvent event) {
        if (listAdapter != null) {
            listAdapter.setLayers(layers.layers);
        }
    }
}