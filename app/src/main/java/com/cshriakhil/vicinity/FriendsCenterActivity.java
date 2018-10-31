package com.cshriakhil.vicinity;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

public class FriendsCenterActivity extends AppCompatActivity {
    private static final String TAG = FriendsCenterActivity.class.getSimpleName();
    ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_center);

        BottomNavigationView navigationView = findViewById(R.id.bottom_nav);
        pager = findViewById(R.id.viewpager);
        setupViewpager();

        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.add_friend:
                        pager.setCurrentItem(0);
                        break;

                    case R.id.my_requests:
                        pager.setCurrentItem(1);
                        break;

                    case R.id.friend_list:
                        pager.setCurrentItem(2);
                        break;
                }
                return true;
            }
        });
    }

    void setupViewpager() {
        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0: // Add new friends by sending request
                        return new AddFriendFragment();
                    case 1: // Requests Screen
                        return new RequestsFragment();
                    case 2: // Check list of friends
                        return new FriendsListFragment();
                }
                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }
        };

        pager.setAdapter(adapter);
    }

}
