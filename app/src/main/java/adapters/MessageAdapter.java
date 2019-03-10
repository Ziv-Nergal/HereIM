package adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import database_classes.Message;
import gis.hereim.R;
import utils.TimeStampParser;

import static activities.MainActivity.sCurrentFirebaseUser;

public class MessageAdapter extends FirebaseRecyclerAdapter <Message, BaseViewHolder<Message>> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    public MessageAdapter(@NonNull FirebaseRecyclerOptions<Message> options) {
        super(options);
    }

    @Override
    public int getItemViewType(int position) {
        Message mCurrentMsg = getItem(position);
        // Determines whether the message is a sent or a received message
        return mCurrentMsg.getSenderId().equals(sCurrentFirebaseUser.getUid()) ? VIEW_TYPE_MESSAGE_SENT : VIEW_TYPE_MESSAGE_RECEIVED;

    }

    @NonNull
    @Override
    public BaseViewHolder<Message> onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        View view;
        BaseViewHolder<Message> viewHolder;

        switch (viewType){
            default:
            case VIEW_TYPE_MESSAGE_SENT:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_sent_message, viewGroup, false);
                viewHolder = new SentMessageHolder(view);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_received_message, viewGroup, false);
                viewHolder = new ReceivedMessageHolder(view);
                break;
        }

        return viewHolder;
    }

    @Override
    protected void onBindViewHolder(@NonNull BaseViewHolder<Message> holder, int position, @NonNull Message message) {
        holder.bindView(message);
    }

    private class ReceivedMessageHolder extends BaseViewHolder<Message> {

        private CircleImageView mUserPhoto;
        private TextView mSenderName;
        private TextView mMsgText;
        private TextView mMsgTime;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            mUserPhoto = itemView.findViewById(R.id.received_msg_cell_photo);
            mMsgText =  itemView.findViewById(R.id.received_msg_cell_body);
            mMsgTime =  itemView.findViewById(R.id.received_msg_cell_time);
            mSenderName =  itemView.findViewById(R.id.received_msg_cell_name);
        }

        @Override
        void bindView(final Message iMessage) {

            mMsgText.setText(iMessage.getMsgText());
            mMsgTime.setText(TimeStampParser.AccurateParse(iMessage.getTimeStamp()));
            mSenderName.setText(iMessage.getSenderName());
            mSenderName.setPaintFlags(mSenderName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            mSenderName.setTextColor(Color.parseColor(iMessage.getSenderColor()));

            Picasso.get().load(iMessage.getSenderPhotoUri()).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.img_blank_profile).into(mUserPhoto, new Callback() {
                @Override
                public void onSuccess() { }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(iMessage.getSenderPhotoUri()).placeholder(R.drawable.img_blank_profile).into(mUserPhoto);
                }
            });
        }
    }

    private class SentMessageHolder extends BaseViewHolder<Message> {

        private TextView mMsgText;
        private TextView mMsgTime;

        SentMessageHolder(View itemView) {
            super(itemView);
            mMsgText =  itemView.findViewById(R.id.sent_msg_cell_body);
            mMsgTime =  itemView.findViewById(R.id.sent_msg_cell_time);
        }

        @Override
        void bindView(Message iMessage) {
            mMsgText.setText(iMessage.getMsgText());
            mMsgTime.setText(TimeStampParser.AccurateParse(iMessage.getTimeStamp()));
        }
    }
}
