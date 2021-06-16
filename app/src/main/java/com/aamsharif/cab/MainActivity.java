package com.aamsharif.cab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button driverButton;
    private Button riderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driverButton = findViewById(R.id.button_driver);
        riderButton = findViewById(R.id.button_rider);

        driverButton.setOnClickListener(view -> nextActivity(User.DRIVER));

        riderButton.setOnClickListener(view -> nextActivity(User.RIDER));
    }

    private void nextActivity(short type) {
        Intent loginRegister = new Intent(MainActivity.this, LoginRegisterActivity.class);
        loginRegister.putExtra(User.TYPE, type);
        startActivity(loginRegister);
    }
}