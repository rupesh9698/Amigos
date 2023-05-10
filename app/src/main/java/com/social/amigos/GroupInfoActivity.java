package com.social.amigos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.social.amigos.adapters.AdapterParticipantAdd;
import com.social.amigos.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class GroupInfoActivity extends AppCompatActivity {

    private String groupId, creatorName, myGroupRole = "";
    private FirebaseAuth firebaseAuth;
    private ActionBar actionBar;
    private AppCompatImageView groupIconIv;
    private AppCompatTextView descriptionTv, createdByTv, editGroupTv, addParticipantTv, leaveGroupTv, participantsTv;
    private RecyclerView participantsRv;
    private ArrayList<ModelUser> userList;
    private AdapterParticipantAdd adapterParticipantAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        groupIconIv = findViewById(R.id.groupIconIv);
        descriptionTv = findViewById(R.id.descriptionTv);
        createdByTv = findViewById(R.id.createdByTv);
        editGroupTv = findViewById(R.id.editGroupTv);
        addParticipantTv = findViewById(R.id.addParticipantTv);
        leaveGroupTv = findViewById(R.id.leaveGroupTv);
        participantsTv = findViewById(R.id.participantsTv);
        participantsRv = findViewById(R.id.participantsRv);

        groupId = getIntent().getStringExtra("groupId");

        firebaseAuth = FirebaseAuth.getInstance();

        loadGroupInfo();
        loadMyGroupRole();

        addParticipantTv.setOnClickListener(v -> {
            Intent intent = new Intent(GroupInfoActivity.this, GroupParticipantAddActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });

        editGroupTv.setOnClickListener(v -> {
            Intent intent = new Intent(GroupInfoActivity.this, GroupEditActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });

        leaveGroupTv.setOnClickListener(v -> {
            String dialogTitle;
            String dialogDescription;
            String positiveButtonTitle;

            if (myGroupRole.equals("creator")) {
                dialogTitle = "Delete Group";
                dialogDescription = "Are you sure you want to Delete group Permaanently";
                positiveButtonTitle = "DELETE";
            } else {
                dialogTitle = "Leave Group";
                dialogDescription = "Are you sure you want to Leave group Permaanently";
                positiveButtonTitle = "LEAVE";
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupInfoActivity.this);
            builder.setTitle(dialogTitle).setMessage(dialogDescription).setPositiveButton(positiveButtonTitle, (dialog, which) -> {
                if (myGroupRole.equals("creator")) {
                    deleteGroup();
                } else {
                    leaveGroup();
                }
            }).setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss()).show();
        });

    }

    private void leaveGroup() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(Objects.requireNonNull(firebaseAuth.getUid())).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(GroupInfoActivity.this, "Group Left Successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(GroupInfoActivity.this, DashboardActivity.class));
            finish();
        }).addOnFailureListener(e -> Toast.makeText(GroupInfoActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteGroup() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(GroupInfoActivity.this, "Group Deleted Successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(GroupInfoActivity.this, DashboardActivity.class));
            finish();
        }).addOnFailureListener(e -> Toast.makeText(GroupInfoActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void loadGroupInfo() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.orderByChild("groupId").equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {

                    //String groupId = ""+ds.child("groupId").getValue();
                    String groupTitle = "" + ds.child("groupTitle").getValue();
                    String groupDescription = "" + ds.child("groupDescription").getValue();
                    final String groupIcon = "" + ds.child("groupIcon").getValue();
                    String createdBy = "" + ds.child("createdBy").getValue();
                    String timestamp = "" + ds.child("timestamp").getValue();

                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(Long.parseLong(timestamp));
                    String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

                    loadCreatorInfo(dateTime, createdBy);

                    actionBar.setTitle(groupTitle);
                    descriptionTv.setText(groupDescription);

                    try {
                        Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_primary).into(groupIconIv);
                    } catch (Exception e) {
                        //groupIconIv.setImageResource(R.drawable.ic_group_primary);
                    }

                    /*
                    groupIconIv.setOnClickListener(v -> {
                        Intent intent = new Intent(GroupInfoActivity.this, ImageViewActivity.class);
                        intent.putExtra("imageUrl", groupIcon);
                        startActivity(intent);
                    });
                    */
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadCreatorInfo(final String dateTime, String createdBy) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(createdBy).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    creatorName = "" + ds.child("name").getValue();
                    createdByTv.setText("Created by " + creatorName + " on " + dateTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMyGroupRole() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {

                    myGroupRole = "" + ds.child("role").getValue();
                    actionBar.setSubtitle(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getEmail() + " ( " + myGroupRole + " ) ");

                    switch (myGroupRole) {
                        case "participant":
                            editGroupTv.setVisibility(View.GONE);
                            addParticipantTv.setVisibility(View.GONE);
                            leaveGroupTv.setText("Leave Group");
                            break;
                        case "admin":
                            editGroupTv.setVisibility(View.GONE);
                            addParticipantTv.setVisibility(View.VISIBLE);
                            leaveGroupTv.setText("Leave Group");
                            break;
                        case "creator":
                            editGroupTv.setVisibility(View.VISIBLE);
                            addParticipantTv.setVisibility(View.VISIBLE);
                            leaveGroupTv.setText("Delete Group");
                            break;
                    }
                }
                loadParticipants();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadParticipants() {

        userList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String uid = "" + ds.child("uid").getValue();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    ref.orderByChild("uid").equalTo(uid).addValueEventListener(new ValueEventListener() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ModelUser modelUser = ds.getValue(ModelUser.class);
                                userList.add(modelUser);
                            }

                            adapterParticipantAdd = new AdapterParticipantAdd(GroupInfoActivity.this, userList, groupId, myGroupRole);
                            participantsRv.setAdapter(adapterParticipantAdd);
                            participantsTv.setText("Participants ( " + userList.size() + " ) ");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}