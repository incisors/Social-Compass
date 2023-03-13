package com.example.socialcompass.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.LiveData;

import com.example.socialcompass.R;
import com.example.socialcompass.model.friend.Friend;
import com.example.socialcompass.model.friend.FriendDao;
import com.example.socialcompass.model.friend.FriendDatabase;
import com.example.socialcompass.model.repository.Repository;
import com.example.socialcompass.old.GPSLocationHandler;
import com.example.socialcompass.old.OrientationService;
import com.example.socialcompass.utility.Utilities;

import java.util.ArrayList;
import java.util.List;

public class CompassActivity extends AppCompatActivity {
    private Icon nodeIcon;

    public synchronized void redrawAllFriends() {
        if (locationService.getLocation().getValue() == null
                || orientationService.getOrientation().getValue() == null
                || friendsList == null) return;

        // TODO synchronize against the activity drawing?
        ConstraintLayout layout = findViewById(R.id.compass_layout);
        layout.removeAllViews();

//        Log.d("CompassView", "Redraw " + friendsList.getValue());

        Pair<Double, Double> loc = locationService.getLocation().getValue();

        final float gpsLat = loc.first.floatValue(),
                gpsLon = loc.second.floatValue();

        List<ImageView> nodes = new ArrayList<>();
        List<TextView> labels = new ArrayList<>();
        List<Friend> friends = friendsList.getValue();

        for (int i = nodes.size(); i < friends.size(); i++) {
            ImageView node = new ImageView(getApplicationContext());
            node.setImageIcon(nodeIcon);
            node.setId(View.generateViewId());
            node.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
            layout.addView(node);

            TextView text = new TextView(getApplicationContext());
            text.setId(View.generateViewId());
            layout.addView(text);

            labels.add(text);
            nodes.add(node);
        }

        ConstraintSet cs = new ConstraintSet();
        cs.clone(layout);

        int i;
        for (i = 0; i < friends.size(); i++) {
            Friend f = friends.get(i);
            float angle = Utilities.getAngle(gpsLat, gpsLon, f.latitude, f.longitude);
            cs.constrainCircle(nodes.get(i).getId(), R.id.compass_layout, 462, angle);
            labels.get(i).setText(String.format("%s\n%.0fmi", f.label, Utilities.calculateDistanceInMiles(gpsLat, gpsLon, f.latitude, f.longitude)));
            cs.constrainCircle(labels.get(i).getId(), R.id.compass_layout, 330, angle);

            nodes.get(i).setVisibility(View.VISIBLE);
            labels.get(i).setVisibility(View.VISIBLE);
        }
        for (; i < nodes.size(); i++) {
            nodes.get(i).setVisibility(View.INVISIBLE);
            labels.get(i).setVisibility(View.INVISIBLE);
        }

        cs.applyTo(layout);
    }

    private GPSLocationHandler locationService;
    private OrientationService orientationService;

    private LiveData<List<Friend>> friendsList;
    private FriendDao friendDao;
    private Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass_new);

        nodeIcon = Icon.createWithResource(getApplicationContext(), R.drawable.address_node);

        friendDao = FriendDatabase.provide(getApplicationContext()).getDao();
        this.repo = new Repository(friendDao);
        friendsList = friendDao.getAllLive();
        friendsList.observe(this, (allFriends) -> {
            this.redrawAllFriends();

        });

        locationService = new GPSLocationHandler(this);
        orientationService = new OrientationService(this);

        orientationService.getOrientation().observe(this, (rotation) -> {
//            this.redrawAllFriends();
            float degrees = (float) Math.toDegrees(rotation);
            ConstraintLayout constraintLayout = findViewById(R.id.compass_screen_layout);
            constraintLayout.setRotation(-1 * degrees);
        });
        locationService.getLocation().observe(this, (a) -> {
            this.redrawAllFriends();
        });

        List<Friend> allFriends = friendDao.getAll();
        /**
         * TODO: fix this ASAP
         * This is a terrible way to fix this
         * Basically, we observe every single nodes and spawn a thread for each of those
         */
        for (Friend friend : allFriends) {
            Log.d("COMPASS_LOG", "DAO friend list updated");
            repo.getSyncedFriend(friend.publicCode).observe(this, (a) -> {});
        }




    }

    public void toFriendsList(View v) {
        SharedPreferences preferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        Intent intent = new Intent(this, FriendListActivity.class);
        String userName = preferences.getString("label", null);
        String userPublicCode = preferences.getString("publicCode", null);

        intent.putExtra("inputName", userName);
        intent.putExtra("publicCode", userPublicCode);
        startActivity(intent);
    }

}