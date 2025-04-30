package com.btssio.applicationrftg;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PanierActivity extends AppCompatActivity {

    private ListView panierListView;
    private Button btnFinaliser;
    private List<Map<String, String>> panier;
    private int filmId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        initializeUI();
        setupListeners();

        // Récupérer le filmId passé par AfficherFilmDetailActivity

        panier = loadPanier();
        afficherPanier();
    }

    private void initializeUI() {
        panierListView = findViewById(R.id.listViewPanier);
        btnFinaliser = findViewById(R.id.btnFinaliser);
        Button btnRetourListe = findViewById(R.id.btnRetourenarrière);
        btnRetourListe.setOnClickListener(v -> {
            finish(); // Ferme cette activité et revient à la liste
        });
    }

    private void supprimerFilmDuPanier() {
        SharedPreferences prefs = getSharedPreferences("PanierPrefs", MODE_PRIVATE);
        String panierJson = prefs.getString("panier", "[]");

        Gson gson = new Gson();
        TypeToken<List<Map<String, String>>> token = new TypeToken<List<Map<String, String>>>() {};
        List<Map<String, String>> panierList = gson.fromJson(panierJson, token.getType());

        if (!panierList.isEmpty()) {
            Map<String, String> filmSupprime = panierList.remove(panierList.size() - 1);
            String idSupprime = filmSupprime.get("filmId");
            prefs.edit().putString("panier", gson.toJson(panierList)).apply();
            Toast.makeText(this, "Film ID " + idSupprime + " supprimé du panier", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Panier vide", Toast.LENGTH_SHORT).show();
        }
    }



    private void setupListeners() {

        btnFinaliser.setOnClickListener(v -> finaliserReservation());
        Button btnSupprimer = findViewById(R.id.btnSupprimer);
        btnSupprimer.setOnClickListener(v -> {
            supprimerFilmDuPanier();
        });
    }





    private void finaliserReservation() {
        Log.d("API Request", "Bouton 'Finaliser' cliqué !");

        if (panier.isEmpty()) {
            Toast.makeText(this, "Votre panier est vide !", Toast.LENGTH_SHORT).show();
            return;
        }

        int customerId = getCustomerId();
        if (customerId == -1) {
            Toast.makeText(this, "Erreur : utilisateur non connecté", Toast.LENGTH_LONG).show();
            return;
        }

        int staffId = 2;
        String rentalDate = getCurrentDateTime();
        String returnDate = getReturnDate();

        for (Map<String, String> film : panier) {
            String filmId = film.get("filmId");
            getInventoryId(Volley.newRequestQueue(this), filmId, inventoryId -> {
                if (inventoryId != null) {
                    envoyerFilmDansServerHttpURLConnection(rentalDate, inventoryId, customerId, returnDate, staffId, rentalDate, filmId);
                    Log.d("API", "inventoryID :) : " +inventoryId);
                } else {
                    handleErrorInventoryId();
                }
            });
        }

        Toast.makeText(this, "Réservation confirmée !", Toast.LENGTH_SHORT).show();
        clearPanier();
        finish();
    }

    private int getCustomerId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getInt("customerId", -1);
    }

    private void handleErrorInventoryId() {
        Log.e("API_ERROR", "Erreur lors de la récupération de l'inventoryId");
        runOnUiThread(() -> Toast.makeText(PanierActivity.this, "Erreur lors de la récupération des données d'inventaire", Toast.LENGTH_SHORT).show());
    }

    private List<Map<String, String>> loadPanier() {
        SharedPreferences prefs = getSharedPreferences("PanierPrefs", MODE_PRIVATE);
        String jsonPanier = prefs.getString("panier", "[]");
        Log.d("API", "contenu jsonPanier 📖 : " + jsonPanier);
        return new Gson().fromJson(jsonPanier, new TypeToken<List<Map<String, String>>>() {}.getType());

    }

    private void afficherPanier() {
        SimpleAdapter adapter = new SimpleAdapter(
                this, panier, android.R.layout.simple_list_item_2,
                new String[]{"title", "releaseYear"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        panierListView.setAdapter(adapter);
    }

    private void envoyerFilmDansServerHttpURLConnection(String rentalDate, int inventoryId, int customerId, String returnDate, int staffId, String lastUpdate, String filmId) {
        Log.d("API", "Film ID : " + filmId);
        Log.d("DEBUG_PANIER", "Contenu panier : " + new Gson().toJson(panier));
        new Thread(() -> {
            try {
                String url = DonneesPartagees.getURLConnexion() + "/toad/rental/add";
                String postData = buildPostData(rentalDate, inventoryId, customerId, returnDate, staffId, lastUpdate);

                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                configureConnection(urlConnection, postData);

                String response = getServerResponse(urlConnection);
                handleServerResponse(urlConnection, response);

            } catch (IOException e) {
                handleRequestError(e);
            }
        }).start();
    }

    private String buildPostData(String rentalDate, int inventoryId, int customerId, String returnDate, int staffId, String lastUpdate) {
        return "rental_date=" + rentalDate +
                "&inventory_id=" + inventoryId +
                "&customer_id=" + customerId +
                "&return_date=" + returnDate +
                "&staff_id=" + staffId +
                "&last_update=" + lastUpdate +
                "&film_id=" + filmId;
    }

    private void configureConnection(HttpURLConnection urlConnection, String postData) throws IOException {
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setDoOutput(true);

        try (OutputStream os = urlConnection.getOutputStream()) {
            byte[] input = postData.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    private String getServerResponse(HttpURLConnection urlConnection) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private void handleServerResponse(HttpURLConnection urlConnection, String response) throws IOException {
        if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            runOnUiThread(() -> Toast.makeText(this, "Film ajouté à la location", Toast.LENGTH_SHORT).show());
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Erreur serveur : " + response, Toast.LENGTH_SHORT).show());
        }
    }

    private void handleRequestError(IOException e) {
        Log.e("HttpURLConnection", "Erreur de requête: " + e.getMessage());
        runOnUiThread(() -> Toast.makeText(this, "Erreur lors de l'envoi", Toast.LENGTH_SHORT).show());
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getReturnDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void clearPanier() {
        SharedPreferences prefs = getSharedPreferences("PanierPrefs", MODE_PRIVATE);
        prefs.edit().remove("panier").apply();
    }

    private void getInventoryId(RequestQueue queue, String filmId, final InventoryCallback callback) {
        if (filmId == null || filmId.trim().isEmpty()) {
            Log.e("API_CALL", "filmId est null ou vide, annulation de la requête");
            callback.onSuccess(null);
            return;
        }

        String url = DonneesPartagees.getURLConnexion() + "/toad/inventory/available/getById?id=" + filmId;
        Log.d("API_CALL", "Requête envoyée: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("API_RESPONSE", "Réponse de getInventoryId: " + response);
                    if (response != null && !response.trim().isEmpty()) {
                        try {
                            int inventoryId = Integer.parseInt(response.trim());
                            callback.onSuccess(inventoryId);
                        } catch (NumberFormatException e) {
                            Log.e("API_ERROR", "Erreur conversion int: " + e.getMessage());
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e("API_ERROR", "Réponse vide ou invalide reçue pour inventoryId");
                        callback.onSuccess(null);
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Erreur de requête: " + error.getMessage());
                    callback.onSuccess(null);
                });

        queue.add(request);
    }


    // Interface pour le callback
    public interface InventoryCallback {
        void onSuccess(Integer inventoryId);
    }
}
