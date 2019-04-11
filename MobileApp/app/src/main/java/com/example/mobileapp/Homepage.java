package com.example.mobileapp;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import org.w3c.dom.Text;

public class Homepage extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        setContentView(R.layout.homepage);
        String username = getIntent().getStringExtra("username");
        TextView uname = findViewById(R.id.TV_username);

        uname.setText(username);
    }
}
