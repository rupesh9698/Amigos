package com.social.amigos;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupEditActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private String[] cameraPermission;
    private String[] storagePermission;
    private Uri image_uri = null;
    private ActionBar actionBar;
    private FirebaseAuth firebaseAuth;
    private CircleImageView groupIconIv;
    private AppCompatEditText groupTitleEt, groupDescriptionEt;
    private ProgressDialog progressDialog;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_edit);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Edit Group");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleEt = findViewById(R.id.groupTitleEt);
        groupDescriptionEt = findViewById(R.id.groupDescriptionEt);
        FloatingActionButton updateGroupBtn = findViewById(R.id.updateGroupBtn);

        groupId = getIntent().getStringExtra("groupId");

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();
        loadGroupInfo();

        groupIconIv.setOnClickListener(v -> showImagePickDialog());

        updateGroupBtn.setOnClickListener(v -> startUpdatingGroup());
    }

    private void startUpdatingGroup() {

        final String groupTitle = Objects.requireNonNull(groupTitleEt.getText()).toString().trim();
        final String groupDescription = Objects.requireNonNull(groupDescriptionEt.getText()).toString().trim();

        if (TextUtils.isEmpty(groupTitle)) {
            Toast.makeText(this, "Group Title is Required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Updating Group Info..!!");
        progressDialog.show();

        if (image_uri == null) {

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("groupTitle", groupTitle);
            hashMap.put("groupDescription", groupDescription);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
            ref.child(groupId).updateChildren(hashMap).addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                Toast.makeText(GroupEditActivity.this, "Group Info Updated Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(GroupEditActivity.this, GroupChatActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
                finish();
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(GroupEditActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {

            String timestamp = "" + System.currentTimeMillis();

            String fileNameAndPath = "Group_Imgs/" + "image" + timestamp;

            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNameAndPath);
            storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> p_uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!p_uriTask.isSuccessful()) ;
                Uri p_downloadUri = p_uriTask.getResult();
                if (p_uriTask.isSuccessful()) {

                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("groupTitle", groupTitle);
                    hashMap.put("groupDescription", groupDescription);
                    hashMap.put("groupIcon", "" + p_downloadUri);

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                    ref.child(groupId).updateChildren(hashMap).addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(GroupEditActivity.this, "Group Info Updated Successfully", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(GroupEditActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(GroupEditActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadGroupInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.orderByChild("groupId").equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //String groupId = ""+ds.child("groupId").getValue();
                    String groupTitle = "" + ds.child("groupTitle").getValue();
                    String groupDescription = "" + ds.child("groupDescription").getValue();
                    String groupIcon = "" + ds.child("groupIcon").getValue();
                    //String createdBy = ""+ds.child("createdBy").getValue();
                    String timestamp = "" + ds.child("timestamp").getValue();

                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(Long.parseLong(timestamp));
                    //String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

                    groupTitleEt.setText(groupTitle);
                    groupDescriptionEt.setText(groupDescription);

                    try {
                        Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_primary).into(groupIconIv);
                    } catch (Exception e) {
                        //groupIconIv.setImageResource(R.drawable.ic_group_primary);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image").setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission();
                } else {
                    pickFromCamera();
                }
            }
            if (which == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                } else {
                    pickFromGallery();
                }
            }
        }).show();
    }

    private void pickFromGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Group Image Icon Title");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Group Image Icon Description");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            actionBar.setSubtitle(user.getPhoneNumber());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
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
                image_uri = data.getData();

                groupIconIv.setImageURI(image_uri);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                groupIconIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}