package com.stickercamera.app.camera.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.common.util.DistanceUtil;
import com.common.util.FileUtils;
import com.common.util.IOUtil;
import com.common.util.ImageLoaderUtils;
import com.common.util.ImageUtils;
import com.common.util.StringUtils;
import com.customview.CameraGrid;
import com.github.skykai.stickercamera.R;
import com.stickercamera.App;
import com.stickercamera.AppConstants;
import com.stickercamera.app.camera.CameraBaseActivity;
import com.stickercamera.app.camera.CameraManager;
import com.stickercamera.app.camera.util.CameraHelper;
import com.stickercamera.app.model.PhotoItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * 相机界面
 * Created by sky on 15/7/6.
 */
public class CameraActivity extends CameraBaseActivity {

    private CameraHelper mCameraHelper;
    private Camera.Parameters parameters = null;
    private Camera cameraInst = null;
    private Bundle bundle = null;
    private int photoWidth = DistanceUtil.getCameraPhotoWidth();
    private int photoNumber = 4;
    private int photoMargin = App.getApp().dp2px(1);
    private float pointX, pointY;
    static final int FOCUS = 1;            // Focus
    static final int ZOOM = 2;            // Zoom
    private int mode;                      //0 Yes Focus 1 is magnified
    private float dist;
    private int PHOTO_SIZE = 2000;
    private int mCurrentCameraId = 0;  //1Is the front 0 Is postpositioned
    private Handler handler = new Handler();

    @InjectView(R.id.masking)
    CameraGrid cameraGrid;
    @InjectView(R.id.photo_area)
    LinearLayout photoArea;
    @InjectView(R.id.panel_take_photo)
    View takePhotoPanel;
    @InjectView(R.id.takepicture)
    Button takePicture;
    @InjectView(R.id.flashBtn)
    ImageView flashBtn;
    @InjectView(R.id.change)
    ImageView changeBtn;
    @InjectView(R.id.back)
    ImageView backBtn;
    @InjectView(R.id.next)
    ImageView galleryBtn;
    @InjectView(R.id.focus_index)
    View focusIndex;
    @InjectView(R.id.surfaceView)
    SurfaceView surfaceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mCameraHelper = new CameraHelper(this);
        ButterKnife.inject(this);
        initView();
        initEvent();
    }

    private void initView() {
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        surfaceView.setFocusable(true);
        surfaceView.setBackgroundColor(TRIM_MEMORY_BACKGROUND);
        surfaceView.getHolder().addCallback(new SurfaceCallback());//for SurfaceView Of the handle to add a callback function

        //Set the camera interface, photo list, and the height of the camera layout (to ensure that the camera preview for the square)
        ViewGroup.LayoutParams layout = cameraGrid.getLayoutParams();
        layout.height = App.getApp().getScreenWidth();
        layout = photoArea.getLayoutParams();
        layout.height = DistanceUtil.getCameraPhotoAreaHeight();
        layout = takePhotoPanel.getLayoutParams();
        layout.height = App.getApp().getScreenHeight()
                - App.getApp().getScreenWidth()
                - DistanceUtil.getCameraPhotoAreaHeight();

        //Add a picture in the system album
        ArrayList<PhotoItem> sysPhotos = FileUtils.getInst().findPicsInDir(
                FileUtils.getInst().getSystemPhotoPath());
        int showNumber = sysPhotos.size() > photoNumber ? photoNumber
                : sysPhotos.size();
        for (int i = 0; i < showNumber; i++) {
            addPhoto(sysPhotos.get(showNumber - 1 - i));
        }
    }

    private void addPhoto(PhotoItem photoItem) {
        ImageView photo = new ImageView(this);
        if (StringUtils.isNotBlank(photoItem.getImageUri())) {
            ImageLoaderUtils.displayLocalImage(photoItem.getImageUri(), photo, null);
        } else {
            photo.setImageResource(R.drawable.default_img);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                photoWidth, photoWidth);
        params.leftMargin = photoMargin;
        params.rightMargin = photoMargin;
        params.gravity = Gravity.CENTER;
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setTag(photoItem.getImageUri());

        if (photoArea.getChildCount() >= photoNumber) {
            photoArea.removeViewAt(photoArea.getChildCount() - 1);
            photoArea.addView(photo, 0, params);
        } else {
            photoArea.addView(photo, 0, params);
        }
        photo.setOnClickListener(v -> {
            if (v instanceof ImageView && v.getTag() instanceof String) {
                CameraManager.getInst().processPhotoItem(CameraActivity.this,
                        new PhotoItem((String) v.getTag(), System.currentTimeMillis()));
            }
        });
    }

    private void initEvent() {
        // Take pictures
        takePicture.setOnClickListener(v -> {
            try {
                cameraInst.takePicture(null, null, new MyPictureCallback());
            } catch (Throwable t) {
                t.printStackTrace();
                toast("Camera failed. Please try again!", Toast.LENGTH_LONG);
                try {
                    cameraInst.startPreview();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

        });
        // flash
        flashBtn.setOnClickListener(v -> turnLight(cameraInst));
        //Front and rear camera switch
        boolean canSwitch = false;
        try {
            canSwitch = mCameraHelper.hasFrontCamera() && mCameraHelper.hasBackCamera();
        } catch (Exception e) {
            //Failed to get camera info
        }
        if (!canSwitch) {
            changeBtn.setVisibility(View.GONE);
        } else {
            changeBtn.setOnClickListener(v -> switchCamera());
        }
        //Jump to album
        galleryBtn.setOnClickListener(v -> startActivity(new Intent(CameraActivity.this, AlbumActivity.class)));
        //Back button
        backBtn.setOnClickListener(v -> finish());
        surfaceView.setOnTouchListener((v, event) -> {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                // Press the main point
                case MotionEvent.ACTION_DOWN:
                    pointX = event.getX();
                    pointY = event.getY();
                    mode = FOCUS;
                    break;
                // Secondary click
                case MotionEvent.ACTION_POINTER_DOWN:
                    dist = spacing(event);
                    // If the distance between two consecutive points is greater than 10, the multi-point mode is determined
                    if (spacing(event) > 10f) {
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = FOCUS;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == FOCUS) {
                        //pointFocus((int) event.getRawX(), (int) event.getRawY());
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            float tScale = (newDist - dist) / dist;
                            if (tScale < 0) {
                                tScale = tScale * 10;
                            }
                            addZoomIn((int) tScale);
                        }
                    }
                    break;
            }
            return false;
        });


        surfaceView.setOnClickListener(v -> {
            try {
                pointFocus((int) pointX, (int) pointY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
            layout.setMargins((int) pointX - 60, (int) pointY - 60, 0, 0);
            focusIndex.setLayoutParams(layout);
            focusIndex.setVisibility(View.VISIBLE);
            ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            sa.setDuration(800);
            focusIndex.startAnimation(sa);
            handler.postDelayed(() -> focusIndex.setVisibility(View.INVISIBLE), 800);
        });

        takePhotoPanel.setOnClickListener(v -> {
            //doNothing Prevent the focus frame from appearing in the photo area
        });

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent result) {
        if (requestCode == AppConstants.REQUEST_PICK && resultCode == RESULT_OK) {
            CameraManager.getInst().processPhotoItem(
                    CameraActivity.this,
                    new PhotoItem(result.getData().getPath(), System
                            .currentTimeMillis()));
        } else if (requestCode == AppConstants.REQUEST_CROP && resultCode == RESULT_OK) {
            Intent newIntent = new Intent(this, PhotoProcessActivity.class);
            newIntent.setData(result.getData());
            startActivity(newIntent);
        }
    }

    /**
     *
     The distance between two points
     */
    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
        //return FloatMath.sqrt(x * x + y * y);
    }

    //Zoom out
    int curZoomValue = 0;

    private void addZoomIn(int delta) {

        try {
            Camera.Parameters params = cameraInst.getParameters();
            Log.d("Camera", "Is support Zoom " + params.isZoomSupported());
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                params.setZoom(curZoomValue);
                cameraInst.setParameters(params);
            } else {
                cameraInst.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fixed focus of the code
    private void pointFocus(int x, int y) {
        cameraInst.cancelAutoFocus();
        parameters = cameraInst.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            showPoint(x, y);
        }
        cameraInst.setParameters(parameters);
        autoFocus();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showPoint(int x, int y) {
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areas = new ArrayList<Camera.Area>();
            // Xy transformation
            int rectY = -x * 2000 / App.getApp().getScreenWidth() + 1000;
            int rectX = y * 2000 / App.getApp().getScreenHeight() - 1000;

            int left = rectX < -900 ? -1000 : rectX - 100;
            int top = rectY < -900 ? -1000 : rectY - 100;
            int right = rectX > 900 ? 1000 : rectX + 100;
            int bottom = rectY > 900 ? 1000 : rectY + 100;
            Rect area1 = new Rect(left, top, right, bottom);
            areas.add(new Camera.Area(area1, 800));
            parameters.setMeteringAreas(areas);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            bundle = new Bundle();
            bundle.putByteArray("bytes", data); //Save picture byte data in bundle among，To achieve data exchange
            new SavePicTask(data).execute();
            camera.startPreview(); // After the photo shoot，Restart the preview
        }
    }

    private class SavePicTask extends AsyncTask<Void, Void, String> {
        private byte[] data;

        protected void onPreExecute() {
            showProgressDialog("Processing");
        }

        SavePicTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveToSDCard(data);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (StringUtils.isNotEmpty(result)) {
                dismissProgressDialog();
                    CameraManager.getInst().processPhotoItem(CameraActivity.this,
                            new PhotoItem(result, System.currentTimeMillis()));
            } else {
                toast("Camera failed,Please try again later!", Toast.LENGTH_LONG);
            }
        }
    }


    /*SurfaceCallback*/
    private final class SurfaceCallback implements SurfaceHolder.Callback {

        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                if (cameraInst != null) {
                    cameraInst.stopPreview();
                    cameraInst.release();
                    cameraInst = null;
                }
            } catch (Exception e) {
                //The camera is turned off
            }

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (null == cameraInst) {
                try {
                    cameraInst = Camera.open();
                    cameraInst.setPreviewDisplay(holder);
                    initCamera();
                    cameraInst.startPreview();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            autoFocus();
        }
    }

    //To achieve automatic focus
    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (cameraInst == null) {
                    return;
                }
                cameraInst.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();//To achieve the initialization of the camera parameters
                        }
                    }
                });
            }
        };
    }

    private Camera.Size adapterSize = null;
    private Camera.Size previewSize = null;

    private void initCamera() {
        parameters = cameraInst.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        //if (adapterSize == null) {
        setUpPicSize(parameters);
        setUpPreviewSize(parameters);
        //}
        if (adapterSize != null) {
            parameters.setPictureSize(adapterSize.width, adapterSize.height);
        }
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1 Continuous focus
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        setDispaly(parameters, cameraInst);
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraInst.startPreview();
        cameraInst.cancelAutoFocus();// 2 If continuous AF is to be achieved，This sentence must be added
    }

    private void setUpPicSize(Camera.Parameters parameters) {

        if (adapterSize != null) {
            return;
        } else {
            adapterSize = findBestPictureResolution();
            return;
        }
    }

    private void setUpPreviewSize(Camera.Parameters parameters) {

        if (previewSize != null) {
        } else {
            previewSize = findBestPreviewResolution();
        }
    }

    /**
     * Minimum resolution of the preview interface
     */
    private static final int MIN_PREVIEW_PIXELS = 480 * 320;
    /**
     * Maximum aspect ratio difference
     */
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private static final String TAG = "Camera";

    /**
     * Find the most appropriate preview interface resolution
     *
     * @return
     */
    private Camera.Size findBestPreviewResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize();

        List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return defaultPreviewResolution;
        }

        // According to the resolution from large to small sort
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        StringBuilder previewResolutionSb = new StringBuilder();
        for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions) {
            previewResolutionSb.append(supportedPreviewResolution.width).append('x').append(supportedPreviewResolution.height)
                    .append(' ');
        }
        Log.v(TAG, "Supported preview resolutions: " + previewResolutionSb);


        // Remove the resolution that does not meet the criteria
        double screenAspectRatio = (double) App.getApp().getScreenWidth()
                / (double) App.getApp().getScreenHeight();
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // Remove the resolution below the lower limit，Take as high a resolution as possible
            if (width * height < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            // In the camera resolution and screen resolution aspect ratio is not equal to the case，
            // Find the smallest set of resolutions
            // Because the camera resolution is width> height，We set the portrait mode，width<height
            // So here's a quick preview of the aspect ratio after comparison
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            // Find the screen resolution exactly match the preview interface resolution directly back
            if (maybeFlippedWidth == App.getApp().getScreenWidth()
                    && maybeFlippedHeight == App.getApp().getScreenHeight()) {
                return supportedPreviewResolution;
            }
        }

        // If no suitable pixel is found, and the candidate pixel is set, the largest proportion is not suitable for a low-profile machine
        if (!supportedPreviewResolutions.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            return largestPreview;
        }

        // Did not find the right one，It returns the default

        return defaultPreviewResolution;
    }

    private Camera.Size findBestPictureResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPictureSizes(); // At least one value is returned

        StringBuilder picResolutionSb = new StringBuilder();
        for (Camera.Size supportedPicResolution : supportedPicResolutions) {
            picResolutionSb.append(supportedPicResolution.width).append('x')
                    .append(supportedPicResolution.height).append(" ");
        }
        Log.d(TAG, "Supported picture resolutions: " + picResolutionSb);

        Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();
        Log.d(TAG, "default picture resolution " + defaultPictureResolution.width + "x"
                + defaultPictureResolution.height);

        // Sort
        List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(
                supportedPicResolutions);
        Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        // Remove the resolution that does not meet the criteria
        double screenAspectRatio = (double) App.getApp().getScreenWidth()
                / (double) App.getApp().getScreenHeight();
        Iterator<Camera.Size> it = sortedSupportedPicResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // In the camera resolution and screen resolution aspect ratio is not equal to the case，Find the smallest set of resolutions
            // Because the camera resolution is width> height，We set the portrait mode，width<height
            // So here you want to swap and then compare the aspect ratio
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }
        }

        //If not found the right one，And there are also candidate pixels，
        // For photos，Whichever is the greater，Rather than selecting the same screen resolution
        if (!sortedSupportedPicResolutions.isEmpty()) {
            return sortedSupportedPicResolutions.get(0);
        }

        //Not find the right to return to the default
        return defaultPictureResolution;
    }


    //Controls the correct orientation of the image
    private void setDispaly(Camera.Parameters parameters, Camera camera) {
        if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    // Implementation of the correct image display
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation",
                    new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "An image error occurred");
        }
    }


    /**
     * The photographed photos will be stored in the SD card
     *
     * @param data
     * @throws IOException
     */
    public String saveToSDCard(byte[] data) throws IOException {
        Bitmap croppedImage;

        //Get the image size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        PHOTO_SIZE = options.outHeight > options.outWidth ? options.outWidth : options.outHeight;
        int height = options.outHeight > options.outWidth ? options.outHeight : options.outWidth;
        options.inJustDecodeBounds = false;
        Rect r;
        if (mCurrentCameraId == 1) {
            r = new Rect(height - PHOTO_SIZE, 0, height, PHOTO_SIZE);
        } else {
            r = new Rect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
        }
        try {
            croppedImage = decodeRegionCrop(data, r);
        } catch (Exception e) {
            return null;
        }
        String imagePath = ImageUtils.saveToFile(FileUtils.getInst().getSystemPhotoPath(), true,
                croppedImage);
        croppedImage.recycle();
        return imagePath;
    }

    private Bitmap decodeRegionCrop(byte[] data, Rect rect) {

        InputStream is = null;
        System.gc();
        Bitmap croppedImage = null;
        try {
            is = new ByteArrayInputStream(data);
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);

            try {
                croppedImage = decoder.decodeRegion(rect, new BitmapFactory.Options());
            } catch (IllegalArgumentException e) {
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeStream(is);
        }
        Matrix m = new Matrix();
        m.setRotate(90, PHOTO_SIZE / 2, PHOTO_SIZE / 2);
        if (mCurrentCameraId == 1) {
            m.postScale(1, -1);
        }
        Bitmap rotatedImage = Bitmap.createBitmap(croppedImage, 0, 0, PHOTO_SIZE, PHOTO_SIZE, m, true);
        if (rotatedImage != croppedImage)
            croppedImage.recycle();
        return rotatedImage;
    }

    /**
     * Flash switch   On -> Off -> Auto
     *
     * @param mCamera
     */
    private void turnLight(Camera mCamera) {
        if (mCamera == null || mCamera.getParameters() == null
                || mCamera.getParameters().getSupportedFlashModes() == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        String flashMode = mCamera.getParameters().getFlashMode();
        List<String> supportedModes = mCamera.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_ON)) {// Off state
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            mCamera.setParameters(parameters);
            flashBtn.setImageResource(R.drawable.camera_flash_on);
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {//Open
            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                flashBtn.setImageResource(R.drawable.camera_flash_auto);
                mCamera.setParameters(parameters);
            } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                flashBtn.setImageResource(R.drawable.camera_flash_off);
                mCamera.setParameters(parameters);
            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            flashBtn.setImageResource(R.drawable.camera_flash_off);
        }
    }


    //Switch the front and rear camera
    private void switchCamera() {
        mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
        releaseCamera();
        Log.d("DDDD", "DDDD----mCurrentCameraId" + mCurrentCameraId);
        setUpCamera(mCurrentCameraId);
    }

    private void releaseCamera() {
        if (cameraInst != null) {
            cameraInst.setPreviewCallback(null);
            cameraInst.release();
            cameraInst = null;
        }
        adapterSize = null;
        previewSize = null;
    }

    /**
     * @param mCurrentCameraId2
     */
    private void setUpCamera(int mCurrentCameraId2) {
        cameraInst = getCameraInstance(mCurrentCameraId2);
        if (cameraInst != null) {
            try {
                cameraInst.setPreviewDisplay(surfaceView.getHolder());
                initCamera();
                cameraInst.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            toast("Switch failed. Please try again!", Toast.LENGTH_LONG);
        }
    }

    private Camera getCameraInstance(final int id) {
        Camera c = null;
        try {
            c = mCameraHelper.openCamera(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }
}