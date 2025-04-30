package com.btssio.applicationrftg;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.btssio.applicationrftg.ui.theme.Film;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request ;
import okhttp3.RequestBody;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.HttpUrl;
import com.btssio.applicationrftg.PanierActivity;

import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;



public class AfficherFilmDetailActivity extends AppCompatActivity {

    private TextView filmTitle;
    private TextView filmReleaseYear;
    private TextView filmDescription;
    private Button btnReserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficher_film_detail);

        // Initialiser les vues
        filmTitle = findViewById(R.id.filmTitle);
        filmReleaseYear = findViewById(R.id.filmReleaseYear);
        filmDescription = findViewById(R.id.filmDescription);
        btnReserver = findViewById(R.id.btnReserver);

        // Récupérer les données du film depuis l'Intent
        int filmId = getIntent().getIntExtra("filmId", -1);
        Log.d("DEBUG", "Film ID récupéré : " + filmId);
        String title = getIntent().getStringExtra("title");
        int releaseYear = getIntent().getIntExtra("releaseYear", -1);
        String description = getIntent().getStringExtra("description");
        int inventoryId = getIntent().getIntExtra("inventoryId", -1);


        // Vérifier si les données sont valides
        if (title != null && releaseYear != -1) {
            filmTitle.setText(title);
            filmReleaseYear.setText("Année : " + releaseYear);
            filmDescription.setText(description);
        } else {
            Toast.makeText(this, "Erreur : Données du film manquantes", Toast.LENGTH_SHORT).show();
            finish(); // Ferme l'activité si les données sont invalides
            return;
        }

        // Ajouter un écouteur de clic au bouton Réserver
        btnReserver.setOnClickListener(v -> ajouterAuPanier(filmId, title, releaseYear, inventoryId));
        Button btnRetourListe = findViewById(R.id.btnRetourListe);
        btnRetourListe.setOnClickListener(v -> {
            finish(); // Ferme cette activité et revient à la liste
        });

    }

    /**
     * Ajoute un film au panier et le sauvegarde dans SharedPreferences.
     */
    private void ajouterAuPanier(int filmId, String filmTitle, int releaseYear, int inventoryId) {
        SharedPreferences sharedPreferences = getSharedPreferences("PanierPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();

        String jsonPanier = sharedPreferences.getString("panier", "[]");
        List<Map<String, String>> panier = gson.fromJson(jsonPanier, new TypeToken<List<Map<String, String>>>() {}.getType());

        if (panier == null) {
            panier = new ArrayList<>();
        }

        // Vérifie si le film existe déjà
        for (Map<String, String> film : panier) {
            if (film.get("title").equalsIgnoreCase(filmTitle) && Integer.parseInt(film.get("releaseYear")) == releaseYear) {
                Toast.makeText(this, "Ce film est déjà dans le panier", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Ajouter film avec son ID
        Map<String, String> nouveauFilm = new HashMap<>();
        nouveauFilm.put("filmId", String.valueOf(filmId));
        nouveauFilm.put("title", filmTitle);
        nouveauFilm.put("releaseYear", String.valueOf(releaseYear));
        //nouveauFilm.put("inventoryId", String.valueOf(inventoryId));

        panier.add(nouveauFilm);

        editor.putString("panier", gson.toJson(panier));
        editor.apply();

        Toast.makeText(this, "Film ajouté au panier !", Toast.LENGTH_SHORT).show();
        Log.d("DEBUG", "Film ajouté : " + filmTitle + " (" + releaseYear + ")");

        Intent intent = new Intent(AfficherFilmDetailActivity.this, PanierActivity.class);
        intent.putExtra("filmId", filmId);
        startActivity(intent);
    }






}
