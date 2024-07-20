package com.social.amigos;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.util.PatternsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgetPasswordActivity extends AppCompatActivity {

    ActionBar actionBar;
    AppCompatEditText emailEt;
    AppCompatButton resetPasswordBtn;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Forgot Password");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        progressDialog = new ProgressDialog(ForgetPasswordActivity.this);

        emailEt = findViewById(R.id.emailEt);
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn);

        resetPasswordBtn.setOnClickListener(v -> {

            String email = Objects.requireNonNull(emailEt.getText()).toString();

            if (email.equals("")) {

                Toast.makeText(ForgetPasswordActivity.this, "Email Cannot be Empty", Toast.LENGTH_SHORT).show();
            } else if (!(PatternsCompat.EMAIL_ADDRESS.matcher(email).matches())) {

                Toast.makeText(ForgetPasswordActivity.this, "The Email Address is Badly Formatted", Toast.LENGTH_SHORT).show();
            } else {

                progressDialog.setMessage("Please wait");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ForgetPasswordActivity.this);

                        alertDialogBuilder.setTitle("Reset Password");

                        alertDialogBuilder.setMessage("Password reset link sent to " + email).setCancelable(false).setPositiveButton("OK", (dialog, id) -> ForgetPasswordActivity.this.finish());

                        AlertDialog alertDialog = alertDialogBuilder.create();
                        progressDialog.dismiss();
                        alertDialog.show();
                    } else {

                        Toast.makeText(ForgetPasswordActivity.this, "" + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}