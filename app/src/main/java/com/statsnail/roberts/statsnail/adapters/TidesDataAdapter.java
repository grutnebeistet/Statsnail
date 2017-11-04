package com.statsnail.roberts.statsnail.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.fragments.TidesFragment;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesDataAdapter extends RecyclerView.Adapter<TidesDataAdapter.TdViewHolder> {
    Context mContext;
    ArrayList<TidesData.Waterlevel> mData;
    DataSnapshot mSnapShot;
    Cursor mCursor;

    public TidesDataAdapter(Context context) {
        mContext = context;
        //mData = data;
        //mSnapShot = snapshot;
    }

    @Override
    public TdViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.tide_list_item, parent, false);
        return new TdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TdViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String flag = null;
        String time = null;
        String level = null;
        
        flag = mCursor.getString(TidesFragment.INDEX_FLAG);
        time = mCursor.getString(TidesFragment.INDEX_LEVEL_TIME);
        level = (mContext.getString(R.string.level_format, mCursor.getString(TidesFragment.INDEX_TIDE_LEVEL)));

        if ((position % 2) != 0) {
            holder.mDivider.setVisibility(View.INVISIBLE);
        }
        if (flag.equals("high"))
            holder.mFlagImg.setImageResource(R.drawable.high_tide);
        else if (flag.equals("low"))
            holder.mFlagImg.setImageResource(R.drawable.low_tide);
        holder.mFlag.setText(mContext.getString(R.string.flag_format, flag));
        holder.mTime.setText(time);
        holder.mLevel.setText(level);
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
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
