package com.social.amigos;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.social.amigos.adapters.AdapterFriendRequests;
import com.social.amigos.models.ModelChatlist;
import com.social.amigos.models.ModelUser;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestsActivity extends AppCompatActivity {

    final Boolean notify = false;
    RecyclerView friendRequestsRv;
    List<ModelChatlist> chatlistList;
    List<ModelUser> userList;
    DatabaseReference reference;
    FirebaseUser currentUser;
    RequestQueue requestQueue;
    AdapterFriendRequests adapterFriendRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Friend Requests");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        friendRequestsRv = findViewById(R.id.friendRequestsRv);

        chatlistList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatlistList.clear();
                for (DataSnapshot ds : snapshot.child("Received Requests").getChildren()) {
                    ModelChatlist chatlist = ds.getValue(ModelChatlist.class);
                    chatlistList.add(chatlist);
                }

                loadFriendRequests();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                try {
                    Toast.makeText(FriendRequestsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {

                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void loadFriendRequests() {
        userList = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelUser user = ds.getValue(ModelUser.class);
                    for (ModelChatlist chatlist : chatlistList) {
                        assert user != null;
                        if (user.getUid() != null && user.getUid().equals(chatlist.getId())) {
                            userList.add(user);
                            break;
                        }
                    }

                    adapterFriendRequests = new AdapterFriendRequests(FriendRequestsActivity.this, userList, notify, requestQueue);
                    friendRequestsRv.setAdapter(adapterFriendRequests);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                try {
                    Toast.makeText(FriendRequestsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {

                }
            }
        });
    }
}