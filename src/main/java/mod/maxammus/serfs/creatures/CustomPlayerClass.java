package mod.maxammus.serfs.creatures;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.server.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.epic.ValreiMapData;
import com.wurmonline.server.intra.IntraServerConnection;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.*;
import com.wurmonline.server.skills.AffinitiesTimed;
import com.wurmonline.server.steam.SteamId;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.PlayerOnlineStatus;
import mod.maxammus.serfs.workarounds.DummySocketConnection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.wurmonline.server.LoginHandler.raiseFirstLetter;

//Through use of reflection CustomPlayerClass actually extends Player during runtime

//Using this class between Serf and Player mostly so hot reloads of Serf
// aren't rejected by the IDE complaining about hierarchy changes.
public class CustomPlayerClass extends Creature {
    private static Logger logger = Logger.getLogger(CustomPlayerClass.class.getName());

    public CustomPlayerClass(PlayerInfo info, SocketConnection connection) throws Exception {
        //During runtime:
        //super(info, connection);
        super();
    }

    public CustomPlayerClass(int id, SocketConnection connection) throws Exception {
        //During runtime:
        //super(id, connection);
        super();
    }

    public static void doLogIn(String name) {
        LoginHandler login = null;
        try {
            login = new LoginHandler(new DummySocketConnection());
        } catch (IOException e) {
            logger.info("Failed to create DummySocketConnection in doLogIn: " + e.getMessage());
            return;
        }
        Server.getInstance().steamHandler.setIsPlayerAuthenticated("0");
        ByteBuffer bb = ByteBuffer.allocate(1024)
                .put((byte) -15)
                .putInt(250990585)
                .put((byte) name.length())
                .put(name.getBytes())
                .put((byte) "0".length())
                .put("0".getBytes())
                .put((byte) Servers.localServer.getSteamServerPassword().length())
                .put(Servers.localServer.getSteamServerPassword().getBytes())
                .put((byte) "0".length())
                .put("0".getBytes())
                .put((byte) 1);
        bb.flip();
        login.reallyHandle(0, bb);
    }
}
