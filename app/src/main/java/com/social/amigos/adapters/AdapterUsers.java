package com.social.amigos.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.social.amigos.R;
import com.social.amigos.ThereProfileActivity;
import com.social.amigos.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder> {

    final Context context;
    final List<ModelUser> userList;
    final FirebaseAuth firebaseAuth;
    final String myUid;

    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {

        final String hisUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        holder.mNameTv.setText(userName);
        holder.mEmailTv.setText(userEmail);

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_img).into(holder.mAvatarIv);
        } catch (Exception ignored) {
        }

        holder.blockIv.setImageResource(R.drawable.ic_unblocked_green);
        checkIsBlocked(hisUID, holder, position);

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ThereProfileActivity.class);
            intent.putExtra("uid", hisUID);
            context.startActivity(intent);
        });

        holder.blockIv.setOnClickListener(v -> {
            AlertDialog alertDialog;
            if (userList.get(position).getBlocked()) {
                alertDialog = new AlertDialog.Builder(context).setTitle("Unblock User ?").setCancelable(false).setMessage("Are you sure to unblock this user..!!").setPositiveButton("Unblock", (dialog, which) -> unBlockUser(hisUID)).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create();
            } else {
                alertDialog = new AlertDialog.Builder(context).setTitle("Block User ?").setCancelable(false).setMessage("Are you sure to block this user..!!").setPositiveButton("Block", (dialog, which) -> blockUser(hisUID)).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create();
            }
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        });
    }

    private void checkIsBlocked(String hisUID, final MyHolder holder, final int position) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Blocked List").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(hisUID).exists()) {
                    holder.blockIv.setImageResource(R.drawable.ic_blocked_red);
                    userList.get(position).setBlocked(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void blockUser(String hisUID) {

        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Users");
        ref1.child(myUid).child("Friend List").child(hisUID).removeValue().addOnCompleteListener(task -> ref1.child(hisUID).child("Friend List").child(myUid).removeValue().addOnCompleteListener(task13 -> ref1.child(myUid).child("Blocked List").child(hisUID).setValue("blocked").addOnCompleteListener(task12 -> {
            DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("Chatlist");
            ref2.child(hisUID).child(myUid).removeValue().addOnCompleteListener(task1 -> {
                DatabaseReference ref3 = FirebaseDatabase.getInstance().getReference("Chatlist");
                ref3.child(myUid).child(hisUID).removeValue().addOnSuccessListener(unused -> Toast.makeText(context, "Blocked Successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        })));
    }

    private void unBlockUser(String hisUID) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("Blocked List").child(hisUID).removeValue().addOnSuccessListener(unused -> Toast.makeText(context, "Unblocked Successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "Failed : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        final CircleImageView mAvatarIv;
        final AppCompatImageView blockIv;
        final AppCompatTextView mNameTv, mEmailTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            mAvatarIv = itemView.findViewById(R.id.avatarIv);
            blockIv = itemView.findViewById(R.id.blockIv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            mEmailTv = itemView.findViewById(R.id.emailTv);
        }
    }
}