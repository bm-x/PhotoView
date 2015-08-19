package com.example.bm.photoview;

import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bm.library.Info;
import com.bm.library.PhotoView;

public class PhotoBrowse extends AppCompatActivity {

    int[] imgs = new int[]{R.mipmap.aaa, R.mipmap.bbb, R.mipmap.ccc, R.mipmap.ddd, R.mipmap.ic_launcher, R.mipmap.image003};

    GridView gv;

    View mParent;
    View mBg;
    PhotoView mPhotoView;
    Info mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_browse);

        mParent = findViewById(R.id.parent);
        mBg = findViewById(R.id.bg);
        mPhotoView = (PhotoView) findViewById(R.id.img);
        gv = (GridView) findViewById(R.id.gv);
        gv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return imgs.length;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Log.i("bm", "getView ");
                PhotoView p = new PhotoView(PhotoBrowse.this);
                p.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().density * 100)));
                p.setScaleType(ImageView.ScaleType.CENTER_CROP);
                p.setImageResource(imgs[position]);
                // 把PhotoView当普通的控件把触摸功能关掉
                p.disenable();
                return p;
            }
        });

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PhotoView p = (PhotoView) view;
                mInfo = p.getInfo();

                mParent.setVisibility(View.VISIBLE);
                mBg.animate().alphaBy(1).start();
                mPhotoView.setImageResource(imgs[position]);
                mPhotoView.animaFrom(mInfo);
            }
        });

        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBg.animate().alphaBy(-1).start();
                mPhotoView.animaTo(mInfo, new Runnable() {
                    @Override
                    public void run() {
                        mParent.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
}
