package com.social.amigos;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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
import com.social.amigos.adapters.AdapterGroupChat;
import com.social.amigos.models.ModelGroupChat;
import com.social.amigos.models.ModelGroupChatList;
import com.social.amigos.models.ModelUser;
import com.social.amigos.notifications.Data;
import com.social.amigos.notifications.Sender;
import com.social.amigos.notifications.Token;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

public class GroupChatActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    LinearLayoutCompat attachBtnLL, cameraBtnLL, galleryBtnLL;
    String[] camerapermission;
    String[] storagepermission;
    private FirebaseAuth firebaseAuth;
    private String myUid, myName, groupId, groupName, myGroupRole = "";
    private CircleImageView groupIconIv;
    private AppCompatTextView groupTitleTv;
    private AppCompatEditText messageEt;
    private RecyclerView chatRv;
    private boolean hidden = true;
    private boolean notify = false;
    private RequestQueue requestQueue;
    private ArrayList<ModelGroupChat> groupChatList;
    private AdapterGroupChat adapterGroupChat;
    private Uri image_uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleTv = findViewById(R.id.groupTitleTv);
        ImageButton attachBtn = findViewById(R.id.attachBtn);
        messageEt = findViewById(R.id.messageEt);
        ImageButton sendBtn = findViewById(R.id.sendBtn);
        chatRv = findViewById(R.id.chatRv);

        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");

        camerapermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagepermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
        loadGroupInfo();
        loadGroupMessages();
        loadMyGroupRole();

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ModelUser user = snapshot.getValue(ModelUser.class);

                assert user != null;
                myName = user.getName();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups").child(groupId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ModelGroupChatList groupChatList = snapshot.getValue(ModelGroupChatList.class);

                assert groupChatList != null;
                groupName = groupChatList.getGroupTitle();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

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

        sendBtn.setOnClickListener(v -> {
            notify = true;
            String message = Objects.requireNonNull(messageEt.getText()).toString().trim();
            if (TextUtils.isEmpty(message)) {
                //Toast.makeText(GroupChatActivity.this, "Empty Message", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    sendMessage(message);
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
            }
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
                    sendBtn.setVisibility(View.GONE);
                } else {
                    sendBtn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "GroupImageTitle");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "GroupImageDescription");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagepermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, camerapermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void sendImageMessage() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending image");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        String fileNamePath = "ChatImages/" + "group_" + System.currentTimeMillis();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNamePath);
        storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> p_uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!p_uriTask.isSuccessful()) ;
            String p_downloadUri = Objects.requireNonNull(p_uriTask.getResult()).toString();

            if (p_uriTask.isSuccessful()) {

                String timestamp = "" + System.currentTimeMillis();

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", firebaseAuth.getUid());
                hashMap.put("message", p_downloadUri);
                hashMap.put("timestamp", timestamp);
                hashMap.put("type", "image");

                final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                ref.child(groupId).child("Messages").child(timestamp).setValue(hashMap).addOnSuccessListener(aVoid -> {
                    messageEt.setText("");
                    progressDialog.dismiss();

                    ref.child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            //ModelUser user = snapshot.getValue(ModelUser.class);
                            String message = "Sent you a Picture";

                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String participant = "" + ds.getKey();
                                if (notify) {
                                    senNotification(participant, myName, message, groupName);
                                }
                            }
                            notify = false;
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        });
    }

    private void loadMyGroupRole() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    myGroupRole = "" + ds.child("role").getValue();
                    invalidateOptionsMenu();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadGroupMessages() {

        groupChatList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelGroupChat model = ds.getValue(ModelGroupChat.class);
                    groupChatList.add(model);
                }
                adapterGroupChat = new AdapterGroupChat(GroupChatActivity.this, groupChatList);
                chatRv.setAdapter(adapterGroupChat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(final String message) throws GeneralSecurityException {
        String timestamp = "" + System.currentTimeMillis();

        String encrpyted = AESCrypt.encrypt(timestamp, message);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", firebaseAuth.getUid());
        hashMap.put("message", encrpyted);
        hashMap.put("timestamp", timestamp);
        hashMap.put("type", "text");

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages").child(timestamp).setValue(hashMap).addOnSuccessListener(aVoid -> {
            messageEt.setText("");

            //final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups");
            ref.child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //ModelUser user = snapshot.getValue(ModelUser.class);

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String participant = "" + ds.getKey();
                        if (notify) {
                            senNotification(participant, myName, message, groupName);
                        }
                    }
                    notify = false;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }).addOnFailureListener(e -> Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void senNotification(final String participant, final String name, final String message, final String groupName) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(participant);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data("" + groupId, "" + myUid, "" + name + " : " + message, "" + groupName, "" + participant, "GroupChatNotification", R.drawable.logo);

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

    private void loadGroupInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.orderByChild("groupId").equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String groupTitle = "" + ds.child("groupTitle").getValue();
                    //String groupDescription = ""+ds.child("groupDescription").getValue();
                    String groupIcon = "" + ds.child("groupIcon").getValue();
                    //String timestamp = ""+ds.child("timestamp").getValue();
                    //String createdBy = ""+ds.child("createdBy").getValue();

                    groupTitleTv.setText(groupTitle);
                    try {
                        Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_white).into(groupIconIv);
                    } catch (Exception e) {
                        groupIconIv.setImageResource(R.drawable.ic_group_white);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_notifications).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        menu.findItem(R.id.action_add_participant).setVisible(myGroupRole.equals("creator") || myGroupRole.equals("admin"));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_participant) {
            Intent intent = new Intent(this, GroupParticipantAddActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        } else if (id == R.id.action_groupinfo) {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                assert data != null;
                image_uri = data.getData();

                sendImageMessage();
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                sendImageMessage();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
}