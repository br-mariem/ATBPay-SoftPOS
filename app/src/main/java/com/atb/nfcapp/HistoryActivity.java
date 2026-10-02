package com.atb.nfcapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RadioGroup rgFilterType;
    private RadioButton rbAll, rbSingle, rbRange;
    private View layoutSingleDate, layoutRangeDate;
    private TextView tvSingleDate, tvStartDate, tvEndDate;
    private Button btnClearFilters;
    private ListView lvTransactions;
    private TextView tvEmptyState;

    private ArrayList<JSONObject> allTransactions = new ArrayList<>();
    private ArrayList<JSONObject> filteredTransactions = new ArrayList<>();
    private TransactionAdapter adapter;

    private Calendar singleCal = Calendar.getInstance();
    private Calendar startCal = Calendar.getInstance();
    private Calendar endCal = Calendar.getInstance();

    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat txDateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        btnBack = findViewById(R.id.btnBack);
        rgFilterType = findViewById(R.id.rgFilterType);
        rbAll = findViewById(R.id.rbAll);
        rbSingle = findViewById(R.id.rbSingle);
        rbRange = findViewById(R.id.rbRange);
        layoutSingleDate = findViewById(R.id.layoutSingleDate);
        layoutRangeDate = findViewById(R.id.layoutRangeDate);
        tvSingleDate = findViewById(R.id.tvSingleDate);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        btnClearFilters = findViewById(R.id.btnClearFilters);
        lvTransactions = findViewById(R.id.lvTransactions);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        btnBack.setOnClickListener(v -> finish());

        rgFilterType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAll) {
                layoutSingleDate.setVisibility(View.GONE);
                layoutRangeDate.setVisibility(View.GONE);
                applyFilters();
            } else if (checkedId == R.id.rbSingle) {
                layoutSingleDate.setVisibility(View.VISIBLE);
                layoutRangeDate.setVisibility(View.GONE);
                applyFilters();
            } else if (checkedId == R.id.rbRange) {
                layoutSingleDate.setVisibility(View.GONE);
                layoutRangeDate.setVisibility(View.VISIBLE);
                applyFilters();
            }
        });

        tvSingleDate.setOnClickListener(v -> showDatePicker(singleCal, tvSingleDate));
        tvStartDate.setOnClickListener(v -> showDatePicker(startCal, tvStartDate));
        tvEndDate.setOnClickListener(v -> showDatePicker(endCal, tvEndDate));

        btnClearFilters.setOnClickListener(v -> {
            rbAll.setChecked(true);
            tvSingleDate.setText("Sélectionner...");
            tvStartDate.setText("Début...");
            tvEndDate.setText("Fin...");
            applyFilters();
        });

        loadTransactions();
    }

    private void showDatePicker(Calendar targetCal, TextView targetView) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            targetCal.set(Calendar.YEAR, year);
            targetCal.set(Calendar.MONTH, month);
            targetCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            targetView.setText(displayDateFormat.format(targetCal.getTime()));
            applyFilters();
        }, targetCal.get(Calendar.YEAR), targetCal.get(Calendar.MONTH), targetCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadTransactions() {
        allTransactions.clear();
        android.content.SharedPreferences prefs = getSharedPreferences("ATB_CONFIGS", MODE_PRIVATE);
        String historyStr = prefs.getString("TX_HISTORY", null);

        try {
            JSONArray array;
            if (historyStr == null) {
                array = new JSONArray();
                // Valeurs d'initialisation par défaut s'il n'y a pas d'historique
                JSONObject init1 = new JSONObject();
                init1.put("title", "Achat");
                init1.put("date", "24/05/23 15:19:16");
                init1.put("amount", -12.000);
                init1.put("type", "debit");
                init1.put("reference", "000193");
                init1.put("approved", true);
                array.put(init1);

                JSONObject init2 = new JSONObject();
                init2.put("title", "Monoprix Tunis");
                init2.put("date", "30/06/26 14:32:00");
                init2.put("amount", -68.450);
                init2.put("type", "debit");
                init2.put("reference", "000194");
                init2.put("approved", true);
                array.put(init2);

                JSONObject init3 = new JSONObject();
                init3.put("title", "Ola Energy Lac 2");
                init3.put("date", "29/06/26 18:10:00");
                init3.put("amount", -50.000);
                init3.put("type", "debit");
                init3.put("reference", "000195");
                init3.put("approved", true);
                array.put(init3);
            } else {
                array = new JSONArray(historyStr);
            }

            for (int i = 0; i < array.length(); i++) {
                allTransactions.add(array.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        applyFilters();
    }

    private void applyFilters() {
        filteredTransactions.clear();

        for (JSONObject tx : allTransactions) {
            try {
                String txDateStr = tx.getString("date");
                Date date = txDateFormat.parse(txDateStr);
                if (date == null) continue;

                if (rbAll.isChecked()) {
                    filteredTransactions.add(tx);
                } else if (rbSingle.isChecked()) {
                    String singleDateStr = tvSingleDate.getText().toString();
                    if (!singleDateStr.equals("Sélectionner...")) {
                        Calendar txCal = Calendar.getInstance();
                        txCal.setTime(date);
                        if (txCal.get(Calendar.YEAR) == singleCal.get(Calendar.YEAR) &&
                                txCal.get(Calendar.MONTH) == singleCal.get(Calendar.MONTH) &&
                                txCal.get(Calendar.DAY_OF_MONTH) == singleCal.get(Calendar.DAY_OF_MONTH)) {
                            filteredTransactions.add(tx);
                        }
                    } else {
                        filteredTransactions.add(tx);
                    }
                } else if (rbRange.isChecked()) {
                    String startStr = tvStartDate.getText().toString();
                    String endStr = tvEndDate.getText().toString();

                    boolean matchesStart = true;
                    boolean matchesEnd = true;

                    if (!startStr.equals("Début...")) {
                        Calendar startLimit = (Calendar) startCal.clone();
                        startLimit.set(Calendar.HOUR_OF_DAY, 0);
                        startLimit.set(Calendar.MINUTE, 0);
                        startLimit.set(Calendar.SECOND, 0);
                        matchesStart = date.after(startLimit.getTime()) || date.equals(startLimit.getTime());
                    }

                    if (!endStr.equals("Fin...")) {
                        Calendar endLimit = (Calendar) endCal.clone();
                        endLimit.set(Calendar.HOUR_OF_DAY, 23);
                        endLimit.set(Calendar.MINUTE, 59);
                        endLimit.set(Calendar.SECOND, 59);
                        matchesEnd = date.before(endLimit.getTime()) || date.equals(endLimit.getTime());
                    }

                    if (matchesStart && matchesEnd) {
                        filteredTransactions.add(tx);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // En cas d'erreur de parsing de date, on l'affiche quand même
                filteredTransactions.add(tx);
            }
        }

        if (filteredTransactions.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            lvTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            lvTransactions.setVisibility(View.VISIBLE);
        }

        if (adapter == null) {
            adapter = new TransactionAdapter();
            lvTransactions.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private class TransactionAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return filteredTransactions.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredTransactions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_transaction, parent, false);
            }

            TextView tvTitle = convertView.findViewById(R.id.tvTitle);
            TextView tvDate = convertView.findViewById(R.id.tvDate);
            TextView tvRef = convertView.findViewById(R.id.tvRef);
            TextView tvAmount = convertView.findViewById(R.id.tvAmount);
            TextView tvStatus = convertView.findViewById(R.id.tvStatus);

            try {
                JSONObject tx = filteredTransactions.get(position);
                tvTitle.setText(tx.getString("title"));
                tvDate.setText(tx.getString("date"));
                tvRef.setText("Réf: " + tx.getString("reference"));

                double amount = tx.getDouble("amount");
                String formattedAmount = String.format(Locale.US, "%,.3f", Math.abs(amount)).replace(',', ' ').replace('.', ',');

                if (amount < 0) {
                    tvAmount.setText("-" + formattedAmount + " TND");
                    tvAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    tvAmount.setText("+" + formattedAmount + " TND");
                    tvAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }

                boolean approved = tx.optBoolean("approved", true);
                if (approved) {
                    tvStatus.setText("APPROUVÉ");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    tvStatus.setText("REFUSÉ");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}