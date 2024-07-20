package com.social.amigos;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.social.amigos.adapters.AdapterUsers;
import com.social.amigos.models.ModelUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PostLikedByActivity extends AppCompatActivity {

    String postId;
    private RecyclerView recyclerView;
    private List<ModelUser> userList;
    private AdapterUsers adapterUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_liked_by);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Post Liked By : ");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        actionBar.setSubtitle(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getEmail());

        recyclerView = findViewById(R.id.recyclerView);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        userList = new ArrayList<>();

        FirebaseFirestore.getInstance().collection("Likes").document(postId).get().addOnSuccessListener(documentSnapshot -> {
            Map data = documentSnapshot.getData();
            assert data != null;
            String objectString = "" + data.keySet();
            String objectSubString = objectString.substring(1, objectString.length() - 1);
            String[] objectSubStringArr = objectSubString.split(",");
            if (data.size() != 0) {
                for (int i = 0; i < data.size(); i++) {
                    String hisUid = "" + objectSubStringArr[i].trim();
                    getUsers(hisUid);
                }
            }
        });
    }

    private void getUsers(String hisUid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(hisUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    userList.add(modelUser);
                }
                adapterUsers = new AdapterUsers(PostLikedByActivity.this, userList);
                recyclerView.setAdapter(adapterUsers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}