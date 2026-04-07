package com.africasys.sentrylink;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.models.SMSMessage;
import com.africasys.sentrylink.repository.AppDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Affiche l'historique de tous les SMS interceptés par le service.
 */
public class SmsLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View viewEmpty;
    private SmsLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sms_log);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerSmsLog);
        viewEmpty = findViewById(R.id.layoutEmpty);

        adapter = new SmsLogAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSms();
    }

    private void loadSms() {
        new Thread(() -> {
            List<SMSMessage> list = AppDatabase.getInstance(this).smsDao().getAllSMS();
            runOnUiThread(() -> {
                adapter.setData(list);
                boolean empty = list.isEmpty();
                viewEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    // ─────────────────────────── Adapter ────────────────────────────

    static class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.ViewHolder> {

        private List<SMSMessage> data;

        SmsLogAdapter(List<SMSMessage> data) {
            this.data = data;
        }

        void setData(List<SMSMessage> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sms_log, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SMSMessage sms = data.get(position);

            holder.tvSender.setText(sms.getAddress() != null ? sms.getAddress() : "—");

            String body = sms.getBody() != null ? sms.getBody() : "";
            holder.tvBody.setText(body.length() > 100 ? body.substring(0, 100) + "…" : body);

            String date = new SimpleDateFormat("dd/MM/yy  HH:mm", Locale.getDefault())
                    .format(new Date(sms.getTimestamp()));
            holder.tvDate.setText(date);

            if (sms.getType() == 1) {
                holder.tvType.setText(holder.itemView.getContext().getString(R.string.sms_type_received));
                holder.tvType.setBackgroundResource(R.drawable.status_badge_active);
                holder.tvType.setTextColor(holder.itemView.getContext().getColor(R.color.accent));
            } else {
                holder.tvType.setText(holder.itemView.getContext().getString(R.string.sms_type_sent));
                holder.tvType.setBackground(null);
                holder.tvType.setTextColor(holder.itemView.getContext().getColor(R.color.status_pending));
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSender, tvBody, tvDate, tvType;

            ViewHolder(View itemView) {
                super(itemView);
                tvSender = itemView.findViewById(R.id.tvSender);
                tvBody = itemView.findViewById(R.id.tvBody);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvType = itemView.findViewById(R.id.tvType);
            }
        }
    }
}
