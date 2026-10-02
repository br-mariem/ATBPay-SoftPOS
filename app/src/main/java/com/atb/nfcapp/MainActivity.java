package com.atb.nfcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private TextView tvForgotPinLogin;

    private static final int REQUEST_SMS_RESET = 4001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPinLogin = findViewById(R.id.tvForgotPinLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                procederConnexion();
            }
        });

        if (tvForgotPinLogin != null) {
            tvForgotPinLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showForgotPinOptionsDialog();
                }
            });
        }
    }

    private void showForgotPinOptionsDialog() {
        String[] options = {"Code de récupération (local)", "Code envoyé par SMS"};
        new AlertDialog.Builder(this)
                .setTitle("PIN oublié")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRecoveryCodeInputDialogFromLogin();
                    } else {
                        launchSmsResetFromLogin();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showRecoveryCodeInputDialogFromLogin() {
        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        String storedHash = prefs.getString("RECOVERY_CODE_HASH", null);
        String storedSalt = prefs.getString("RECOVERY_CODE_SALT", null);

        if (storedHash == null || storedSalt == null) {
            Toast.makeText(this, "Aucun code de récupération n'a été configuré. "
                    + "Utilisez la vérification par SMS, ou contactez votre support.", Toast.LENGTH_LONG).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setHint("XXXX-XXXX");

        new AlertDialog.Builder(this)
                .setTitle("Code de récupération")
                .setMessage("Saisissez votre code de récupération pour réinitialiser votre PIN.")
                .setView(input)
                .setPositiveButton("Valider", (dialog, which) -> {
                    String entered = PinSecurity.normalizeRecoveryCode(input.getText().toString());
                    String h = prefs.getString("RECOVERY_CODE_HASH", null);
                    String s = prefs.getString("RECOVERY_CODE_SALT", null);

                    if (h != null && s != null && PinSecurity.hash(entered, s).equals(h)) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("RECOVERY_CODE_HASH");
                        editor.remove("RECOVERY_CODE_SALT");
                        editor.apply();

                        showDefineNewPinDialog(prefs);
                    } else {
                        Toast.makeText(this, "Code de récupération incorrect.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void launchSmsResetFromLogin() {
        Intent intent = new Intent(MainActivity.this, SmsCodeActivity.class);
        intent.putExtra("MODE", "PIN_RESET");
        startActivityForResult(intent, REQUEST_SMS_RESET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SMS_RESET && resultCode == RESULT_OK) {
            SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
            showDefineNewPinDialog(prefs);
        }
    }

    private void showDefineNewPinDialog(final SharedPreferences prefs) {
        androidx.appcompat.widget.LinearLayoutCompat container = new androidx.appcompat.widget.LinearLayoutCompat(this);
        container.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        final EditText newPinInput = new EditText(this);
        newPinInput.setHint("Nouveau PIN (4 chiffres)");
        newPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        final EditText confirmPinInput = new EditText(this);
        confirmPinInput.setHint("Confirmer le PIN");
        confirmPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        container.addView(newPinInput);
        container.addView(confirmPinInput);

        new AlertDialog.Builder(this)
                .setTitle("Nouveau code confidentiel")
                .setMessage("Identité confirmée. Définissez votre nouveau PIN de connexion.")
                .setView(container)
                .setCancelable(false)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newPin = newPinInput.getText().toString().trim();
                    String confirmPin = confirmPinInput.getText().toString().trim();

                    if (newPin.length() != 4 || !TextUtils.isDigitsOnly(newPin)) {
                        Toast.makeText(this, "Le PIN doit contenir exactement 4 chiffres.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPin.equals(confirmPin)) {
                        Toast.makeText(this, "Les deux codes ne correspondent pas.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newSalt = PinSecurity.generateSalt();
                    String newHash = PinSecurity.hash(newPin, newSalt);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("LOGIN_PASSWORD_HASH", newHash);
                    editor.putString("LOGIN_PASSWORD_SALT", newSalt);
                    editor.remove("LOGIN_PASSWORD");
                    editor.apply();

                    Toast.makeText(this, "PIN réinitialisé avec succès. Vous pouvez vous reconnecter.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void procederConnexion() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Veuillez saisir votre identifiant");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Veuillez saisir votre code PIN");
            etPassword.requestFocus();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        String storedHash = prefs.getString("LOGIN_PASSWORD_HASH", null);
        String storedSalt = prefs.getString("LOGIN_PASSWORD_SALT", null);

        boolean passwordCorrect;
        if (storedHash == null || storedSalt == null) {
            passwordCorrect = password.equals("1234");
            if (passwordCorrect) {
                String newSalt = PinSecurity.generateSalt();
                String newHash = PinSecurity.hash(password, newSalt);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("LOGIN_PASSWORD_HASH", newHash);
                editor.putString("LOGIN_PASSWORD_SALT", newSalt);
                editor.apply();
            }
        } else {
            String enteredHash = PinSecurity.hash(password, storedSalt);
            passwordCorrect = enteredHash.equals(storedHash);
        }

        if (!passwordCorrect) {
            etPassword.setError("Code PIN de connexion incorrect !");
            etPassword.requestFocus();
            Toast.makeText(this, "Code PIN incorrect !", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Connexion réussie chez ATB Pay !", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        intent.putExtra("USER_NAME", "Marwen Ben Ali");
        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}