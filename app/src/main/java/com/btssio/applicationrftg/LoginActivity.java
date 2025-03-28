package com.btssio.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailInput);
        passwordEditText = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        errorText = findViewById(R.id.errorText);

        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Lancer la requête de vérification
        checkLoginCredentials(email, password);
    }

    private void checkLoginCredentials(String email, String password) {
        // Lancer un thread pour effectuer la requête réseau en arrière-plan
        new Thread(() -> {
            try {
                // Créer l'URL avec l'email fourni
                String urlString = "http://10.0.2.2:8080/toad/customer/getByEmail?email=" + email;
                URL url = new URL(urlString);



                // Ouvrir une connexion HTTP
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000); // Timeout de connexion
                urlConnection.setReadTimeout(5000);    // Timeout de lecture

                // Lire la réponse du serveur
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Si la réponse est reçue avec succès
                String responseData = response.toString();
                Log.d("API_RESPONSE", "Réponse reçue : " + responseData);

                // Vous devez analyser le JSON pour obtenir un client (pas une liste de clients)
                Customer customer = parseCustomerFromJson(responseData);

                // Appeler la méthode pour valider les informations de connexion
                boolean isValid = validateLogin(email, password, customer);

                // Afficher un message à l'utilisateur en fonction du résultat
                runOnUiThread(() -> {
                    if (isValid) {
                        Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, AfficherListeDvdsActivity.class);
                        startActivity(intent);
                        finish(); // Ferme l'activité de connexion
                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                });


            } catch (IOException e) {
                runOnUiThread(() -> {
                    errorText.setText("Failed to connect to server."); // Éviter un texte vide
                    errorText.setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                });

            }
        }).start();
    }

    // Méthode de validation du login
    private boolean validateLogin(String email, String password, Customer customer) {
        if (customer != null && customer.getEmail().equals(email)) {
            return customer.getPassword().equals(password);
        }
        return false;
    }

    // Exemple de méthode pour parser le client depuis un JSON (vous devez l'implémenter)
    private Customer parseCustomerFromJson(String json) {

        try {
            Gson gson = new Gson();
            return gson.fromJson(json, Customer.class); // Convertit le JSON en objet Customer
        } catch (JsonSyntaxException e) {
            Log.e("JSON_PARSING", "Erreur de parsing JSON : " + e.getMessage());
            return null;
        }
    }
}
