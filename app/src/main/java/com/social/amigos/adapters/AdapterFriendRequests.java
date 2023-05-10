package com.social.amigos.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.social.amigos.R;
import com.social.amigos.ThereProfileActivity;
import com.social.amigos.models.ModelUser;
import com.social.amigos.notifications.Data;
import com.social.amigos.notifications.Sender;
import com.social.amigos.notifications.Token;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

public class AdapterFriendRequests extends RecyclerView.Adapter<AdapterFriendRequests.MyHolder> {

    final Context context;
    final List<ModelUser> userList;
    final RequestQueue requestQueue;
    Boolean notify;

    public AdapterFriendRequests(Context context, List<ModelUser> userList, Boolean notify, RequestQueue requestQueue) {
        this.context = context;
        this.userList = userList;
        this.notify = notify;
        this.requestQueue = requestQueue;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_friend_requests_list, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterFriendRequests.MyHolder holder, int position) {

        String hisUid = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();

        holder.nameTv.setText(userName);

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_img).into(holder.profileIv);
        } catch (Exception ignored) {
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ThereProfileActivity.class);
            intent.putExtra("uid", hisUid);
            context.startActivity(intent);
        });

        holder.acceptFriendRequestBtn.setOnClickListener(v -> {
            AlertDialog alertDialog = new AlertDialog.Builder(context).setTitle("Accept Friend Request ?").setCancelable(false).setPositiveButton("Accept", (dialog, which) -> {
                notify = true;
                acceptFriendRequest(hisUid);
            }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        });

        holder.rejectFriendRequestBtn.setOnClickListener(v -> {
            AlertDialog alertDialog = new AlertDialog.Builder(context).setTitle("Reject Friend Request ?").setCancelable(false).setPositiveButton("Reject", (dialog, which) -> rejectFriendRequest(hisUid)).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        });
    }

    private void acceptFriendRequest(String hisUid) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child("Received Requests").child(hisUid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reference.child(FirebaseAuth.getInstance().getUid()).child("Friend List").child(hisUid).child("id").setValue(hisUid).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        reference.child(hisUid).child("Sent Requests").child(FirebaseAuth.getInstance().getUid()).removeValue().addOnCompleteListener(task2 -> reference.child(hisUid).child("Friend List").child(FirebaseAuth.getInstance().getUid()).child("id").setValue(FirebaseAuth.getInstance().getUid()).addOnCompleteListener(task21 -> {
                            if (task21.isSuccessful()) {

                                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getUid());
                                database.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        ModelUser user = snapshot.getValue(ModelUser.class);
                                        if (notify) {
                                            assert user != null;
                                            sendFriendNotification(hisUid, user.getName());
                                        }
                                        notify = false;
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                                Toast.makeText(context, "New Friend Added Successfully..✔", Toast.LENGTH_SHORT).show();
                            }
                        }));
                    }
                });
            }
        });
    }

    private void sendFriendNotification(String hisUid, String name) {

        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data("", "" + FirebaseAuth.getInstance().getUid(), "" + name + " Accepted your Friend Request", "New Friend Added", "" + hisUid, "AcceptRequestNotification", R.drawable.logo);

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

    private void rejectFriendRequest(String hisUid) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child("Received Requests").child(hisUid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reference.child(hisUid).child("Sent Requests").child(FirebaseAuth.getInstance().getUid()).removeValue().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(context, "Request Rejected Successfully..✔", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        final CircleImageView profileIv;
        final AppCompatTextView nameTv;
        final AppCompatButton acceptFriendRequestBtn, rejectFriendRequestBtn;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            profileIv = itemView.findViewById(R.id.profileIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            acceptFriendRequestBtn = itemView.findViewById(R.id.acceptFriendRequestBtn);
            rejectFriendRequestBtn = itemView.findViewById(R.id.rejectFriendRequestBtn);
        }
    }
}
