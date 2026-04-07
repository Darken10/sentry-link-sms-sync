package com.africasys.sentrylink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.R;
import com.africasys.sentrylink.models.SMSMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter bulles de conversation (style messagerie instantanée).
 * Type 2 = envoyé → bulle droite cyan.
 * Type 1 = reçu → bulle gauche sombre.
 */
public class MessageBubbleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_SENT = 1;
    private static final int VIEW_RECEIVED = 2;

    private final List<SMSMessage> messages;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageBubbleAdapter(List<SMSMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType() == 2 ? VIEW_SENT : VIEW_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_SENT) {
            return new BubbleHolder(inf.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new BubbleHolder(inf.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SMSMessage msg = messages.get(position);
        BubbleHolder h = (BubbleHolder) holder;

        // Retirer le préfixe SL1 si présent (message chiffré non déchiffré en base)
        String body = msg.getBody() != null ? msg.getBody() : "";
        if (body.startsWith("SL1"))
            body = body.substring(3);
        h.tvBody.setText(body);

        h.tvTime.setText(timeFmt.format(new Date(msg.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class BubbleHolder extends RecyclerView.ViewHolder {
        final TextView tvBody;
        final TextView tvTime;

        BubbleHolder(@NonNull View v) {
            super(v);
            tvBody = v.findViewById(R.id.tvMessageBody);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
