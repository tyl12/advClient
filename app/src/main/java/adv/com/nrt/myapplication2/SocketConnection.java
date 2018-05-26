package adv.com.nrt.myapplication2;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SocketConnection {
    private static final String TAG = "SocketConnection";

    Socket socket = null;
    OutputStreamWriter osw;
    DataOutputStream dos;
    DataInputStream dis;
    boolean isConnected = false;

    void convert(byte[] data, int startIndex, int value) {
        for(int i = 0; i < 4; i++) {
            data[startIndex + i] = (byte)(value>>(24-i*8));
        }
    }

    void sendEntry(String entry) {

        try {
            dos.writeInt(entry.length());
            dos.flush();
            osw.write(entry, 0, entry.length());
            osw.flush();
            Log.d("chenqiao", "entry.length = " + entry.length());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    String recvEntry() {
        int len;
        String s = null;
        try {
            len = dis.readInt();
           // System.out.println(len);
            byte[] array = new byte[len];
            dis.read(array);
            s = new String(array);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            isConnected = false;
        }

        //System.out.println("recv: " + s);
        mSocketListener.onReceive(s);
        return s;
    }

    public interface SocketListener{
        void onReceive(String str);
        void onError();
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
            socket = new Socket("192.168.0.100", 8882);
            dos = new DataOutputStream(socket.getOutputStream());
            osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            dis = new DataInputStream(socket.getInputStream());

            isConnected = true;
            sendEntry(str);
            String macStr = FileUtils.getMacAddress();
            System.out.println("mac addr is" + macStr);
            Log.e(TAG, "mac addr is" + macStr);
            sendEntry(macStr);

            while (isConnected) {
                    String key = recvEntry();
                    String value = recvEntry();
                //System.out.println("receive client msg: " + key + " & " + value);
            }
            //Thread.sleep(1000*10);
        } catch (IOException e) {
            System.err.print(e);
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
