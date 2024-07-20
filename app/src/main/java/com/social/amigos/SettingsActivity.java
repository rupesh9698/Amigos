package com.social.amigos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Settings");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        Button developer = findViewById(R.id.action_developer);
        Button logout = findViewById(R.id.action_logout);

        developer.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, DeveloperActivity.class)));

        logout.setOnClickListener(v -> {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("token", "");
            ref.child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).updateChildren(hashMap);
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}