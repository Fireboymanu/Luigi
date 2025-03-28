package com.btssio.applicationrftg;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.btssio.applicationrftg.ui.theme.Film;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class PanierActivity extends AppCompatActivity {

    private ListView panierListView;
    private Button btnFinaliser;
    private List<Film> panier; // Liste des films dans le panier

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        panierListView = findViewById(R.id.listViewPanier);
        btnFinaliser = findViewById(R.id.btnFinaliser);

        // Charger le panier depuis SharedPreferences
        panier = loadPanier();

        // Afficher les films du panier
        afficherPanier();

        // Bouton pour finaliser la réservation
        btnFinaliser.setOnClickListener(v -> {
            Log.d("API Request", "Bouton 'Finaliser' cliqué !");

            // Vérifie que le panier n'est pas vide
            if (panier.isEmpty()) {
                Toast.makeText(this, "Votre panier est vide !", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simule des données d'exemple (à remplacer par des vraies valeurs de ton application)
            for (Film film : panier) {
                envoyerFilmDansServer(
                        "2024-03-21",  // rentalDate (à remplacer par la vraie date)
                        film.getInventoryId(),  // inventoryId (supposons que Film a un ID d'inventaire)
                        5,  // customerId (remplace par le vrai ID client)
                        "2024-03-28",  // returnDate (optionnel)
                        2,  // staffId (ID du staff qui gère la location)
                        "2024-03-21"   // lastUpdate (date de mise à jour)
                );
            }

            Toast.makeText(this, "Réservation confirmée !", Toast.LENGTH_SHORT).show();
            clearPanier(); // Vider le panier après réservation
            finish(); // Retour à l'écran précédent
        });

    }

    // Charger le panier depuis SharedPreferences
    private List<Film> loadPanier() {
        SharedPreferences prefs = getSharedPreferences("PanierPrefs", MODE_PRIVATE);
        String jsonPanier = prefs.getString("panier", "[]");
        return new Gson().fromJson(jsonPanier, new TypeToken<List<Film>>() {}.getType());
    }

    // Afficher les films dans la ListView
    private void afficherPanier() {
        List<Map<String, String>> data = new ArrayList<>();
        for (Film film : panier) {
            Map<String, String> map = new HashMap<>();
            map.put("title", film.getTitle());
            map.put("releaseYear", String.valueOf(film.getReleaseYear()));
            data.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(
                this, data, android.R.layout.simple_list_item_2,
                new String[]{"title", "releaseYear"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        panierListView.setAdapter(adapter);
    }



    public void envoyerFilmDansServer(String rentalDate, int inventoryId, int customerId, String returnDate, int staffId, String lastUpdate) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8080/toad/rental/add";

        // Construction du body de la requête
        RequestBody requestBody = new FormBody.Builder()
                .add("rental_date", rentalDate)
                .add("inventory_id", String.valueOf(inventoryId))
                .add("customer_id", String.valueOf(customerId))
                .add("return_date", returnDate)
                .add("staff_id", String.valueOf(staffId))
                .add("last_update", lastUpdate)
                .build();

        // Construction de la requête
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try {
            Log.d("API Request", "Envoi de la requête POST à l'API...");


            // Appel réseau synchrone
            Response response = client.newCall(request).execute();

            // Log du code HTTP de la réponse
            Log.d("API Request", "Code de réponse: " + response.code());  // Log du code HTTP

            // Vérifier si la requête a réussi (code HTTP 200)
            if (response.isSuccessful()) {
                // Si la requête a réussi
                String responseBody = response.body().string(); // Obtenir le contenu de la réponse
                Log.d("API Request", "Réponse du serveur: " + responseBody);  // Log du corps de la réponse

                // Vous pouvez ajouter des conditions spécifiques pour vérifier des informations dans la réponse
                if (responseBody.contains("Film enregistré avec succès")) {  // Exemple de vérification dans le corps de la réponse
                    runOnUiThread(() -> Toast.makeText(PanierActivity.this, "Film enregistré avec succès", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(PanierActivity.this, "Réponse inattendue: " + responseBody, Toast.LENGTH_SHORT).show());
                }
            } else {
                // Si la requête n'a pas réussi, afficher un message d'erreur avec le code de statut
                String errorMessage = "Erreur lors de l'envoi. Code HTTP: " + response.code();
                Log.e("API Request", errorMessage);  // Log de l'erreur avec code HTTP
                runOnUiThread(() -> Toast.makeText(PanierActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("API Request", "Erreur de connexion: " + e.getMessage());  // Log de l'exception
            // Si une exception se produit (problème de connexion par exemple)
            runOnUiThread(() -> Toast.makeText(PanierActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
        }
    }



    // Vider le panier après la réservation
    private void clearPanier() {
        SharedPreferences prefs = getSharedPreferences("PanierPrefs", MODE_PRIVATE);
        prefs.edit().remove("panier").apply();
    }
}
