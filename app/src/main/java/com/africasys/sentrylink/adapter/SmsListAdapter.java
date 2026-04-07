package com.africasys.sentrylink.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.africasys.sentrylink.R;
import com.africasys.sentrylink.dtos.SmsResponseDTO;
import java.util.List;

public class SmsListAdapter extends ArrayAdapter<SmsResponseDTO> {

    private Context context;
    private List<SmsResponseDTO> smsList;

    public SmsListAdapter(Context context, List<SmsResponseDTO> smsList) {
        super(context, 0, smsList);
        this.context = context;
        this.smsList = smsList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.sms_list_item, parent, false);
            holder = new ViewHolder();
            holder.smsPhone = convertView.findViewById(R.id.sms_phone);
            holder.smsMessage = convertView.findViewById(R.id.sms_message);
            holder.smsDate = convertView.findViewById(R.id.sms_date);
            holder.smsType = convertView.findViewById(R.id.sms_type);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SmsResponseDTO sms = smsList.get(position);

        if (sms != null) {
            holder.smsPhone.setText(sms.getPhoneNumber());
            holder.smsMessage.setText(sms.getMessageBody());
            holder.smsDate.setText(sms.getFormattedDate());
            
            String typeText = sms.getMessageType() == 1 ? "🔽 Reçu" : "🔼 Envoyé";
            holder.smsType.setText(typeText);
            
            // Change color based on type
            int backgroundColor = sms.getMessageType() == 1 ? 
                context.getColor(R.color.status_received) :
                context.getColor(R.color.accent_dark);
            holder.smsType.setBackgroundColor(backgroundColor);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView smsPhone;
        TextView smsMessage;
        TextView smsDate;
        TextView smsType;
    }
}
