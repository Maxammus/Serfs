package mod.maxammus.serfs.util;

import org.gotti.wurmunlimited.modsupport.ModSupportDb;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DBUtil {
    private static final Logger logger = Logger.getLogger(DBUtil.class.getName());

    public static void createDBs() {
        Map<String, String> tableCreation = new HashMap<>();
        tableCreation.put("TaskQueues", "CREATE TABLE IF NOT EXISTS TaskQueues (" +
                "QUEUEID LONG PRIMARY KEY NOT NULL, " +
                "PLAYERID LONG NOT NULL, " +
                "NAME TEXT NOT NULL, " +
                "PAUSED BOOLEAN NOT NULL)");
        tableCreation.put("TaskGroups", "CREATE TABLE IF NOT EXISTS TaskGroups (" +
                "QUEUEID LONG PRIMARY KEY NOT NULL, " +
                "GROUPWIDE BOOLEAN NOT NULL," +
                "FOREIGN KEY (QUEUEID) REFERENCES TaskQueues(QUEUEID) ON DELETE CASCADE" +
                ")");
        tableCreation.put("TaskAreas", "CREATE TABLE IF NOT EXISTS TaskAreas (" +
                "QUEUEID LONG PRIMARY KEY NOT NULL, " +
                "TILEX INT NOT NULL," +
                "TILEY INT NOT NULL," +
                "LENGTH INT NOT NULL," +
                "WIDTH INT NOT NULL," +
                "FLOOR INT NOT NULL," +
                "LAYER INT NOT NULL," +
                "COUNTER INT NOT NULL," +
                "ROTATION REAL NOT NULL," +
                "CHAINEDTASKS BOOLEAN NOT NULL," +
                "PRECHECKTASKS BOOLEAN NOT NULL," +
                "FOREIGN KEY (QUEUEID) REFERENCES TaskQueues(QUEUEID) ON DELETE CASCADE)");
        tableCreation.put("SerfAssignments", "CREATE TABLE IF NOT EXISTS SerfAssignments (" +
                "QUEUEID LONG NOT NULL, " +
                "SERFID LONG NOT NULL, " +
                "FOREIGN KEY (QUEUEID) REFERENCES TaskQueues(QUEUEID) ON DELETE CASCADE)");
        tableCreation.put("TaskContainerAssignments", "CREATE TABLE IF NOT EXISTS TaskContainerAssignments (" +
                "QUEUEID LONG NOT NULL, " +
                "ITEMID LONG NOT NULL, " +
                "FOREIGN KEY (QUEUEID) REFERENCES TaskQueues(QUEUEID) ON DELETE CASCADE)");
        tableCreation.put("TaskGroupAssignments", "CREATE TABLE IF NOT EXISTS TaskGroupAssignments (" +
                "QUEUEID LONG NOT NULL, " +
                "GROUPID LONG NOT NULL, " +
                "FOREIGN KEY (QUEUEID) REFERENCES TaskQueues(QUEUEID) ON DELETE CASCADE)");
        tableCreation.put("TaskProfiles", "CREATE TABLE IF NOT EXISTS TaskProfiles (" +
                "PROFILEID LONG PRIMARY KEY NOT NULL," +
                "PLAYERID LONG NOT NULL," +
                "SELECTEDQUEUE LONG," +
                "REPEAT INT NOT NULL," +
                "WHILETIMERSHOWS BOOLEAN NOT NULL," +
                "WHILEACTIONAVAILABLE BOOLEAN NOT NULL," +
                "READD BOOLEAN NOT NULL," +
                "ACTIVEITEMTEMPLATE INT," +
                "TAKECONTAINER LONG," +
                "DROPCONTAINER LONG," +
                "TAKECONTAINERGROUP TEXT," +
                "DROPCONTAINERGROUP TEXT)");
        tableCreation.put("Tasks", "CREATE TABLE IF NOT EXISTS Tasks (" +
                "TASKID LONG PRIMARY KEY NOT NULL," +
                "QUEUEID LONG NOT NULL, " +
                "QUEUEPOSITION INT NOT NULL," +
                "ACTION INT NOT NULL," +
                "POSX REAL NOT NULL," +
                "POSY REAL NOT NULL," +
                "POSZ REAL NOT NULL," +
                "LAYER INT NOT NULL," +
                "ASSIGNED LONG," +
                "TARGET LONG NOT NULL," +
                "DOTIMES INT NOT NULL," +
                "WHILETIMERSHOWS BOOLEAN NOT NULL," +
                "WHILEACTIONAVAILABLE BOOLEAN NOT NULL," +
                "READD BOOLEAN NOT NULL," +
                "ACTIVEITEMTEMPLATE INT," +
                "TARGETITEMTEMPLATE INT," +
                "TARGETCREATURETEMPLATE INT," +
                "PARENT LONG NOT NULL," +
                "REPEATEDCOUNT INT NOT NULL," +
                "STARTED BOOLEAN NOT NULL," +
                "INITIALIZED BOOLEAN NOT NULL," +
                "TAKECONTAINER LONG," +
                "DROPCONTAINER LONG," +
                "TAKECONTAINERGROUP TEXT NOT NULL," +
                "DROPCONTAINERGROUP TEXT NOT NULL," +
                "EXACTTARGET BOOLEAN NOT NULL," +
                "FOREIGN KEY (PARENT) REFERENCES TaskQueues(QUEUEID) ON DELETE CASCADE)");
            tableCreation.forEach((tableName, sql) -> {
                try (Connection dbcon = ModSupportDb.getModSupportDb();
                     PreparedStatement ps = dbcon.prepareStatement(sql)) {
                    ps.execute();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create table " + tableName, e);
                }
            });
    }

    public static boolean executeSingleStatement(String statement, Object... args) {
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement(statement)) {

            for(int i = 0; i < args.length; i++)
                ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warning("Exception in executeSimpleStatement: " + e.getMessage() + " - Statement: \"" + statement + "\" Args: " + Arrays.toString(args));
            return false;
        }
        return true;
    }

    public static void addStatementToBatch(PreparedStatement ps, Object... args) throws SQLException {
        for(int i = 0; i < args.length; i++)
            ps.setObject(i + 1, args[i]);
        ps.addBatch();
    }
}