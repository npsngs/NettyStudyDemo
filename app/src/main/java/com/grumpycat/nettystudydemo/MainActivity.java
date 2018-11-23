package com.grumpycat.nettystudydemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acti_main);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_https_server:
                startActivity(new Intent(this, HttpsServerActi.class));
        }
    }
}
