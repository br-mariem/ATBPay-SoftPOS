package com.atb.nfcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ManageAccountActivity extends AppCompatActivity {

    private ImageButton btnBack, btnHeaderInfo;
    private TextView tvTitle;

    private LinearLayout layoutMyProfile;
    private ScrollView layoutChangePassword;
    private ScrollView layoutTerminalConfig;

    private TextView tvProfileGsm;
    private Button btnGoToChangePassword;
    private TextView tvRegenerateRecoveryCode;

    private EditText etOld1, etOld2, etOld3, etOld4;
    private EditText etNew1, etNew2, etNew3, etNew4;
    private EditText etConfirm1, etConfirm2, etConfirm3, etConfirm4;
    private Button btnSubmitChangePassword;
    private TextView tvForgotPin;
    private TextView tvUseRecoveryCode;

    private EditText etTerminalId, etMerchantId, etStoreId, etGsmNumber, etIpAddress, etPort;
    private Button btnValidate;

    private boolean isChangingPassword = false;
    private boolean showTerminalConfig = false;
    private boolean bypassOldPinCheck = false;
    private static final int REQUEST_PIN_RESET = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account);

        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        btnHeaderInfo = findViewById(R.id.btnHeaderInfo);

        layoutMyProfile = findViewById(R.id.layoutMyProfile);
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        layoutTerminalConfig = findViewById(R.id.layoutTerminalConfig);

        tvProfileGsm = findViewById(R.id.tvProfileGsm);
        btnGoToChangePassword = findViewById(R.id.btnGoToChangePassword);
        tvRegenerateRecoveryCode = findViewById(R.id.tvRegenerateRecoveryCode);

        etOld1 = findViewById(R.id.etOld1);
        etOld2 = findViewById(R.id.etOld2);
        etOld3 = findViewById(R.id.etOld3);
        etOld4 = findViewById(R.id.etOld4);

        etNew1 = findViewById(R.id.etNew1);
        etNew2 = findViewById(R.id.etNew2);
        etNew3 = findViewById(R.id.etNew3);
        etNew4 = findViewById(R.id.etNew4);

        etConfirm1 = findViewById(R.id.etConfirm1);
        etConfirm2 = findViewById(R.id.etConfirm2);
        etConfirm3 = findViewById(R.id.etConfirm3);
        etConfirm4 = findViewById(R.id.etConfirm4);

        btnSubmitChangePassword = findViewById(R.id.btnSubmitChangePassword);
        tvForgotPin = findViewById(R.id.tvForgotPin);
        tvUseRecoveryCode = findViewById(R.id.tvUseRecoveryCode);

        etTerminalId = findViewById(R.id.etTerminalId);
        etMerchantId = findViewById(R.id.etMerchantId);
        etStoreId = findViewById(R.id.etStoreId);
        etGsmNumber = findViewById(R.id.etGsmNumber);
        etIpAddress = findViewById(R.id.etIpAddress);
        etPort = findViewById(R.id.etPort);
        btnValidate = findViewById(R.id.btnValidate);

        showTerminalConfig = getIntent().getBooleanExtra("show_terminal_config", false);
        final SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);

        if (showTerminalConfig) {
            setupTerminalConfigView(prefs);
        } else {
            setupProfileView(prefs);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackPressed();
            }
        });
    }

    private void setupProfileView(final SharedPreferences prefs) {
        layoutMyProfile.setVisibility(View.VISIBLE);
        layoutChangePassword.setVisibility(View.GONE);
        layoutTerminalConfig.setVisibility(View.GONE);

        tvTitle.setText("Paramètres du Profil");
        if (btnHeaderInfo != null) btnHeaderInfo.setVisibility(View.GONE);
        isChangingPassword = false;
        bypassOldPinCheck = false;

        String gsm = prefs.getString("GSM_NUMBER", "+216 29968399");
        if (tvProfileGsm != null) {
            tvProfileGsm.setText(gsm);
        }

        if (btnGoToChangePassword != null) {
            btnGoToChangePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isChangingPassword = true;
                    bypassOldPinCheck = false;
                    layoutMyProfile.setVisibility(View.GONE);
                    layoutChangePassword.setVisibility(View.VISIBLE);
                    tvTitle.setText("Changer mot de passe");
                    if (btnHeaderInfo != null) btnHeaderInfo.setVisibility(View.VISIBLE);

                    clearPinFields(etOld1, etOld2, etOld3, etOld4);
                    clearPinFields(etNew1, etNew2, etNew3, etNew4);
                    clearPinFields(etConfirm1, etConfirm2, etConfirm3, etConfirm4);
                    setOldPinFieldsEnabled(true);
                    etOld1.requestFocus();
                }
            });
        }

        if (tvRegenerateRecoveryCode != null) {
            tvRegenerateRecoveryCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmAndRegenerateRecoveryCode(prefs);
                }
            });
        }

        setupPinAutoAdvance(etOld1, etOld2, etOld3, etOld4);
        setupPinAutoAdvance(etNew1, etNew2, etNew3, etNew4);
        setupPinAutoAdvance(etConfirm1, etConfirm2, etConfirm3, etConfirm4);

        if (tvForgotPin != null) {
            tvForgotPin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ManageAccountActivity.this, SmsCodeActivity.class);
                    intent.putExtra("MODE", "PIN_RESET");
                    startActivityForResult(intent, REQUEST_PIN_RESET);
                }
            });
        }

        if (tvUseRecoveryCode != null) {
            tvUseRecoveryCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRecoveryCodeInputDialog(prefs);
                }
            });
        }

        if (btnSubmitChangePassword != null) {
            btnSubmitChangePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newPass = getPinFromFields(etNew1, etNew2, etNew3, etNew4);
                    String confirmPass = getPinFromFields(etConfirm1, etConfirm2, etConfirm3, etConfirm4);

                    if (newPass.length() < 4 || confirmPass.length() < 4) {
                        Toast.makeText(ManageAccountActivity.this, "Veuillez remplir tous les codes confidentiels à 4 chiffres.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!bypassOldPinCheck) {
                        String oldPass = getPinFromFields(etOld1, etOld2, etOld3, etOld4);
                        if (oldPass.length() < 4) {
                            Toast.makeText(ManageAccountActivity.this, "Veuillez remplir tous les codes confidentiels à 4 chiffres.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String storedHash = prefs.getString("LOGIN_PASSWORD_HASH", null);
                        String storedSalt = prefs.getString("LOGIN_PASSWORD_SALT", null);
                        boolean oldPassCorrect;
                        if (storedHash == null || storedSalt == null) {
                            oldPassCorrect = oldPass.equals("0000");
                        } else {
                            oldPassCorrect = PinSecurity.hash(oldPass, storedSalt).equals(storedHash);
                        }

                        if (!oldPassCorrect) {
                            Toast.makeText(ManageAccountActivity.this, "L'ancien code confidentiel saisi est incorrect.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(ManageAccountActivity.this, "Le nouveau code confidentiel et sa confirmation ne correspondent pas.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newSalt = PinSecurity.generateSalt();
                    String newHash = PinSecurity.hash(newPass, newSalt);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("LOGIN_PASSWORD_HASH", newHash);
                    editor.putString("LOGIN_PASSWORD_SALT", newSalt);
                    editor.remove("LOGIN_PASSWORD");
                    editor.apply();

                    bypassOldPinCheck = false;
                    Toast.makeText(ManageAccountActivity.this, "Votre code confidentiel a été modifié avec succès !", Toast.LENGTH_LONG).show();

                    setupProfileView(prefs);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIN_RESET && resultCode == RESULT_OK) {
            bypassOldPinCheck = true;
            clearPinFields(etOld1, etOld2, etOld3, etOld4);
            clearPinFields(etNew1, etNew2, etNew3, etNew4);
            clearPinFields(etConfirm1, etConfirm2, etConfirm3, etConfirm4);
            setOldPinFieldsEnabled(false);
            Toast.makeText(this, "Identité confirmée. Définissez votre nouveau code confidentiel.", Toast.LENGTH_LONG).show();
            etNew1.requestFocus();
        }
    }

    private void showRecoveryCodeInputDialog(final SharedPreferences prefs) {
        String storedHash = prefs.getString("RECOVERY_CODE_HASH", null);
        String storedSalt = prefs.getString("RECOVERY_CODE_SALT", null);

        if (storedHash == null || storedSalt == null) {
            Toast.makeText(this, "Aucun code de récupération n'a encore été généré. "
                    + "Configurez-en un depuis votre profil avant d'en avoir besoin.", Toast.LENGTH_LONG).show();
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

                        bypassOldPinCheck = true;
                        clearPinFields(etOld1, etOld2, etOld3, etOld4);
                        clearPinFields(etNew1, etNew2, etNew3, etNew4);
                        clearPinFields(etConfirm1, etConfirm2, etConfirm3, etConfirm4);
                        setOldPinFieldsEnabled(false);
                        Toast.makeText(this, "Code valide (désormais utilisé). Définissez votre nouveau code confidentiel, "
                                + "puis pensez à régénérer un nouveau code de récupération.", Toast.LENGTH_LONG).show();
                        etNew1.requestFocus();
                    } else {
                        Toast.makeText(this, "Code de récupération incorrect.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void confirmAndRegenerateRecoveryCode(final SharedPreferences prefs) {
        boolean alreadyExists = prefs.contains("RECOVERY_CODE_HASH");

        String message = alreadyExists
                ? "Générer un nouveau code de récupération invalidera l'ancien. Continuer ?"
                : "Un code de récupération vous permettra de réinitialiser votre PIN sans SMS, "
                  + "en cas d'oubli. Vous devrez le noter et le conserver en lieu sûr : il ne sera "
                  + "plus jamais réaffiché. Continuer ?";

        new AlertDialog.Builder(this)
                .setTitle("Code de récupération")
                .setMessage(message)
                .setPositiveButton("Générer", (dialog, which) -> {
                    String newCode = PinSecurity.generateRecoveryCode();
                    String newSalt = PinSecurity.generateSalt();
                    String newHash = PinSecurity.hash(newCode, newSalt);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("RECOVERY_CODE_HASH", newHash);
                    editor.putString("RECOVERY_CODE_SALT", newSalt);
                    editor.apply();

                    showRecoveryCodeGeneratedDialog(newCode);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showRecoveryCodeGeneratedDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("Votre code de récupération")
                .setMessage("Notez ce code et conservez-le en lieu sûr. Il ne sera plus jamais affiché :\n\n"
                        + code + "\n\nCe code est à usage unique : il sera invalidé dès sa première utilisation.")
                .setCancelable(false)
                .setPositiveButton("J'ai noté ce code", null)
                .show();
    }

    private void setOldPinFieldsEnabled(boolean enabled) {
        etOld1.setEnabled(enabled);
        etOld2.setEnabled(enabled);
        etOld3.setEnabled(enabled);
        etOld4.setEnabled(enabled);
        float alpha = enabled ? 1f : 0.4f;
        etOld1.setAlpha(alpha);
        etOld2.setAlpha(alpha);
        etOld3.setAlpha(alpha);
        etOld4.setAlpha(alpha);
    }

    private void setupTerminalConfigView(final SharedPreferences prefs) {
        layoutMyProfile.setVisibility(View.GONE);
        layoutChangePassword.setVisibility(View.GONE);
        layoutTerminalConfig.setVisibility(View.VISIBLE);

        tvTitle.setText("Configuration Terminal");
        if (btnHeaderInfo != null) btnHeaderInfo.setVisibility(View.GONE);
        isChangingPassword = false;

        etTerminalId.setText(prefs.getString("TERMINAL_ID", "00000005"));
        etMerchantId.setText(prefs.getString("MERCHANT_ID", "0599903309"));
        etStoreId.setText(prefs.getString("STORE_ID", "0599903309"));
        etGsmNumber.setText(prefs.getString("GSM_NUMBER", "+216 29968399"));
        etIpAddress.setText(prefs.getString("IP_ADDRESS", "41.230.10.77"));
        etPort.setText(prefs.getString("PORT", "3443"));

        btnValidate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String termId = etTerminalId.getText().toString().trim();
                String merchId = etMerchantId.getText().toString().trim();
                String storId = etStoreId.getText().toString().trim();
                String gsmNum = etGsmNumber.getText().toString().trim();
                String ipAddr = etIpAddress.getText().toString().trim();
                String portNum = etPort.getText().toString().trim();

                if (termId.isEmpty() || merchId.isEmpty() || storId.isEmpty() || gsmNum.isEmpty() || ipAddr.isEmpty() || portNum.isEmpty()) {
                    Toast.makeText(ManageAccountActivity.this, "Veuillez remplir tous les champs de configuration.", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("TERMINAL_ID", termId);
                editor.putString("MERCHANT_ID", merchId);
                editor.putString("STORE_ID", storId);
                editor.putString("GSM_NUMBER", gsmNum);
                editor.putString("IP_ADDRESS", ipAddr);
                editor.putString("PORT", portNum);
                editor.apply();

                Toast.makeText(ManageAccountActivity.this, "Configurations enregistrées avec succès !", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupPinAutoAdvance(final EditText et1, final EditText et2, final EditText et3, final EditText et4) {
        et1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) et2.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        et2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) et3.requestFocus();
                else if (s.length() == 0) et1.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        et3.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) et4.requestFocus();
                else if (s.length() == 0) et2.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        et4.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) et3.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        View.OnKeyListener backListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    EditText et = (EditText) v;
                    if (et.getText().length() == 0) {
                        if (et == et2) { et1.setText(""); et1.requestFocus(); return true; }
                        else if (et == et3) { et2.setText(""); et2.requestFocus(); return true; }
                        else if (et == et4) { et3.setText(""); et3.requestFocus(); return true; }
                    }
                }
                return false;
            }
        };

        et2.setOnKeyListener(backListener);
        et3.setOnKeyListener(backListener);
        et4.setOnKeyListener(backListener);
    }

    private String getPinFromFields(EditText et1, EditText et2, EditText et3, EditText et4) {
        return et1.getText().toString().trim() + et2.getText().toString().trim()
                + et3.getText().toString().trim() + et4.getText().toString().trim();
    }

    private void clearPinFields(EditText et1, EditText et2, EditText et3, EditText et4) {
        et1.setText("");
        et2.setText("");
        et3.setText("");
        et4.setText("");
    }

    private void handleBackPressed() {
        if (isChangingPassword) {
            final SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
            setupProfileView(prefs);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }
}