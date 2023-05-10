package com.social.amigos;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.social.amigos.adapters.AdapterComments;
import com.social.amigos.models.ModelComment;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

@SuppressWarnings("ALL")
public class PostDetailActivity extends AppCompatActivity {

    String hisUid, myUid, myEmail, myName, myDp, postId, pLikes, hisDp, hisName, pImage;
    int likeInt;
    boolean mProcessComment = false;
    boolean mProcessLike = false;
    ProgressDialog pd;
    CircleImageView uPictureIv, cAvatarIv;
    AppCompatImageView pImageIv;
    AppCompatTextView uNameTv, pTimeTiv, pTitleTv, pLocationTv, pDescriptionTv, pLikesTv, pCommentsTv;
    AppCompatImageButton moreBtn, sendBtn;
    AppCompatButton likeBtn, shareBtn;
    RecyclerView recyclerView;
    List<ModelComment> commentList;
    AdapterComments adapterComments;
    AppCompatEditText commentEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTiv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pLocationTv = findViewById(R.id.pLocationTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        recyclerView = findViewById(R.id.recyclerView);

        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);

        loadPostInfo();

        checkUserStatus();

        loadUserInfo();

        setLikes();

        actionBar.setSubtitle("Logged in as : " + myEmail);

        loadComments();

        sendBtn.setOnClickListener(v -> postComment());

        likeBtn.setOnClickListener(v -> likePost());

        moreBtn.setOnClickListener(v -> showMoreOptions());

        commentEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.toString().trim().length() == 0) {
                    sendBtn.setVisibility(View.GONE);
                } else {
                    sendBtn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        shareBtn.setOnClickListener(v -> {
            String pTitle = pTitleTv.getText().toString().trim();
            String pDescription = pDescriptionTv.getText().toString().trim();

            BitmapDrawable bitmapDrawable = (BitmapDrawable) pImageIv.getDrawable();
            if (bitmapDrawable == null) {
                shareTextOnly(pTitle, pDescription);
            } else {
                Bitmap bitmap = bitmapDrawable.getBitmap();
                shareImageAndText(pTitle, pDescription, bitmap);
            }
        });

        pLikesTv.setOnClickListener(v -> {
            Intent intent1 = new Intent(PostDetailActivity.this, PostLikedByActivity.class);
            intent1.putExtra("postId", postId);
            startActivity(intent1);
        });
    }

    private void addToHisNotification(String hisUid, String pId, String notification) {
        String timestamp = "" + System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap).addOnSuccessListener(aVoid -> {

        }).addOnFailureListener(e -> {

        });
    }

    private void shareTextOnly(String pTitle, String pDescription) {

        String shareBody = pTitle + "\n" + pDescription;

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sIntent, "Share Via"));
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {

        String shareBody = pTitle + "\n" + pDescription;

        Uri uri = saveImageToShare(bitmap);

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent, "Share Using"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.social.amigos.fileprovider", file);
        } catch (Exception e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void loadComments() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        commentList = new ArrayList<>();

        FirebaseFirestore.getInstance().collection("Posts").document(postId).collection("Comments").get().addOnSuccessListener(snapshot -> {
            List<DocumentSnapshot> list = snapshot.getDocuments();
            commentList.clear();
            for (DocumentSnapshot ds : list) {
                ModelComment modelComment = ds.toObject(ModelComment.class);
                commentList.add(modelComment);
                adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                recyclerView.setAdapter(adapterComments);
            }
        });
    }

    private void showMoreOptions() {

        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        if (hisUid.equals(myUid)) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                beginDeletePost();
                beginDeleteLikes();
                Intent intent = new Intent(PostDetailActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            } else if (id == 1) {
                Intent intent = new Intent(PostDetailActivity.this, AddPostActivity.class);
                intent.putExtra("key", "editPost");
                intent.putExtra("editPostId", postId);
                startActivity(intent);
            }
            return false;
        });
        popupMenu.show();
    }

    private void beginDeleteLikes() {

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

    private void beginDeletePost() {
        if (pImage.equals("noImage")) {
            deleteWithoutImage();
        } else {
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting..!!");

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(aVoid -> FirebaseFirestore.getInstance().collection("Posts").document(postId).delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(PostDetailActivity.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }
        })).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteWithoutImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting..!!");

        FirebaseFirestore.getInstance().collection("Posts").document(postId).delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(PostDetailActivity.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setLikes() {
        CollectionReference likesRef = FirebaseFirestore.getInstance().collection("Likes");
        likesRef.document(postId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.getString(myUid) != null) {
                likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                likeBtn.setText("Liked");
            } else {
                likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                likeBtn.setText("Like");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void likePost() {

        mProcessLike = true;

        CollectionReference likesRef = FirebaseFirestore.getInstance().collection("Likes");
        CollectionReference postsRef = FirebaseFirestore.getInstance().collection("Posts");
        likesRef.document(postId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.getString(myUid) != null) {
                postsRef.document(postId).get().addOnSuccessListener(documentSnapshot1 -> {
                    int currentLikes = Integer.parseInt(Objects.requireNonNull(documentSnapshot1.getString("pLikes")));
                    HashMap<String, Object> hashMap = new HashMap();
                    hashMap.put(myUid, FieldValue.delete());
                    likesRef.document(postId).update(hashMap).addOnSuccessListener(unused -> {
                        HashMap<String, Object> hashMap2 = new HashMap();
                        hashMap2.put("pLikes", "" + (currentLikes - 1));
                        postsRef.document(postId).update(hashMap2).addOnSuccessListener(unused1 -> {
                            mProcessLike = false;
                            setLikes();
                            pLikesTv.setText((likeInt - 1) + " Likes");
                            likeInt -= 1;
                        });
                    });
                });
            } else {
                postsRef.document(postId).get().addOnSuccessListener(documentSnapshot12 -> {
                    int currentLikes = Integer.parseInt(Objects.requireNonNull(documentSnapshot12.getString("pLikes")));
                    HashMap<String, Object> hashMap = new HashMap();
                    hashMap.put(myUid, "Liked");
                    likesRef.document(postId).set(hashMap).addOnSuccessListener(unused -> {
                        HashMap<String, Object> hashMap2 = new HashMap();
                        hashMap2.put("pLikes", "" + (currentLikes + 1));
                        postsRef.document(postId).update(hashMap2).addOnSuccessListener(unused12 -> {
                            mProcessLike = false;
                            setLikes();
                            pLikesTv.setText((likeInt + 1) + " Likes");
                            likeInt += 1;
                            addToHisNotification("" + hisUid, "" + postId, "Liked your post");
                        });
                    });
                });
            }
        });
    }

    private void postComment() {

        pd = new ProgressDialog(this);
        pd.setMessage("Adding Comment");

        String comment = Objects.requireNonNull(commentEt.getText()).toString().trim();

        if (TextUtils.isEmpty(comment)) {
            Toast.makeText(this, "Comment is Empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uPhone", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);

        FirebaseFirestore.getInstance().collection("Posts").document(postId).collection("Comments").document(timeStamp).set(hashMap).addOnSuccessListener(unused -> {
            pd.dismiss();
            commentEt.setText("");
            updateCommentCount();
            addToHisNotification("" + hisUid, "" + postId, "Commented your post");
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCommentCount() {
        mProcessComment = true;
        DocumentReference documentReference = FirebaseFirestore.getInstance().collection("Posts").document(postId);
        documentReference.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String comments = documentSnapshot.getString("pComments");
                int newCommentVal = Integer.parseInt(comments) + 1;
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("pComments", "" + newCommentVal);
                documentReference.update(hashMap).addOnSuccessListener(unused -> mProcessComment = false);
            }
        });
    }

    private void loadUserInfo() {

        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    myName = "" + ds.child("name").getValue();
                    myDp = "" + ds.child("image").getValue();

                    try {
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(cAvatarIv);
                    } catch (Exception e) {
                        //Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadPostInfo() {

        FirebaseFirestore.getInstance().collection("Posts").document(postId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String pTitle = documentSnapshot.getString("pTitle");
                String pLocation = documentSnapshot.getString("pLocation");
                String pDescr = documentSnapshot.getString("pDescr");
                pLikes = documentSnapshot.getString("pLikes");
                likeInt = Integer.parseInt(Objects.requireNonNull(pLikes));
                String pTimeStamp = documentSnapshot.getString("pTime");
                pImage = documentSnapshot.getString("pImage");
                hisDp = documentSnapshot.getString("uDp");
                hisUid = documentSnapshot.getString("uid");
                hisName = documentSnapshot.getString("uName");
                String commentCount = documentSnapshot.getString("pComments");

                Calendar calendar = Calendar.getInstance(Locale.getDefault());
                calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                pTitleTv.setText(pTitle);
                pLocationTv.setText(pLocation);
                pDescriptionTv.setText(pDescr);
                pLikesTv.setText(pLikes + "Likes");
                pTimeTiv.setText(pTime);
                pCommentsTv.setText(commentCount + " Comments");
                uNameTv.setText(hisName);

                if (pImage.equals("noImage")) {
                    pImageIv.setVisibility(View.GONE);
                } else {

                    pImageIv.setVisibility(View.VISIBLE);
                    try {
                        Picasso.get().load(pImage).into(pImageIv);
                    } catch (Exception ignored) {

                    }
                }

                try {
                    Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img).into(uPictureIv);
                } catch (Exception e) {
                    //Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv);
                }
            }
        });
    }

    private void checkUserStatus() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            myEmail = user.getEmail();
            myUid = user.getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();

        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(PostDetailActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_notifications).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }
}