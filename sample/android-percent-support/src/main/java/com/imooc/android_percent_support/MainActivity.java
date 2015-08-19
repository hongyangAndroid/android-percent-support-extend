package com.imooc.android_percent_support;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity
{
    private ListView mListView;
    private LayoutInflater mInflater;

    private static final int LISTVIEW_POS = 6;

    private String[] mTitle = {
            "PercentLinearLayout",
            "PercentW or PercentH",
            "PercentRelativeLayout 1",
            "PercentFrameLayout",
            "PercentRelativeLayout 2",
            "PercentLinearLayout in ScrollView2",
            "PercentInListView",
            "PercentPadding"
    };
    private int[] mContentIds = {
            R.layout.view5,
            R.layout.view1,
            R.layout.view2,
            R.layout.view3,
            R.layout.view4,
            R.layout.view6,
            -1,
            R.layout.view7_padding,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        mInflater = LayoutInflater.from(this);
        mListView = (ListView) findViewById(R.id.id_listview);

        mListView.setAdapter(new ArrayAdapter<String>(this, -1, mTitle)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                if (convertView == null)
                {
                    convertView = mInflater.inflate(R.layout.item_category, parent, false);
                }
                TextView tv = (TextView) convertView.findViewById(R.id.id_title);
                tv.setText(getItem(position));
                return convertView;
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                switch (position)
                {
                    case LISTVIEW_POS:
                        Intent intent = new Intent(MainActivity.this, ListViewTestActivity.class) ;
                        startActivity(intent);
                        return ;
                }


                Intent intent = new Intent(MainActivity.this, ItemActivity.class);
                intent.putExtra("contentId", mContentIds[position]);
                intent.putExtra("title",mTitle[position]);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
