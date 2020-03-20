package com.seerslab.argear.sample.ui

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.Camera
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.seerslab.argear.exceptions.InvalidContentsException
import com.seerslab.argear.exceptions.NetworkException
import com.seerslab.argear.exceptions.SignedUrlGenerationException
import com.seerslab.argear.sample.AppConfig
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.api.ContentsResponse
import com.seerslab.argear.sample.camera.ReferenceCamera
import com.seerslab.argear.sample.camera.ReferenceCamera1
import com.seerslab.argear.sample.camera.ReferenceCamera2
import com.seerslab.argear.sample.common.PermissionHelper
import com.seerslab.argear.sample.data.BeautyItemData
import com.seerslab.argear.sample.databinding.ActivityCameraBinding
import com.seerslab.argear.sample.model.ItemModel
import com.seerslab.argear.sample.network.DownloadAsyncResponse
import com.seerslab.argear.sample.network.DownloadAsyncTask
import com.seerslab.argear.sample.rendering.CameraTexture
import com.seerslab.argear.sample.rendering.ScreenRenderer
import com.seerslab.argear.sample.util.FileDeleteAsyncTask
import com.seerslab.argear.sample.util.FileDeleteAsyncTask.OnAsyncFileDeleteListener
import com.seerslab.argear.sample.util.PreferenceUtil
import com.seerslab.argear.sample.viewmodel.ContentsViewModel
import com.seerslab.argear.session.*
import com.seerslab.argear.session.ARGContents.BulgeType
import com.seerslab.argear.session.ARGMedia.Ratio
import com.seerslab.argear.session.config.ARGCameraConfig
import com.seerslab.argear.session.config.ARGConfig
import com.seerslab.argear.session.config.ARGInferenceConfig
import com.seerslab.argear.session.config.ARGInferenceConfig.Feature
import java.io.File
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraActivity : AppCompatActivity() {

    companion object {
        private val TAG = CameraActivity::class.java.simpleName
    }

    private lateinit var camera: ReferenceCamera
    private lateinit var glView: GLView
    private lateinit var screenRenderer: ScreenRenderer
    private lateinit var cameraTexture: CameraTexture

    private var screenRatio: ARGFrame.Ratio = ARGFrame.Ratio.RATIO_4_3

    private var itemDownloadPath: String? = null
    private var mediaPath: String? = null
    private var videoFilePath: String? = null

    private var isShooting = false
    private var filterVignette = false
    private var filterBlur = false
    private var filterLevel = 100

    private var currentFilterItemId: String? = null
    private var currentStickerItem: ItemModel? = null
    private var hasTrigger = false

    private var deviceWidth = 0
    private var deviceHeight = 0
    var gLViewWidth = 0
        private set
    var gLViewHeight = 0
        private set

    private lateinit var fragmentManager: FragmentManager
    private lateinit var filterFragment: FilterFragment
    private lateinit var stickerFragment: StickerFragment
    private lateinit var beautyFragment: BeautyFragment
    private lateinit var bulgeFragment: BulgeFragment

    private lateinit var contentsViewModel: ContentsViewModel
    val beautyItemData: BeautyItemData = BeautyItemData()

    private var triggerToast: Toast? = null

    private lateinit var argSession: ARGSession
    private lateinit var argMedia: ARGMedia

    private lateinit var dataBinding: ActivityCameraBinding

    private var isInitializeSdk: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val realSize = Point()
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getRealSize(realSize)

        deviceWidth = realSize.x
        deviceHeight = realSize.y
        gLViewWidth = realSize.x
        gLViewHeight = realSize.y

        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_camera)
        contentsViewModel = ViewModelProvider(this).get(ContentsViewModel::class.java)
        contentsViewModel.contents.observe(this, Observer<ContentsResponse?> {
            it?.let {setLastUpdateAt(this@CameraActivity, it.lastUpdatedAt)}
        })

        fragmentManager = supportFragmentManager
        filterFragment = FilterFragment()
        stickerFragment = StickerFragment()
        beautyFragment = BeautyFragment()
        bulgeFragment = BulgeFragment()

        itemDownloadPath = filesDir.absolutePath
        mediaPath = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DCIM + "/ARGEAR"
        val dir = File(mediaPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        initRatioUI()
    }

    override fun onResume() {
        super.onResume()
        // Check Permission and initialize SDK
        if (!isInitializeSdk) {
            if (hasPermission()) {
                initSdk()
                isInitializeSdk = true
            }
        }

        if (::camera.isInitialized && ::argSession.isInitialized) {
            camera.startCamera()
            argSession.resume()
            setGLViewSize(camera.previewSize)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::camera.isInitialized && ::argSession.isInitialized) {
            camera.stopCamera()
            argSession.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::camera.isInitialized && ::argSession.isInitialized) {
            camera.destroy()
            argSession.destroy()
        }
    }

    override fun onBackPressed() {
        if (dataBinding.moreLayout.root.visibility == View.VISIBLE) {
            dataBinding.moreLayout.root.visibility = View.GONE
            return
        }
        if (filterFragment.isAdded
            || stickerFragment.isAdded
            || beautyFragment.isAdded
            || bulgeFragment.isAdded
        ) {
            dataBinding.functionsLayout.visibility = View.VISIBLE
            dataBinding.shutterLayout.visibility = View.VISIBLE
        }
        if (beautyFragment.isAdded) {
            closeBeauty()
        }
        super.onBackPressed()
    }

    private fun hasPermission(): Boolean {
        if (!PermissionHelper.hasPermission(this)) {
            if (PermissionHelper.shouldShowRequestPermissionRationale(this)) {
                dataBinding.root.visibility = View.GONE
                Toast.makeText(this, "Please check your permissions!", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            PermissionHelper.requestPermission(this)
            return false
        }
        dataBinding.root.visibility = View.VISIBLE
        return true
    }

    private fun initSdk() {
        val config = ARGConfig(
            AppConfig.API_URL,
            AppConfig.API_KEY,
            AppConfig.SECRET_KEY,
            AppConfig.AUTH_KEY
        )

        val inferenceConfig: Set<Feature> = EnumSet.of(
            Feature.FACE_HIGH_TRACKING
        )

        // Session Init
        argSession = ARGSession(this, config, inferenceConfig)
        argMedia = ARGMedia(argSession)

        setBeauty(beautyItemData.getBeautyValues())

        // Init GLView, Camera
        initGLView()
        initCamera()
    }

    private fun initRatioUI() {
        if (screenRatio == ARGFrame.Ratio.RATIO_FULL) { // full
            dataBinding.topRatioView.visibility = View.INVISIBLE
            dataBinding.bottomRatioView.visibility = View.INVISIBLE
        } else if (screenRatio == ARGFrame.Ratio.RATIO_4_3) { // 3 : 4
            dataBinding.bottomRatioView.y = deviceWidth.toFloat() * 4 / 3
            dataBinding.bottomRatioView.layoutParams.height = deviceHeight - deviceWidth * 4 / 3
            dataBinding.topRatioView.visibility = View.INVISIBLE
            dataBinding.bottomRatioView.visibility = View.VISIBLE
        } else { // 1 : 1
            val viewTopRationHeight = (((deviceWidth * 4) / 3) - deviceWidth) / 2
            dataBinding.topRatioView.layoutParams.height = viewTopRationHeight
            dataBinding.bottomRatioView.y = viewTopRationHeight + deviceWidth.toFloat()
            dataBinding.bottomRatioView.layoutParams.height = deviceHeight - viewTopRationHeight + deviceWidth
            dataBinding.topRatioView.visibility = View.VISIBLE
            dataBinding.bottomRatioView.visibility = View.VISIBLE
        }

        if (dataBinding.topRatioView.visibility == View.VISIBLE) {
            dataBinding.topRatioView.requestLayout()
        }

        if (dataBinding.bottomRatioView.visibility == View.VISIBLE) {
            dataBinding.bottomRatioView.requestLayout()
        }

        if (beautyFragment.isAdded) {
            beautyFragment.updateUIStyle(screenRatio)
        }
    }

    private fun initGLView() {
        val cameraLayout: FrameLayout = dataBinding.cameraLayout
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        screenRenderer = ScreenRenderer()
        cameraTexture = CameraTexture()

        glView = GLView(this, glViewListener)
        glView.setZOrderMediaOverlay(true)
        cameraLayout.addView(glView, params)
    }

    private fun initCamera() {
        if (AppConfig.USE_CAMERA_API == 1) {
            camera = ReferenceCamera1(this, cameraListener)
        } else {
            camera = ReferenceCamera2(
                this,
                cameraListener,
                windowManager.defaultDisplay.rotation
            )
        }
    }

    fun onClickButtons(v: View) {
        when (v.id) {
            R.id.more_button -> {
                if (dataBinding.moreLayout.root.visibility == View.GONE) {
                    dataBinding.moreLayout.root.visibility = View.VISIBLE
                } else {
                    dataBinding.moreLayout.root.visibility = View.GONE
                }
            }
            R.id.ratio_full_radiobutton -> {
                screenRatio = ARGFrame.Ratio.RATIO_FULL
                setGLViewSize(camera.previewSize)
                initRatioUI()
            }
            R.id.ratio43_radiobugtton -> {
                screenRatio = ARGFrame.Ratio.RATIO_4_3
                setGLViewSize(camera.previewSize)
                initRatioUI()
            }
            R.id.ratio11_radiobutton -> {
                screenRatio = ARGFrame.Ratio.RATIO_1_1
                setGLViewSize(camera.previewSize)
                initRatioUI()
            }
            R.id.debug_landmark_checkbox, R.id.debug_rect_checkbox -> setDrawLandmark(
                dataBinding.moreLayout.debugLandmarkCheckbox.isChecked,
                dataBinding.moreLayout.debugRectCheckbox.isChecked
            )
            R.id.sticker_button -> showStickers()
            R.id.filter_button -> showFilters()
            R.id.beauty_button -> showBeauty()
            R.id.bulge_button -> showBulge()
            R.id.shutter_button -> {
                if (dataBinding.shutterPhotoButton.viewSelected) {
                    isShooting = true
                    dataBinding.shutterButton.isChecked = false
                } else {
                    if (!dataBinding.shutterButton.isChecked()) {
                        stopRecording()
                    } else {
                        startRecording()
                    }
                }
            }
            R.id.shutter_photo_button -> {
                dataBinding.shutterPhotoButton.viewSelected = true
                dataBinding.shutterVideoButton.viewSelected = false
                dataBinding.shutterButton.setBackgroundResource(R.drawable.btn_shutter_photo_blue)
            }
            R.id.shutter_video_button -> {
                dataBinding.shutterPhotoButton.viewSelected = false
                dataBinding.shutterVideoButton.viewSelected = true
                dataBinding.shutterButton.setBackgroundResource(R.drawable.btn_shutter_video_blue)
            }
            R.id.camera_switch_button -> {
                argSession.pause()
                camera.changeCameraFacing()
                argSession.resume()
            }
        }
    }

    private fun setGLViewSize(cameraPreviewSize: IntArray?) {
        if (cameraPreviewSize == null) return

        val previewWidth = cameraPreviewSize[1]
        val previewHeight = cameraPreviewSize[0]

        if (screenRatio == ARGFrame.Ratio.RATIO_FULL) {
            gLViewHeight = deviceHeight
            gLViewWidth = (deviceHeight.toFloat() * previewWidth / previewHeight).toInt()
        } else {
            gLViewWidth = deviceWidth
            gLViewHeight = (deviceWidth.toFloat() * previewHeight / previewWidth).toInt()
        }

        if ((gLViewWidth != glView.viewWidth || gLViewHeight != glView.height)
        ) {
            glView.holder.setFixedSize(gLViewWidth, gLViewHeight)
        }
    }

    fun setMeasureSurfaceView(view: View) {
        if (view.parent is FrameLayout) {
            view.layoutParams = FrameLayout.LayoutParams(gLViewWidth, gLViewHeight)
        } else if (view.parent is RelativeLayout) {
            view.layoutParams = RelativeLayout.LayoutParams(gLViewWidth, gLViewHeight)
        }

        /* to align center */
        if (screenRatio == ARGFrame.Ratio.RATIO_FULL && gLViewWidth > deviceWidth) {
            view.x = (deviceWidth - gLViewWidth) / 2.toFloat()
        } else {
            view.x = 0.0f
        }
    }

    fun updateTriggerStatus(triggerstatus: Int) {
        runOnUiThread {
            // TRIGGER_MOUTH_FLAG       = (1 << 0)
            // TRIGGER_HEAD_FLAG        = (1 << 1)
            // TRIGGER_DELAY_FLAG       = (1 << 2)
            // TRIGGER_BLINK_EYES_FLAG  = (1 << 3)
            if (currentStickerItem != null && hasTrigger) {
                var strTrigger: String? = null
                if (triggerstatus and 1 != 0) {
                    strTrigger = "Open your mouth."
                } else if (triggerstatus and 2 != 0) {
                    strTrigger = "Move your head side to side."
                } else if (triggerstatus and 8 != 0) {
                    strTrigger = "Blink your eyes."
                } else {
                    if (triggerToast != null) {
                        triggerToast?.cancel()
                        triggerToast = null
                    }
                }
                if (strTrigger != null) {
                    triggerToast =
                        Toast.makeText(this@CameraActivity, strTrigger, Toast.LENGTH_SHORT)
                    triggerToast?.setGravity(Gravity.CENTER, 0, 0)
                    triggerToast?.show()
                    hasTrigger = false
                }
            }
        }
    }

    private fun showSlot(fragment: Fragment?) {
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.slot_container, fragment!!)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun showStickers() {
        showSlot(stickerFragment)
        dataBinding.functionsLayout.visibility = View.GONE
        dataBinding.shutterLayout.visibility = View.GONE
    }

    private fun showFilters() {
        showSlot(filterFragment)
        dataBinding.functionsLayout.visibility = View.GONE
        dataBinding.shutterLayout.visibility = View.GONE
    }

    private fun showBeauty() {
        dataBinding.functionsLayout.visibility = View.GONE
        dataBinding.shutterLayout.visibility = View.GONE

        argSession.contents().clear(ARGContents.Type.ARGItem)

        val args = Bundle()
        args.putSerializable(BeautyFragment.BEAUTY_PARAM1, screenRatio)
        beautyFragment.arguments = args

        showSlot(beautyFragment)
    }

    private fun closeBeauty() {
        currentStickerItem?.let {
            setItem(
                ARGContents.Type.ARGItem,
                itemDownloadPath + "/" + currentStickerItem?.uuid,
                it
            )
        }
    }

    private fun showBulge() {
        dataBinding.functionsLayout.visibility = View.GONE
        dataBinding.shutterLayout.visibility = View.GONE

        argSession.contents().clear(ARGContents.Type.ARGItem)
        showSlot(bulgeFragment)
    }

    fun closeBulge() {
        argSession.contents().clear(ARGContents.Type.Bulge)
        currentStickerItem?.let {
            setItem(
                ARGContents.Type.ARGItem,
                itemDownloadPath + "/" + currentStickerItem?.uuid,
                currentStickerItem
            )
        }
    }

    fun setItem(type: ARGContents.Type?, path: String?, itemModel: ItemModel?) {
        currentStickerItem = null
        hasTrigger = false

        if (type == null || path == null || itemModel == null) return
        argSession.contents().setItem(type, path, itemModel.uuid, object : ARGContents.Callback {
            override fun onSuccess() {
                if (type == ARGContents.Type.ARGItem) {
                    currentStickerItem = itemModel
                    hasTrigger = itemModel.hasTrigger
                }
            }

            override fun onError(e: Throwable) {
                if (e is InvalidContentsException) {
                    Log.e(TAG, "InvalidContentsException")
                }
            }
        })
    }

    fun setSticker(item: ItemModel) {
        val filePath = itemDownloadPath + "/" + item.uuid
        if (getLastUpdateAt(this@CameraActivity) > getStickerUpdateAt(this@CameraActivity, item.uuid ?: "")) {
            FileDeleteAsyncTask(File(filePath), object : OnAsyncFileDeleteListener {
                override fun processFinish(result: Any?) {
                    Log.d(TAG, "file delete success!")

                    setStickerUpdateAt(this@CameraActivity, item.uuid ?: "", getLastUpdateAt(this@CameraActivity))
                    requestSignedUrl(item, filePath, true)
                }
            }).execute()
        } else {
            if (File(filePath).exists()) {
                setItem(ARGContents.Type.ARGItem, filePath, item)
            } else {
                requestSignedUrl(item, filePath, true)
            }
        }
    }

    fun clearStickers() {
        currentStickerItem = null
        hasTrigger = false
        argSession.contents().clear(ARGContents.Type.ARGItem)
    }

    fun setFilter(item: ItemModel) {
        currentFilterItemId = item.uuid
        val filePath = itemDownloadPath + "/" + item.uuid

        if (getLastUpdateAt(this@CameraActivity) > getFilterUpdateAt(this@CameraActivity, item.uuid ?: "")) {
            FileDeleteAsyncTask(File(filePath), object : FileDeleteAsyncTask.OnAsyncFileDeleteListener {
                override fun processFinish(result: Any?) {
                    Log.d(TAG, "file delete success!")

                    setFilterUpdateAt(this@CameraActivity, item.uuid ?: "", getLastUpdateAt(this@CameraActivity))
                    requestSignedUrl(item, filePath, false)
                }
            }).execute()
        } else {
            if (File(filePath).exists()) {
                setItem(ARGContents.Type.FilterItem, filePath, item)
            } else {
                requestSignedUrl(item, filePath, false)
            }
        }
    }

    fun clearFilter() {
        currentFilterItemId = null
        argSession.contents().clear(ARGContents.Type.FilterItem)
    }

    fun setFilterStrength(strength: Int) {
        if (filterLevel + strength in 1..99) {
            filterLevel += strength
        }
        argSession.contents().setFilterLevel(filterLevel)
    }

    fun setVignette() {
        filterVignette = !filterVignette
        argSession.contents().setFilterOption(ARGContents.FilterOption.VIGNETTING, filterVignette)
    }

    fun setBlurVignette() {
        filterBlur = !filterBlur
        argSession.contents().setFilterOption(ARGContents.FilterOption.BLUR, filterBlur)
    }

    fun setBeauty(params: FloatArray?) {
        argSession.contents().setBeauty(params)
    }

    fun setBulgeFunType(type: Int) {
        var bulgeType = BulgeType.NONE
        when (type) {
            1 -> bulgeType = BulgeType.FUN1
            2 -> bulgeType = BulgeType.FUN2
            3 -> bulgeType = BulgeType.FUN3
            4 -> bulgeType = BulgeType.FUN4
            5 -> bulgeType = BulgeType.FUN5
            6 -> bulgeType = BulgeType.FUN6
        }
        argSession.contents().setBulge(bulgeType)
    }

    private fun setDrawLandmark(landmark: Boolean, faceRect: Boolean) {
        val set = EnumSet.of(
            ARGInferenceConfig.Debug.NONE
        )
        if (landmark) {
            set.add(ARGInferenceConfig.Debug.FACE_LANDMARK)
        }
        if (faceRect) {
            set.add(ARGInferenceConfig.Debug.FACE_RECT_HW)
            set.add(ARGInferenceConfig.Debug.FACE_RECT_SW)
            set.add(ARGInferenceConfig.Debug.FACE_AXIES)
        }
        argSession.setDebugInference(set)
    }

    private fun takePictureOnGlThread(textureId: Int) {
        isShooting = false

        val ratio: Ratio = when (screenRatio) {
            ARGFrame.Ratio.RATIO_FULL -> {
                Ratio.RATIO_16_9
            }
            ARGFrame.Ratio.RATIO_4_3 -> {
                Ratio.RATIO_4_3
            }
            else -> {
                Ratio.RATIO_1_1
            }
        }
        val path = mediaPath + "/" + System.currentTimeMillis() + ".jpg"
        argMedia.takePicture(textureId, path, ratio)
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://$path")))

        runOnUiThread {
            Toast.makeText(this@CameraActivity, "The file has been saved to your Gallery.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@CameraActivity, ImageViewerActivity::class.java)
            val b = Bundle()
            b.putString(ImageViewerActivity.INTENT_IMAGE_URI, path)
            intent.putExtras(b)
            startActivity(intent)
        }
    }

    private fun startRecording() {
        if (!::camera.isInitialized) return

        val bitrate = 10 * 1000 * 1000 // 10M
        val ratio: Ratio = when (screenRatio) {
            ARGFrame.Ratio.RATIO_FULL -> {
                Ratio.RATIO_16_9
            }
            ARGFrame.Ratio.RATIO_4_3 -> {
                Ratio.RATIO_4_3
            }
            else -> {
                Ratio.RATIO_1_1
            }
        }
        val previewSize: IntArray? = camera.previewSize
        if (previewSize == null) {
            Toast.makeText(this, "Error recording. : camera previewSize null", Toast.LENGTH_SHORT).show()
            return
        }
        videoFilePath = mediaPath + "/" + System.currentTimeMillis() + ".mp4"
        argMedia.initRecorder(
            videoFilePath, previewSize[0], previewSize[1], bitrate,
            false, false, false, ratio
        )
        argMedia.startRecording()
        Toast.makeText(this, "start recording.", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        dataBinding.shutterButton.isEnabled = false
        argMedia.stopRecording()

        Handler().postDelayed({
            sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$videoFilePath")
                )
            )
            Toast.makeText(this@CameraActivity, "The file has been saved to your Gallery.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@CameraActivity, PlayerActivity::class.java)
            val b = Bundle()
            b.putString(PlayerActivity.INTENT_URI, videoFilePath)
            intent.putExtras(b)
            startActivity(intent)

            dataBinding.shutterButton.isEnabled = true
        }, 500)
    }

    private fun setLastUpdateAt(
        context: Context,
        updateAt: Long
    ) {
        PreferenceUtil.putLongValue(
            context,
            AppConfig.USER_PREF_NAME,
            "ContentLastUpdateAt",
            updateAt
        )
    }

    private fun getLastUpdateAt(context: Context): Long {
        return PreferenceUtil.getLongValue(context, AppConfig.USER_PREF_NAME, "ContentLastUpdateAt")
    }

    private fun setFilterUpdateAt(context: Context, itemId: String, updateAt: Long) {
        PreferenceUtil.putLongValue(context, AppConfig.USER_PREF_NAME_FILTER, itemId, updateAt)
    }

    private fun getFilterUpdateAt(context: Context, itemId: String): Long {
        return PreferenceUtil.getLongValue(context, AppConfig.USER_PREF_NAME_FILTER, itemId)
    }

    private fun setStickerUpdateAt(context: Context, itemId: String, updateAt: Long) {
        PreferenceUtil.putLongValue(context, AppConfig.USER_PREF_NAME_STICKER, itemId, updateAt)
    }

    private fun getStickerUpdateAt(context: Context, itemId: String): Long {
        return PreferenceUtil.getLongValue(context, AppConfig.USER_PREF_NAME_STICKER, itemId)
    }

    // region - network
    private fun requestSignedUrl(
        item: ItemModel,
        path: String,
        isArItem: Boolean
    ) {
        dataBinding.progressBar.visibility = View.VISIBLE
        argSession.auth().requestSignedUrl(item.zipFileUrl, item.title, item.type, object : ARGAuth.Callback {
                override fun onSuccess(url: String) {
                    requestDownload(path, url, item, isArItem)
                }

                override fun onError(e: Throwable) {
                    if (e is SignedUrlGenerationException) {
                        Log.e(
                            TAG,
                            "SignedUrlGenerationException !! "
                        )
                    } else if (e is NetworkException) {
                        Log.e(TAG, "NetworkException !!")
                    }
                    dataBinding.progressBar.visibility = View.INVISIBLE
                }
            })
    }

    private fun requestDownload(
        targetPath: String,
        url: String,
        item: ItemModel,
        isSticker: Boolean
    ) {
        DownloadAsyncTask(targetPath, url, object : DownloadAsyncResponse {
            override fun processFinish(result: Boolean) {
                dataBinding.progressBar.visibility = View.INVISIBLE
                if (result) {
                    if (isSticker) {
                        setItem(ARGContents.Type.ARGItem, targetPath, item)
                    } else {
                        setItem(ARGContents.Type.FilterItem, targetPath, item)
                    }
                    Log.d(TAG, "download success!")
                } else {
                    Log.d(TAG, "download failed!")
                }
            }
        }).execute()
    }

    // endregion
    // region - GLViewListener
    private var glViewListener: GLView.GLViewListener = object : GLView.GLViewListener {
        override fun onSurfaceCreated(
            gl: GL10?,
            config: EGLConfig?
        ) {
            screenRenderer.create(gl, config)
            cameraTexture.createCameraTexture()
        }

        override fun onDrawFrame(gl: GL10?, width: Int?, height: Int?) {
//            if (cameraTexture.surfaceTexture == null) {
//                return
//            }
            camera.setCameraTexture(
                cameraTexture.textureId,
                cameraTexture.surfaceTexture
            )

            val localWidth = width ?: 0
            val localHeight = height ?: 0
            val frame = argSession.drawFrame(gl, screenRatio, localWidth, localHeight)
            frame?.let {
                screenRenderer.draw(it, localWidth, localHeight)
                if (hasTrigger) updateTriggerStatus(it.itemTriggerFlag)
                if (argMedia.isRecording) argMedia.updateFrame(it.textureId)
                if (isShooting) takePictureOnGlThread(it.textureId)

                // getRawData
                // val bf = frame.getRawData(0, false, false)
            }
        }
    }
    // endregion

    // region - CameraListener
    private var cameraListener: ReferenceCamera.CameraListener = object : ReferenceCamera.CameraListener {
        override fun setConfig(
            previewWidth: Int,
            previewHeight: Int,
            verticalFov: Float,
            horizontalFov: Float,
            orientation: Int,
            isFrontFacing: Boolean,
            fps: Float
        ) {
            argSession.setCameraConfig(
                ARGCameraConfig(
                    previewWidth,
                    previewHeight,
                    verticalFov,
                    horizontalFov,
                    orientation,
                    isFrontFacing,
                    fps
                )
            )
        }

        // region - for camera api 1
        override fun updateFaceRects(faces: Array<Camera.Face>?) {
            argSession.updateFaceRects(faces)
        }

        override fun feedRawData(data: ByteArray?) {
            argSession.feedRawData(data)
        }

        // endregion
        // region - for camera api 2
        override fun updateFaceRects(numFaces: Int, bbox: Array<IntArray>?) {
            argSession.updateFaceRects(numFaces, bbox)
        }

        override fun feedRawData(data: Image?) {
            argSession.feedRawData(data)
        } // endregion
    } // endregion
}