package com.example.tojung;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    TextView chat;
    Button option_button;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private static final String MY_SECRET_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        option_button =(Button) findViewById(R.id.option_button); //옵션버튼
        option_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) { //옵션 버튼 클릭시 화면 이동
                Intent intent = new Intent(getApplicationContext(), option.class);
                startActivity(intent);
            }
        });
    }
}