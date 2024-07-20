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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupCreateActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_create);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle("Create Group");

        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleEt = findViewById(R.id.groupTitleEt);
        groupDescriptionEt = findViewById(R.id.groupDescriptionEt);
        FloatingActionButton createGroupBtn = findViewById(R.id.createGroupBtn);

        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        groupIconIv.setOnClickListener(v -> showImagePickDialog());

        createGroupBtn.setOnClickListener(v -> startCreatingGroup());
    }

    private void startCreatingGroup() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating Group");

        final String groupTitle = Objects.requireNonNull(groupTitleEt.getText()).toString().trim();
        final String groupDescription = Objects.requireNonNull(groupDescriptionEt.getText()).toString().trim();

        if (TextUtils.isEmpty(groupTitle)) {
            Toast.makeText(this, "Enter Group Title", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);

        final String g_timestamp = "" + System.currentTimeMillis();

        if (image_uri == null) {
            createGroup("" + g_timestamp, "" + groupTitle, "" + groupDescription, "");
        } else {
            String fileNameAndPath = "Group_Imgs/" + "image" + g_timestamp;
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNameAndPath);
            storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> p_uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!p_uriTask.isSuccessful()) ;
                Uri p_downloadUri = p_uriTask.getResult();
                if (p_uriTask.isSuccessful()) {
                    createGroup("" + g_timestamp, "" + groupTitle, "" + groupDescription, "" + p_downloadUri);
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(GroupCreateActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void createGroup(final String g_timestamp, String groupTitle, String groupDescription, String groupIcon) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("groupId", "" + g_timestamp);
        hashMap.put("groupTitle", "" + groupTitle);
        hashMap.put("groupDescription", "" + groupDescription);
        hashMap.put("groupIcon", "" + groupIcon);
        hashMap.put("timestamp", "" + g_timestamp);
        hashMap.put("createdBy", "" + firebaseAuth.getUid());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(g_timestamp).setValue(hashMap).addOnSuccessListener(aVoid -> {
            HashMap<String, String> hashMap1 = new HashMap<>();
            hashMap1.put("uid", firebaseAuth.getUid());
            hashMap1.put("role", "creator");
            hashMap1.put("timestamp", g_timestamp);

            DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Groups");
            ref1.child(g_timestamp).child("Participants").child(Objects.requireNonNull(firebaseAuth.getUid())).setValue(hashMap1).addOnSuccessListener(aVoid1 -> {
                progressDialog.dismiss();
                Toast.makeText(GroupCreateActivity.this, "Group Created Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(GroupCreateActivity.this, GroupChatActivity.class);
                intent.putExtra("groupId", g_timestamp);
                startActivity(intent);
                finish();
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(GroupCreateActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(GroupCreateActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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