package com.social.amigos.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.social.amigos.AddPostActivity;
import com.social.amigos.PostDetailActivity;
import com.social.amigos.PostLikedByActivity;
import com.social.amigos.R;
import com.social.amigos.ThereProfileActivity;
import com.social.amigos.models.ModelPost;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

@SuppressWarnings("ALL")
public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder> {

    final Context context;
    final List<ModelPost> postList;
    final String myUid;
    boolean mProcessLike = false;

    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false);
        return new MyHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, int position) {

        final String uid = postList.get(position).getUid();
        String uDp = postList.get(position).getuDp();
        String uName = postList.get(position).getuName();
        final String pId = postList.get(position).getpId();
        final String pTitle = postList.get(position).getpTitle();
        String pLocation = postList.get(position).getpLocation();
        final String pDescription = postList.get(position).getpDescr();
        final String pImage = postList.get(position).getpImage();
        String pTimeStamp = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes();
        String pComments = postList.get(position).getpComments();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTime);
        holder.pTitleTv.setText(pTitle);
        holder.pLocationTv.setText(pLocation);
        holder.pDescriptionTv.setText(pDescription);
        holder.pLikesTv.setText(pLikes + " Likes");
        holder.pCommentsTv.setText(pComments + " Comments");

        setLikes(holder, pId);

        try {
            Picasso.get().load(uDp).placeholder(R.drawable.ic_default_img).into(holder.uPictureIv);
        } catch (Exception ignored) {
        }

        if (pImage.equals("noImage")) {
            holder.pImageIv.setVisibility(View.GONE);
        } else {
            holder.pImageIv.setVisibility(View.VISIBLE);
            try {
                Picasso.get().load(pImage).into(holder.pImageIv);
            } catch (Exception ignored) {
            }
        }

        holder.moreBtn.setOnClickListener(v -> showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage));

        final int[] pLikes1 = {Integer.parseInt(postList.get(position).getpLikes())};

        holder.likeBtn.setOnClickListener(v -> {
            mProcessLike = true;
            final String postIde = postList.get(position).getpId();
            //final int[] pLikes1 = {Integer.parseInt(postList.get(position).getpLikes())};

            CollectionReference likesRef = FirebaseFirestore.getInstance().collection("Likes");
            CollectionReference postsRef = FirebaseFirestore.getInstance().collection("Posts");
            likesRef.document(postIde).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.getString(myUid) != null) {
                    postsRef.document(postIde).get().addOnSuccessListener(documentSnapshot12 -> {
                        int currentLikes = Integer.parseInt(Objects.requireNonNull(documentSnapshot12.getString("pLikes")));
                        HashMap<String, Object> hashMap1 = new HashMap();
                        hashMap1.put(myUid, FieldValue.delete());
                        likesRef.document(postIde).update(hashMap1).addOnSuccessListener(unused -> {
                            HashMap<String, Object> hashMap2 = new HashMap();
                            hashMap2.put("pLikes", "" + (currentLikes - 1));
                            postsRef.document(postIde).update(hashMap2).addOnSuccessListener(unused12 -> {
                                mProcessLike = false;
                                setLikes(holder, pId);
                                holder.pLikesTv.setText(pLikes1[0] - 1 + " Likes");
                                pLikes1[0] -= 1;
                            });
                        });
                    });
                } else {
                    postsRef.document(postIde).get().addOnSuccessListener(documentSnapshot1 -> {
                        int currentLikes = Integer.parseInt(Objects.requireNonNull(documentSnapshot1.getString("pLikes")));
                        HashMap<String, Object> hashMap1 = new HashMap();
                        hashMap1.put(myUid, "Liked");
                        likesRef.document(postIde).update(hashMap1).addOnSuccessListener(unused -> {
                            HashMap<String, Object> hashMap2 = new HashMap();
                            hashMap2.put("pLikes", "" + (currentLikes + 1));
                            postsRef.document(postIde).update(hashMap2).addOnSuccessListener(unused1 -> {
                                mProcessLike = false;
                                setLikes(holder, pId);
                                holder.pLikesTv.setText(pLikes1[0] + 1 + " Likes");
                                pLikes1[0] += 1;
                                addToHisNotification("" + uid, "" + pId);
                            });
                        });
                    });
                }
            });
        });

        holder.commentBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
            ((Activity) context).finish();
        });

        holder.shareBtn.setOnClickListener(v -> {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.pImageIv.getDrawable();
            if (bitmapDrawable == null) {
                shareTextOnly(pTitle, pDescription);
            } else {
                Bitmap bitmap = bitmapDrawable.getBitmap();
                shareImageAndText(pTitle, pDescription, bitmap);
            }
        });

        holder.profileLayout.setOnClickListener(v -> {
            if (!(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid().equals(uid))) {
                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid", uid);
                context.startActivity(intent);
                ((Activity) context).finish();
            }
        });

        holder.pLikesTv.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostLikedByActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
        });
    }

    private void addToHisNotification(String hisUid, String pId) {
        String timestamp = "" + System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", "Liked your post");
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap);
    }

    private void shareTextOnly(String pTitle, String pDescription) {

        String shareBody = pTitle + "\n" + pDescription;
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        context.startActivity(Intent.createChooser(sIntent, "Share Via"));
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {

        String shareBody = pTitle + "\n" + pDescription;
        Uri uri = saveImageToShare(bitmap);
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.setType("image/png");
        context.startActivity(Intent.createChooser(sIntent, "Share Using"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "com.social.amigos.fileprovider", file);
        } catch (Exception e) {
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    @SuppressLint("SetTextI18n")
    private void setLikes(final MyHolder holder, final String postKey) {
        CollectionReference likesRef = FirebaseFirestore.getInstance().collection("Likes");
        likesRef.document(postKey).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.getString(myUid) != null) {
                holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                holder.likeBtn.setText("Liked");
            } else {
                holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                holder.likeBtn.setText("Like");
            }
        });
    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        if (uid.equals(myUid)) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
            //popupMenu.getMenu().add(Menu.NONE, 2, 0, "Repost");
        }

        popupMenu.getMenu().add(Menu.NONE, 2, 0, "Repost"); //add this list
        popupMenu.getMenu().add(Menu.NONE, 3, 0, "View Detail");

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                beginDeletePost(pId, pImage);
                beginDeleteLikes(pId);
            } else if (id == 1) {
                Intent intent = new Intent(context, AddPostActivity.class);
                intent.putExtra("key", "editPost");
                intent.putExtra("editPostId", pId);
                context.startActivity(intent);
            } else if (id == 2) {
                Intent intent1 = new Intent(context, AddPostActivity.class);
                intent1.putExtra("key", "repost");
                intent1.putExtra("repostId", pId);
                context.startActivity(intent1);
            } else if (id == 3) {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", pId);
                context.startActivity(intent);
                ((Activity) context).finish();
            }
            return false;
        });
        popupMenu.show();
    }

    private void beginDeleteLikes(String postId) {

        DatabaseReference removeLikes = FirebaseDatabase.getInstance().getReference("Likes").child(postId);
        removeLikes.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void beginDeletePost(String pId, String pImage) {
        if (pImage.equals("noImage")) {
            deleteWithoutImage(pId);
        } else {
            deleteWithImage(pId, pImage);
        }
    }

    private void deleteWithImage(final String pId, String pImage) {
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting..!!");

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(aVoid -> FirebaseFirestore.getInstance().collection("Posts").document(pId).delete().addOnSuccessListener(unused -> {
            Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
            pd.dismiss();
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }));
    }

    private void deleteWithoutImage(String pId) {

        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting..!!");

        FirebaseFirestore.getInstance().collection("Posts").document(pId).delete().addOnSuccessListener(unused -> {
            Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
            pd.dismiss();
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        final CircleImageView uPictureIv;
        final AppCompatImageView pImageIv;
        final AppCompatTextView uNameTv, pTimeTv, pTitleTv, pLocationTv, pDescriptionTv, pLikesTv, pCommentsTv;
        final AppCompatImageButton moreBtn;
        final AppCompatButton likeBtn, commentBtn, shareBtn;
        final LinearLayoutCompat profileLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pLocationTv = itemView.findViewById(R.id.pLocationTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }
}