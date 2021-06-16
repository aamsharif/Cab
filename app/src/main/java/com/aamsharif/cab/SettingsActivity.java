package com.aamsharif.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private short userType;

    private CircleImageView profileImageView;
    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText driverCarNumber;
    private ImageView closeButton;
    private ImageView saveButton;
    private TextView profileChangeBtn;

    private final String databaseUrl = "https://cabapp-a0b1b-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private DatabaseReference userInfoReference;
    private String userID;

    private Uri imageUri;
    private String profilePicDownloadUrl;
    private StorageReference profilePicStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userType = getIntent().getShortExtra(User.TYPE, User.RIDER);

        profileImageView = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.phone_number);

        if(userType == User.DRIVER){
            driverCarNumber = findViewById(R.id.driver_car_number);
            driverCarNumber.setVisibility(View.VISIBLE);
        }

        closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish());

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            if(areValidUserInputs()) {
                if (imageUri != null) {// user has a profile pic
                    uploadProfilePictureAndSave();
                } else {
                    userInfoReference.child(userID)
                            .updateChildren(getUserInputs());
                    finish();
                }

            }
        });

        profileChangeBtn = findViewById(R.id.change_picture_btn);
        profileChangeBtn.setOnClickListener(v -> CropImage.activity()
                .setAspectRatio(1, 1)
                .start(SettingsActivity.this));

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userInfoReference = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Users")
                .child(userType == User.DRIVER ? "Drivers" : "Riders");
        profilePicStorageReference = FirebaseStorage.getInstance().getReference().child("Profile Pictures");

        retrieveAndShowUserInfo();
    }

    private void retrieveAndShowUserInfo() {
        userInfoReference.child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    nameEditText.setText(name);
                    phoneEditText.setText(phone);

                    if (userType == User.DRIVER) {
                        String carNumber = dataSnapshot.child("car number").getValue().toString();
                        driverCarNumber.setText(carNumber);
                    }

                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: ", databaseError.toException());
            }
        });
    }

    private HashMap<String, Object> getUserInputs() {
        HashMap<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", nameEditText.getText().toString());
        userInfo.put("phone", phoneEditText.getText().toString());
        if (userType == User.DRIVER)
            userInfo.put("car number", driverCarNumber.getText().toString());

        return userInfo;
    }

    private void uploadProfilePictureAndSave() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait, while we are settings your account information");
        progressDialog.show();

        final StorageReference fileRef = profilePicStorageReference.child(userID  +  ".jpg");
        UploadTask uploadTask = fileRef.putFile(imageUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful())
                throw task.getException();

            // Continue with the task to get the download URL
            return fileRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUrl = task.getResult();
                profilePicDownloadUrl = downloadUrl.toString();

                HashMap<String, Object> userInfo = getUserInputs();
                userInfo.put("image", profilePicDownloadUrl);

                userInfoReference.child(userID).updateChildren(userInfo);
                progressDialog.dismiss();
            } else {
                // Handle failures
                Log.e(TAG, "onComplete: uploadTask Failed", task.getException());
            }
            finish();
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if(data == null) {
                    Log.e(TAG, "onActivityResult: RESULT_OK but data == null");
                    return;
                }
                // at this point, data != null. so, result != null also
                imageUri = result.getUri();
                profileImageView.setImageURI(imageUri);
            } else if (resultCode == RESULT_CANCELED)
                Log.e(TAG, "onActivityResult: RESULT_CANCELLED");
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
                Log.e(TAG, "onActivityResult: ", result.getError());
        }
    }

    private boolean areValidUserInputs() {
        if (TextUtils.isEmpty(nameEditText.getText().toString())) {
            Toast.makeText(this, "Please provide your name.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this, "Please provide your phone number.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (userType == User.DRIVER  &&  TextUtils.isEmpty(driverCarNumber.getText().toString())) {
            Toast.makeText(this, "Please provide your car number.", Toast.LENGTH_SHORT).show();
            return false;
        } else return true;
    }
}