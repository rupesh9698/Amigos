package com.social.amigos;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

public class DeveloperActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Developer");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        SubsamplingScaleImageView developer = findViewById(R.id.developer);
        developer.setImage(ImageSource.resource(R.drawable.developer));

        AppCompatImageButton instagram = findViewById(R.id.instagram);
        AppCompatImageButton facebook = findViewById(R.id.facebook);
        AppCompatImageButton twitter = findViewById(R.id.twitter);

        instagram.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://www.instagram.com/the.rupesh/");
            Intent instagram1 = new Intent(Intent.ACTION_VIEW, uri);
            instagram1.setPackage("com.instagram.android");
            try {
                startActivity(instagram1);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/the.rupesh/")));
            }
        });

        facebook.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://www.facebook.com/rupesh9698/");
            Intent facebook1 = new Intent(Intent.ACTION_VIEW, uri);
            facebook1.setPackage("com.facebook.katana");
            try {
                startActivity(facebook1);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/rupesh9698/")));
            }
        });

        twitter.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://twitter.com/therupeshbagde");
            Intent twitter1 = new Intent(Intent.ACTION_VIEW, uri);
            twitter1.setPackage("com.twitter.android");
            try {
                startActivity(twitter1);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/therupeshbagde")));
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}