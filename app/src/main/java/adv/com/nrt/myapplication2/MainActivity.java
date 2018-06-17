package adv.com.nrt.myapplication2;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Random;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends Activity implements SocketConnection.SocketListener {
    private static final String TAG = "ShelterApp";

    private Context mContext;
    private SurfaceView mSurface;
    private MediaPlayer mp;
    private ImageView mAdvPic;
    private String playingUrl;
    //private int position = 0;
    public ArrayList<VideoPathQuery> p_video_path = new ArrayList<VideoPathQuery>();
    //p_video_path为视频地址存放bean，
    public int Video_index = 0;
    private boolean isStoped = false;
    private SocketConnection socketConnection = null;
    private long DOWNLOAD_MD_ID = 1;
    private long DOWNLOAD_DATA_JSON_ID = 2;
    private boolean isNeedUpdateMD = false;
    private boolean isNeedUpdateJSON = false;
    private static final int MSG_PLAY_NORMAL = 1;
    private static final int MSG_PLAY_NEXT = 2;
    private static final int MSG_PLAY_PICTRUE = 4;
    private static final int MSG_DOWNDOWN_JSON = 3;
    //private static final int MSG_RENAME_MD = 5;
    private static final int MSG_UPDATE_FILELIST = 6;
    //private static final int MSG_DOWNLOAD_DATA = 7;
    private static final int MSG_CHECK_MD = 8;
    private static final int MSG_DOWNLOAD_ALL = 9;
    private static final int MSG_CHECK_JSON = 10;
    private static final int MSG_UPDATE_PLAY_LIST = 11;
    private boolean isSurfaceCreated = false;
    private int changeDuration = Constants.CHANGE_DURATION;
    private int recvTimeout = Constants.RECEIVE_TIMEOUT;
    private boolean isRandom = Constants.IS_RANDOM;

    private JSONArray mNeedUPdateJSONArray = null;
    private int flagForPicture = 0;
    private boolean isFirstCreate = false;

    public class ConnectThread extends Thread{
        public void run() {
            ConnectedSocket();
        }
    }

    enum MyAction {
        DOWNLOAD_MD, DOWNLOAD_MD_TEMP, DOWNLOAD_DATA_JSON,
        DOWNLOAD_FILE, DEFAULT
    }

    private MyAction mAction = MyAction.DEFAULT;
    //当前视频播放到那里

    private static final int MSG_PLAYNEXT = 1;

    private void nextIndex() {
        if (isRandom) {
            Random random = new Random();
            Video_index = random.nextInt(p_video_path.size());
        } else {
            Video_index++;
        }
        if (Video_index == p_video_path.size()) {
            Video_index = 0;
        }
    }


    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage : msg : " + msg.what);
            switch (msg.what) {
                case MSG_PLAY_NEXT:
                    nextIndex();
                    play(p_video_path.get(Video_index).getVideo_url());
                    break;
                case MSG_PLAY_NORMAL:
                    if (p_video_path.size() == 0) {
                        break;
                    }
                    if (Video_index == p_video_path.size()) {
                        Video_index = 0;
                    }
                    play(p_video_path.get(Video_index).getVideo_url());
                    break;
                case MSG_PLAY_PICTRUE:
                    Video_index++;
                    if (Video_index == p_video_path.size()) {
                        Video_index = 0;
                    }
                    play(p_video_path.get(Video_index).getVideo_url());
                    break;

                case MSG_DOWNDOWN_JSON: {
                    dataJSONFileVerify();
                    break;
                }
                case MSG_DOWNLOAD_ALL: {
                    downLoadAllDataFiles();
                    break;

                }
                case MSG_UPDATE_FILELIST: {
                    initData();
                    break;
                }
                case MSG_CHECK_MD: {
                    mdsumFileCheck();
                    break;
                }
                case MSG_CHECK_JSON: {//download 更新的数据
                    dataJSONFileVerify();
                    break;
                }
                case MSG_UPDATE_PLAY_LIST: {
                    initData();
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private long lastTime = 0l;


    @Override
    public void onError() {
        Log.d(TAG, "Socket error, connected again");
        ConnectedSocket();
    }

    @Override
    public void onReceive(String s) {
        if (s == null || s.equals("advertise")) {
            // Log.d(TAG, "invalid msg ...");
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();
        //Log.d(TAG, "onReceive send message currentTimeMillis = " + currentTimeMillis
        //    + "; lastTime = " + lastTime);
        if (lastTime == 0l) {
            lastTime = currentTimeMillis;
        }
        if (currentTimeMillis - lastTime < recvTimeout && currentTimeMillis != lastTime) {
            return;
        }
        lastTime = currentTimeMillis;
        Log.d(TAG, "onReceive s = " + s);
        Log.d(TAG, "onReceive enter sync");
        synchronized (p_video_path) {
            if (p_video_path.size() == 0) {
                Log.e(TAG, "p_video_path is null");
                return;
            }
            //Video_index = Integer.parseInt(s);
            int id = Integer.parseInt(s);
//            Random random = new Random();
//            int id = random.nextInt(8);
            // Log.d(TAG, "onReceive id = " + id);
            for (int i = 0; i < p_video_path.size(); i++) {
                if (p_video_path.get(i).getID() == id) {
                    Video_index = i;
                }
            }


            try {
                Log.d(TAG, "onReceive send message Video_index = " + Video_index + " ad_id =" + id);
                myHandler.removeMessages(MSG_PLAY_NEXT);
                myHandler.removeMessages(MSG_PLAY_NORMAL);
                myHandler.sendEmptyMessage(MSG_PLAY_NORMAL);
            } catch (Exception e) {
                Log.e(TAG, "exception !!! ", e);
            }
        }
        Log.d(TAG, "onReceive exit sync");

    }


    private void vedioSurfaceView() {
        mSurface = (SurfaceView) findViewById(R.id.sf);
        mSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurface.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                Log.d(TAG, "start request permission");
                if (isFirstCreate) {
                    Log.d(TAG, "start play normal");
                    isFirstCreate = false;
                    isSurfaceCreated = true;
                    myHandler.sendEmptyMessage(MSG_PLAY_NORMAL);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                if(mediaPlayer.isPlaying()){
//                    mediaPlayer.stop();
//                }
                Log.d(TAG, "surface destroyed");
                isSurfaceCreated = false;
            }
        });

        mp = new MediaPlayer();
    }

    private void play(String uri) {
        Log.d(TAG, "start play ,uri is" + uri.toString());
        String filepath = uri;
        if (filepath != null && !filepath.equals("")) {
            if (filepath.endsWith("MP4") || filepath.endsWith("mp4") || filepath.endsWith("avi")) {
                Log.d(TAG, "start play mp4");
                myHandler.removeMessages(MSG_PLAY_NEXT);
                mSurface.setVisibility(View.VISIBLE);
                mAdvPic.setVisibility(View.GONE);
                if (playingUrl != filepath) {
                    playVideo(filepath);
                    playingUrl = filepath;
                }

            } else if (filepath.endsWith("png") || filepath.endsWith("jpg") ||
                    filepath.endsWith("PNG") || filepath.endsWith("JPG")) {
                Log.d(TAG, "start play png");
                mSurface.setVisibility(View.GONE);
                mAdvPic.setVisibility(View.VISIBLE);
                if (mp != null && mp.isPlaying() == true) {
                    mp.stop();
                }
                boolean ret = playPicture(filepath);

                playingUrl = filepath;
                myHandler.removeMessages(MSG_PLAY_PICTRUE);
                myHandler.removeMessages(MSG_PLAY_NEXT);
                if (ret == true) {
                    myHandler.sendEmptyMessageDelayed(MSG_PLAY_NEXT, changeDuration);
                } else {
                    myHandler.sendEmptyMessage(MSG_PLAY_NEXT);
                }
                flagForPicture = Video_index;
            } else {
                //do nothing or can show default adv
            }
        }
    }

    private boolean playPicture(String filePath) {
        Bitmap bitmap = null;
        FileInputStream fis = null;


        try {
            fis = new FileInputStream(filePath);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            //这个isjustdecodebounds很重要
            opt.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(filePath, opt);

            //获取到这个图片的原始宽度和高度
            int picWidth = opt.outWidth;
            int picHeight = opt.outHeight;

            //获取屏的宽度和高度
            WindowManager windowManager = getWindowManager();
            Display display = windowManager.getDefaultDisplay();
            int screenWidth = display.getWidth();
            int screenHeight = display.getHeight();

            //isSampleSize是表示对图片的缩放程度，比如值为2图片的宽度和高度都变为以前的1/2
            opt.inSampleSize = 1;
            //根据屏的大小和图片大小计算出缩放比例
            if (picWidth > picHeight) {
                if (picWidth > screenWidth)
                    opt.inSampleSize = picWidth / screenWidth;
            } else {
                if (picHeight > screenHeight)

                    opt.inSampleSize = picHeight / screenHeight;
            }
            Log.d(TAG, "picWidth = " + picWidth + "picH =" + picHeight + "opt.inSampleSize** =" + opt.inSampleSize);

            //这次再真正地生成一个有像素的，经过缩放了的bitmap
            opt.inJustDecodeBounds = false;
            //opt.inTempStorage=new byte[1920*1080*4];

            Drawable dr = mAdvPic.getDrawable();
            if (dr != null) {
                Bitmap preBmp = ((BitmapDrawable) dr).getBitmap();
                if (preBmp != null) {
                    Log.d(TAG, "release the bitmap");
                    mAdvPic.setImageBitmap(null);
                    mAdvPic.setImageDrawable(null);
                    preBmp.recycle();
                    preBmp = null;
                } else {
                    Log.d(TAG, "preBmp =null");
                }
            }
            try {
                bitmap = BitmapFactory.decodeStream(fis, null, opt);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                System.gc();
            }
            opt = null;
            if (bitmap!= null) {
                mAdvPic.setImageBitmap(bitmap);
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Log.d(TAG, "playpic finally");
                if (fis != null) {
                    fis.close();
                    fis = null;

                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 开启播放
     * [java] view plain copy
     */
    private void playVideo(String Url) {
        Log.d(TAG, "playVideo: Url = " + Url);
        try {
            synchronized (mp) {
                if (null == mp) {
                    return;
                }
                mp.reset();
                mp.setDataSource(Url);//设置路径
                mp.setDisplay(mSurface.getHolder());//设置video影片以surfaceviewholder播放
                mp.setVolume(100, 100);
                mp.prepare();
                mp.start();
                Log.e(TAG, "开始播放视频");
                //myHandler.removeMessages(MSG_PLAY_NEXT);
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d(TAG, "palyvideo enter sync");
                        synchronized (p_video_path) {
                            playingUrl = null;
                            myHandler.sendEmptyMessage(MSG_PLAY_NEXT);
                        }
                        Log.d(TAG, "palyvideo  exit sync");

                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "视频播放错误", e);

        }
    }

    private void initData() {
        Log.d(TAG, "init data");
        p_video_path = FileUtils.getFileList("/mnt/sdcard/Shelter");
        try {
            FileInputStream fileInputStream = new FileInputStream(Constants.FILE_CFG);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            BufferedReader bfr = new BufferedReader(inputStreamReader);
            String line = bfr.readLine();
            if (line != null) {
                changeDuration = Integer.parseInt(line);
            }
            line = bfr.readLine();
            if (line != null) {
                recvTimeout = Integer.parseInt(line);
            }
            line = bfr.readLine();
            if (line != null) {
                isRandom = Boolean.parseBoolean(line);
            }
            Log.d(TAG,"changeDuration =" + changeDuration +"recvTimeout = " + recvTimeout + "isRandom=" + isRandom );
            bfr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"},1);
        setContentView(R.layout.activity_main);
        mContext = this;
        mAdvPic = (ImageView) findViewById(R.id.image_adv_pic);
        isFirstCreate = true;
        //init();
        registerBroadcastReceiver(this);
        //mdsumFileVerify();
        initData();
        socketConnection = new SocketConnection();
        socketConnection.setListener(MainActivity.this);
        //开机后，先下载md5校验文件并进行校验
        //downloadMD5AndCheckSum();
        //根据md5校验状态决定是否下载json数据
        //downloadJsonDateAndCheck();
        //根据json中更新的项下载对应的广告文件


        vedioSurfaceView();
        //ConnectedSocket();
        new ConnectThread().start();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        //Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
    }


    private boolean downloadMD5AndCheckSum() {
        mdsumFileVerify();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://adv.com.nrt.myapplication2/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        if (socketConnection != null) {
            socketConnection.closeSocket();
        }
        synchronized (mp) {
            if (mp != null && mp.isPlaying()) {
                mp.stop();
            }
            if (mp != null) {
                mp.release();
            }
            mp = null;
        }
        //this.finish();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketConnection != null) {
            socketConnection.closeSocket();
        }

        unregisterReceiver(mDownloadFinishReceiver);
    }

    private void registerBroadcastReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        context.registerReceiver(mDownloadFinishReceiver, intentFilter);
    }

    private void ConnectedSocket() {
        Log.d(TAG, "ConnectedSocket ....enter");
        if (mp == null) return;
        boolean ret = true;
                try {
                    Log.d(TAG, "ConnectedSocket connect .....");
                    ret = socketConnection.connect();
                    while(!ret) {
                        Thread.sleep(50);
                        Log.d(TAG, "Socket error, connected again");
                        ret = socketConnection.connect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    socketConnection.closeSocket();
                    socketConnection = null;

                }

        //myHandler.sendEmptyMessage(MSG_PLAY_NORMAL);
    }

    public void testSocket(View view) {
        //do nothing
    }


    private void jsonDataFileDownload() {
        File file = new File("/mnt/sdcard/Shelter/data.json");
        if (file != null && !file.exists()) {
            DOWNLOAD_DATA_JSON_ID = FileUtils.downloadAdv(MainActivity.this, "http://192.168.0.100/data.json", "/mnt/sdcard/Shelter/data.json");
            isNeedUpdateJSON = true;
        } else {
            DOWNLOAD_DATA_JSON_ID = FileUtils.downloadAdv(MainActivity.this, "http://192.168.0.100/data.json", "/mnt/sdcard/Shelter/datatemp.json");
        }

    }

    private void downLoadDataFile() {
        //json文件比较逻辑,返回 false表示两文件不一样
        new Thread(new Runnable() {
            @Override
            public void run() {
                String temJsonContext = FileUtils.ReadFile(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_TMP_DATA_LIST);
                String jsonContext = FileUtils.ReadFile(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_DATA_JSON);
                JSONArray needDownloadArray = null;
                try {
                    JSONArray tmpJsonArray = new JSONArray(temJsonContext);
                    JSONArray jsonArray = new JSONArray(jsonContext);
                    needDownloadArray = new JSONArray();
                    int smallSize = jsonArray.length();
                    int bigSize = tmpJsonArray.length();
                    Log.d(TAG, "Size: " + bigSize);
                    if (smallSize < bigSize) {
                        for (int i = smallSize; i < bigSize; i++) {
                            JSONObject jsonObject = tmpJsonArray.getJSONObject(i);
                            needDownloadArray.put(jsonObject);
                            Log.d(TAG, "[" + i + "]JSON_DATA_ID=" + jsonObject.get(Constants.JSON_DATA_ID));
                            FileUtils.downloadAdv(mContext, Constants.HTTPS_SERVER_ADDRESS + Constants.FILE_SUBDIR
                                            + "/" + jsonObject.get(Constants.JSON_DATA_ID)
                                            + "/" + jsonObject.get(Constants.JSON_DATA_NAME),
                                    Constants.FILE_ADDRESS_ROOT + Constants.FILE_SUBDIR
                                            + "/" + jsonObject.get(Constants.JSON_DATA_ID)
                                            + "/" + jsonObject.get(Constants.JSON_DATA_NAME)
                            );
                            Log.d(TAG, "[" + i + "]JSON_DATA_NAME_=" + jsonObject.get(Constants.JSON_DATA_NAME));
                        }
                    }


                } catch (JSONException e) {
                    Log.e(TAG, "parsal exception e: ", e);
                }

            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (1 == requestCode) {
            Log.d(TAG, "grantResults.length is " + grantResults.length + "grantResults[0] is " + grantResults[0]);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                playVideo("/mnt/sdcard/Shelter/vedio_teacher.mp4");
            } else {
                Log.d(TAG, "permission denied=");
                finish();
            }
        } else {
            Log.d(TAG, "permission denied");
            finish();
        }
    }

    BroadcastReceiver mDownloadFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                System.out.println("下载完成");

            /*
             * 获取下载完成对应的下载ID, 这里下载完成指的不是下载成功, 下载失败也算是下载完成,
             * 所以接收到下载完成广播后, 还需要根据 id 手动查询对应下载请求的成功与失败.
             */
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);

                if (id == DOWNLOAD_MD_ID ) {
                    Log.d(TAG, "download md file done!");
                    if (mAction == MyAction.DOWNLOAD_MD_TEMP) {//md5sum 文件本地有。需要做数据比较判断
                        Log.d(TAG, "download md temp file download done,and start to checking md file !");
                        myHandler.sendEmptyMessage(MSG_CHECK_MD);
                    } else {//第一次下载md5sum 文件。直接下载data.json文件并直接下载数据
                        Log.d(TAG, "download md  file download done,and start to download data.json file!");
                        myHandler.sendEmptyMessage(MSG_CHECK_JSON);
                    }
                } else if (id == DOWNLOAD_DATA_JSON_ID) {
                    myHandler.sendEmptyMessage(MSG_DOWNLOAD_ALL);
                } else {

                    if (mAction == MyAction.DOWNLOAD_FILE) {
                        myHandler.sendEmptyMessage(MSG_UPDATE_FILELIST);
                        myHandler.sendEmptyMessage(MSG_PLAY_NORMAL);
                    }
                }
                // initData();
                // 根据获取到的ID，使用上面第3步的方法查询是否下载成功
            }
        }
    };



    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://adv.com.nrt.myapplication2/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    /**
     * 比较datatemp.json 和data.json 并把差异数据下载下载。
     * 下载完成后将datatemp.json命名为data.json
     */
    private void downLoadUpdateDataFile(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String temJsonContext = FileUtils.ReadFile(Constants.FILE_ADDRESS_ROOT+Constants.HTTPS_TMP_DATA_LIST);
                String jsonContext = FileUtils.ReadFile(Constants.FILE_ADDRESS_ROOT+Constants.HTTPS_DATA_JSON);
                try {
                    JSONArray tmpJsonArray = new JSONArray(temJsonContext);
                    JSONArray jsonArray = new JSONArray(jsonContext);
                    int smallSize = jsonArray.length();
                    int bigSize = tmpJsonArray.length();
                    Log.d(TAG,"Size: " + bigSize);
                    if( smallSize<bigSize ){
                        for (int i = 0; i < bigSize; i++) {
                            JSONObject jsonObject = tmpJsonArray.getJSONObject(i);
                            String id = jsonObject.getString(Constants.JSON_DATA_ID);
                            boolean isNeedDownload = true;
                            for (int j = 0; j<= smallSize; j++) {
                                JSONObject samll = jsonArray.getJSONObject(j);
                                String smallid = samll.getString(Constants.JSON_DATA_ID);
                                if (id != null && smallid != null &&id.equals(smallid)) {
                                    isNeedDownload = false;
                                    break;
                                }
                            }
                            if (isNeedDownload) {
                                FileUtils.downloadAdv(mContext, Constants.HTTPS_SERVER_ADDRESS + Constants.FILE_SUBDIR
                                                + "/" + jsonObject.get(Constants.JSON_DATA_ID)
                                                + "/" +jsonObject.get(Constants.JSON_DATA_NAME),
                                        Constants.FILE_ADDRESS_ROOT + Constants.FILE_SUBDIR
                                                + "/" + jsonObject.get(Constants.JSON_DATA_ID)
                                                + "/" +jsonObject.get(Constants.JSON_DATA_NAME)
                                );
                                mAction = MyAction.DOWNLOAD_FILE;
                            }
                            Log.d(TAG,"[" + i + "]JSON_DATA_NAME_=" + jsonObject.get(Constants.JSON_DATA_NAME));
                        }
                    }
                    //datatemp.json文件处理结束 需要删除data.json 并把datatemp.json 命名为data.json
                    updateDataJsonFile();
                }catch (JSONException e) {
                    Log.e(TAG, "parsal exception e: ", e);
                }
            }
        }).start();
    }

    /**
     * 本地数据为空 下载服务器上data.json 然后根据列表下载所有文件
     */
    private void downLoadAllDataFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String jsonContext = FileUtils.ReadFile(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_DATA_JSON);
                try {
                    JSONArray jsonArray = new JSONArray(jsonContext);
                    if (jsonArray == null || jsonArray.length() <= 0) {
                        return;
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Log.d(TAG, "download file name is " + jsonObject.get(Constants.JSON_DATA_NAME));
                        FileUtils.downloadAdv(mContext, Constants.HTTPS_SERVER_ADDRESS + Constants.FILE_SUBDIR
                                        + "/" + jsonObject.get(Constants.JSON_DATA_ID)
                                        + "/" + jsonObject.get(Constants.JSON_DATA_NAME),
                                Constants.FILE_ADDRESS_ROOT + Constants.FILE_SUBDIR
                                        + "/" + jsonObject.get(Constants.JSON_DATA_ID)
                                        + "/" + jsonObject.get(Constants.JSON_DATA_NAME)
                        );
                    }
                    mAction = MyAction.DOWNLOAD_FILE;
                } catch (JSONException e) {
                    Log.e(TAG, "downLoadAllDataFiles parsal exception e: ", e);
                }
            }
        }).start();
    }

    /**
     * 下载并处理完成后删除之前的md文件并且将temp文件命名为md5sum文件
     */
    private void updateMDFile() {
        File filetmd = new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_SUM_FLAG);
        if (filetmd != null && filetmd.exists()) {
            filetmd.delete();
        }
        File fileTemp = new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_SUM_FLAG_TEMP);
        if (fileTemp != null && fileTemp.exists()) {
            fileTemp.renameTo(new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_SUM_FLAG));
        }
    }
    /**
     * 下载并处理完成后删除之前的文件并且将daratemp.json文件命名为data.json文件
     */
    private void updateDataJsonFile() {
        File filetmd = new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_DATA_JSON);
        if (filetmd != null && filetmd.exists()) {
            filetmd.delete();
        }
        File fileTemp = new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_TMP_DATA_LIST);
        if (fileTemp != null && fileTemp.exists()) {
            fileTemp.renameTo(new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_DATA_JSON));
        }
    }

    /**
     * 下载并处理完成后删除之前的文件并且将daratemp.json文件命名为data.json文件
     */
    private void deleteJsonFileAndDataFiles() {
        Log.d(TAG, "deleteJsonFileAndDataFiles ------------");
        File filetmd = new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_DATA_JSON);
        if (filetmd != null && filetmd.exists()) {
            filetmd.delete();
        }
        delFolder(Constants.FILE_ADDRESS_ROOT + Constants.FILE_SUBDIR);//删除文件夹下所有文件
        mkdirDataFolder();//创建新的文件夹
    }

    private void mkdirDataFolder() {
        File file = new File(Constants.FILE_ADDRESS_ROOT + Constants.FILE_SUBDIR);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                Log.d(TAG, "delete file name is " + file.getAbsolutePath());
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }
    public  void delFolder(String folderPath) {
        Log.d(TAG, "delFolder ------------folderPath = " + folderPath);
        try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查data.json是否存在 不存在则下载 已存在则下载服务器并命名为datatemp.json
     */
    private void dataJSONFileVerify(){
        Log.d(TAG, "dataJSONFileVerify ---------------");
        File file = new File("/mnt/sdcard/Shelter/data.json");
        if (file !=null && !file.exists()){
            Log.d(TAG, "first init download data.json file  ---------------");
            DOWNLOAD_DATA_JSON_ID = FileUtils.downloadAdv(MainActivity.this,"http://192.168.0.100/data.json", "/mnt/sdcard/Shelter/data.json");
            isNeedUpdateJSON = true;
            mAction = MyAction.DOWNLOAD_DATA_JSON;
            //第一次下载data.json
        } else {
            Log.d(TAG, "delete data.json file and /data folder");
            //先删除data.json 和 data数据
            deleteJsonFileAndDataFiles();
            DOWNLOAD_DATA_JSON_ID = FileUtils.downloadAdv(MainActivity.this,"http://192.168.0.100/data.json", "/mnt/sdcard/Shelter/data.json");
        }
    }

    /**
     * 检查md5sum文件是否存在。不存在则下载下来。已存在则下载并命名为md5sumtemp
     */
    private void mdsumFileVerify(){

        Log.d(TAG, "mdsumFileVerify ---------- start");
        File file = new File("/mnt/sdcard/Shelter/md5sum");
        if (file !=null && !file.exists()){
            Log.d(TAG, "file is not exist /mnt/sdcard/Shelter/md5sum so need to download it");
            DOWNLOAD_MD_ID = FileUtils.downloadAdv(MainActivity.this,"http://192.168.0.100/md5sum", "/mnt/sdcard/Shelter/md5sum");
            mAction = MyAction.DOWNLOAD_MD;
        } else {
            Log.d(TAG, "download md5wum file and save to tmep");

            mAction = MyAction.DOWNLOAD_MD_TEMP;
            DOWNLOAD_MD_ID = FileUtils.downloadAdv(MainActivity.this,"http://192.168.0.100/md5sum", "/mnt/sdcard/Shelter/md5sumtemp");
        }
    }


    /**
     * 比较2个md5sum 文件 看是否有差异
     * YES: 下载data.json 文件, 更新md5sum 文件
     * NO: 删除temp文件即可
     */
    private void mdsumFileCheck(){
        boolean isNeed = FileUtils.checkNeedUpdate();
        Log.d(TAG, "check resule is " + isNeed);
        if (isNeed) {
            //download datatemp.json and save to data_temp.json
            //delete md5sum and rename md5sumtemp to md5sum
            Log.d(TAG, "send message to download json file ");
            myHandler.sendEmptyMessage(MSG_DOWNDOWN_JSON);
            updateMDFile();
        } else {
            Log.d(TAG, "md5sum file is same, so delete temp file");
            myHandler.sendEmptyMessage(MSG_UPDATE_FILELIST);
            myHandler.sendEmptyMessage(MSG_PLAY_NORMAL);
            File file = new File(Constants.FILE_ADDRESS_ROOT + Constants.HTTPS_SUM_FLAG_TEMP);
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }
}
