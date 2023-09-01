package com.wjp.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    OrderClass orders[] = {  };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        ListView lv = findViewById(R.id.main_list);
        lv.setAdapter(new ListAdapter());
    }

    private class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return orders.length;
        }

        @Override
        public OrderClass getItem(int position) {
            return orders[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(MainActivity.this);
            tv.setText(getItem(position).name);
            return tv;
        }
    }

    private static class OrderClass {
        String name;
        Class cls;

        OrderClass(String name, Class cls){
            this.name = name;
            this.cls = cls;
        }
    }
}
