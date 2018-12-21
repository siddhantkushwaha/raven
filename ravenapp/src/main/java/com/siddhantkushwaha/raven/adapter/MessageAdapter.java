package com.siddhantkushwaha.raven.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.siddhantkushwaha.raven.R;
import com.siddhantkushwaha.raven.commonUtility.DateTimeUtils;
import com.siddhantkushwaha.raven.localEntity.RavenMessage;
import com.siddhantkushwaha.raven.manager.ThreadManager;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.security.GeneralSecurityException;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

public class MessageAdapter extends RealmRecyclerViewAdapter {

    private Context context;
    private OrderedRealmCollection<RavenMessage> data;

    private OnClickListener onClickListener;
    private OnLongClickListener onLongClickListener;

    public MessageAdapter(Context context, @Nullable OrderedRealmCollection data, boolean autoUpdate) {
        super(data, autoUpdate);

        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = null;
        switch (viewType) {
            case 1:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_message_sent, viewGroup, false);
                return new MessageAdapter.SentMessageHolder(view);
            case 2:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_message_received, viewGroup, false);
                return new MessageAdapter.ReceivedMessageHolder(view);
            default:
                return null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getMessageType(FirebaseAuth.getInstance().getUid());
    }

    @Nullable
    @Override
    public RavenMessage getItem(int index) {
        return data.get(index);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        RavenMessage ravenMessage = data.get(position);

        boolean showDate = true;
        int previousMessageType = -1;
        if (position > 0) {

            RavenMessage previousRavenMessage = data.get(position - 1);

            String currentMessageTime = ravenMessage.getTimestamp();
            if (currentMessageTime == null) currentMessageTime = ravenMessage.getLocalTimestamp();

            String previousMessageTime = previousRavenMessage.getTimestamp();
            if (previousMessageTime == null)
                previousMessageTime = previousRavenMessage.getLocalTimestamp();

            if (DateTimeUtils.dateCmp(DateTime.parse(currentMessageTime), DateTime.parse(previousMessageTime)) == 0)
                showDate = false;

            previousMessageType = this.getItemViewType(position - 1);
        }

        switch (holder.getItemViewType()) {
            case 1:
                ((MessageAdapter.SentMessageHolder) holder).bind(ravenMessage, previousMessageType, showDate, position);
                break;
            case 2:
                ((MessageAdapter.ReceivedMessageHolder) holder).bind(ravenMessage, previousMessageType, showDate, position);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        View view;
        LinearLayout bannerLayout;
        TextView bannerText;
        TextView messageText;
        TextView timeText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            view = itemView;
            bannerLayout = itemView.findViewById(R.id.bannerLayout);
            bannerText = itemView.findViewById(R.id.bannerText);
            messageText = itemView.findViewById(R.id.text);
            timeText = itemView.findViewById(R.id.sent_time);
        }

        void bind(RavenMessage ravenMessage, int previousMessageType, boolean showDate, final int position) {

            makeBanner(showDate, bannerLayout, bannerText, ravenMessage);

            try {
                messageText.setText(ThreadManager.decryptMessage(ravenMessage.getThreadId(), ravenMessage.getText()));
                messageText.setTextColor(ContextCompat.getColor(context, R.color.colorBlack));
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_white));
            } catch (NullPointerException e) {
                messageText.setText("Message Deleted.");
                messageText.setTextColor(ContextCompat.getColor(context, R.color.colorWhite));
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_red));
            } catch (GeneralSecurityException e) {
                messageText.setText("Couldn't Decrypt");
                messageText.setTextColor(ContextCompat.getColor(context, R.color.colorWhite));
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_red));
            } catch (Exception e) {
                messageText.setText("There was a problem.");
                messageText.setTextColor(ContextCompat.getColor(context, R.color.colorWhite));
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_red));
            }

            if (ravenMessage.getTimestamp() != null) {

                timeText.setVisibility(View.VISIBLE);

                DateTime time = DateTime.parse(ravenMessage.getTimestamp());
                DateTimeFormatter build = DateTimeFormat.forPattern("hh:mm a");
                timeText.setText(build.print(time));
            } else {
                timeText.setVisibility(View.GONE);
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null)
                        onClickListener.onClickListener(v, position);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onLongClickListener != null)
                        onLongClickListener.onLongClickListener(v, position);
                    return false;
                }
            });
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        View view;
        LinearLayout bannerLayout;
        TextView bannerText;
        TextView messageText;
        TextView timeText;
        ImageView status;

        SentMessageHolder(View itemView) {
            super(itemView);
            view = itemView;
            bannerLayout = itemView.findViewById(R.id.bannerLayout);
            bannerText = itemView.findViewById(R.id.bannerText);
            messageText = itemView.findViewById(R.id.text);
            timeText = itemView.findViewById(R.id.sent_time);
            status = itemView.findViewById(R.id.status);
        }

        void bind(RavenMessage ravenMessage, int previousMessageType, boolean showDate, final int position) {

            makeBanner(showDate, bannerLayout, bannerText, ravenMessage);

            try {
                messageText.setText(ThreadManager.decryptMessage(ravenMessage.getThreadId(), ravenMessage.getText()));
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_indigo));
            } catch (NullPointerException e) {
                messageText.setText("Message Deleted.");
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_red));
            } catch (GeneralSecurityException e) {
                messageText.setText("Couldn't Decrypt");
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_red));
            } catch (Exception e) {
                messageText.setText("There was a problem.");
                messageText.setBackground(context.getDrawable(R.drawable.background_message_holder_red));
            }

            if (ravenMessage.getTimestamp() != null) {

                timeText.setVisibility(View.VISIBLE);

                DateTime time = DateTime.parse(ravenMessage.getTimestamp());
                DateTimeFormatter build = DateTimeFormat.forPattern("hh:mm a");
                timeText.setText(build.print(time));

                status.setBackground(context.getDrawable(R.drawable.badge_message_status_sent));
            } else {

                timeText.setVisibility(View.GONE);
                status.setBackground(context.getDrawable(R.drawable.badge_message_status_pending));
            }

            if (ravenMessage.getSeenAt() != null) {
                status.setBackground(context.getDrawable(R.drawable.badge_message_status_seen));
            }

            view.setOnClickListener(v -> {
                if (onClickListener != null)
                    onClickListener.onClickListener(v, position);
            });

            view.setOnLongClickListener(v -> {
                if (onLongClickListener != null)
                    onLongClickListener.onLongClickListener(v, position);
                return false;
            });
        }
    }


    public interface OnClickListener {
        void onClickListener(View view, int position);
    }

    public interface OnLongClickListener {
        void onLongClickListener(View view, int position);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    private void makeBanner(boolean showDate, LinearLayout bannerLayout, TextView bannerText, RavenMessage ravenMessage) {

        if (showDate) {

            bannerLayout.setVisibility(View.VISIBLE);
            bannerText.setVisibility(View.VISIBLE);

            String messageTime = ravenMessage.getTimestamp();
            if (messageTime == null)
                messageTime = ravenMessage.getLocalTimestamp();

            bannerText.setText(DateTimeFormat.forPattern("MMMM dd, yyyy").print(DateTime.parse(messageTime)));

        } else {
            bannerLayout.setVisibility(View.GONE);
            bannerText.setVisibility(View.GONE);
        }
    }
}
