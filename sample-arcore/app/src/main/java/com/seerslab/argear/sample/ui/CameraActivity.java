package com.seerslab.argear.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.ARCoreSession;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.seerslab.argear.exceptions.InvalidContentsException;
import com.seerslab.argear.exceptions.NetworkException;
import com.seerslab.argear.exceptions.SignedUrlGenerationException;
import com.seerslab.argear.sample.AppConfig;
import com.seerslab.argear.sample.R;
import com.seerslab.argear.sample.api.ContentsResponse;
import com.seerslab.argear.sample.common.PermissionHelper;
import com.seerslab.argear.sample.data.BeautyItemData;
import com.seerslab.argear.sample.databinding.ActivityCameraBinding;
import com.seerslab.argear.sample.model.ItemModel;
import com.seerslab.argear.sample.network.DownloadAsyncResponse;
import com.seerslab.argear.sample.network.DownloadAsyncTask;
import com.seerslab.argear.sample.rendering.ScreenRenderer;
import com.seerslab.argear.sample.util.FileDeleteAsyncTask;
import com.seerslab.argear.sample.util.PreferenceUtil;
import com.seerslab.argear.sample.viewmodel.ContentsViewModel;
import com.seerslab.argear.session.ARGAuth;
import com.seerslab.argear.session.ARGContents;
import com.seerslab.argear.session.ARGFrame;
import com.seerslab.argear.session.ARGMedia;
import com.seerslab.argear.session.ARGSession;
import com.seerslab.argear.session.config.ARGCameraConfig;
import com.seerslab.argear.session.config.ARGConfig;
import com.seerslab.argear.session.config.ARGInferenceConfig;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private String mItemDownloadPath;
    private String mMediaPath;
    private String mVideoFilePath;
    private boolean mIsShooting = false;

    private boolean mFilterVignette = false;
    private boolean mFilterBlur = false;
    private int mFilterLevel = 100;
    private ItemModel mCurrentStickeritem = null;
    private boolean mHasTrigger = false;

    private FragmentManager mFragmentManager;
    private FilterFragment mFilterFragment;
    private StickerFragment mStickerFragment;
    private BeautyFragment mBeautyFragment;
    private BulgeFragment mBulgeFragment;

    private ContentsViewModel mContentsViewModel;
    private BeautyItemData mBeautyItemData;

    private ActivityCameraBinding mDataBinding;
    private Toast mTriggerToast = null;

    private ARGSession mARGSession;
    private ARGMedia mARGMedia;
    private ScreenRenderer mScreenRenderer = new ScreenRenderer();

    private final ArrayList<FloatBuffer> verticesList = new ArrayList<>();
    private final ArrayList<float []> poseMatrixList = new ArrayList<>();

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private Size mTextureSize;
    private ARCoreSession mARCoreSession;

    float[] mProjectionMatrix = new float[16];
    float[] mViewMatrix = new float[16];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_camera);

        // Set up renderer.
        mDataBinding.surfaceview.setPreserveEGLContextOnPause(true);
        mDataBinding.surfaceview.setEGLContextClientVersion(2);
        mDataBinding.surfaceview.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mDataBinding.surfaceview.setRenderer(this);
        mDataBinding.surfaceview.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mDataBinding.surfaceview.setWillNotDraw(false);

        mBeautyItemData = new BeautyItemData();
        mContentsViewModel = new ViewModelProvider(this).get(ContentsViewModel.class);
        mContentsViewModel.getContents().observe(this, new Observer<ContentsResponse>() {
            @Override
            public void onChanged(ContentsResponse contentsResponse) {
                if (contentsResponse == null) return;
                setLastUpdateAt(CameraActivity.this, contentsResponse.lastUpdatedAt);
            }
        });

        mFragmentManager = getSupportFragmentManager();
        mFilterFragment = new FilterFragment();
        mStickerFragment = new StickerFragment();
        mBeautyFragment = new BeautyFragment();
        mBulgeFragment = new BulgeFragment();

        mItemDownloadPath = getFilesDir().getAbsolutePath();
        mMediaPath = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/ARGEAR";
        File dir = new File(mMediaPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        createSession();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.hasPermission(this)) {
            if (PermissionHelper.shouldShowRequestPermissionRationale(this)) {
                mDataBinding.getRoot().setVisibility(View.GONE);
                Toast.makeText(this, "Please check your permissions!", Toast.LENGTH_SHORT).show();
                return;
            }
            PermissionHelper.requestPermission(this);
            return;
        }

        mARGSession.resume();

        mARCoreSession.resume(this);

        mDataBinding.surfaceview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mARGSession != null) {
            mARGSession.pause();
        }

        mDataBinding.surfaceview.onPause();

        mARCoreSession.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDataBinding.surfaceview.onPause();

        mARGSession.destroy();
    }

    @Override
    public void onBackPressed() {

        if (mDataBinding.moreLayout.getRoot().getVisibility() == View.VISIBLE) {
            mDataBinding.moreLayout.getRoot().setVisibility(View.GONE);
            return;
        }

        if ((mFilterFragment != null && mFilterFragment.isAdded())
                || (mStickerFragment != null && mStickerFragment.isAdded())
                || (mBeautyFragment != null && mBeautyFragment.isAdded())
                || (mBulgeFragment != null && mBulgeFragment.isAdded())
        ) {
            mDataBinding.functionsLayout.setVisibility(View.VISIBLE);
            mDataBinding.shutterLayout.setVisibility(View.VISIBLE);
        }

        if (mBeautyFragment != null && mBeautyFragment.isAdded()) {
            closeBeauty();
        }

        super.onBackPressed();
    }

    public void onClickButtons(View v) {
        switch (v.getId()) {
            case R.id.more_button: {
                if (mDataBinding.moreLayout.getRoot().getVisibility() == View.GONE) {
                    mDataBinding.moreLayout.getRoot().setVisibility(View.VISIBLE);
                } else {
                    mDataBinding.moreLayout.getRoot().setVisibility(View.GONE);
                }
                break;
            }
            case R.id.debug_landmark_checkbox:
            case R.id.debug_rect_checkbox:
                setDrawLandmark(mDataBinding.moreLayout.debugLandmarkCheckbox.isChecked(),
                        mDataBinding.moreLayout.debugRectCheckbox.isChecked());
                break;
            case R.id.sticker_button:
                showStickers();
                break;
            case R.id.filter_button:
                showFilters();
                break;
            case R.id.beauty_button:
                showBeauty();
                break;
            case R.id.bulge_button:
                showBulge();
                break;
             case R.id.shutter_button: {
//                if (mDataBinding.shutterPhotoButton.getViewSelected()) {
//                    mIsShooting = true;
//                    mDataBinding.shutterButton.setChecked(false);
//                } else {
//                    if (!mDataBinding.shutterButton.isChecked()) {
//                        stopRecording();
//                    } else {
//                        startRecording();
//                    }
//                }
                break;
            }
            case R.id.shutter_photo_button: {
                mDataBinding.shutterPhotoButton.setViewSelected(true);
                mDataBinding.shutterVideoButton.setViewSelected(false);
                mDataBinding.shutterButton.setBackgroundResource(R.drawable.btn_shutter_photo_blue);
                break;
            }
            case R.id.shutter_video_button: {
                mDataBinding.shutterPhotoButton.setViewSelected(false);
                mDataBinding.shutterVideoButton.setViewSelected(true);
                mDataBinding.shutterButton.setBackgroundResource(R.drawable.btn_shutter_video_blue);
                break;
            }
            case R.id.camera_switch_button:
                if (mARCoreSession.isAugmentedFaceMode()) {
                    mDataBinding.surfaceview.setOnTouchListener(mARCoreSession.tapHelper);
                    clearStickers();
                    clearFilter();
                    mARGSession.pause();
                } else {
                    // Set up tap listener.
                    mDataBinding.surfaceview.setOnTouchListener(null);
                    mARGSession.resume();
                }
                mDataBinding.surfaceview.onPause();
                mARCoreSession.changeMode(this);
                mDataBinding.surfaceview.onResume();
                break;
        }
    }

    private void createSession() {

        // create ARCore session
        mARCoreSession = new ARCoreSession(this);

        // create ARGear session
        ARGConfig argConfig
                = new ARGConfig(AppConfig.API_URL, AppConfig.API_KEY, AppConfig.SECRET_KEY, AppConfig.AUTH_KEY);
        mARGSession = new ARGSession(this, argConfig);
        //mARGMedia = new ARGMedia(mARGSession);

        setBeauty(mBeautyItemData.getBeautyValues());
    }

    public BeautyItemData getBeautyItemData() {
        return mBeautyItemData;
    }

    public void updateTriggerStatus(final int triggerstatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TRIGGER_MOUTH_FLAG       = (1 << 0)
                // TRIGGER_HEAD_FLAG        = (1 << 1)
                // TRIGGER_DELAY_FLAG       = (1 << 2)
                // TRIGGER_BLINK_EYES_FLAG  = (1 << 3)
                if (mCurrentStickeritem != null && mHasTrigger) {
                    String strTrigger = null;
                    if ((triggerstatus & 1) != 0) {
                        strTrigger = "Open your mouth.";
                    } else if ((triggerstatus & 2) != 0) {
                        strTrigger = "Move your head side to side.";
                    } else if ((triggerstatus & 8) != 0) {
                        strTrigger = "Blink your eyes.";
                    } else {
                        if (mTriggerToast != null) {
                            mTriggerToast.cancel();
                            mTriggerToast = null;
                        }
                    }

                    if (strTrigger != null) {
                        mTriggerToast = Toast.makeText(CameraActivity.this, strTrigger, Toast.LENGTH_SHORT);
                        mTriggerToast.setGravity(Gravity.CENTER, 0, 0);
                        mTriggerToast.show();
                        mHasTrigger = false;
                    }
                }
            }
        });
    }

    private void showSlot(Fragment fragment) {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.slot_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void showStickers(){
        showSlot(mStickerFragment);
        clearBulge();
        mDataBinding.functionsLayout.setVisibility(View.GONE);
        mDataBinding.shutterLayout.setVisibility(View.GONE);
    }

    private void showFilters(){
        showSlot(mFilterFragment);
        mDataBinding.functionsLayout.setVisibility(View.GONE);
        mDataBinding.shutterLayout.setVisibility(View.GONE);
    }

    private void showBeauty() {
        mDataBinding.functionsLayout.setVisibility(View.GONE);
        mDataBinding.shutterLayout.setVisibility(View.GONE);

        clearStickers();
        clearBulge();

        Bundle args = new Bundle();
        args.putSerializable(BeautyFragment.BEAUTY_PARAM1, ARGFrame.Ratio.RATIO_FULL);
        mBeautyFragment.setArguments(args);
        showSlot(mBeautyFragment);
    }

    private void closeBeauty() {

    }

    private void showBulge() {
        mDataBinding.functionsLayout.setVisibility(View.GONE);
        mDataBinding.shutterLayout.setVisibility(View.GONE);

        clearStickers();

        showSlot(mBulgeFragment);
    }

    public void clearBulge() {
        mARGSession.contents().clear(ARGContents.Type.Bulge);
    }

    public void setItem(ARGContents.Type type, String path, ItemModel itemModel) {

        mCurrentStickeritem = null;
        mHasTrigger = false;

        mARGSession.contents().setItem(type, path, itemModel.uuid, new ARGContents.Callback() {
            @Override
            public void onSuccess() {
                if (type == ARGContents.Type.ARGItem) {
                    mCurrentStickeritem = itemModel;
                    mHasTrigger = itemModel.hasTrigger;
                }
            }

            @Override
            public void onError(Throwable e) {
                mCurrentStickeritem = null;
                mHasTrigger = false;
                if (e instanceof InvalidContentsException) {
                    Log.e(TAG, "InvalidContentsException");
                }
            }
        });
    }

    public void setSticker(ItemModel item) {
        String filePath = mItemDownloadPath + "/" + item.uuid;
        if (getLastUpdateAt(CameraActivity.this) > getStickerUpdateAt(CameraActivity.this, item.uuid)) {
            new FileDeleteAsyncTask(new File(filePath), new FileDeleteAsyncTask.OnAsyncFileDeleteListener() {
                @Override
                public void processFinish(Object result) {
                    Log.d(TAG, "file delete success!");

                    setStickerUpdateAt(CameraActivity.this, item.uuid, getLastUpdateAt(CameraActivity.this));
                    requestSignedUrl(item, filePath, true);
                }
            }).execute();
        } else {
            if (new File(filePath).exists()) {
                setItem(ARGContents.Type.ARGItem, filePath, item);
            } else {
                requestSignedUrl(item, filePath, true);
            }
        }
    }

    public void clearStickers() {
        mCurrentStickeritem = null;
        mHasTrigger = false;

        mARGSession.contents().clear(ARGContents.Type.ARGItem);
    }

    public void setFilter(ItemModel item) {

        String filePath = mItemDownloadPath + "/" + item.uuid;
        if (getLastUpdateAt(CameraActivity.this) > getFilterUpdateAt(CameraActivity.this, item.uuid)) {
            new FileDeleteAsyncTask(new File(filePath), new FileDeleteAsyncTask.OnAsyncFileDeleteListener() {
                @Override
                public void processFinish(Object result) {
                    Log.d(TAG, "file delete success!");

                    setFilterUpdateAt(CameraActivity.this, item.uuid, getLastUpdateAt(CameraActivity.this));
                    requestSignedUrl(item, filePath, false);
                }
            }).execute();
        } else {
            if (new File(filePath).exists()) {
                setItem(ARGContents.Type.FilterItem, filePath, item);
            } else {
                requestSignedUrl(item, filePath, false);
            }
        }
    }

    public void clearFilter() {
        mARGSession.contents().clear(ARGContents.Type.FilterItem);
    }

    public void setFilterStrength(int strength) {
        if ((mFilterLevel + strength) < 100 && (mFilterLevel + strength) > 0) {
            mFilterLevel += strength;
        }
        mARGSession.contents().setFilterLevel(mFilterLevel);
    }

    public void setVignette() {
        mFilterVignette = !mFilterVignette;
        mARGSession.contents().setFilterOption(ARGContents.FilterOption.VIGNETTING, mFilterVignette);
    }

    public void setBlurVignette() {
        mFilterBlur = !mFilterBlur;
        mARGSession.contents().setFilterOption(ARGContents.FilterOption.BLUR, mFilterBlur);
    }

    public void setBeauty(float[] params) {
        mARGSession.contents().setBeauty(params);
    }

    public void setBulgeFunType(int type) {
        ARGContents.BulgeType bulgeType = ARGContents.BulgeType.NONE;
        switch (type) {
            case 1:
                bulgeType = ARGContents.BulgeType.FUN1;
                break;
            case 2:
                bulgeType = ARGContents.BulgeType.FUN2;
                break;
            case 3:
                bulgeType = ARGContents.BulgeType.FUN3;
                break;
            case 4:
                bulgeType = ARGContents.BulgeType.FUN4;
                break;
            case 5:
                bulgeType = ARGContents.BulgeType.FUN5;
                break;
            case 6:
                bulgeType = ARGContents.BulgeType.FUN6;
                break;
        }
        mARGSession.contents().setBulge(bulgeType);
    }

    private void setDrawLandmark(boolean landmark, boolean faceRect) {

        EnumSet<ARGInferenceConfig.Debug> set = EnumSet.of(ARGInferenceConfig.Debug.NONE);

        if(landmark){
            set.add(ARGInferenceConfig.Debug.FACE_LANDMARK);
        }

        if(faceRect) {
            set.add(ARGInferenceConfig.Debug.FACE_RECT_HW);
            set.add(ARGInferenceConfig.Debug.FACE_RECT_SW);
            set.add(ARGInferenceConfig.Debug.FACE_AXIES);
        }

        mARGSession.setDebugInference(set);
    }

    private void takePictureOnGlThread(int textureId) {
        mIsShooting = false;

        String path = mMediaPath + "/" + System.currentTimeMillis() + ".jpg";
        mARGMedia.takePicture(textureId, path, ARGMedia.Ratio.RATIO_16_9);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"+path)));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "captured a photo.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CameraActivity.this, ImageViewerActivity.class);
                Bundle b = new Bundle();
                b.putString(ImageViewerActivity.INTENT_IMAGE_URI, path);
                intent.putExtras(b);
                startActivity(intent);
            }
        });
    }

    private void startRecording() {

        int bitrate = 10 * 1000 * 1000; // 10M

        mVideoFilePath = mMediaPath + "/" + System.currentTimeMillis() + ".mp4";

        mARGMedia.initRecorder(mVideoFilePath, 1920, 1080, bitrate,
                false, false, false, ARGMedia.Ratio.RATIO_16_9);
        mARGMedia.startRecording();

        Toast.makeText(this, "start recording.", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        mDataBinding.shutterButton.setEnabled(false);
        mARGMedia.stopRecording();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"+mVideoFilePath)));

                Toast.makeText(CameraActivity.this, "stop recording.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CameraActivity.this, PlayerActivity.class);
                Bundle b = new Bundle();
                b.putString(PlayerActivity.INTENT_URI, mVideoFilePath);
                intent.putExtras(b);
                startActivity(intent);

                mDataBinding.shutterButton.setEnabled(true);
            }
        }, 500);
    }

    private void setLastUpdateAt(Context context, long updateAt) {
        PreferenceUtil.putLongValue(context, AppConfig.USER_PREF_NAME, "ContentLastUpdateAt", updateAt);
    }

    private long getLastUpdateAt(Context context) {
        return PreferenceUtil.getLongValue(context, AppConfig.USER_PREF_NAME, "ContentLastUpdateAt");
    }

    private void setFilterUpdateAt(Context context, String itemId, long updateAt) {
        PreferenceUtil.putLongValue(context, AppConfig.USER_PREF_NAME_FILTER, itemId, updateAt);
    }

    private long getFilterUpdateAt(Context context, String itemId) {
        return PreferenceUtil.getLongValue(context, AppConfig.USER_PREF_NAME_FILTER, itemId);
    }

    private void setStickerUpdateAt(Context context, String itemId, long updateAt) {
        PreferenceUtil.putLongValue(context, AppConfig.USER_PREF_NAME_STICKER, itemId, updateAt);
    }

    private long getStickerUpdateAt(Context context, String itemId) {
        return PreferenceUtil.getLongValue(context, AppConfig.USER_PREF_NAME_STICKER, itemId);
    }


    // region - network
    private void requestSignedUrl(ItemModel item, String path, final boolean isArItem) {
        mDataBinding.progressBar.setVisibility(View.VISIBLE);
        mARGSession.auth().requestSignedUrl(item.zipFileUrl, item.title, item.type, new ARGAuth.Callback() {
            @Override
            public void onSuccess(String url) {
                requestDownload(path, url, item, isArItem);
            }

            @Override
            public void onError(Throwable e) {
                if (e instanceof SignedUrlGenerationException) {
                    Log.e(TAG, "SignedUrlGenerationException !! ");
                } else if (e instanceof NetworkException) {
                    Log.e(TAG, "NetworkException !!");
                }

                mDataBinding.progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void requestDownload(String targetPath, String url, ItemModel item, boolean isSticker) {
        new DownloadAsyncTask(targetPath, url, new DownloadAsyncResponse() {
            @Override
            public void processFinish(boolean result) {
                mDataBinding.progressBar.setVisibility(View.INVISIBLE);
                if (result) {
                    if (isSticker) {
                        setItem(ARGContents.Type.ARGItem, targetPath, item);
                    } else {
                        setItem(ARGContents.Type.FilterItem, targetPath, item);
                    }
                    Log.d(TAG, "download success!");
                } else {
                    Log.d(TAG, "download failed!");
                }
            }
        }).execute();
    }
    // endregion


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mARCoreSession.onSurfaceCreated(this, gl, config);
        mScreenRenderer.create(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
        mARCoreSession.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Frame frame = mARCoreSession.onDrawFrame(this, gl);

        if (frame == null || frame.getCamera() == null) return;

        if (frame.getTimestamp() == 0) return;

        if (mARCoreSession.isAugmentedFaceMode()) {
            verticesList.clear();
            poseMatrixList.clear();

            for (AugmentedFace face : mARCoreSession.getSession().getAllTrackables(AugmentedFace.class)) {
                if (face.getTrackingState() == TrackingState.TRACKING) {
                    FloatBuffer faceVertices = face.getMeshVertices();
                    verticesList.add(faceVertices);

                    float[] facePoseMatrix = new float[16];
                    Pose facePose = face.getCenterPose();
                    facePose.toMatrix(facePoseMatrix, 0);
                    poseMatrixList.add(facePoseMatrix);
                }
            }

            frame.getCamera().getProjectionMatrix(mProjectionMatrix, 0, 0.1f, 100.0f);
            frame.getCamera().getViewMatrix(mViewMatrix, 0);

            Size textureSize = mARCoreSession.getSession().getCameraConfig().getTextureSize();
            if (mTextureSize == null || !mTextureSize.equals(textureSize)) {
                mTextureSize = textureSize;
                mARGSession.setCameraConfig(new ARGCameraConfig(textureSize.getWidth(),
                        textureSize.getHeight(),
                        0,
                        0,
                        0,
                        true,
                        0));
            }

            mARGSession.applyAdditionalFaceInfo(verticesList, poseMatrixList, mProjectionMatrix, mViewMatrix);
            mARGSession.feedTexture(mARCoreSession.getTextureId(), mTextureSize);

            ARGFrame argFrame = mARGSession.drawFrame(gl, ARGFrame.Ratio.RATIO_FULL, mScreenWidth, mScreenHeight);
            mScreenRenderer.draw(argFrame, mScreenWidth, mScreenHeight, mTextureSize.getWidth(), mTextureSize.getHeight());

            if (mHasTrigger) updateTriggerStatus(argFrame.getItemTriggerFlag());
        }
    }
}
