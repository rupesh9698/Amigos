package com.social.amigos.adapters;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.scottyab.aescrypt.AESCrypt;
import com.social.amigos.R;
import com.social.amigos.models.ModelChat;
import com.squareup.picasso.Picasso;

import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    final Context context;
    final List<ModelChat> chatList;
    final String imageUrl;
    FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public AdapterChat.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);
        }
        return new MyHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final AdapterChat.MyHolder holder, final int position) {

        final String myUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);

        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();
        String type = chatList.get(position).getType();

        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timeStamp));
        String dateTime = DateFormat.format("dd/MM/yyyy\nhh:mm aa", cal).toString();

        if (type.equals("text")) {
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);

            try {
                String decrypt = AESCrypt.decrypt(timeStamp, message);
                holder.messageTv.setText(decrypt);

            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        } else {
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);

            try {
                Picasso.get().load(message).placeholder(R.drawable.ic_image_black).into(holder.messageIv);
            } catch (Exception ignored) {
            }
        }

        holder.timeTv.setText(dateTime);
        try {
            Picasso.get().load(imageUrl).into(holder.profileIv);
        } catch (Exception ignored) {
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (final DataSnapshot ds : snapshot.getChildren()) {
                    if (Objects.equals(ds.child("sender").getValue(), myUID)) {
                        holder.messageLayout.setOnLongClickListener(view -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Delete");
                            builder.setMessage("Are you sure to delete this message ?");
                            builder.setPositiveButton("Delete", (dialogInterface, i) -> ds.getRef().removeValue());
                            builder.setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());
                            builder.create().show();
                            return false;
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        if (position == chatList.size() - 1) {
            if (chatList.get(position).isSeen()) {
                holder.isSeenTv.setText("seen");
            } else {
                holder.isSeenTv.setText("delivered");
            }
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        final CircleImageView profileIv;
        final AppCompatImageView messageIv;
        final AppCompatTextView messageTv, timeTv, isSeenTv;
        final LinearLayoutCompat messageLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            profileIv = itemView.findViewById(R.id.profileIv);
            messageIv = itemView.findViewById(R.id.messageIv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }
}