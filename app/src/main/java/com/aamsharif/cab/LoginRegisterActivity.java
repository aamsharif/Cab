package com.aamsharif.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginRegisterActivity extends AppCompatActivity {
    private short userType;
    private String user;

    private final String databaseUrl = "https://cabapp-a0b1b-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private TextView statusTextView;
    private Button entryButton;
    private TextView registerTextView;
    private EditText emailEditText;
    private EditText passwordEditText;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);

        statusTextView = findViewById(R.id.status_text);
        entryButton = findViewById(R.id.entry_button);
        registerTextView = findViewById(R.id.register_text);
        emailEditText = findViewById(R.id.user_email);
        passwordEditText = findViewById(R.id.user_password);

        userType = getIntent().getShortExtra(User.TYPE, User.RIDER);
        if(userType == User.DRIVER)
            user = "Driver";
        else
            user = "Rider";

        statusTextView.setText(user + " Login");

        entryButton.setTag(User.LOGIN);

        mAuth = FirebaseAuth.getInstance();

        entryButton.setOnClickListener(view -> {
            final String email = emailEditText.getText().toString();
            final String password = passwordEditText.getText().toString();

            if(TextUtils.isEmpty(email)){
                Toast.makeText(LoginRegisterActivity.this, "Email address is empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            if(TextUtils.isEmpty(password)){
                Toast.makeText(LoginRegisterActivity.this, "Password is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog = new ProgressDialog(LoginRegisterActivity.this);
            final short action = (short) entryButton.getTag();
            showProgress(user, action);

            if(action == User.LOGIN) {
                login(email, password);
            } else {
                register(email, password);
            }
        });

        registerTextView.setOnClickListener(view -> {
            entryButton.setText("Register");
            registerTextView.setVisibility(View.INVISIBLE);
            statusTextView.setText(user + " Registration");

            entryButton.setTag(User.REGISTER);
        });
    }

    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        nextActivity();
                        Toast.makeText(LoginRegisterActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firebase Sign In", task.getException().getMessage());
                        Toast.makeText(LoginRegisterActivity.this, "Login failed. Please, retry...", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                });
    }

    private void nextActivity() {
        Intent nextActivity = new Intent(LoginRegisterActivity.this,
                userType == User.DRIVER ? DriversMapActivity.class : RidersMapActivity.class);
        nextActivity.putExtra(User.TYPE, userType);
        startActivity(nextActivity);
    }

    private void register(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        String userID = mAuth.getCurrentUser().getUid();
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance(databaseUrl).getReference()
                                .child("Users")
                                .child(user + "s")
                                .child(userID);
                        databaseReference.setValue(true);

                        nextActivity();
                        Toast.makeText(LoginRegisterActivity.this, user + " registration successful.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firebase User Creation", task.getException().getMessage());
                        Toast.makeText(LoginRegisterActivity.this, "Registration failed. Please, retry...", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                });
    }

    private void showProgress(String user, short action) {
        if(action == User.LOGIN) {
            progressDialog.setTitle(user + " Login");
            progressDialog.setMessage("Please wait, while we are logging you in...");
        } else {
            progressDialog.setTitle(user + " Registration");
            progressDialog.setMessage("Please wait, while we are registering...");
        }
        progressDialog.show();
    }
}