package com.btssio.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.btssio.applicationrftg.ui.theme.Film;
import java.util.List;
import android.database.MatrixCursor;
import android.widget.SimpleCursorAdapter;


public class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<URL, Integer, String > {
    private volatile AfficherListeDvdsActivity screen; // reference to the screen activity
    private MatrixCursor dvdCursor;
    private SimpleCursorAdapter adapter;

    public AppelerServiceRestGETAfficherListeDvdsTask(AfficherListeDvdsActivity s) {
        this.screen = s;
    }

    @Override
    protected void onPreExecute() {
        // Optional: Display a loading indicator or perform pre-processing
    }

    @Override
    protected String doInBackground(URL... urls) {
            // Utiliser l'URL de l'API pour récupérer les films dans la BDD:)
            String apiUrl = DonneesPartagees.getURLConnexion() + "/toad/film/all";
            try {
                URL url = new URL(apiUrl);
                return appelerServiceRestHttp(url);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Film>>(){}.getType();
                List<Film> films = gson.fromJson(result, listType);

                // Ajouter les films dans le MatrixCursor
                for (Film film : films) {
                    dvdCursor.addRow(new Object[]{film.getId(), film.getTitle(), film.getReleaseYear()});
                }

                // Notifier l'adaptateur que les données ont changé
                adapter.notifyDataSetChanged();

            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
    }


    private String appelerServiceRestHttp(URL urlAAppeler) {
        HttpURLConnection urlConnection = null;
        StringBuilder sResultatAppel = new StringBuilder();

        try {
            urlConnection = (HttpURLConnection) urlAAppeler.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", System.getProperty("http.agent"));

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // Only proceed if response code is 200 (OK)
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                int codeCaractere;
                while ((codeCaractere = in.read()) != -1) {
                    sResultatAppel.append((char) codeCaractere);
                }
                in.close();
            } else {
                Log.d("mydebug", ">>>Received non-OK response code: " + responseCode);
            }
        } catch (IOException ioe) {
            Log.d("mydebug", ">>>Pour appelerServiceRestHttp - IOException ioe = " + ioe.toString());
        } catch (Exception e) {
            Log.d("mydebug", ">>>Pour appelerServiceRestHttp - Exception = " + e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return sResultatAppel.toString();
    }
}
