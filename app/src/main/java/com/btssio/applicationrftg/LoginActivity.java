package com.btssio.applicationrftg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText urlEditText; // Ajout de ce champ pour récupérer l'URL
    private Button loginButton;
    private TextView errorText;
    private Spinner spinnerURLs;

    private String[] listeURLs;

    //  Modification : Utilisation d'un `ExecutorService` pour exécuter les requêtes réseau en arrière-plan
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    //  Modification : `Handler` pour mettre à jour l'UI après une requête réseau
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisation des éléments UI
        emailEditText = findViewById(R.id.emailInput);
        passwordEditText = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        errorText = findViewById(R.id.errorText);
        urlEditText = findViewById(R.id.URLText); // Ajout pour récupérer l'URL manuellement
        spinnerURLs = findViewById(R.id.spinnerURLs);

        //  Modification : Initialisation du Spinner (pour choisir l'URL)
        listeURLs = getResources().getStringArray(R.array.listeURLs);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.listeURLs, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapter);
        spinnerURLs.setOnItemSelectedListener(this);

        //  Modification : Click listener propre (plus clair et plus efficace)
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String urlBase = urlEditText.getText().toString(); // Récupération de l'URL entrée

        //  Modification : Vérification des champs vides avant la requête
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        DonneesPartagees.setURLConnexion(urlBase); // Mise à jour de l'URL
        checkLoginCredentials(email, password);
    }

    private void checkLoginCredentials(String email, String password) {
        //  Modification : Utilisation de `ExecutorService` pour éviter `NetworkOnMainThreadException`
        executorService.execute(() -> {
            try {
                // Construction de l'URL
                String urlString = DonneesPartagees.getURLConnexion() + "/toad/customer/getByEmail?email=" + email;
                URL url = new URL(urlString);
                Log.d("DEBUG_URL", "URL envoyée : " + urlString);

                // Connexion HTTP
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                // Lecture de la réponse du serveur
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                //  Modification : Utilisation de `Gson` pour convertir JSON → Objet Java
                Customer customer = parseCustomerFromJson(response.toString());

                // Vérification des identifiants
                boolean isValid = validateLogin(email, password, customer);

                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("customerId", customer.getCustomerId());  // Stocke l'ID client
                editor.apply();


                //  Modification : Utilisation d'un `Handler` pour mettre à jour l'UI depuis le thread principal
                handler.post(() -> {
                    if (isValid) {
                        Toast.makeText(getApplicationContext(), "Connexion réussie !", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, AfficherListeDvdsActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                Log.e("HTTP_ERROR", "Erreur réseau : " + e.getMessage());
                handler.post(() -> {
                    Toast.makeText(LoginActivity.this, "Connexion au serveur impossible", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Gestion du spinner pour choisir l'URL
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        DonneesPartagees.setURLConnexion(listeURLs[position]);
        urlEditText.setText(listeURLs[position]); // 🔹 Modification : Met l'URL choisie dans l'input
        Toast.makeText(getApplicationContext(), "URL sélectionnée : " + listeURLs[position], Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // Vérification du login
    private boolean validateLogin(String email, String password, Customer customer) {
        return customer != null && customer.getEmail().equals(email) && customer.getPassword().equals(password);
    }

    //  Modification : Parsing JSON amélioré (évite les crashs si JSON incorrect)
    private Customer parseCustomerFromJson(String json) {
        try {
            return new Gson().fromJson(json, Customer.class);
        } catch (JsonSyntaxException e) {
            Log.e("JSON_ERROR", "Erreur parsing JSON : " + e.getMessage());
            return null;
        }
    }

    //  Modification : Fermeture propre du `ExecutorService` pour éviter des fuites mémoire
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
