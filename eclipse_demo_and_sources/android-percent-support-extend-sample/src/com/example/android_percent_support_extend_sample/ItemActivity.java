package com.example.android_percent_support_extend_sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;


public class ItemActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(getIntent().getIntExtra("contentId", R.layout.activity_item));

        setTitle(getIntent().getStringExtra("title"));

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
