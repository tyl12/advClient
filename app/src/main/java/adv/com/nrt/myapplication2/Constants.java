package adv.com.nrt.myapplication2;

/**
 * Created by Jack.peng on 2018/4/10.
 */

public class Constants {
    //1. mac address
    public static final String HTTPS_SERVER_ADDRESS = "http://192.168.0.100/";
    public static final String HTTPS_SUM_FLAG = "md5sum";
    public static final String HTTPS_SUM_FLAG_TEMP = "md5sumtemp";
    public static final String HTTPS_TMP_DATA_LIST = "datatemp.json";
    public static final String HTTPS_DATA_JSON = "data.json";
    public static final String HOST_NAME = "192.168.0.100";
    public static final String FILE_ADDRESS_ROOT = "/sdcard/Shelter/";
    public static final String FILE_CFG = "/sdcard/ad.cfg";
    public static final String FILE_SUBDIR = "data/";
    public static final String JSON_DATA_ID = "adID";
    public static final String JSON_DATA_NAME = "name";
    public static final int CHANGE_DURATION = 60000;
    public static final int RECEIVE_TIMEOUT = 3000;
    public static final boolean IS_RANDOM = true;
    public static final int WEIGHT_PORT = 8113;
    public static final int AD_PORT = 8882;





    /**
     * 1. 链接https service 查询是否广告有变化。如果有变化需要下载下来；
     *    检查md5sum 文件是否发生变化，如果有变化就需要下载 data_list文件 检查文件列表，
     *    diff data_list内容，按照diff 内容下载对应文件。
     * 2. 链接广告机服务器，建立链接后注册 mac地址。接受到广告机切换广告的信息后切换播放广告；
     * 3. 需要区分是否播放图片广告；
     * 4. 本地循环播放；
     * 5. 是否需要清理过期广告；
     *
     */

    /**
     * 1. 广告机 和本地没有对接完全。
     * 2. https server端下载还没有调通
     * 3. 本地需要播放 图片格式广告 没有做好。
     * 4.md5sum只作校验，我们需要比较data json文件，根本地的比较下，然后更新本地，进行播放。
     */

    /**
     * 4.24 更新 需要做的工作
     * 1. download 文件
     * 2. 30s以内的消息过滤掉
     * 3. Socket断开后需要重新连接
     * 4. 广告切换
     *
     */
}
