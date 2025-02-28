package mod.maxammus.serfs.workarounds;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.communication.SocketServer;
import com.wurmonline.server.Server;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class DummySocketConnection extends SocketConnection {
    int unflushedChecks = 0;
    private final ByteBuffer byteBuffer = null;
    public DummySocketConnection() throws IOException {
        super("", 0, false);
        //At runtime:
        //super()
        //byteBuffer = ByteBuffer.allocate(65534);
        //encryptRandom = new Random(105773331L);
        //decryptRandom = new Random(105773331L);

    }

    @Override
    public ByteBuffer getBuffer() {
        byteBuffer.clear();
        return byteBuffer;
    }

    @Override
    public void tick() throws IOException {
    }

    public String getIp() { return "127.0.0.1"; }
    //needs to sometimes return < 4096, and sometimes more to have VisionArea.sendNextStrip correctly set VisionArea.resumed
    public int getUnflushed() { return 4096 - (++unflushedChecks % 100); }
    public void clearBuffer() { byteBuffer.clear(); }
    public boolean isConnected() { return true; }
    public void flush() throws IOException { byteBuffer.flip(); }
    public boolean tickWriting(long aNanosToWaitForLock) throws IOException { return true; }
    public void setLogin(boolean li) { }
    public void closeChannel() { }
    public void disconnect() { }
    public void sendShutdown() { }
}
