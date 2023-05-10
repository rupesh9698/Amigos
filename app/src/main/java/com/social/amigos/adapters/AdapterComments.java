package com.social.amigos.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.social.amigos.R;
import com.social.amigos.models.ModelComment;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.Myholder> {

    final Context context;
    final List<ModelComment> commentList;
    final String myUid, postId;

    public AdapterComments(Context context, List<ModelComment> commentList, String myUid, String postId) {
        this.context = context;
        this.commentList = commentList;
        this.myUid = myUid;
        this.postId = postId;
    }

    @NonNull
    @Override
    public Myholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_comments, parent, false);
        return new Myholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Myholder holder, int position) {

        final String uid = commentList.get(position).getUid();
        final String name = commentList.get(position).getuName();
        String image = commentList.get(position).getuDp();
        final String cid = commentList.get(position).getcId();
        String comment = commentList.get(position).getComment();
        String timestamp = commentList.get(position).getTimestamp();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        holder.nameTv.setText(name);
        holder.commentTv.setText(comment);
        holder.timeTv.setText(pTime);

        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_default_img).into(holder.avatarIv);
        } catch (Exception ignored) {
        }
        holder.itemView.setOnLongClickListener(v -> {
            if (myUid.equals(uid)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());
                builder.setTitle("Delete");
                builder.setMessage("Are you Sure to delete this comment ?");
                builder.setPositiveButton("Delete", (dialog, which) -> deleteComment(cid));
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }
            return false;
        });
    }

    private void deleteComment(String cid) {

        FirebaseFirestore.getInstance().collection("Posts").document(postId).collection("Comments").document(cid).delete();
        FirebaseFirestore.getInstance().collection("Posts").document(postId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String comments = documentSnapshot.getString("pComments");
                int newCommentVal = Integer.parseInt(comments) - 1;
                HashMap<String, Object> hashMap = new HashMap();
                hashMap.put("pComments", "" + newCommentVal);
                FirebaseFirestore.getInstance().collection("Posts").document(postId).update(hashMap);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class Myholder extends RecyclerView.ViewHolder {

        final CircleImageView avatarIv;
        final AppCompatTextView nameTv, commentTv, timeTv;

        public Myholder(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            commentTv = itemView.findViewById(R.id.commentTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}