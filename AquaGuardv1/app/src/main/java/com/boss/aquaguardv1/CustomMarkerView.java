package com.boss.aquaguardv1;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import java.util.ArrayList;

public class CustomMarkerView extends MarkerView {

    TextView tvContent;
    ArrayList<Entry> entries;
    ArrayList<String> adviceList;

    public CustomMarkerView(Context context, int layoutResource, ArrayList<Entry> entries, ArrayList<String> adviceList) {
        super(context, layoutResource);
        this.entries = entries;
        this.adviceList = adviceList;
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        String advice = adviceList.get(index);
        tvContent.setText("Rain: " + e.getY() + " mm\n" + advice);
        super.refreshContent(e, highlight);
    }
}
