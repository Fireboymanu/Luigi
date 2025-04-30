package com.btssio.applicationrftg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.btssio.applicationrftg.ui.theme.Film;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class AfficherListeDvdsActivity extends AppCompatActivity {

    private SimpleCursorAdapter adapter;
    private ListView listView;
    private MatrixCursor dvdCursor;
    private Button btnDeconnexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG", ">>> onCreate() appelé dans AfficherListeDvdsActivity");
        setContentView(R.layout.activity_afficherlistedvds);

        listView = findViewById(R.id.listView);

        String[] columns = new String[]{"_id", "title", "releaseYear"};
        dvdCursor = new MatrixCursor(columns);

        String[] from = new String[]{"_id", "title", "releaseYear"};
        int[] to = new int[]{R.id.filmName, R.id.filmDate};
        adapter = new SimpleCursorAdapter(this, R.layout.liste_item, dvdCursor, from, to, 0);

        ListView listviewDvds = findViewById(R.id.listView);
        listviewDvds.setAdapter(adapter);
        listviewDvds.setTextFilterEnabled(true);

        // Lancer la tâche asynchrone pour récupérer les films
        Log.d("DEBUG", "Lancement de FetchFilmsTask");
        new FetchFilmsTask().execute(DonneesPartagees.getURLConnexion() + "/toad/film/all");


        // Ajouter un listener pour les clics sur les films
        listviewDvds.setOnItemClickListener((parent, view, position, id) -> {
            // Récupérer le curseur de l'élément sélectionné
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);

            // Vérifier les indices des colonnes avant de les utiliser
            int filmIdIndex = cursor.getColumnIndex("_id");
            int titleColumnIndex = cursor.getColumnIndex("title");
            int releaseYearColumnIndex = cursor.getColumnIndex("releaseYear");

            Log.d("DEBUG", "filmIdIndex: " + filmIdIndex);
            Log.d("DEBUG", "titleColumnIndex: " + titleColumnIndex);
            Log.d("DEBUG", "releaseYearColumnIndex: " + releaseYearColumnIndex);

            // Assurez-vous que les indices sont valides
            if (titleColumnIndex != -1 && releaseYearColumnIndex != -1) {
                // Si les indices sont valides, on récupère les valeurs
                int filmId = cursor.getInt(filmIdIndex);
                String title = cursor.getString(titleColumnIndex);
                int releaseYear = cursor.getInt(releaseYearColumnIndex);
                String description = "Description du film"; // Ajoute une description si disponible

                Log.d("DEBUG", "Film ID (récupéré du cursor) : " + cursor.getInt(filmIdIndex));
                Log.d("DEBUG", "Film Title (récupéré du cursor) : " + cursor.getString(titleColumnIndex));
                Log.d("DEBUG", "Film Release Year (récupéré du cursor) : " + cursor.getInt(releaseYearColumnIndex));


                // Créer un intent pour l'activité de détail
                Intent intent = new Intent(AfficherListeDvdsActivity.this, AfficherFilmDetailActivity.class);
                intent.putExtra("filmId", filmId);
                intent.putExtra("title", title);
                intent.putExtra("releaseYear", releaseYear);
                intent.putExtra("description", description);

                // Lancer l'activité de détail
                startActivity(intent);
            } else {
                Log.e("ERROR", "Les indices des colonnes sont invalides !");
            }
        });

        Button btnVoirPanier = findViewById(R.id.btnVoirPanier);
        btnVoirPanier.setOnClickListener(v -> {
            // Lancer l'activité PanierActivity
            Intent intent = new Intent(AfficherListeDvdsActivity.this, PanierActivity.class);
            startActivity(intent);
        });

        btnDeconnexion = findViewById(R.id.btnDeconnexion);

        btnDeconnexion.setOnClickListener(v -> {
            deconnexion();
        });


    }
    private void deconnexion() {
        // Supprimer les infos de connexion enregistrées
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Rediriger vers l'écran de login
        Intent intent = new Intent(AfficherListeDvdsActivity.this, LoginActivity.class); // ou ton activité de connexion
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // empêche de revenir avec le bouton retour
        startActivity(intent);
        finish();
    }



    // AsyncTask pour récupérer les films
    private class FetchFilmsTask extends AsyncTask<String, Void, List<Film>> {

        @Override
        protected List<Film> doInBackground(String... urls) {


            String apiUrl = urls[0];
            Log.d("DEBUG", "Appel API à l'URL : " + apiUrl);

            String response = fetchFilmsFromApi(apiUrl);

            // Utiliser Gson pour convertir la réponse JSON en une liste de films
            Gson gson = new Gson();
            TypeToken<List<Film>> token = new TypeToken<List<Film>>() {
            };
            return gson.fromJson(response, token.getType());

        }

        @Override
        protected void onPostExecute(List<Film> films) {
            super.onPostExecute(films);

            // Ajouter les films au MatrixCursor
            if (films != null) {
                dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear"}); // Reset

                Log.d("DEBUG", "Taille de la liste films : " + films.size());

                for (Film film : films) {
                    Log.d("DEBUG", "Ajout du film : " + film.getId() + ", " + film.getTitle() + " (" + film.getReleaseYear() + ")");
                    dvdCursor.addRow(new Object[]{film.getId(), film.getTitle(), film.getReleaseYear()});
                }

                adapter.changeCursor(dvdCursor); // Mise à jour du curseur
                adapter.notifyDataSetChanged();
            } else {
                Log.e("ERROR", "Aucun film récupéré !");
            }
        }


        // Méthode pour appeler l'API et récupérer les données
        private String fetchFilmsFromApi(String apiUrl) {
            HttpURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(apiUrl);
                Log.d("DEBUG", "Tentative de connexion à l'URL : " + apiUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                int responseCode = urlConnection.getResponseCode();
                Log.d("DEBUG", "Code réponse HTTP : " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                } else {
                    Log.e("API_ERROR", "Erreur HTTP : " + responseCode);
                    InputStream errorStream = urlConnection.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorMsg = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorMsg.append(line);
                        }
                        errorReader.close();
                        Log.e("API_ERROR", "Message d'erreur serveur : " + errorMsg.toString());
                    }
                }

            } catch (Exception e) {
                Log.e("API_ERROR", "Erreur lors de la connexion ou lecture : " + e.toString());
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            Log.d("DEBUG", "Contenu réponse brute : " + result.toString());
            return result.toString();
        }

    }

    }
