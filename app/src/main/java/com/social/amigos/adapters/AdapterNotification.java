package com.social.amigos.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.social.amigos.PostDetailActivity;
import com.social.amigos.R;
import com.social.amigos.models.ModelNotification;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterNotification extends RecyclerView.Adapter<AdapterNotification.HolderNotification> {

    private final Context context;
    private final ArrayList<ModelNotification> notificationsList;
    private final FirebaseAuth firebaseAuth;

    public AdapterNotification(Context context, ArrayList<ModelNotification> notificationsList) {
        this.context = context;
        this.notificationsList = notificationsList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderNotification onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_notification, parent, false);
        return new HolderNotification(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final HolderNotification holder, int position) {

        final ModelNotification model = notificationsList.get(position);

        String notification = model.getNotification();
        final String timestamp = model.getTimestamp();
        String senderUid = model.getsUid();
        final String pId = model.getpId();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(senderUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = "" + ds.child("name").getValue();
                    String image = "" + ds.child("image").getValue();
                    String email = "" + ds.child("email").getValue();

                    model.setsName(name);
                    model.setsEmail(email);
                    model.setsImage(image);

                    holder.nameTv.setText(name);

                    try {
                        Picasso.get().load(image).placeholder(R.drawable.ic_default_img).into(holder.avatarIv);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        holder.notificationTv.setText(notification);
        holder.timeTv.setText(pTime);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Are you sure to delete this Notification ?");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Notifications").child(timestamp).removeValue().addOnSuccessListener(aVoid -> Toast.makeText(context, "Notification Deleted", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create().show();
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    static class HolderNotification extends RecyclerView.ViewHolder {

        final CircleImageView avatarIv;
        final AppCompatTextView nameTv, notificationTv, timeTv;

        public HolderNotification(@NonNull View itemView) {
            super(itemView);

            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            notificationTv = itemView.findViewById(R.id.notificationTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}