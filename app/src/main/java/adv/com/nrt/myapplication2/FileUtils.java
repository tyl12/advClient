/**
 * Created by Jack.peng on 2018/4/6.
 */
package adv.com.nrt.myapplication2;
import android.app.DownloadManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.lang.reflect.Array;

import adv.com.nrt.myapplication2.VideoPathQuery;

public class FileUtils {
    private static final String TAG = "FileUtils";
    /**
     * 1. save file to sdcard from http client.
     * 2. get video form sdcard
     *
     */
    public static String getMacAddress(){
 /*获取mac地址有一点需要注意的就是android 6.0版本后，以下注释方法不再适用，不管任何手机都会返回"02:00:00:00:00:00"这个默认的mac地址，这是googel官方为了加强权限管理而禁用了getSYstemService(Context.WIFI_SERVICE)方法来获得mac地址。*/
        //        String macAddress= "";
//        WifiManager wifiManager = (WifiManager) MyApp.getContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        macAddress = wifiInfo.getMacAddress();
//        return macAddress;

        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("wlan0");

            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("eth1");
            }
            if (networkInterface == null) {
                return "02:00:00:00:00:02";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "02:00:00:00:00:02";
        }
        return macAddress;
    }

    /**
     * 判断当前存储卡是否可用
     **/
    public static boolean checkSDCardAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取当前存储卡根目录
     **/
    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = checkSDCardAvailable();
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    /**
     * 获取文件列表
     */
    static ArrayList<VideoPathQuery> filelist = new ArrayList<VideoPathQuery>();
    public static ArrayList<VideoPathQuery> getFileList(String strPath) {
        if(!checkSDCardAvailable()){
            Log.e(TAG, "getFileList sdcard is not availiable !!!");
            return null;
        }
        //ArrayList<VideoPathQuery> filelist = new ArrayList<VideoPathQuery>();
        File dir = new File(strPath);
        File[] files = dir.listFiles(); // 该文件目录下文件全部放入数组
        Log.d(TAG,"length =" + files.length);

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                Log.d(TAG, "fileName = " + fileName);
                if (files[i].isDirectory()) { // 判断是文件还是文件夹
                    getFileList(files[i].getAbsolutePath()); // 获取文件绝对路径
                } else if (fileName.endsWith(".avi")||fileName.endsWith(".mp4")
                        ||fileName.endsWith(".jpg")||fileName.endsWith(".png")
                        ||fileName.endsWith(".JPG")||fileName.endsWith(".png")) { // 判断文件名是否以.avi结尾
                    String strFileName = files[i].getAbsolutePath();
                    System.out.println("---" + strFileName);
                    VideoPathQuery v = new VideoPathQuery();
                    v.setID(getID(files[i].getAbsolutePath()));
                    v.setVideo_url(strFileName);
                    if (filelist.isEmpty()) {
                        filelist.add(v);
                    } else {
                        int j = 0;
                        for (j = 0; j < filelist.size(); j++ ) {
                            if (filelist.get(j).getID() > v.getID()) {
                                break;
                            }
                        }
                        filelist.add(j, v);
                    }

                } else {
                    continue;
                }
            }

        }
        return filelist;
    }

    private static int getID(String path){
        String id = "";

        String[] parse = path.split("/");
        Log.d(TAG, "parse  path = " + path + " parse: " + parse[0] + "; 1 = " + parse[1] + " n = " + parse[parse.length-2]);
        id = parse[parse.length-2];
        return Integer.parseInt(id);

    }





    public static long downloadAdv(Context context ,String fileLink, String savePath){
        Log.d(TAG, "downloadadv chenqiao-----");


        String downloadUrl = fileLink;
        if (null == downloadUrl) {
            downloadUrl = "http://vali.cp31.ott.cibntv.net/657218CC74045833B15D5668B8/03000B01005AB348A3929EF55FB964B07D07D3-CCB1-40F8-83ED-310996976FF7.mp4?ctype=21&sid=052379627554621e56922_B7d0a7f1e09fce26dfd28853318f6e837&type=hd2&sid=052379627554621e56922&token=8144&oip=466504725&did=e0e5090a56dc115366d737693745f155&ctype=21&ev=1&ep=tPnu3Epym3SIcJDW1HJt5BPVl%2F2vyAqzRtjNhTMriomPicgCrCzX3iz9bS8hcgz%2FrsYF6draYUY02AtjWQKsWnbUZ7KsqR%2FU3IqjqWZGZn8HvvF8YK9kHNZSP2fDQ%2FRR";
        }
        // 创建下载请求
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));

        /*
         * 设置在通知栏是否显示下载通知(下载进度), 有 3 个值可选:
         *    VISIBILITY_VISIBLE:                   下载过程中可见, 下载完后自动消失 (默认)
         *    VISIBILITY_VISIBLE_NOTIFY_COMPLETED:  下载过程中和下载完成后均可见
         *    VISIBILITY_HIDDEN:                    始终不显示通知
         */
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

        // 设置通知的标题和描述
//        request.setTitle("通知标题XXX");
//        request.setDescription("对于该请求文件的描述");

        /*
         * 设置允许使用的网络类型, 可选值:
         *     NETWORK_MOBILE:      移动网络
         *     NETWORK_WIFI:        WIFI网络
         *     NETWORK_BLUETOOTH:   蓝牙网络
         * 默认为所有网络都允许
         */
        // request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

        // 添加请求头
        // request.addRequestHeader("User-Agent", "Chrome Mozilla/5.0");

        // 设置下载文件的保存位置 :"/mnt/sdcard/Shelter/"
        File saveFile = new File(savePath);
        Log.d(TAG,"Environment.getExternalStorageDirectory() is "+Environment.getExternalStorageDirectory());
        request.setDestinationUri(Uri.fromFile(saveFile));
        /*
         * 2. 获取下载管理器服务的实例, 添加下载任务
         */
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        // 将下载请求加入下载队列, 返回一个下载ID
        return manager.enqueue(request);

        // 如果中途想取消下载, 可以调用remove方法, 根据返回的下载ID取消下载, 取消下载后下载保存的文件将被删除
        // manager.remove(downloadId);
    }

    public static String readFileByLine(String patch){
        String s="";
        File f=new File(patch);
        BufferedReader br=null;
        try{
            System.out.println("按照行读取文件内容");
            br=new BufferedReader(new FileReader(f));
            String temp;
            while((temp=br.readLine())!=null){
                s+=temp;
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("file content:"+s);
        return s;
    }

    public static boolean checkNeedUpdate(){
        String temp = readFileByLine(Constants.FILE_ADDRESS_ROOT+Constants.HTTPS_SUM_FLAG_TEMP);
        String old = readFileByLine(Constants.FILE_ADDRESS_ROOT+Constants.HTTPS_SUM_FLAG);
        if (!temp.equals("") && !old.equals("")) {
            return !temp.equals(old);
        }
        return true;
    }

    public static String ReadFile(String Path){
        BufferedReader reader = null;
        Log.d(TAG,"Path is" + Path);
        String laststr = "";
        try{
            FileInputStream fileInputStream = new FileInputStream(Path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            while((tempString = reader.readLine()) != null){
                laststr += tempString;
            }
            reader.close();
        }catch(IOException e){
            Log.d(TAG,"error msg is :",e);
        }finally{
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG,"laststr is"+laststr);
        return laststr;
    }

    public static JSONArray parseJsonFileToListAndDownload() {
        String temJsonContext = ReadFile(Constants.FILE_ADDRESS_ROOT+Constants.HTTPS_TMP_DATA_LIST);
        String jsonContext = ReadFile(Constants.FILE_ADDRESS_ROOT+Constants.HTTPS_DATA_JSON);
        JSONArray needDownloadArray = null;
        try {
            JSONArray tmpJsonArray = new JSONArray(temJsonContext);
            JSONArray jsonArray = new JSONArray(jsonContext);
            needDownloadArray = new JSONArray();
            int smallSize = jsonArray.length();
            int bigSize = tmpJsonArray.length();
            Log.d(TAG,"Size: " + bigSize);
            if( smallSize<bigSize ){
                for (int i = smallSize; i < bigSize; i++) {
                    JSONObject jsonObject = tmpJsonArray.getJSONObject(i);
                    needDownloadArray.put(jsonObject);
                    Log.d(TAG,"[" + i + "]JSON_DATA_ID=" + jsonObject.get(Constants.JSON_DATA_ID));
                    Log.d(TAG,"[" + i + "]JSON_DATA_NAME_=" + jsonObject.get(Constants.JSON_DATA_NAME));
                }
            }


        }catch (JSONException e) {
            Log.e(TAG, "parsal exception e: ", e);
        }
        return needDownloadArray;
    }


    public static String getJson(String fileName,Context context) {
        //将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
