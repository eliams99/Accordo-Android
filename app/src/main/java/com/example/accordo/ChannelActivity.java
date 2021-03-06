package com.example.accordo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.stfalcon.imageviewer.StfalconImageViewer;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;

public class ChannelActivity extends AppCompatActivity implements OnPostRecyclerViewClickListener {
    private final String TAG = ChannelActivity.class.toString();
    private CommunicationController cc;
    private String ctitle;
    private SwipeRefreshLayout postsSwipeRefreshLayout;
    private static final int ACTION_REQUEST_GALLERY = 1;
    private PostAdapter adapter;
    private RecyclerView rv;
    private ArrayList<Bitmap> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        // Prende l'indice del'elemento della RecyclerView dei canali della WallActivity che è stato cliccato
        Intent intent = getIntent();
        ctitle = intent.getStringExtra("ctitle");

        // Setta la toolbar (titolo in alto della Activity, tasti back e refresh)
        setToolbar();

        // Gestore evento di click sul bottone di invio del post
        findViewById(R.id.sendButton).setOnClickListener(v -> {
            try {
                addPost();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        // Gestore evento di swipe to refresh per aggiornare i post
        postsSwipeRefreshLayout = findViewById(R.id.postsSwiperefresh);
        postsSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Utils.getThemeAttr(R.attr.colorPost, this));
        postsSwipeRefreshLayout.setColorSchemeColors(Utils.getThemeAttr(R.attr.colorPrimary, this));
        postsSwipeRefreshLayout.setOnRefreshListener(
                () -> getPosts(false)
        );

        // Gestore evento di click sul bottone "Allega" per mostrare il popUp con la sceltra tra immagine e posizione
        Button attachButton = findViewById(R.id.attachButton);
        attachButton.setOnClickListener(v -> {
            PopupAttach popupAttach = new PopupAttach();
            popupAttach.showPopupWindow(v, findViewById(R.id.sendButton), this);
        });
    }

    private void setToolbar() {
        androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.channelToolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(ctitle);             // Setta il titolo della toolbar con il nome del canale
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPosts(false);
    }

    /**
     * Fa la richiesta di rete del {@link CommunicationController} per ottenere i post. Nella
     * callback chiama {@link #setRecyclerView()} e {@link #getPictures()}
     * @param keepBottom Indica se la recycler view non deve fare l'animazione che scorre verso il basso
     */
    private void getPosts(boolean keepBottom) {
        cc = new CommunicationController(this);
        try {
            cc.getChannel(ctitle,
                    response -> {
                        try {
                            Model.getInstance(this).addPosts(response);     // Setta le informazioni dei post e il contenuto dei post testo
                            setRecyclerView();
                            if (!keepBottom) {
                                scrollDownRecyclerView();
                            } else {
                                rv.scrollToPosition(0);
                            }
                            getPictures();                              // Setta le immagini profilo degli utenti
                            postsSwipeRefreshLayout.setRefreshing(false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> Log.e(TAG, "request error: " + error.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chiama il metodo {@link PictureController#setProfilePictures(Runnable)} e {@link PictureController#setPostImages(Runnable)}.
     * Definisce le callback quando vengono ottenute le immagini
     */
    private void getPictures() {
        PictureController pc = new PictureController(this);
        pc.setProfilePictures(() -> adapter.notifyData());
        pc.setPostImages(() -> {
            adapter.notifyData();
            // Aggiunge a images le immagini ricevute per poi mostrarle nel carousel
            images = new ArrayList<>();
            for (TextImagePost imagePost : Model.getInstance(this).getAllImagePosts()) {
                if (imagePost.getContent() != null) {
                    images.add(Utils.getBitmapFromBase64(imagePost.getContent(), this));
                }
            }
            Collections.reverse(images);
        });
    }

    /**
     * Fa la richiesta di rete del {@link CommunicationController} per aggiungere un post di tipo
     * testo, prendendolo dalla EditText
     * @throws JSONException
     */
    private void addPost() throws JSONException {
        String postText = ((EditText)findViewById(R.id.postEditText)).getText().toString();
        cc = new CommunicationController(this);
        cc.addPost(ctitle, postText, Post.TEXT,
                response -> {
                    ((EditText)findViewById(R.id.postEditText)).setText("");
                    getPosts(true);
                },
                error -> {
                    Log.e(TAG, "Errore aggiunta post: " + error);
                    Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.sendButton), R.string.error_sending_text_post, Snackbar.LENGTH_LONG);
                    snackbar.setAnchorView(findViewById(R.id.sendButton))
                            .show();
                });
    }

    /**
     * Setta {@link #rv} impostando il layout in modo che sia inverso (post più recente in basso)
     * e scorrere automaticamente {@link #rv} in basso
     */
    private void setRecyclerView() {
        rv = findViewById(R.id.postsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rv.setLayoutManager(linearLayoutManager);
        adapter = new PostAdapter(this, this);
        rv.setAdapter(adapter);
    }

    /**
     * Scorre in basso la RecyclerView con animazione
     */
    private void scrollDownRecyclerView() {
        rv.scrollToPosition(16);            // Torna su di colpo (senza animazione)
        rv.smoothScrollToPosition(0);       // Poi gli ultimi 16 elementi della rv li fa con animazione
        rv.scheduleLayoutAnimation();
    }

    /**
     * Gestore dell'evento di click sull'immagine di un post di tipo immagine.
     * Mostra l'immagine a schermo intero tramite {@link StfalconImageViewer.Builder} e, scorrendp,
     * le immagini degli altri post, salvate in {@link #images}
     * @param v {@link ImageView} che è stata cliccata
     * @param position Posizione dell'elemento cliccato in {@link #rv}
     */
    @Override
    public void onRecyclerViewImageClick(View v, int position) {
        ImageView contentImageView = (ImageView) v;
        int imagePosition = Utils.getImagePositionInPosts(position, Model.getInstance(this).getAllPosts());
        new StfalconImageViewer.Builder<>(this, images, (imageView, image) -> Glide.with(this)
                .load(image)
                .into(imageView)).withStartPosition(imagePosition).withTransitionFrom(contentImageView).show();
    }

    /**
     * Gestore dell'evento di click sulla posizione di un posti di tipo posizione. Fa partire
     * {@link SendLocationActivity} passandole la posizione del post in {@link #rv}
     * @param v {@link android.widget.LinearLayout} della posizione che è stato cliccato
     * @param position Posizione dell'elemento cliccato in {@link #rv}
     */
    @Override
    public void onRecyclerViewLocationClick(View v, int position) {
        Intent intent = new Intent(ChannelActivity.this, SendLocationActivity.class);
        intent.putExtra("postIndex", position);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    /**
     * Gestore evento di click su bottone della {@link android.widget.Toolbar} (refresh)
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            postsSwipeRefreshLayout.setRefreshing(true);
            getPosts(false);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Gestore evento di lick su tipo allegato (immagine o posizione) in {@link PopupAttach}.
     * Crea la relativa activity: file manager o {@link SendLocationActivity}
     * @param type Tipo di allegato: "i" immagine, "l" posizione
     */
    public void onAttachClick(String type) {
        if (type.equals(Post.IMAGE)) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Scegli immagine"), 1);
        } else if (type.equals(Post.LOCATION)){
            Intent intent = new Intent(ChannelActivity.this, SendLocationActivity.class);
            intent.putExtra("ctitle", ctitle);
            startActivity(intent);
        }
    }

    /**
     * Chiamata quando viene selezionata l'immagine da inviare nel file manager. Apre
     * {@link SendImageActivity} per inviare l'immagine
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED && resultCode == RESULT_OK && requestCode == ACTION_REQUEST_GALLERY) {
            Intent intent = new Intent(ChannelActivity.this, SendImageActivity.class);
            intent.putExtra("imagePath", data.getData());
            intent.putExtra("ctitle", ctitle);
            startActivity(intent);
        }
    }
}
