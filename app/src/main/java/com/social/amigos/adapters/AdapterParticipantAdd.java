package com.social.amigos.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.social.amigos.R;
import com.social.amigos.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterParticipantAdd extends RecyclerView.Adapter<AdapterParticipantAdd.HolderParticipantAdd> {

    private final Context context;
    private final ArrayList<ModelUser> userList;
    private final String groupId;
    private final String myGroupRole;

    public AdapterParticipantAdd(Context context, ArrayList<ModelUser> userList, String groupId, String myGroupRole) {
        this.context = context;
        this.userList = userList;
        this.groupId = groupId;
        this.myGroupRole = myGroupRole;
    }

    @NonNull
    @Override
    public HolderParticipantAdd onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_participant_add, parent, false);
        return new HolderParticipantAdd(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderParticipantAdd holder, int position) {
        final ModelUser modelUser = userList.get(position);
        final String name = modelUser.getName();
        String email = modelUser.getEmail();
        String image = modelUser.getImage();
        final String uid = modelUser.getUid();

        holder.nameTv.setText(name);
        holder.emailTv.setText(email);
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_default_img).into(holder.avatarIv);
        } catch (Exception ignored) {
        }

        checkIfAlreadyExists(modelUser, holder);

        holder.itemView.setOnClickListener(v -> {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
            ref.child(groupId).child("Participants").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String hisPreviousRole = "" + snapshot.child("role").getValue();
                        String[] options;
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Choose Option");
                        if (myGroupRole.equals("creator")) {
                            if (hisPreviousRole.equals("admin")) {
                                options = new String[]{"Remove Admin", "Remove User"};
                                builder.setItems(options, (dialog, which) -> {
                                    if (which == 0) {
                                        removeAdmin(modelUser);
                                    } else {
                                        removeParticipant(modelUser);
                                    }
                                }).show();
                            } else if (hisPreviousRole.equals("participant")) {
                                options = new String[]{"Make Admin", "Remove User"};
                                builder.setItems(options, (dialog, which) -> {
                                    if (which == 0) {
                                        makeAdmin(modelUser);
                                    } else {
                                        removeParticipant(modelUser);
                                    }
                                }).show();
                            }
                        } else if (myGroupRole.equals("admin")) {
                            switch (hisPreviousRole) {
                                case "creator":
                                    Toast.makeText(context, "You Cant Remove Creator of this Group", Toast.LENGTH_SHORT).show();
                                    break;
                                case "admin":
                                    options = new String[]{"Remove Admin", "Remove User"};
                                    builder.setItems(options, (dialog, which) -> {
                                        if (which == 0) {
                                            removeAdmin(modelUser);
                                        } else {
                                            removeParticipant(modelUser);
                                        }
                                    }).show();
                                    break;
                                case "participant":
                                    options = new String[]{"Make Admin", "Remove User"};
                                    builder.setItems(options, (dialog, which) -> {
                                        if (which == 0) {
                                            makeAdmin(modelUser);
                                        } else {
                                            removeParticipant(modelUser);
                                        }
                                    }).show();
                                    break;
                            }
                        }
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Add Participant").setMessage("Add User in this Group ?").setPositiveButton("ADD", (dialog, which) -> addParticipant(modelUser)).setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss()).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        });
    }

    private void addParticipant(ModelUser modelUser) {
        String timestamp = "" + System.currentTimeMillis();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", modelUser.getUid());
        hashMap.put("role", "participant");
        hashMap.put("timestamp", "" + timestamp);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(modelUser.getUid()).setValue(hashMap).addOnSuccessListener(aVoid -> Toast.makeText(context, "Added Successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void makeAdmin(ModelUser modelUser) {

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("role", "admin");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(modelUser.getUid()).updateChildren(hashMap).addOnSuccessListener(aVoid -> Toast.makeText(context, "The User is Now Admin", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeParticipant(ModelUser modelUser) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(modelUser.getUid()).removeValue().addOnSuccessListener(aVoid -> Toast.makeText(context, "Removed Successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeAdmin(ModelUser modelUser) {

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("role", "participant");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(modelUser.getUid()).updateChildren(hashMap).addOnSuccessListener(aVoid -> Toast.makeText(context, "The User is No Longer Admin", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkIfAlreadyExists(ModelUser modelUser, final HolderParticipantAdd holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").child(modelUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String hisRole = "" + snapshot.child("role").getValue();
                    holder.statusTv.setText(hisRole);
                } else {
                    holder.statusTv.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class HolderParticipantAdd extends RecyclerView.ViewHolder {

        private final CircleImageView avatarIv;
        private final AppCompatTextView nameTv, emailTv, statusTv;

        public HolderParticipantAdd(@NonNull View itemView) {
            super(itemView);

            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            emailTv = itemView.findViewById(R.id.emailTv);
            statusTv = itemView.findViewById(R.id.statusTv);
        }
    }
}