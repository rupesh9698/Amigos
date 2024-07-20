package com.social.amigos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class InfoActivity extends AppCompatActivity {

    String currentUserID;
    ProgressDialog progressDialog;
    private AppCompatEditText setupFullNameEt, setupPhoneEt;
    private DatabaseReference UserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);

        setupFullNameEt = findViewById(R.id.setupFullNameEt);
        setupPhoneEt = findViewById(R.id.setupPhoneEt);
        Button setupRegisterBtn = findViewById(R.id.setupRegisterBtn);
        progressDialog = new ProgressDialog(InfoActivity.this);

        setupRegisterBtn.setOnClickListener(v -> saveAccountSetupinfo());
    }

    private void saveAccountSetupinfo() {

        String fullName = Objects.requireNonNull(setupFullNameEt.getText()).toString();
        String phoneNumber = Objects.requireNonNull(setupPhoneEt.getText()).toString();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Enter Full Name", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
        } else if (phoneNumber.length() != 10) {
            Toast.makeText(this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.setMessage("Saving Information");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            HashMap userMap = new HashMap();
            userMap.put("name", fullName);
            userMap.put("phone", phoneNumber);
            userMap.put("onlineStatus", "online");
            userMap.put("image", "");
            userMap.put("cover", "");

            UserRef.updateChildren(userMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    progressDialog.dismiss();
                    SendUserToProfileActivity();
                    //Toast.makeText(InfoActivity.this,"Account is created successfully",Toast.LENGTH_LONG).show();
                } else {
                    progressDialog.dismiss();
                    String message = Objects.requireNonNull(task.getException()).getMessage();
                    Toast.makeText(InfoActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(InfoActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void SendUserToProfileActivity() {
        Intent profileIntent = new Intent(InfoActivity.this, DashboardActivity.class);
        profileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(profileIntent);
        finish();
    }
}