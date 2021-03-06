package adv.com.nrt.myapplication2;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import android.util.Base64;

public class SocketConnection {
    private static final String TAG = "SocketConnection";

    Socket socket = null;
    OutputStreamWriter osw;
    DataOutputStream dos;
    DataInputStream dis;
    boolean isConnected = false;
    String mHostname;
    int mPort;
    boolean mIsWeight;
    public SocketConnection(String hostname, int port, boolean isWeight) {
        mHostname = hostname;
        mPort = port;
        mIsWeight = isWeight;
    }

    void convert(byte[] data, int startIndex, int value) {
        for(int i = 0; i < 4; i++) {
            data[startIndex + i] = (byte)(value>>(24-i*8));
        }
    }

    void sendKey(String entry) {
        try {
            //no base64 encode
            dos.writeInt(entry.length());
            dos.flush();
            osw.write(entry, 0, entry.length());
            osw.flush();
            System.out.println("socketInfo:sendKey: " + "key="+entry+", length="+entry.length());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    void sendEntry(String entry) {
        try {
            //base64 encode
            byte[] data=entry.getBytes("UTF-8");
            String base64 = Base64.encodeToString(data, Base64.DEFAULT);
            dos.writeInt(base64.length());
            dos.flush();
            osw.write(base64, 0, base64.length());
            osw.flush();
            System.out.println("socketInfo:sendEntry: " + "entry="+entry+", base64="+ base64 + ", length="+ base64.length());

            /*
            //no base64 encode
            dos.writeInt(entry.length());
            dos.flush();
            osw.write(entry, 0, entry.length());
            osw.flush();
            */

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    String recvKey() {
        int len;
        String s = null;
        try {
            //no base64 decode
            len = dis.readInt();

            System.out.println("socketInfo:recvKey: " + "length="+len);

           // System.out.println(len);
            byte[] array = new byte[len];
            dis.read(array);
            s = new String(array);

            System.out.println("socketInfo:recvKey: " + "key="+s);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            isConnected = false;
        }

        System.out.println("recv: " + s);
        mSocketListener.onReceive(s, mIsWeight);

        return s;
    }
    String recvEntry() {
        int len;
        String s = null;
        try {
            //base64 decode
            len = dis.readInt();

            System.out.println("socketInfo:recvEntry: " + "length="+len);
            byte[] array = new byte[len];
            dis.read(array);
            String bs = new String(array);
            byte[] data = Base64.decode(bs, Base64.DEFAULT);
            s = new String(data);
            System.out.println("socketInfo:recvEntry: " + "entry="+s);
            /*
            //no base64 decode
            len = dis.readInt();
           // System.out.println(len);
            byte[] array = new byte[len];
            dis.read(array);
            s = new String(array);
            */
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            isConnected = false;
        }

        System.out.println("recv: " + s);
        mSocketListener.onReceive(s, mIsWeight);

        return s;
    }

    public interface SocketListener{
        void onReceive(String str, boolean isWeight);
    }


    private SocketListener mSocketListener;
    public void setListener(SocketListener listener){
        mSocketListener = listener;
    }
    public boolean connect() throws Exception {

        //System.out.println("test E");
        String str = "register";
        //String msg = "message";
        try {
            //socket = new Socket("67.218.158.111", 8881);
            Log.e(TAG, "hostname = " + mHostname + " port = " + mPort);
            socket = new Socket(mHostname, mPort);
            dos = new DataOutputStream(socket.getOutputStream());
            osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            dis = new DataInputStream(socket.getInputStream());

            isConnected = true;
            sendKey(str);
            String macStr = FileUtils.getMacAddress();
            System.out.println("mac addr is: " + macStr);
            Log.e(TAG, "mac addr is: " + macStr);
            sendEntry(macStr);

            while (isConnected) {
                    String key = recvKey();
                    String value = recvEntry();
                    System.out.println("receive client msg: " + key + " & " + value);
            }
            //Thread.sleep(1000*10);
        } catch (IOException e) {
            System.err.print(e);
            Log.e(TAG, "exception = " + e);
            isConnected = false;


        } finally {
            if (socket != null) {
                socket.close();
                dos.close();
                osw.close();
                dis.close();
                socket = null;
                dos = null;
                osw = null;
                dis = null;
            }
            isConnected = false;
            return false;
        }
    }

    public void closeSocket(){
        if (null != socket && socket.isConnected()) {
            try {
                socket.close();
                isConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (socket != null) {
                    socket = null;
                }
                isConnected = false;
            }
        }
    }
}
