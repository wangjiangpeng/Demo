package com.wjp.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    OrderClass orders[] = { new OrderClass("camera", CameraActivity.class) };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        ListView lv = findViewById(R.id.main_list);
        lv.setAdapter(new ListAdapter());
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, orders[position].cls);
                startActivity(intent);
            }
        });
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
            tv.setTextSize(50);
            tv.setGravity(Gravity.CENTER);
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
