package com.nivida.smartnode.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nivida.smartnode.R;


/**
 * Created by Chintak Patel on 7/5/2016.
 */
public class CustomAdapter extends BaseAdapter {

    private Context context;
    String[] titles={"Contact Us","Share"};
    int[] symbols={R.drawable.contactus,R.drawable.share,R.drawable.logout};
    Typeface tf;



    public CustomAdapter(Context context){
        this.context=context;
        tf=Typeface.createFromAsset(context.getAssets(),"fonts/raleway.ttf");
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int position)
    {
        return titles[position];
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        View view=null;
        //if(convertView==null){
            LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view= inflater.inflate(R.layout.custom_drawer_item,parent,false);
        /*}
        *//*else{
            view=convertView;
        }*/

        final TextView txt_title=(TextView) view.findViewById(R.id.txt_title);
        final ImageView img_icon=(ImageView) view.findViewById(R.id.imgIcon);


        txt_title.setText(titles[position]);
        img_icon.setImageResource(symbols[position]);
        txt_title.setTypeface(tf);

        return view;
    }
}
