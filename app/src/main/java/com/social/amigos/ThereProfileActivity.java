package com.social.amigos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.social.amigos.adapters.AdapterPosts;
import com.social.amigos.models.ModelPost;
import com.social.amigos.models.ModelUser;
import com.social.amigos.notifications.Data;
import com.social.amigos.notifications.Sender;
import com.social.amigos.notifications.Token;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class ThereProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    AppCompatImageView avatarIv, coverIv;
    AppCompatTextView nameTv, emailTv, phoneTv;
    AppCompatButton sendFriendRequestBtn;
    RecyclerView postsRecyclerView;
    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String hisUid, myUid;
    private boolean notify = false;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R.id.phoneTv);
        avatarIv = findViewById(R.id.avatarIv);
        coverIv = findViewById(R.id.coverIv);
        postsRecyclerView = findViewById(R.id.recyclerview_posts);
        sendFriendRequestBtn = findViewById(R.id.sendFriendRequestBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("uid");

        if (hisUid.equals(myUid)) {
            sendFriendRequestBtn.setVisibility(View.GONE);
        }

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(myUid)).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child("Sent Requests").child(hisUid).exists()) {
                    sendFriendRequestBtn.setText("CANCEL FRIEND REQUEST");
                    postsRecyclerView.setVisibility(View.GONE);
                    sendFriendRequestBtn.setOnClickListener(v -> cancelFriendRequest());
                } else if (snapshot.child("Received Requests").child(hisUid).exists()) {
                    sendFriendRequestBtn.setText("ACCEPT FRIEND REQUEST");
                    postsRecyclerView.setVisibility(View.GONE);
                    sendFriendRequestBtn.setOnClickListener(v -> {
                        notify = true;
                        acceptFriendRequest();
                    });
                } else if (snapshot.child("Friend List").child(hisUid).exists()) {
                    sendFriendRequestBtn.setText("SEND MESSAGE");
                    sendFriendRequestBtn.setOnClickListener(v -> sendMessage());
                } else {
                    postsRecyclerView.setVisibility(View.GONE);
                    sendFriendRequestBtn.setText("SEND FRIEND REQUEST");
                    if (snapshot.child("Blocked List").child(hisUid).exists()) {
                        sendFriendRequestBtn.setOnClickListener(v -> Toast.makeText(ThereProfileActivity.this, "You Blocked This User 💔", Toast.LENGTH_SHORT).show());
                    } else {
                        sendFriendRequestBtn.setOnClickListener(v -> {
                            notify = true;
                            sendFriendRequest();
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ThereProfileActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        reference.child(hisUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child("Blocked List").child(myUid).exists()) {
                    sendFriendRequestBtn.setOnClickListener(v -> Toast.makeText(ThereProfileActivity.this, "User Blocked You 💔", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    String phone = "" + ds.child("phone").getValue();
                    final String image = "" + ds.child("image").getValue();
                    final String cover = "" + ds.child("cover").getValue();

                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        Picasso.get().load(image).into(avatarIv);
                    } catch (Exception e) {
                        //Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);
                    }
                    try {
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception ignored) {

                    }

                    /*
                    avatarIv.setOnClickListener(v -> {
                        if (!TextUtils.isEmpty(image)) {
                            Intent intent1 = new Intent(ThereProfileActivity.this, ImageViewActivity.class);
                            intent1.putExtra("imageUrl", image);
                            startActivity(intent1);
                        }
                    });

                    coverIv.setOnClickListener(v -> {
                        if (!TextUtils.isEmpty(cover)) {
                            Intent intent12 = new Intent(ThereProfileActivity.this, ImageViewActivity.class);
                            intent12.putExtra("imageUrl", cover);
                            startActivity(intent12);
                        }
                    });
                    */
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        postList = new ArrayList<>();
        checkUserStatus();
        loadHistPosts();
    }

    private void sendFriendRequest() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(myUid)).child("Sent Requests").child(hisUid).child("id").setValue(hisUid).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                reference.child(hisUid).child("Received Requests").child(myUid).child("id").setValue(myUid).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                        database.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                ModelUser user = snapshot.getValue(ModelUser.class);

                                if (notify) {
                                    assert user != null;
                                    String title = "New Friend Request";
                                    String message = " Sent a Friend Request";
                                    String notificationType = "SendRequestNotification";
                                    sendFriendNotification(hisUid, user.getName(), message, title, notificationType);
                                }
                                notify = false;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        Toast.makeText(ThereProfileActivity.this, "Request Sent Successfully..✔", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void cancelFriendRequest() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(myUid)).child("Sent Requests").child(hisUid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reference.child(hisUid).child("Received Requests").child(myUid).removeValue().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(ThereProfileActivity.this, "Request Cancelled Successfully..✔", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void acceptFriendRequest() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(myUid)).child("Received Requests").child(hisUid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reference.child(myUid).child("Friend List").child(hisUid).child("id").setValue(hisUid).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        reference.child(hisUid).child("Sent Requests").child(myUid).removeValue().addOnCompleteListener(task2 -> reference.child(hisUid).child("Friend List").child(myUid).child("id").setValue(myUid).addOnCompleteListener(task21 -> {
                            if (task21.isSuccessful()) {

                                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                                database.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        ModelUser user = snapshot.getValue(ModelUser.class);

                                        if (notify) {
                                            assert user != null;
                                            String title = "New Friend Added";
                                            String message = " Accepted your Friend Request";
                                            String notificationType = "AcceptRequestNotification";
                                            sendFriendNotification(hisUid, user.getName(), message, title, notificationType);
                                        }
                                        notify = false;
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                                Toast.makeText(ThereProfileActivity.this, "New Friend Added Successfully..✔", Toast.LENGTH_SHORT).show();
                            }
                        }));
                    }
                });
            }
        });
    }

    private void sendMessage() {
        Intent intent = new Intent(ThereProfileActivity.this, ChatActivity.class);
        intent.putExtra("hisUid", hisUid);
        startActivity(intent);
    }

    private void sendFriendNotification(String hisUid, String name, String message, String title, String notificationType) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data("", "" + myUid, "" + name + message, "" + title, "" + hisUid, "" + notificationType, R.drawable.logo);

                    assert token != null;
                    Sender sender = new Sender(data, token.getToken());

                    try {
                        JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObj, response -> Timber.d("onResponse: %s", response.toString()), error -> Timber.d("onResponse: %s", error.toString())) {
                            @Override
                            public Map<String, String> getHeaders() {

                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=AAAAVjrUzl8:APA91bEZPsitpyKKorWJ0rbBSNVIcNRNwMhu5T-VPZv3O1cQb2Q6c1-TiDVpqm7wWgCXMRp9HwBMqzNlQTTNnXGNDPh4UgfyGt91xG65EbCA6AIxdWCqr7rvpSds9PF_HVFi2flvJ_AY");

                                return headers;
                            }
                        };

                        requestQueue.add(jsonObjectRequest);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadHistPosts() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        postsRecyclerView.setLayoutManager(layoutManager);

        FirebaseFirestore.getInstance().collection("Posts").get().addOnSuccessListener(snapshot -> {
            List<DocumentSnapshot> list = snapshot.getDocuments();
            postList.clear();
            for (DocumentSnapshot ds : list) {
                ModelPost myPosts = ds.toObject(ModelPost.class);
                assert myPosts != null;
                if (myPosts.getUid().equals(hisUid)) {
                    postList.add(myPosts);
                    adapterPosts = new AdapterPosts(ThereProfileActivity.this, postList);
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }
        });
    }

    private void searchHistPosts(final String searchQuery) {

        LinearLayoutManager layoutManager = new LinearLayoutManager(ThereProfileActivity.this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        postsRecyclerView.setLayoutManager(layoutManager);

        FirebaseFirestore.getInstance().collection("Posts").get().addOnSuccessListener(snapshot -> {
            List<DocumentSnapshot> list = snapshot.getDocuments();
            postList.clear();
            for (DocumentSnapshot ds : list) {
                ModelPost myPosts = ds.toObject(ModelPost.class);
                assert myPosts != null;
                if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) || myPosts.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())) {
                    postList.add(myPosts);
                }
                adapterPosts = new AdapterPosts(ThereProfileActivity.this, postList);
                postsRecyclerView.setAdapter(adapterPosts);
            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //Null
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ThereProfileActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_notifications).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    searchHistPosts(query);
                } else {
                    loadHistPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (!TextUtils.isEmpty(query)) {
                    searchHistPosts(query);
                } else {
                    loadHistPosts();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}