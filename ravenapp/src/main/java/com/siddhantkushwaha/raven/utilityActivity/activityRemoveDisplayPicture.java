package com.siddhantkushwaha.raven.utilityActivity;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class activityRemoveDisplayPicture extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("remove_image", true);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
