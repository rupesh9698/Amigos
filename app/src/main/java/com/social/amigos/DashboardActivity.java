package com.social.amigos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.social.amigos.fragments.ChatListFragment;
import com.social.amigos.fragments.GroupChatsFragment;
import com.social.amigos.fragments.HomeFragment;
import com.social.amigos.fragments.ProfileFragment;
import com.social.amigos.fragments.UsersFragment;

import java.util.Objects;

public class DashboardActivity extends AppCompatActivity {

    public static final int UPDATE_CODE = 18;
    AppUpdateManager appUpdateManager;
    ActionBar actionBar;

    private final BottomNavigationView.OnNavigationItemSelectedListener selectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                actionBar.setTitle("Chats");
                ChatListFragment fragment1 = new ChatListFragment();
                FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                ft1.replace(R.id.content, fragment1, "");
                ft1.commit();
                return true;
            } else if (itemId == R.id.nav_group_chats) {
                actionBar.setTitle("Group Chats");
                GroupChatsFragment fragment2 = new GroupChatsFragment();
                FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                ft2.replace(R.id.content, fragment2, "");
                ft2.commit();
                return true;
            } else if (itemId == R.id.nav_home) {
                actionBar.setTitle("Home");
                HomeFragment fragment3 = new HomeFragment();
                FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                ft3.replace(R.id.content, fragment3, "");
                ft3.commit();
                return true;
            } else if (itemId == R.id.nav_users) {
                actionBar.setTitle("Users");
                UsersFragment fragment4 = new UsersFragment();
                FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                ft4.replace(R.id.content, fragment4, "");
                ft4.commit();
                return true;
            } else if (itemId == R.id.nav_profile) {
                actionBar.setTitle("Profile");
                ProfileFragment fragment5 = new ProfileFragment();
                FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                ft5.replace(R.id.content, fragment5, "");
                ft5.commit();
                return true;
            }
            return false;
        }
    };
    String mUID;
    final InstallStateUpdatedListener listener = installState -> {

        if (installState.installStatus() == InstallStatus.DOWNLOADED) {

            Snackbar snackbar = Snackbar.make(findViewById(R.id.content), "App update almost done..!!", Snackbar.LENGTH_INDEFINITE);

            snackbar.setAction("Reload", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    appUpdateManager.completeUpdate();
                }
            });

            snackbar.setTextColor(Color.parseColor("#FF0000"));
            snackbar.show();
        }
    };
    private FirebaseAuth firebaseAuth;
    private DatabaseReference RootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        actionBar = getSupportActionBar();

        firebaseAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();

        SharedPreferences sharedPreferences = getSharedPreferences("fragmentPosition", MODE_PRIVATE);
        String savedPosition = sharedPreferences.getString("fragment", "Chats");
        switch (savedPosition) {
            case "Chats":
                actionBar.setTitle("Chats");
                ChatListFragment fragment1 = new ChatListFragment();
                FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                ft1.replace(R.id.content, fragment1, "");
                ft1.commit();
                navigationView.setSelectedItemId(R.id.nav_chat);
                break;
            case "Group Chats":
                actionBar.setTitle("Group Chats");
                GroupChatsFragment fragment2 = new GroupChatsFragment();
                FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                ft2.replace(R.id.content, fragment2, "");
                ft2.commit();
                navigationView.setSelectedItemId(R.id.nav_group_chats);
                break;
            case "Home":
                actionBar.setTitle("Home");
                HomeFragment fragment3 = new HomeFragment();
                FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                ft3.replace(R.id.content, fragment3, "");
                ft3.commit();
                navigationView.setSelectedItemId(R.id.nav_home);
                break;
            case "Users":
                actionBar.setTitle("Users");
                UsersFragment fragment4 = new UsersFragment();
                FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                ft4.replace(R.id.content, fragment4, "");
                ft4.commit();
                navigationView.setSelectedItemId(R.id.nav_users);
                break;
            case "Profile":
                actionBar.setTitle("Profile");
                ProfileFragment fragment5 = new ProfileFragment();
                FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                ft5.replace(R.id.content, fragment5, "");
                ft5.commit();
                navigationView.setSelectedItemId(R.id.nav_profile);
                break;
        }
        checkUserStatus();
        inAppUpdate();
    }

    public void updateToken() {

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
                String mToken = task.getResult();
                ref.child(mUID).child("token").setValue(mToken);
            } else {
                Toast.makeText(getApplicationContext(), "Unable to get Installation ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            mUID = user.getUid();
            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();
            updateToken();
        } else {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }

    private void inAppUpdate() {

        appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(appUpdateInfo -> {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, DashboardActivity.this, UPDATE_CODE);
                } catch (IntentSender.SendIntentException e) {
                    Toast.makeText(DashboardActivity.this, "Update Error : " + e, Toast.LENGTH_SHORT).show();
                }
            }
        });

        appUpdateManager.registerListener(listener);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void VerifyUserExistance() {

        String currentUserID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!((dataSnapshot.child("name").exists()) && (dataSnapshot.child("phone").exists()))) {
                    Intent settingsIntent = new Intent(DashboardActivity.this, InfoActivity.class);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(settingsIntent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(DashboardActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            SendUserToMainActivity();
        } else {
            VerifyUserExistance();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPDATE_CODE) {

            if (resultCode != RESULT_OK) {

                //Write anything
            }
        }
    }
}