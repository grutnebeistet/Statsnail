package com.statsnail.roberts.statsnail.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.fragments.TidesFragment;

import butterknife.BindView;
import butterknife.ButterKnife;


public class WindsDataAdapter extends RecyclerView.Adapter<WindsDataAdapter.TdViewHolder> {
    Context mContext;
    Cursor mCursor;

    public WindsDataAdapter(Context context) {
        mContext = context;
    }

    @Override
    public TdViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.wind_list_item, parent, false);
        return new TdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TdViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String time = mCursor.getString(TidesFragment.INDEX_WIND_TIME);
        String speed = mCursor.getString(TidesFragment.INDEX_WIND_SPEED);
        String winDir = mCursor.getString(TidesFragment.INDEX_WIND_DIR);

        int dirImg = 0;
        switch (winDir) {
            case "N":
                dirImg = R.drawable.north;
                break;
            case "E":
                dirImg = R.drawable.east;
                break;
            case "S":
                dirImg = R.drawable.south;
                break;
            case "W":
                dirImg = R.drawable.west;
                break;
            case "NE":
                dirImg = R.drawable.north_east;
                break;
            case "NW":
                dirImg = R.drawable.north_west;
                break;
            case "SE":
                dirImg = R.drawable.south_east;
                break;
            case "SW":
                dirImg = R.drawable.south_west;
                break;

        }

        holder.mSpeed.setText(speed + " m/s");
        holder.mTime.setText(time);
        holder.mWinDirImg.setImageResource(dirImg);
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
        @BindView(R.id.wind_speed)
        TextView mSpeed;
        @BindView(R.id.wind_time)
        TextView mTime;

 /*       @BindView(R.id.divider_list_item)
        View mDivider;*/
        @BindView(R.id.wind_dir_img)
        ImageView mWinDirImg;

        TdViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
