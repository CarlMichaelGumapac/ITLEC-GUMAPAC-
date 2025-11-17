package com.example.xmlresourceslab;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.button);
        Log.i("DEBUG_TAG", "MainActivity started successfully");
        Log.d("DEBUG_TAG", "Debugging message");
        Log.w("DEBUG_TAG", "Warning message");
        Log.e("DEBUG_TAG", "Error message");
    }
}