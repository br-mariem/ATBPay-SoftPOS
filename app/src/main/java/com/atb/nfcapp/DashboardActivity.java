package com.atb.nfcapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView btnMenu;
    private LinearLayout drawerItemProfile;
    private LinearLayout drawerItemHistory;
    private LinearLayout drawerItemSettings;

    private CardView cardAchat;
    private CardView cardAnnulation;
    private ImageView btnLogout;
    private TextView btnViewAll;
    private View tabExplore;
    private View tabBookmark;
    private View tabSettings;
    private View tabProfile;

    private CardView cardTx1, cardTx2, cardTx3;
    private TextView tvTxTitle1, tvTxTitle2, tvTxTitle3;
    private TextView tvTxDate1, tvTxDate2, tvTxDate3;
    private TextView tvTxReference1, tvTxReference2, tvTxReference3;
    private TextView tvTxCard1, tvTxCard2, tvTxCard3;
    private TextView tvTxAmount1, tvTxAmount2, tvTxAmount3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        drawerItemProfile = findViewById(R.id.drawerItemProfile);
        drawerItemHistory = findViewById(R.id.drawerItemHistory);
        drawerItemSettings = findViewById(R.id.drawerItemSettings);

        cardAchat = findViewById(R.id.cardAchat);
        cardAnnulation = findViewById(R.id.cardAnnulation);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewAll = findViewById(R.id.btnViewAll);
        tabExplore = findViewById(R.id.tabExplore);
        tabBookmark = findViewById(R.id.tabBookmark);
        tabSettings = findViewById(R.id.tabSettings);
        tabProfile = findViewById(R.id.tabProfile);

        cardTx1 = findViewById(R.id.cardTx1);
        cardTx2 = findViewById(R.id.cardTx2);
        cardTx3 = findViewById(R.id.cardTx3);

        tvTxTitle1 = findViewById(R.id.tvTxTitle1);
        tvTxTitle2 = findViewById(R.id.tvTxTitle2);
        tvTxTitle3 = findViewById(R.id.tvTxTitle3);

        tvTxDate1 = findViewById(R.id.tvTxDate1);
        tvTxDate2 = findViewById(R.id.tvTxDate2);
        tvTxDate3 = findViewById(R.id.tvTxDate3);

        tvTxReference1 = findViewById(R.id.tvTxReference1);
        tvTxReference2 = findViewById(R.id.tvTxReference2);
        tvTxReference3 = findViewById(R.id.tvTxReference3);

        tvTxCard1 = findViewById(R.id.tvTxCard1);
        tvTxCard2 = findViewById(R.id.tvTxCard2);
        tvTxCard3 = findViewById(R.id.tvTxCard3);

        tvTxAmount1 = findViewById(R.id.tvTxAmount1);
        tvTxAmount2 = findViewById(R.id.tvTxAmount2);
        tvTxAmount3 = findViewById(R.id.tvTxAmount3);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        if (drawerItemProfile != null) {
            drawerItemProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    Intent intent = new Intent(DashboardActivity.this, ManageAccountActivity.class);
                    intent.putExtra("show_terminal_config", false);
                    startActivity(intent);
                }
            });
        }

        if (drawerItemHistory != null) {
            drawerItemHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (drawerItemSettings != null) {
            drawerItemSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    Intent intent = new Intent(DashboardActivity.this, ManageAccountActivity.class);
                    intent.putExtra("show_terminal_config", true);
                    startActivity(intent);
                }
            });
        }

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "Déconnexion en cours...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                finish();
            }
        });

        cardAchat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AmountActivity.class);
                intent.putExtra("TX_TYPE", "achat");
                startActivity(intent);
            }
        });

        cardAnnulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
                String txHistoryStr = prefs.getString("TX_HISTORY", null);
                double lastAmount = 45.000;
                if (txHistoryStr != null) {
                    try {
                        org.json.JSONArray txArray = new org.json.JSONArray(txHistoryStr);
                        if (txArray.length() > 0) {
                            org.json.JSONObject lastTx = txArray.getJSONObject(0);
                            lastAmount = Math.abs(lastTx.optDouble("amount", 45.0));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showSecureKeyboardDialog(lastAmount);
            }
        });

        btnViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        if (tabBookmark != null) {
            tabBookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (tabExplore != null) {
            tabExplore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (tabSettings != null) {
            tabSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, ManageAccountActivity.class);
                    intent.putExtra("show_terminal_config", true);
                    startActivity(intent);
                }
            });
        }

        if (tabProfile != null) {
            tabProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, ManageAccountActivity.class);
                    intent.putExtra("show_terminal_config", false);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLastTransaction();
    }

    private void refreshLastTransaction() {
        SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        String txHistoryStr = prefs.getString("TX_HISTORY", null);

        if (cardTx1 != null) cardTx1.setVisibility(View.GONE);
        if (cardTx2 != null) cardTx2.setVisibility(View.GONE);
        if (cardTx3 != null) cardTx3.setVisibility(View.GONE);

        if (txHistoryStr == null) return;

        try {
            JSONArray txArray = new JSONArray(txHistoryStr);
            int count = Math.min(txArray.length(), 3);

            for (int i = 0; i < count; i++) {
                JSONObject tx = txArray.getJSONObject(i);
                String title = tx.optString("title", "Achat");
                String date = tx.optString("date", "");
                String reference = tx.optString("reference", "");
                double amount = tx.optDouble("amount", 0.0);
                String type = tx.optString("type", "debit");
                // CORRIGÉ : lecture du vrai PAN sauvegardé par ReceiptActivity,
                // au lieu du texte "476173******0027" codé en dur pour les 3 cartes.
                String pan = tx.optString("pan", "Indisponible");

                String sign = type.equalsIgnoreCase("credit") ? "+" : "-";
                String formattedAmount = String.format(java.util.Locale.US, "%,.3f", Math.abs(amount));
                formattedAmount = formattedAmount.replace(',', ' ').replace('.', ',');

                if (i == 0) {
                    if (cardTx1 != null) cardTx1.setVisibility(View.VISIBLE);
                    if (tvTxTitle1 != null) tvTxTitle1.setText(title);
                    if (tvTxDate1 != null) tvTxDate1.setText(date);
                    if (tvTxReference1 != null) tvTxReference1.setText("Référence: " + reference);
                    if (tvTxCard1 != null) tvTxCard1.setText("Numéro de carte: " + pan);
                    if (tvTxAmount1 != null) tvTxAmount1.setText(sign + formattedAmount + " TND");
                } else if (i == 1) {
                    if (cardTx2 != null) cardTx2.setVisibility(View.VISIBLE);
                    if (tvTxTitle2 != null) tvTxTitle2.setText(title);
                    if (tvTxDate2 != null) tvTxDate2.setText(date);
                    if (tvTxReference2 != null) tvTxReference2.setText("Référence: " + reference);
                    if (tvTxCard2 != null) tvTxCard2.setText("Numéro de carte: " + pan);
                    if (tvTxAmount2 != null) tvTxAmount2.setText(sign + formattedAmount + " TND");
                } else if (i == 2) {
                    if (cardTx3 != null) cardTx3.setVisibility(View.VISIBLE);
                    if (tvTxTitle3 != null) tvTxTitle3.setText(title);
                    if (tvTxDate3 != null) tvTxDate3.setText(date);
                    if (tvTxReference3 != null) tvTxReference3.setText("Référence: " + reference);
                    if (tvTxCard3 != null) tvTxCard3.setText("Numéro de carte: " + pan);
                    if (tvTxAmount3 != null) tvTxAmount3.setText(sign + formattedAmount + " TND");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSecureKeyboardDialog(final double lastAmount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_secure_keyboard, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final ImageView dot1 = dialogView.findViewById(R.id.dot1);
        final ImageView dot2 = dialogView.findViewById(R.id.dot2);
        final ImageView dot3 = dialogView.findViewById(R.id.dot3);
        final ImageView dot4 = dialogView.findViewById(R.id.dot4);
        final ImageView[] dots = {dot1, dot2, dot3, dot4};

        final StringBuilder enteredPin = new StringBuilder();

        int[] buttonIds = {
                R.id.btnSecKey0, R.id.btnSecKey1, R.id.btnSecKey2,
                R.id.btnSecKey3, R.id.btnSecKey4, R.id.btnSecKey5,
                R.id.btnSecKey6, R.id.btnSecKey7, R.id.btnSecKey8,
                R.id.btnSecKey9
        };
        final String[] digitValues = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        };

        for (int i = 0; i < buttonIds.length; i++) {
            Button btn = dialogView.findViewById(buttonIds[i]);
            if (btn != null) {
                final String digitStr = digitValues[i];
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (enteredPin.length() < 4) {
                            enteredPin.append(digitStr);
                            updatePinDots(enteredPin.length(), dots);
                        }
                    }
                });
            }
        }

        Button btnSecKeyClear = dialogView.findViewById(R.id.btnSecKeyClear);
        if (btnSecKeyClear != null) {
            btnSecKeyClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enteredPin.setLength(0);
                    updatePinDots(0, dots);
                }
            });
        }

        Button btnSecKeyConfirm = dialogView.findViewById(R.id.btnSecKeyConfirm);
        if (btnSecKeyConfirm != null) {
            btnSecKeyConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
                    String correctCode = prefs.getString("SECURITY_CODE", "0000");
                    if (enteredPin.toString().equals(correctCode)) {
                        dialog.dismiss();
                        Toast.makeText(DashboardActivity.this, "Code correct !", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(DashboardActivity.this, SmsCodeActivity.class);
                        intent.putExtra("AMOUNT", lastAmount);
                        intent.putExtra("TX_TYPE", "annulation");
                        startActivity(intent);
                    } else {
                        Toast.makeText(DashboardActivity.this, "Code de sécurité incorrect. Réessayez.", Toast.LENGTH_SHORT).show();
                        enteredPin.setLength(0);
                        updatePinDots(0, dots);
                    }
                }
            });
        }

        TextView tvSecCancel = dialogView.findViewById(R.id.tvSecCancel);
        if (tvSecCancel != null) {
            tvSecCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void updatePinDots(int length, ImageView[] dots) {
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null) {
                if (i < length) {
                    dots[i].setImageResource(R.drawable.pin_dot_filled);
                } else {
                    dots[i].setImageResource(R.drawable.pin_dot_empty);
                }
            }
        }
    }
}