package com.example.accordo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.example.accordo.Utils.getBase64FromBitmap;

public class SendImageActivity extends AppCompatActivity {
    private static final int ACTION_REQUEST_CAMERA = 0;
    private static final int ACTION_REQUEST_GALLERY = 1;
    private static final String TAG = SendImageActivity.class.toString();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_image);

        Uri imagePath = (Uri) getIntent().getParcelableExtra("imagePath");
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(imagePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);
        ImageView imageView = findViewById(R.id.pickedImageImageView);
        imageView.setImageBitmap(imageBitmap);

        String ctitle = getIntent().getStringExtra("ctitle");

        findViewById(R.id.sendImageButton).setOnClickListener(v -> {
            String base64Image = getBase64FromBitmap(imageBitmap);
            CommunicationController cc = new CommunicationController(this);
            try {
                cc.addPost(ctitle, base64Image, "i",
                        response -> {
                            super.onBackPressed();
                            /*Intent channelActivityIntent = new Intent(ImagePickActivity.this, ChannelActivity.class);
                            channelActivityIntent.putExtra("ctitle", ctitle);
                            startActivity(channelActivityIntent);*/
                        },
                        error -> {
                            Context context = getApplicationContext();
                            CharSequence text = "Errore invio immagine";
                            int duration = Toast.LENGTH_LONG;
                            Toast toast = Toast.makeText(context, text, duration);toast.show();
                            Log.e(TAG, "Errore invio immagine: " + error.networkResponse);
                            super.onBackPressed();
                        } );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }
}