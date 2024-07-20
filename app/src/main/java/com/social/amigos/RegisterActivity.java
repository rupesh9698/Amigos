package com.social.amigos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    ActionBar actionBar;
    AppCompatEditText emailEt, passwordEt;
    AppCompatButton registerBtn;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Register");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        registerBtn = findViewById(R.id.registerBtn);
        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(RegisterActivity.this);

        registerBtn.setOnClickListener(view -> {
            String email = Objects.requireNonNull(emailEt.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordEt.getText()).toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.setError("Invalid Email");
                emailEt.setFocusable(true);
            } else if (password.length() < 6) {
                passwordEt.setError("Password Length Must be greater than 6 Characters");
                passwordEt.setFocusable(true);
            } else {
                registerUser(email, password);
            }
        });
    }

    private void registerUser(String email, String password) {

        progressDialog.show();
        progressDialog.setMessage("Creating New Account");
        //progressDialog.setMessage("Register");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();

                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        HashMap<String, Object> hashMap = new HashMap<>();
                        assert user != null;
                        hashMap.put("email", user.getEmail());
                        hashMap.put("uid", user.getUid());
                        hashMap.put("onlineStatus", "online");
                        hashMap.put("image", "");
                        hashMap.put("cover", "");

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                        reference.child("Users").child(user.getUid()).updateChildren(hashMap).addOnCompleteListener(task1 -> {

                            if (task1.isSuccessful()) {

                                Intent registerIntent = new Intent(RegisterActivity.this, InfoActivity.class);
                                registerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(registerIntent);
                                finish();
                            }
                        });

                    } else {
                        // If sign in fails, display a message to the user.
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}