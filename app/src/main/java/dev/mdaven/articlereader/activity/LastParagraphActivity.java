package dev.mdaven.articlereader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class LastParagraphActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String text = getIntent().getStringExtra(Intent.EXTRA_PROCESS_TEXT);
        if(text != null){
            MainActivity.setLastParagraphText(text);
            setResult(Activity.RESULT_OK, new Intent());
        }
        finish();
    }
}