package com.social.amigos.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.social.amigos.GroupCreateActivity;
import com.social.amigos.NotificationsActivity;
import com.social.amigos.R;
import com.social.amigos.SettingsActivity;
import com.social.amigos.adapters.AdapterGroupChatList;
import com.social.amigos.models.ModelGroupChatList;

import java.util.ArrayList;
import java.util.Objects;

public class GroupChatsFragment extends Fragment {

    private RecyclerView groupsRv;
    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelGroupChatList> groupChatLists;
    private AdapterGroupChatList adapterGroupChatList;

    public GroupChatsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chats, container, false);
        groupsRv = view.findViewById(R.id.groupsRv);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fragmentPosition", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fragment", "Group Chats");
        editor.apply();
        firebaseAuth = FirebaseAuth.getInstance();
        loadGroupChatsList();
        return view;
    }

    private void loadGroupChatsList() {
        groupChatLists = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatLists.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.child("Participants").child(Objects.requireNonNull(firebaseAuth.getUid())).exists()) {
                        ModelGroupChatList model = ds.getValue(ModelGroupChatList.class);
                        groupChatLists.add(model);
                    }
                }
                adapterGroupChatList = new AdapterGroupChatList(getActivity(), groupChatLists);
                groupsRv.setAdapter(adapterGroupChatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void searchGroupChatsList(final String query) {
        groupChatLists = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatLists.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.child("Participants").child(Objects.requireNonNull(firebaseAuth.getUid())).exists()) {
                        if (ds.child("groupTitle").toString().toLowerCase().contains(query.toLowerCase())) {
                            ModelGroupChatList model = ds.getValue(ModelGroupChatList.class);
                            groupChatLists.add(model);
                        }
                    }
                }
                adapterGroupChatList = new AdapterGroupChatList(getActivity(), groupChatLists);
                groupsRv.setAdapter(adapterGroupChatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
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

        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!TextUtils.isEmpty(s.trim())) {
                    searchGroupChatsList(s);
                } else {
                    loadGroupChatsList();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                if (!TextUtils.isEmpty(s.trim())) {
                    searchGroupChatsList(s);
                } else {
                    loadGroupChatsList();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            startActivity(new Intent(getActivity(), NotificationsActivity.class));
        } else if (id == R.id.action_create_group) {
            startActivity(new Intent(getActivity(), GroupCreateActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}