package com.statsnail.roberts.statsnail.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.sheets.v4.model.ValueRange;

import com.statsnail.roberts.statsnail.activities.HarvestActivity;
import com.statsnail.roberts.statsnail.R;


public class HarvestLogAdapter extends RecyclerView.Adapter<HarvestLogAdapter.LogViewHolder> {
    static String TAG = HarvestLogAdapter.class.getSimpleName();
    private Context mContext;
    private ValueRange mValueRange;
    private Cursor mCursor;

    public HarvestLogAdapter(Context context) {
        mContext = context;
        //mValueRange = valueRange;

        Log.i(TAG, "HarvestAdapter count: " + getItemCount());
    }

    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_log_grading, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        //String row = mValueRange.getValues().get(position).toString();
        //Log.i(TAG, "Row: " + row);
        mCursor.moveToPosition(position);
        String name = mCursor.getString(HarvestActivity.INDEX_HARVEST_USER);
        int harvestID = mCursor.getInt(HarvestActivity.INDEX_HARVEST_ID);
        int isGraded = mCursor.getInt(HarvestActivity.INDEX_HARVEST_GRADED);
        String gradedDate = mCursor.getString(HarvestActivity.INDEX_HARVEST_DATE); // TODO GRADED DATE*
        String gradedBy = mCursor.getString(HarvestActivity.INDEX_HARVEST_GRADED);

        String graded;
        if (gradedBy != null){
            graded = " Graded";
            holder.tvLogGradedBy.setText(gradedBy);
            holder.tvLogGradedBy.setTextColor(ContextCompat.getColor(mContext, R.color.dim_text));
        }else {
            graded = " NOT Graded";
            holder.tvLogGradedBy.setText(graded);
            holder.tvLogGradedBy.setTextColor(Color.RED);
        }
        holder.tvLogItemNo.setText(""+harvestID); // TODO res string w/placeholder
        holder.tvLogDate.setText(gradedDate);
        Log.i(TAG, "adapter, name: " + name);

    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView tvLogItemNo;
        final TextView tvLogGradedBy;
        final TextView tvLogDate;
        LogViewHolder(View view) {
            super(view);

            tvLogItemNo = (TextView)view.findViewById(R.id.ll_harvest_no);
            tvLogDate = (TextView)view.findViewById(R.id.ll_harvest_date);
            tvLogGradedBy = (TextView)view.findViewById(R.id.li_log_graded);
        }
    }
    public void swapCursor(Cursor newData){
        mCursor = newData;
    }
}
