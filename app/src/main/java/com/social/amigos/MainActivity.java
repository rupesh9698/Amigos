package com.social.amigos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.util.PatternsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ActionBar actionBar;
    AppCompatEditText emailEt, passwordEt;
    AppCompatTextView forgetPasswordTv, createNewAccountTv;
    AppCompatButton loginBtn;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Login");

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        forgetPasswordTv = findViewById(R.id.forgetPasswordTv);
        createNewAccountTv = findViewById(R.id.createNewAccountTv);
        loginBtn = findViewById(R.id.loginBtn);
        progressDialog = new ProgressDialog(MainActivity.this);

        forgetPasswordTv.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, ForgetPasswordActivity.class);
            startActivity(intent);
        });

        loginBtn.setOnClickListener(v -> {

            String email = Objects.requireNonNull(emailEt.getText()).toString();
            String password = Objects.requireNonNull(passwordEt.getText()).toString();

            if (email.equals("")) {

                Toast.makeText(MainActivity.this, "Email Cannot be Empty", Toast.LENGTH_SHORT).show();
            } else if (password.equals("")) {

                Toast.makeText(MainActivity.this, "Password Cannot be Empty", Toast.LENGTH_SHORT).show();
            } else if (!(PatternsCompat.EMAIL_ADDRESS.matcher(email).matches())) {

                Toast.makeText(MainActivity.this, "The Email Address is Badly Formatted", Toast.LENGTH_SHORT).show();
            } else {

                progressDialog.setMessage("Please wait");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, task -> {

                    if (task.isSuccessful()) {

                        Intent main = new Intent(MainActivity.this, DashboardActivity.class);
                        startActivity(main);
                        finish();

                    } else {

                        if (task.getException() != null) {
                            Toast.makeText(MainActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    progressDialog.dismiss();
                });
            }
        });

        createNewAccountTv.setOnClickListener(v -> {
            Intent registerIntent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        });
    }
}