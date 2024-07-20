package com.social.amigos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class AddPostActivity extends AppCompatActivity implements LocationListener {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;
    ActionBar actionBar;
    String[] camerapermission;
    String[] storagepermission;
    AppCompatEditText titleEt, descriptionEt, locationEt;
    AppCompatImageView imageIv;
    AppCompatButton uploadBtn;
    AppCompatImageButton locationBtn;
    String name, email, uid, dp;
    String editTitle, editDescription, editImage, editLocation;
    LocationManager locationManager;
    Uri image_rui = null;
    ProgressDialog pd;

    @SuppressLint({"WrongViewCast", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Add New Post");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        camerapermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagepermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        pd = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        titleEt = findViewById(R.id.pTitleEt);
        descriptionEt = findViewById(R.id.pDescriptionEt);
        locationEt = findViewById(R.id.pLocationEt);
        imageIv = findViewById(R.id.pImageIv);

        locationBtn = findViewById(R.id.pLocationBtn);
        uploadBtn = findViewById(R.id.pUploadBtn);

        grantGpsPermission();
        checkLocationIsEnabledOrNot();
        getLocationInApp();

        Intent intent = getIntent();

        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {

            if ("text/plain".equals(type)) {
                handleSendText(intent);
            } else {
                handleSendImage(intent);
            }
        }

        final String isUpdateKey = "" + intent.getStringExtra("key");
        final String editPostId = "" + intent.getStringExtra("editPostId");
        final String repostId = "" + intent.getStringExtra("repostId");

        if (isUpdateKey.equals("editPost")) {
            actionBar.setTitle("Update Post");
            uploadBtn.setText("Update");
            loadPostData(editPostId);
        } else if (isUpdateKey.equals("repost")) {
            actionBar.setTitle("Repost");
            uploadBtn.setText("Repost");
            loadPostDataForRespost(repostId);
        } else {
            actionBar.setTitle("Add New Post");
            uploadBtn.setText("Upload");
        }

        actionBar.setSubtitle("Logged in as : " + email);

        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    name = "" + ds.child("name").getValue();
                    email = "" + ds.child("email").getValue();
                    dp = "" + ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        locationBtn.setOnClickListener(v -> {
            grantGpsPermission();
            checkLocationIsEnabledOrNot();
            getLocationInApp();
        });

        imageIv.setOnClickListener(v -> showImagePickDialog());

        uploadBtn.setOnClickListener(v -> {
            String title = Objects.requireNonNull(titleEt.getText()).toString().trim();
            String location = Objects.requireNonNull(locationEt.getText()).toString().trim();
            String description = Objects.requireNonNull(descriptionEt.getText()).toString().trim();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(AddPostActivity.this, "Enter Title..!!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(location)) {
                Toast.makeText(AddPostActivity.this, "Enter Location..!!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(description)) {
                Toast.makeText(AddPostActivity.this, "Enter Description..!!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isUpdateKey.equals("editPost")) {
                beginUpdate(title, location, description, editPostId);
            } else if (isUpdateKey.equals("repost")) {
                repostData(title, location, description);
            } else {
                uploadData(title, location, description);
            }
        });
    }

    private void handleSendImage(Intent intent) {
        Uri imageURI = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageURI != null) {
            image_rui = imageURI;
            imageIv.setImageURI(image_rui);
        }
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            descriptionEt.setText(sharedText);
        }
    }

    private void loadPostDataForRespost(String repostId) {

        FirebaseFirestore.getInstance().collection("Posts").document(repostId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                editTitle = "" + documentSnapshot.getString("pTitle");
                editLocation = "" + documentSnapshot.getString("pLocation");
                editDescription = "" + documentSnapshot.getString("pDescr");
                editImage = "" + documentSnapshot.getString("pImage");

                titleEt.setText(editTitle);
                locationEt.setText(editLocation);
                descriptionEt.setText(editDescription);

                if (!editImage.equals("noImage")) {
                    try {
                        Picasso.get().load(editImage).into(imageIv);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private void getLocationInApp() {
        pd.show();
        pd.setMessage("Please Wait..!!");
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 50, 5, this);
            pd.dismiss();
        } catch (SecurityException e) {
            e.printStackTrace();
            pd.dismiss();
        }
    }

    private void checkLocationIsEnabledOrNot() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gpsEnabled && !networkEnabled) {
            new AlertDialog.Builder(AddPostActivity.this).setTitle("Enable GPS Service").setCancelable(false).setPositiveButton("Enable", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("Cancel", null).show();
        }
    }

    private void grantGpsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 300);
        }
    }

    private void beginUpdate(String title, String location, String description, String editPostId) {
        pd.setMessage("Updating Post");
        pd.show();
        pd.setCanceledOnTouchOutside(false);

        if (!editImage.equals("noImage")) {
            updateWasWithImage(title, location, description, editPostId);
        } else if (imageIv.getDrawable() != null) {

            updateWithNowImage(title, location, description, editPostId);
        } else {
            updateWithoutImage(title, location, description, editPostId);
        }
    }

    private void updateWithoutImage(String title, String location, String description, String editPostId) {

        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("uid", uid);
        hashMap.put("uName", name);
        hashMap.put("uEmail", email);
        hashMap.put("uDp", dp);
        hashMap.put("pLocation", location);
        hashMap.put("pTitle", title);
        hashMap.put("pDescr", description);
        hashMap.put("pImage", "noImage");

        FirebaseFirestore.getInstance().collection("Posts").document(editPostId).update(hashMap).addOnSuccessListener(unused -> {
            pd.dismiss();
            Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
            DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(DashboardIntent);
            finish();
            Toast.makeText(AddPostActivity.this, "Post Updated", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateWithNowImage(final String title, final String location, final String description, final String editPostId) {

        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;

        Bitmap bitmap = ((BitmapDrawable) imageIv.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;

            String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();

            if (uriTask.isSuccessful()) {

                HashMap<String, Object> hashMap = new HashMap<>();

                hashMap.put("uid", uid);
                hashMap.put("uName", name);
                hashMap.put("uEmail", email);
                hashMap.put("uDp", dp);
                hashMap.put("pLocation", location);
                hashMap.put("pTitle", title);
                hashMap.put("pDescr", description);
                hashMap.put("pImage", downloadUri);

                FirebaseFirestore.getInstance().collection("Posts").document(editPostId).update(hashMap).addOnSuccessListener(unused -> {
                    pd.dismiss();
                    Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
                    DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(DashboardIntent);
                    finish();
                    Toast.makeText(AddPostActivity.this, "Post Updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateWasWithImage(final String title, final String location, final String description, final String editPostId) {

        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete().addOnSuccessListener(aVoid -> {

            String timeStamp = String.valueOf(System.currentTimeMillis());
            String filePathAndName = "Posts/" + "post_" + timeStamp;

            Bitmap bitmap = ((BitmapDrawable) imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful()) ;

                String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();

                if (uriTask.isSuccessful()) {

                    HashMap<String, Object> hashMap = new HashMap<>();

                    hashMap.put("uid", uid);
                    hashMap.put("uName", name);
                    hashMap.put("uEmail", email);
                    hashMap.put("uDp", dp);
                    hashMap.put("pLocation", location);
                    hashMap.put("pTitle", title);
                    hashMap.put("pDescr", description);
                    hashMap.put("pImage", downloadUri);

                    FirebaseFirestore.getInstance().collection("Posts").document(editPostId).update(hashMap).addOnSuccessListener(unused -> {
                        pd.dismiss();
                        Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
                        DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(DashboardIntent);
                        finish();
                        Toast.makeText(AddPostActivity.this, "Post Updated", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).addOnFailureListener(e -> {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadPostData(String editPostId) {

        FirebaseFirestore.getInstance().collection("Posts").document(editPostId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                editTitle = documentSnapshot.getString("pTitle");
                editLocation = documentSnapshot.getString("pLocation");
                editDescription = documentSnapshot.getString("pDescr");
                editImage = documentSnapshot.getString("pImage");

                titleEt.setText(editTitle);
                locationEt.setText(editLocation);
                descriptionEt.setText(editDescription);

                if (!editImage.equals("noImage")) {
                    try {
                        Picasso.get().load(editImage).into(imageIv);
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    private void uploadData(final String title, final String location, final String description) {

        pd.setMessage("Uploading Post");
        pd.show();
        pd.setCanceledOnTouchOutside(false);

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;

        if (imageIv.getDrawable() != null) {

            Bitmap bitmap = ((BitmapDrawable) imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful()) ;

                String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();

                if (uriTask.isSuccessful()) {

                    HashMap<Object, String> hashMap = new HashMap<>();
                    hashMap.put("uid", uid);
                    hashMap.put("uName", name);
                    hashMap.put("uEmail", email);
                    hashMap.put("uDp", dp);
                    hashMap.put("pLikes", "0");
                    hashMap.put("pLocation", location);
                    hashMap.put("pComments", "0");
                    hashMap.put("pId", timeStamp);
                    hashMap.put("pTitle", title);
                    hashMap.put("pDescr", description);
                    hashMap.put("pImage", downloadUri);
                    hashMap.put("pTime", timeStamp);

                    FirebaseFirestore.getInstance().collection("Posts").document(timeStamp).set(hashMap).addOnSuccessListener(unused -> {
                        pd.dismiss();
                        Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
                        DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(DashboardIntent);
                        finish();

                        prepareNotification("" + timeStamp, "" + name + " Uploaded New Post", "" + title + " : " + description);
                    }).addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).addOnFailureListener(e -> {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            HashMap<Object, String> hashMap = new HashMap<>();
            hashMap.put("uid", uid);
            hashMap.put("uName", name);
            hashMap.put("uEmail", email);
            hashMap.put("uDp", dp);
            hashMap.put("pLikes", "0");
            hashMap.put("pLocation", location);
            hashMap.put("pComments", "0");
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescr", description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", timeStamp);

            FirebaseFirestore.getInstance().collection("Posts").document(timeStamp).set(hashMap).addOnSuccessListener(unused -> {
                pd.dismiss();
                Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
                DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(DashboardIntent);
                finish();

                prepareNotification("" + timeStamp, "" + name + " Uploaded New Post", "" + title + " : " + description);
            }).addOnFailureListener(e -> {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void repostData(final String title, final String location, final String description) {

        pd.setMessage("Reposting");
        pd.show();
        pd.setCanceledOnTouchOutside(false);

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;

        if (imageIv.getDrawable() != null) {

            Bitmap bitmap = ((BitmapDrawable) imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful()) ;

                String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();

                if (uriTask.isSuccessful()) {

                    HashMap<Object, String> hashMap = new HashMap<>();
                    hashMap.put("uid", uid);
                    hashMap.put("uName", name);
                    hashMap.put("uEmail", email);
                    hashMap.put("uDp", dp);
                    hashMap.put("pLikes", "0");
                    hashMap.put("pLocation", location);
                    hashMap.put("pComments", "0");
                    hashMap.put("pId", timeStamp);
                    hashMap.put("pTitle", title);
                    hashMap.put("pDescr", description);
                    hashMap.put("pImage", downloadUri);
                    hashMap.put("pTime", timeStamp);

                    FirebaseFirestore.getInstance().collection("Posts").document(timeStamp).set(hashMap).addOnSuccessListener(unused -> {
                        pd.dismiss();
                        Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
                        DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(DashboardIntent);
                        finish();

                        prepareNotification("" + timeStamp, "" + name + " Uploaded New Post", "" + title + " : " + description);
                    }).addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).addOnFailureListener(e -> {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            HashMap<Object, String> hashMap = new HashMap<>();
            hashMap.put("uid", uid);
            hashMap.put("uName", name);
            hashMap.put("uEmail", email);
            hashMap.put("uDp", dp);
            hashMap.put("pLikes", "0");
            hashMap.put("pLocation", location);
            hashMap.put("pComments", "0");
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescr", description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", timeStamp);

            FirebaseFirestore.getInstance().collection("Posts").document(timeStamp).set(hashMap).addOnSuccessListener(unused -> {
                pd.dismiss();

                Intent DashboardIntent = new Intent(AddPostActivity.this, DashboardActivity.class);
                DashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(DashboardIntent);
                finish();

                prepareNotification("" + timeStamp, "" + name + " Uploaded New Post", "" + title + " : " + description);
            }).addOnFailureListener(e -> {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void prepareNotification(String pId, String title, String description) {
        String NOTIFICATION_TOPIC = "/topics/" + "POST";
        String NOTIFICATION_TYPE = "PostNotification";

        JSONObject notificationJo = new JSONObject();
        JSONObject notificationBodyJo = new JSONObject();

        try {
            notificationBodyJo.put("notificationType", NOTIFICATION_TYPE);
            notificationBodyJo.put("sender", uid);
            notificationBodyJo.put("pId", pId);
            notificationBodyJo.put("pTitle", title);
            notificationBodyJo.put("pDescription", description);

            notificationJo.put("to", NOTIFICATION_TOPIC);

            notificationJo.put("data", notificationBodyJo);
        } catch (JSONException e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        sendPostNotification(notificationJo);
    }

    private void sendPostNotification(JSONObject notificationJo) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, response -> Timber.d("onResponse: %s", response.toString()), error -> Toast.makeText(AddPostActivity.this, "" + error.toString(), Toast.LENGTH_SHORT).show()) {
            @Override
            public Map<String, String> getHeaders() {

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "key=AAAAVjrUzl8:APA91bEZPsitpyKKorWJ0rbBSNVIcNRNwMhu5T-VPZv3O1cQb2Q6c1-TiDVpqm7wWgCXMRp9HwBMqzNlQTTNnXGNDPh4UgfyGt91xG65EbCA6AIxdWCqr7rvpSds9PF_HVFi2flvJ_AY");

                return headers;
            }
        };

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void showImagePickDialog() {

        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");

        builder.setItems(options, (dialog, which) -> {
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
        });

        builder.create().show();
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

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            email = user.getEmail();
            uid = user.getUid();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_notifications).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(AddPostActivity.this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
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

                imageIv.setImageURI(image_rui);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                imageIv.setImageURI(image_rui);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(@NonNull Location location) {

        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            locationEt.setText("" + addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + " " + addresses.get(0).getPostalCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }
}