package com.social.amigos;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.scottyab.aescrypt.AESCrypt;
import com.social.amigos.adapters.AdapterChat;
import com.social.amigos.models.ModelChat;
import com.social.amigos.models.ModelUser;
import com.social.amigos.notifications.Data;
import com.social.amigos.notifications.Sender;
import com.social.amigos.notifications.Token;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

public class ChatActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 100;
    private static final int IMAGE_PICK_GALLERY_CODE = 200;
    RecyclerView recyclerView;
    CircleImageView profileIv;
    AppCompatImageView moreIv;
    AppCompatTextView nameTv, userStatusTv;
    AppCompatEditText messageEt;
    AppCompatImageButton sendBtn, attachBtn;
    LinearLayoutCompat attachBtnLL, cameraBtnLL, galleryBtnLL;
    boolean hidden = true;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;
    AdapterChat adapterChat;
    List<ModelChat> chatList;
    String hisUid, myUid, hisName, hisImage;
    String userBlockedMeOrNot = "", youBlockedThemOrNot = "";
    boolean isBlocked = false;
    String[] camerapermission;
    String[] storagepermission;
    Uri image_rui = null;
    private RequestQueue requestQueue;
    private boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);

        recyclerView = findViewById(R.id.chat_recyclerView);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(linearLayoutManager);

        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        profileIv = findViewById(R.id.proifleIv);
        moreIv = findViewById(R.id.moreIv);
        sendBtn = findViewById(R.id.sendBtn);
        messageEt = findViewById(R.id.messageEt);
        attachBtn = findViewById(R.id.attachBtn);

        attachBtnLL = findViewById(R.id.attachBtnLL);
        attachBtnLL.setVisibility(View.GONE);

        cameraBtnLL = findViewById(R.id.cameraBtnLL);
        cameraBtnLL.setOnClickListener(view -> {
            if (!checkCameraPermission()) {
                requestCameraPermission();
            } else {
                pickFromCamera();
                attachBtnLL.setVisibility(View.GONE);
            }
        });

        galleryBtnLL = findViewById(R.id.galleryBtnLL);
        galleryBtnLL.setOnClickListener(view -> {
            if (!checkStoragePermission()) {
                requestStoragePermission();
            } else {
                pickFromGallery();
                attachBtnLL.setVisibility(View.GONE);
            }
        });

        camerapermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagepermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        firebaseAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");

        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    hisName = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();

                    if (typingStatus.equals(myUid)) {
                        userStatusTv.setText("typing...");
                    } else {
                        String onlineStatus = "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")) {
                            userStatusTv.setText(onlineStatus);
                        } else if (onlineStatus.equals("offline")) {
                            userStatusTv.setText(onlineStatus);
                        } else {
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
                            userStatusTv.setText("Last seen : " + dateTime);
                        }
                    }

                    nameTv.setText(hisName);

                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);
                    } catch (Exception e) {
                        //Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendBtn.setOnClickListener(view -> {
            notify = true;
            String message = Objects.requireNonNull(messageEt.getText()).toString().trim();

            if (!(TextUtils.isEmpty(message))) {
                try {
                    if (userBlockedMeOrNot.equals("Blocked")) {
                        Toast.makeText(ChatActivity.this, "User Blocked You 💔", Toast.LENGTH_SHORT).show();
                    } else if (youBlockedThemOrNot.equals("Blocked")) {
                        Toast.makeText(ChatActivity.this, "You Blocked This User", Toast.LENGTH_SHORT).show();
                    } else {
                        sendMessage(message);
                    }
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
            }
            messageEt.setText("");
            messageEt.setHint("Start typing");
        });

        attachBtn.setOnClickListener(v -> {
            int cx = attachBtnLL.getLeft();
            int cy = attachBtnLL.getBottom();
            makeEffect(attachBtnLL, cx, cy);
        });

        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                    sendBtn.setVisibility(View.GONE);
                } else {
                    checkTypingStatus(hisUid);
                    sendBtn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        moreIv.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(ChatActivity.this, moreIv, Gravity.END);
            if (isBlocked) popupMenu.getMenu().add(Menu.NONE, 0, 0, "Un-Block");
            else popupMenu.getMenu().add(Menu.NONE, 0, 0, "Block");

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 0) {
                    if (isBlocked) {
                        unBlockUser();
                    } else {
                        blockUser();
                    }
                }
                return false;
            });
            popupMenu.show();
        });

        readMessages();

        checkIsBlocked();

        seenMessage();

    }

    private void makeEffect(final LinearLayoutCompat layout, int cx, int cy) {

        int radius = Math.max(layout.getWidth(), layout.getHeight());

        if (hidden) {
            Animator anim = android.view.ViewAnimationUtils.createCircularReveal(layout, cx, cy, 0, radius);
            layout.setVisibility(View.VISIBLE);
            anim.start();
            hidden = false;

        } else {
            Animator anim = android.view.ViewAnimationUtils.createCircularReveal(layout, cx, cy, radius, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    layout.setVisibility(View.GONE);
                    hidden = true;
                }
            });
            anim.start();

        }
    }

    private void checkIsBlocked() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Blocked List").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child(hisUid).exists()) {
                    isBlocked = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void blockUser() {

        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Users");
        ref1.child(myUid).child("Friend List").child(hisUid).removeValue().addOnCompleteListener(task -> ref1.child(hisUid).child("Friend List").child(myUid).removeValue().addOnCompleteListener(task12 -> ref1.child(myUid).child("Blocked List").child(hisUid).setValue("blocked").addOnCompleteListener(task121 -> {
            DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("Chatlist");
            ref2.child(hisUid).child(myUid).removeValue().addOnCompleteListener(task1 -> {
                DatabaseReference ref3 = FirebaseDatabase.getInstance().getReference("Chatlist");
                ref3.child(myUid).child(hisUid).removeValue().addOnSuccessListener(unused -> Toast.makeText(ChatActivity.this, "Blocked Successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        })));
    }

    private void unBlockUser() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("Blocked List").child(hisUid).removeValue().addOnSuccessListener(unused -> Toast.makeText(ChatActivity.this, "Unblocked Successfully", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pickFromGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temp Descr");
        image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_rui);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagepermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, camerapermission, CAMERA_REQUEST_CODE);
    }

    private void seenMessage() {

        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    assert chat != null;
                    try {
                        if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                            HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                            hasSeenHashMap.put("isSeen", true);
                            ds.getRef().updateChildren(hasSeenHashMap);
                        }
                    } catch (Exception e) {
                        //Toast.makeText(ChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages() {

        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    try {
                        assert chat != null;
                        if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) || chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)) {
                            chatList.add(chat);
                        }
                    } catch (Exception e) {
                        //Toast.makeText(ChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    recyclerView.setAdapter(adapterChat);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void sendMessage(final String message) throws GeneralSecurityException {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());

        String encrpyted = AESCrypt.encrypt(timestamp, message);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", encrpyted);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("type", "text");
        databaseReference.child("Chats").push().setValue(hashMap);

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ModelUser user = snapshot.getValue(ModelUser.class);

                if (notify) {
                    assert user != null;
                    senNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid);
        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendImageMessage(Uri image_rui) throws IOException {
        notify = true;

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending image");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        final String timeStamp = "" + System.currentTimeMillis();

        String fileNameAndPath = "ChatImages/" + "post_" + timeStamp;

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_rui);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            progressDialog.dismiss();
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();

            if (uriTask.isSuccessful()) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", myUid);
                hashMap.put("receiver", hisUid);
                hashMap.put("message", downloadUri);
                hashMap.put("timestamp", timeStamp);
                hashMap.put("type", "image");
                hashMap.put("isSeen", false);

                databaseReference.child("Chats").push().setValue(hashMap);

                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify) {
                            assert user != null;
                            senNotification(hisUid, user.getName(), "Sent you a Picture");
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid);
                chatRef1.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            chatRef1.child("id").setValue(hisUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid);
                chatRef2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            chatRef2.child("id").setValue(myUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        }).addOnFailureListener(e -> {

        });
    }

    private void senNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data("", "" + myUid, "" + name + " : " + message, "New Message", "" + hisUid, "ChatNotification", R.drawable.logo);

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

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        FirebaseDatabase.getInstance().getReference("Users").child(hisUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child("Blocked List").child(myUid).exists()) {
                    userBlockedMeOrNot = "Blocked";
                } else {
                    userBlockedMeOrNot = "Not Blocked";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        FirebaseDatabase.getInstance().getReference("Users").child(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child("Blocked List").child(hisUid).exists()) {
                    youBlockedThemOrNot = "Blocked";
                } else {
                    youBlockedThemOrNot = "Not Blocked";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        String timestamp = String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Permissions Required", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Permission Required", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                assert data != null;
                image_rui = data.getData();

                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_notifications).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }
}