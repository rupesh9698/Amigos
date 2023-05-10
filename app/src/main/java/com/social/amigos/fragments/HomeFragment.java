package com.social.amigos.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.social.amigos.AddPostActivity;
import com.social.amigos.NotificationsActivity;
import com.social.amigos.R;
import com.social.amigos.SettingsActivity;
import com.social.amigos.adapters.AdapterPosts;
import com.social.amigos.models.ModelFriendlist;
import com.social.amigos.models.ModelPost;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public FirebaseAuth firebaseAuth;
    RecyclerView recyclerView;
    SwipeRefreshLayout postFragmentSrl;
    List<ModelPost> postList;
    List<ModelFriendlist> friendList;
    AdapterPosts adapterPosts;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        postFragmentSrl = view.findViewById(R.id.postFragmentSrl);
        recyclerView = view.findViewById(R.id.postsRecyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fragmentPosition", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fragment", "Home");
        editor.apply();
        firebaseAuth = FirebaseAuth.getInstance();
        postList = new ArrayList<>();
        friendList = new ArrayList<>();

        postFragmentSrl.setOnRefreshListener(() -> {
            postList.clear();
            loadPosts();
            postFragmentSrl.setRefreshing(false);
        });

        loadPosts();
        return view;
    }

    private void loadPosts() {

        FirebaseFirestore.getInstance().collection("Posts").get().addOnSuccessListener(snapshot -> {
            List<DocumentSnapshot> list = snapshot.getDocuments();
            postList.clear();
            for (DocumentSnapshot ds : list) {
                ModelPost modelPost = ds.toObject(ModelPost.class);
                assert modelPost != null;
                String postUid = modelPost.getUid();
                postList.clear();

                DatabaseReference dbRef2 = FirebaseDatabase.getInstance().getReference("Users").child(postUid).child("Friend List");
                dbRef2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot datasnapshot2) {
                        friendList.clear();
                        for (DataSnapshot ds2 : datasnapshot2.getChildren()) {

                            ModelFriendlist modelFriendlist = ds2.getValue(ModelFriendlist.class);
                            assert modelFriendlist != null;
                            String myUid = modelFriendlist.getId();

                            if (myUid.equals(firebaseAuth.getUid())) {
                                ModelPost modelPost = ds.toObject(ModelPost.class);
                                postList.add(modelPost);
                                adapterPosts = new AdapterPosts(getActivity(), postList);
                                recyclerView.setAdapter(adapterPosts);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_notifications) {
            startActivity(new Intent(getActivity(), NotificationsActivity.class));
        } else if (id == R.id.action_add_post) {
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}