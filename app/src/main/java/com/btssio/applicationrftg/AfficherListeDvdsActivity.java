package com.btssio.applicationrftg;

import android.content.Intent;
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class AfficherListeDvdsActivity extends AppCompatActivity {

    private SimpleCursorAdapter adapter;
    private ListView listView;
    private MatrixCursor dvdCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficherlistedvds);

        listView = findViewById(R.id.listView);

        String[] columns = new String[]{"_id", "title", "releaseYear"};
        dvdCursor = new MatrixCursor(columns);

        String[] from = new String[]{"title", "releaseYear"};
        int[] to = new int[]{R.id.filmName, R.id.filmDate};
        adapter = new SimpleCursorAdapter(this, R.layout.liste_item, dvdCursor, from, to, 0);

        ListView listviewDvds = findViewById(R.id.listView);
        listviewDvds.setAdapter(adapter);
        listviewDvds.setTextFilterEnabled(true);

        // Lancer la tâche asynchrone pour récupérer les films
        new FetchFilmsTask().execute("http://10.0.2.2:8080/toad/film/all");

        // Ajouter un listener pour les clics sur les films
        listviewDvds.setOnItemClickListener((parent, view, position, id) -> {
            // Récupérer le curseur de l'élément sélectionné
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);

            // Vérifier les indices des colonnes avant de les utiliser
            int titleColumnIndex = cursor.getColumnIndex("title");
            int releaseYearColumnIndex = cursor.getColumnIndex("releaseYear");

            // Assurez-vous que les indices sont valides
            if (titleColumnIndex != -1 && releaseYearColumnIndex != -1) {
                // Si les indices sont valides, on récupère les valeurs
                String title = cursor.getString(titleColumnIndex);
                int releaseYear = cursor.getInt(releaseYearColumnIndex);
                String description = "Description du film"; // Ajoute une description si disponible

                // Créer un intent pour l'activité de détail
                Intent intent = new Intent(AfficherListeDvdsActivity.this, AfficherFilmDetailActivity.class);
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


    }


    // AsyncTask pour récupérer les films
    private class FetchFilmsTask extends AsyncTask<String, Void, List<Film>> {

        @Override
        protected List<Film> doInBackground(String... urls) {
            String apiUrl = urls[0];
            String response = fetchFilmsFromApi(apiUrl);

            // Utiliser Gson pour convertir la réponse JSON en une liste de films
            Gson gson = new Gson();
            TypeToken<List<Film>> token = new TypeToken<List<Film>>() {};
            return gson.fromJson(response, token.getType());
        }

        @Override
        protected void onPostExecute(List<Film> films) {
            super.onPostExecute(films);

            // Ajouter les films au MatrixCursor
            if (films != null) {
                dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear"}); // Reset

                for (Film film : films) {
                    Log.d("DEBUG", "Ajout du film : " + film.getTitle() + " (" + film.getReleaseYear() + ")");
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
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return result.toString();
        }
    }
}
