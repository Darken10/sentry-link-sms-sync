package com.africasys.sentrylink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.R;
import com.africasys.sentrylink.models.Contact;

import java.util.List;

/**
 * Adapter pour la sélection d'un contact dans {@code NewConversationActivity}.
 */
public class ContactPickerAdapter extends RecyclerView.Adapter<ContactPickerAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    private List<Contact> contacts;
    private final OnContactClickListener listener;

    public ContactPickerAdapter(List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    public void updateList(List<Contact> newList) {
        this.contacts = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);

        // Avatar : première lettre du nom
        String initial = (contact.name != null && !contact.name.isEmpty())
                ? String.valueOf(contact.name.charAt(0)).toUpperCase()
                : "?";
        holder.tvAvatar.setText(initial);

        // Nom
        holder.tvName.setText(contact.name != null ? contact.name : contact.phoneNumber);

        // Identifiant + type
        if (contact.identifier != null && !contact.identifier.isEmpty()) {
            String badge = contact.identifier;
            if (contact.type != null && !contact.type.isEmpty()) {
                badge += " · " + contact.type;
            }
            holder.tvIdentifier.setText(badge);
            holder.tvIdentifier.setVisibility(View.VISIBLE);
        } else {
            holder.tvIdentifier.setVisibility(View.GONE);
        }

        // Numéro
        holder.tvPhone.setText(contact.phoneNumber != null ? contact.phoneNumber : "");

        holder.itemView.setOnClickListener(v -> listener.onContactClick(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvName;
        final TextView tvIdentifier;
        final TextView tvPhone;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvIdentifier = itemView.findViewById(R.id.tvIdentifier);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }
    }
}
