package com.stickercamera.app.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.agile.libads.ads.AdsWrapper;
import com.alibaba.fastjson.JSON;
import com.common.util.DataUtils;
import com.common.util.StringUtils;
import com.customview.LabelView;
import com.github.skykai.stickercamera.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.melnykov.fab.FloatingActionButton;
import com.stickercamera.App;
import com.stickercamera.AppConstants;
import com.stickercamera.app.camera.CameraManager;
import com.stickercamera.app.model.FeedItem;
import com.stickercamera.app.model.TagItem;
import com.stickercamera.base.BaseActivity;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Main interface
 * Created by sky on 2015/7/20.
 * Weibo: http://weibo.com/2030683111
 * Email: 1132234509@qq.com
 */
public class MainActivity extends BaseActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    @InjectView(R.id.fab)
    FloatingActionButton fab;
    @InjectView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @InjectView(R.id.adView)
    AdView adView;
    private List<FeedItem> feedList;
    private PictureAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        EventBus.getDefault().register(this);

        initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                requestPermission();
            } else {
                loadImage();
            }
        } else {
            loadImage();
        }

        String token = FirebaseInstanceId.getInstance().getToken();
        AdsWrapper adsWrapper = new AdsWrapper.Builder().with(MainActivity.this).addTestDeviceIds(new String[]{AdRequest.DEVICE_ID_EMULATOR, token}).build();
        adsWrapper.requestBannerAd(adView);


    }

    private void loadImage() {
        //Turns on the camera if there is no picture
        String str = DataUtils.getStringPreferences(App.getApp(), AppConstants.FEED_INFO);
        if (StringUtils.isNotEmpty(str)) {
            feedList = JSON.parseArray(str, FeedItem.class);
        }
        if (feedList == null) {
            CameraManager.getInst().openCamera(MainActivity.this);
        } else {
            mAdapter.setList(feedList);
        }
    }

    private boolean checkPermission() {
        int resultCamera = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        int resultWrite = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int resultPhoneState = ContextCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_STATE);
        return resultCamera == PackageManager.PERMISSION_GRANTED && resultWrite == PackageManager.PERMISSION_GRANTED && resultPhoneState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean externalAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean phoneStateAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && externalAccepted && phoneStateAccepted) {
                        loadImage();
                    } else {
                        Toast.makeText(this, "You have to give those permission for start app", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public void onEventMainThread(FeedItem feedItem) {
        if (feedList == null) {
            feedList = new ArrayList<FeedItem>();
        }
        feedList.add(0, feedItem);
        DataUtils.setStringPreferences(App.getApp(), AppConstants.FEED_INFO, JSON.toJSONString(feedList));
        mAdapter.setList(feedList);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        titleBar.hideLeftBtn();
        titleBar.hideRightBtn();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PictureAdapter();
        mRecyclerView.setAdapter(mAdapter);
        fab.setOnClickListener(v -> CameraManager.getInst().openCamera(MainActivity.this));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }


    // Photo adapter
    public class PictureAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<FeedItem> items = new ArrayList<FeedItem>();

        public void setList(List<FeedItem> list) {
            if (items.size() > 0) {
                items.clear();
            }
            items.addAll(list);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picture, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FeedItem feedItem = items.get(position);
            holder.picture.setImageBitmap(BitmapFactory.decodeFile(feedItem.getImgPath()));
            Log.d("ImagePath", feedItem.getImgPath());
            holder.setTagList(feedItem.getTagList());

            holder.share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, "Hey view/download this image");
                    String path = null;
                    try {
                        path = MediaStore.Images.Media.insertImage(getContentResolver(), feedItem.getImgPath(), "", null);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Uri screenshotUri = Uri.parse(path);

                    intent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                    intent.setType("image/*");
                    startActivity(Intent.createChooser(intent, "Share image via..."));
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            // Remove labels to avoid duplication when recycling
            holder.pictureLayout.removeViews(1, holder.pictureLayout.getChildCount() - 1);
            super.onViewRecycled(holder);
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            // There may be a problem with a delay of 200 ms to load in order to wait for the pictureLayout to already show getWidth on the screen just for the specific value
            holder.pictureLayout.getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (TagItem feedImageTag : holder.getTagList()) {
                        LabelView tagView = new LabelView(MainActivity.this);
                        tagView.init(feedImageTag);
                        tagView.draw(holder.pictureLayout,
                                (int) (feedImageTag.getX() * ((double) holder.pictureLayout.getWidth() / (double) 1242)),
                                (int) (feedImageTag.getY() * ((double) holder.pictureLayout.getWidth() / (double) 1242)),
                                feedImageTag.isLeft());
                        tagView.wave();
                    }
                }
            }, 200);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.pictureLayout)
        RelativeLayout pictureLayout;
        @InjectView(R.id.picture)
        ImageView picture;
        @InjectView(R.id.share)
        LinearLayout share;

        private List<TagItem> tagList = new ArrayList<>();

        public List<TagItem> getTagList() {
            return tagList;
        }

        public void setTagList(List<TagItem> tagList) {
            if (this.tagList.size() > 0) {
                this.tagList.clear();
            }
            this.tagList.addAll(tagList);
        }

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }
}
