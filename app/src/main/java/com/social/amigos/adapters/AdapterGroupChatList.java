package com.social.amigos.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.scottyab.aescrypt.AESCrypt;
import com.social.amigos.GroupChatActivity;
import com.social.amigos.R;
import com.social.amigos.models.ModelGroupChatList;
import com.squareup.picasso.Picasso;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterGroupChatList extends RecyclerView.Adapter<AdapterGroupChatList.HolderGroupChatList> {

    private final Context context;
    private final ArrayList<ModelGroupChatList> groupChatLists;

    public AdapterGroupChatList(Context context, ArrayList<ModelGroupChatList> groupChatLists) {
        this.context = context;
        this.groupChatLists = groupChatLists;
    }

    @NonNull
    @Override
    public HolderGroupChatList onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_groupchats_list, parent, false);
        return new HolderGroupChatList(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGroupChatList holder, int position) {

        ModelGroupChatList model = groupChatLists.get(position);
        final String groupId = model.getGroupId();
        String groupIcon = model.getGroupIcon();
        String groupTitle = model.getGroupTitle();

        holder.nameTv.setText("");
        holder.timeTv.setText("");
        holder.messageTv.setText("");

        loadLastMessage(model, holder);

        holder.groupTitleTv.setText(groupTitle);
        try {
            Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_primary).into(holder.groupIconIv);
        } catch (Exception ignored) {
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GroupChatActivity.class);
            intent.putExtra("groupId", groupId);
            context.startActivity(intent);
        });
    }

    private void loadLastMessage(ModelGroupChatList model, final HolderGroupChatList holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(model.getGroupId()).child("Messages").limitToLast(1).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {

                    String message = "" + ds.child("message").getValue();
                    String timestamp = "" + ds.child("timestamp").getValue();
                    String sender = "" + ds.child("sender").getValue();
                    String messageType = "" + ds.child("type").getValue();

                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(Long.parseLong(timestamp));
                    String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

                    if (messageType.equals("image")) {
                        holder.messageTv.setText("Picture");
                    } else {
                        try {
                            String decrypt = AESCrypt.decrypt(timestamp, message);
                            holder.messageTv.setText(decrypt);
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                        }
                    }
                    holder.timeTv.setText(dateTime);

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    ref.orderByChild("uid").equalTo(sender).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String fullname = "" + ds.child("name").getValue();
                                String name = fullname.split(" ")[0];
                                holder.nameTv.setText(name + " : ");
                            }
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

    @Override
    public int getItemCount() {
        return groupChatLists.size();
    }

    static class HolderGroupChatList extends RecyclerView.ViewHolder {

        private final CircleImageView groupIconIv;
        private final AppCompatTextView groupTitleTv, nameTv, messageTv, timeTv;

        public HolderGroupChatList(@NonNull View itemView) {
            super(itemView);

            groupIconIv = itemView.findViewById(R.id.groupIconIv);
            groupTitleTv = itemView.findViewById(R.id.groupTitleTv);
            nameTv = itemView.findViewById(R.id.nameTv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}