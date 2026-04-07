package com.africasys.sentrylink.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.R;
import com.africasys.sentrylink.models.Contact;
import com.africasys.sentrylink.models.ConversationSummary;

import java.util.List;

/**
 * Adapter RecyclerView pour la liste des conversations SentryLink.
 *
 * <p>
 * Seuls les messages portant le préfixe {@code [SL]} sont représentés.
 * Pour chaque conversation, le nom du contact (s'il est en base) est affiché à
 * la place du numéro de téléphone brut.
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    /** Callback déclenché au clic sur une conversation. */
    public interface OnConversationClickListener {
        void onClick(ConversationSummary summary, Contact contact);
    }

    private final Context context;
    private final List<ConversationSummary> summaries;
    /** Map phone_number → Contact (résolu avant la création de l'adapter). */
    private final java.util.Map<String, Contact> contactMap;
    private final OnConversationClickListener listener;

    public ConversationAdapter(Context context,
            List<ConversationSummary> summaries,
            java.util.Map<String, Contact> contactMap,
            OnConversationClickListener listener) {
        this.context = context;
        this.summaries = summaries;
        this.contactMap = contactMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationSummary summary = summaries.get(position);
        Contact contact = contactMap.get(summary.phoneNumber);

        // --- Nom affiché ---
        String displayName = (contact != null && contact.name != null && !contact.name.isEmpty())
                ? contact.name
                : summary.phoneNumber;
        holder.tvContactName.setText(displayName);

        // --- Initiale pour l'avatar ---
        String initial = displayName.isEmpty() ? "?" : displayName.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);

        // --- Identifiant métier ---
        if (contact != null && contact.identifier != null && !contact.identifier.isEmpty()) {
            holder.tvIdentifier.setText(contact.identifier
                    + (contact.type != null ? "  ·  " + contact.type : ""));
            holder.tvIdentifier.setVisibility(View.VISIBLE);
        } else {
            holder.tvIdentifier.setVisibility(View.GONE);
        }

        // --- Dernier message (aperçu) ---
        // Le corps est chiffré (format [SL]…) : on affiche un aperçu générique
        holder.tvLastMessage.setText(context.getString(R.string.conv_encrypted_preview));

        // --- Direction ---
        holder.tvDirection.setText(summary.lastMessageType == 1 ? "↙" : "↗");
        holder.tvDirection.setTextColor(
                summary.lastMessageType == 1
                        ? context.getColor(R.color.status_received)
                        : context.getColor(R.color.accent));

        // --- Horodatage relatif ---
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                summary.lastTimestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
        holder.tvTimestamp.setText(relativeTime);

        // --- Badge compteur ---
        if (summary.messageCount > 0) {
            holder.tvBadge.setText(summary.messageCount > 99 ? "99+" : String.valueOf(summary.messageCount));
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        // --- Clic ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onClick(summary, contact);
        });
    }

    @Override
    public int getItemCount() {
        return summaries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvContactName;
        final TextView tvIdentifier;
        final TextView tvLastMessage;
        final TextView tvDirection;
        final TextView tvTimestamp;
        final TextView tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvIdentifier = itemView.findViewById(R.id.tvIdentifier);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvDirection = itemView.findViewById(R.id.tvDirection);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
