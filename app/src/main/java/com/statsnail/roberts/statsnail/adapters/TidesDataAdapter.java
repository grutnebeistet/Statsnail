package com.statsnail.roberts.statsnail.adapters;

import android.content.Context;
import android.provider.ContactsContract;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.models.LocationData;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesDataAdapter extends RecyclerView.Adapter<TidesDataAdapter.TdViewHolder> {
    Context mContext;
    ArrayList<LocationData.Waterlevel> mData;
    DataSnapshot mSnapShot;

    public TidesDataAdapter(Context context, ArrayList<LocationData.Waterlevel> data) {
        mContext = context;
        mData = data;
        //mSnapShot = snapshot;
    }

    @Override
    public TdViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.tide_list_item, parent, false);
        return new TdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TdViewHolder holder, int position) {
        LocationData.Waterlevel waterlevel = mData.get(position);
        String flag = null;
        String time = null;
        String level = null;
/*
        DataSnapshot currentSnap = mSnapShot.child(String.valueOf(position));

        flag = currentSnap.child("flag").getValue().toString();
        time = currentSnap.child("time").getValue().toString();
        level = currentSnap.child("level").getValue().toString();*/
        flag = waterlevel.flag;
        time = waterlevel.dateTime;
        level = waterlevel.waterValue;

        if (position == 1 || position == 3) {
            holder.mDivider.setVisibility(View.INVISIBLE);
        }
        if (flag.equals("high"))
            holder.mFlagImg.setImageResource(R.drawable.high_tide);
        else if (flag.equals("low"))
            holder.mFlagImg.setImageResource(R.drawable.low_tide);
        holder.mFlag.setText(mContext.getString(R.string.flag_format, flag));
        holder.mTime.setText(Utils.getFormattedTime(time));
        holder.mLevel.setText(level + " cm");
    }

    @Override

    public int getItemCount() {
        //return (int) mSnapShot.getChildrenCount();
        return mData.size();
    }

    class TdViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tide_item_flag)
        TextView mFlag;
        @BindView(R.id.tide_item_time)
        TextView mTime;
        @BindView(R.id.tide_item_value)
        TextView mLevel;
        @BindView(R.id.divider_list_item)
        View mDivider;
        @BindView(R.id.tide_item_flag_img)
        ImageView mFlagImg;

        TdViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
