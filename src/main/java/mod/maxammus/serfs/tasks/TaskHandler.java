package mod.maxammus.serfs.tasks;

import com.wurmonline.math.Vector3f;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.BehaviourDispatcher;
import com.wurmonline.server.behaviours.NoSuchBehaviourException;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.structures.NoSuchWallException;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.DBUtil;
import mod.maxammus.serfs.util.ListUtil;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//Stores and manages data relating to players
public class TaskHandler {
    static Logger logger = Logger.getLogger(TaskHandler.class.getName());
    public static long queueIdCounter = 0;
    public static long taskIdCounter = 0;
    public static long profileIdCounter = 0;
    public static final ArrayList<TaskHandler> taskHandlers = new ArrayList<>();
    public long playerId;
    public ArrayList<Serf> serfs = new ArrayList<>();
    public ArrayList<TaskGroup> taskGroups = new ArrayList<>();
    public ArrayList<TaskArea> taskAreas = new ArrayList<>();
    public ArrayList<TaskProfile> taskProfiles = new ArrayList<>();
    public static Map<Long, TaskQueue> taskQueues = new ConcurrentHashMap<>();

    public TaskHandler(long playerId) {
        this.playerId = playerId;
        taskHandlers.add(this);
    }

    public static void init() {
        logger.info("Loading serf mod databases");
        final long start = System.nanoTime();
        int serfCount = 0;
        String tableName;
        Map<Long, TaskHandler> serfQueues = new HashMap<>(Serf.serfCount);
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             //combine the tables of queues with the same QUEUEID
             PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM TaskQueues " +
                     "LEFT JOIN TaskGroups ON TaskQueues.QUEUEID = TaskGroups.QUEUEID " +
                     "LEFT JOIN TaskAreas ON TaskQueues.QUEUEID = TaskAreas.QUEUEID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long queueid = rs.getLong("QUEUEID");
                long playerid = rs.getLong("PLAYERID");
                TaskHandler taskHandler = getTaskHandler(playerid);
                TaskQueue taskQueue;
                rs.getBoolean("GROUPWIDE");
                if (!rs.wasNull()) {
                    taskQueue = new TaskGroup(rs);
                    taskHandler.taskGroups.add((TaskGroup) taskQueue);
                } else {
                    rs.getBoolean("CHAINEDTASKS");
                    if (!rs.wasNull()) {
                        taskQueue = new TaskArea(rs);
                        taskHandler.taskAreas.add((TaskArea) taskQueue);
                    }
                    //Personal serf TaskQueue, store id to match it to the serf when serf assignments are loaded later.
                    else {
                        serfCount++;
                        taskQueue = new TaskQueue(rs);
                        serfQueues.put(queueid, taskHandler);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (serfCount != Serf.serfCount)
            logger.warning("Loaded " + serfCount + " queues for serfs from database.  " + Serf.serfCount + " serfs loaded from creature database!");


        tableName = "TaskProfiles";
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TaskHandler taskHandler = getTaskHandler(rs.getLong("PLAYERID"));
                taskHandler.taskProfiles.add(new TaskProfile(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tableName = "SerfAssignments";
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long serfid = rs.getLong("SERFID");
                long queueid = rs.getLong("QUEUEID");
                Serf serf = (Serf) Creatures.getInstance().getCreature(serfid);
                TaskQueue taskQueue = taskQueues.get(queueid);
                taskQueue.assignedSerfs.add(serf);
                if(taskQueue instanceof TaskGroup)
                    ((TaskGroup)taskQueue).groupwideTasks.put(serf, new ArrayList<>());
                //Check if this is the serf's personal TaskQueue
                if (serfQueues.containsKey(queueid)) {
                    TaskHandler taskHandler = serfQueues.get(queueid);
                    taskHandler.serfs.add(serf);
                    serf.ownerId = taskHandler.playerId;
                    serf.taskQueue = taskQueue;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tableName = "TaskContainerAssignments";
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long itemId = rs.getLong("ITEMID");
                Item container = Items.getItem(itemId);
                taskQueues.get(rs.getLong("QUEUEID")).containers.add(container);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tableName = "TaskGroupAssignments";
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TaskGroup taskGroup = (TaskGroup) taskQueues.get(rs.getLong("GROUPID"));
                TaskArea taskArea = (TaskArea) taskQueues.get(rs.getLong("QUEUEID"));
                taskArea.assignedGroups.add(taskGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tableName = "Tasks";
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             //Order by asc to put tasks back into the spot in queue they were saved in.
             PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM " + tableName + " ORDER BY QUEUEPOSITION ASC;");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Task task = new Task(rs);
                try {
                    taskQueues.get(rs.getLong("QUEUEID")).queue.add(task);
                } catch (NullPointerException e) {
                    logger.warning("Loaded task " + task.taskId +" with invalid queue");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT MAX(QUEUEID) FROM TaskQueues");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                queueIdCounter = rs.getLong("MAX(QUEUEID)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT MAX(TASKID) FROM Tasks");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                taskIdCounter = rs.getLong("MAX(TASKID)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("SELECT MAX(PROFILEID) FROM TaskProfiles");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                profileIdCounter = rs.getLong("MAX(PROFILEID)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.log(Level.INFO, "Loaded serf mod databases.  That took " + (System.nanoTime() - start) / 1000000.0f + " ms.");
    }

    public static TaskHandler getTaskHandler(long playerId) {
        //Check if player's data is already in array.
        TaskHandler toReturn = ListUtil.findOrNull(taskHandlers, data -> data.playerId == playerId);
        if (toReturn == null) {
            toReturn = new TaskHandler(playerId);
        }
        return toReturn;
    }

    public static void poll() {
        for (TaskHandler taskHandler : taskHandlers) {
            for (Serf serf : taskHandler.serfs) {
                serf.pollTasks();
            }
        }
    }

    //Gets serf to simulate an action request.  Can use owner's communicator to send the results to the owner's client
    //or the serf's communicator to fill the serf's lastAvailableActions list
    //pretend to be the serf using their item
    //moveToTarget to check if the target has the action available, false to check if the serf can currently do it
    //bit hacky but probably fine
    public static void requestSerfActions(Communicator comm, byte requestId, long target, Serf serf, long itemId, boolean moveToTarget) {
        serf.lastAvailableActions = new ArrayList<>();
        int type = WurmId.getType(target);
        Vector3f oldPos = serf.getPos3f();
        Vector3f targetPos = null;

        if (Task.isItemType(type)) {
            Item item = Items.getItemOptional(target).orElse(null);
            if (item != null)
                targetPos = item.getPos3f();
        } else if (Task.isCreatureType(type)) {
            Creature creature = Creatures.getInstance().getCreatureOrNull(target);
            if (creature != null)
                targetPos = creature.getPos3f();
        } else if (Task.isTileType(type))
            //TODO: z probably needs tile height added to have correct world position
            targetPos = new Vector3f(Tiles.decodeTileX(target), Tiles.decodeTileY(target), Tiles.decodeHeightOffset(target)).mult(4);

        if (targetPos == null)
            return;

        if (moveToTarget)
            serf.getStatus().setPositionXYZ(targetPos.x, targetPos.y, targetPos.z);
        try {
            BehaviourDispatcher.requestActions(serf, comm, requestId, itemId, target);
        } catch (NoSuchPlayerException | NoSuchCreatureException | NoSuchItemException | NoSuchBehaviourException |
                 NoSuchWallException e) {
            e.printStackTrace();
        }
        serf.getStatus().setPositionXYZ(oldPos.x, oldPos.y, oldPos.z);
        serf.getStatus().setChanged(false);
    }

    public void addProfile() {
        TaskProfile profile = new TaskProfile(playerId);
        taskProfiles.add(profile);
    }

    public void removeProfile(int index) {
        if(taskProfiles.get(index).deleteFromDb())
            taskProfiles.remove(index);
        else {
            Player player = Players.getInstance().getPlayerOrNull(playerId);
            if(player != null)
                player.getCommunicator().sendNormalServerMessage("Database error");
            logger.warning("Couldn't remove profile " + taskProfiles.get(index).profileId + " from database.");
        }
    }

    public void addGroup(String name) {
        if (name == null || name.equals(""))
            return;
        else if (ListUtil.findOrNull(taskGroups, taskArea -> taskArea.name.equals(name)) != null)
            return;
        TaskGroup group = new TaskGroup(playerId, name);
        taskGroups.add(group);
    }

    public void removeGroup(long queueId) {
        TaskGroup group = (TaskGroup) getQueue(queueId);
        if(group.deleteFromDb())
            taskGroups.remove(group);
        else {
            Player player = Players.getInstance().getPlayerOrNull(playerId);
            if(player != null)
                player.getCommunicator().sendNormalServerMessage("Database error");
            logger.warning("Couldn't remove group " + queueId + " from database.");
        }
    }

    public void addArea(String name, int x, int y, int layer, int floor, int length, int width, float rotation) {
        if (name == null || name.equals(""))
            return;
        else if (ListUtil.findOrNull(taskAreas, taskArea -> taskArea.name.equals(name)) != null)
            return;
        TaskArea taskArea = new TaskArea(playerId, name, x, y, layer, floor, length, width, rotation);
        this.taskAreas.add(taskArea);
    }

    public void removeArea(long queueId) {
        TaskArea taskQueue = (TaskArea) getQueue(queueId);
        if(taskQueue.deleteFromDb())
            taskAreas.remove(taskQueue);
        else {
            Player player = Players.getInstance().getPlayerOrNull(playerId);
            if(player != null)
                player.getCommunicator().sendNormalServerMessage("Database error");
            logger.warning("Couldn't remove area " + queueId + " from database.");
        }
    }

    public void receiveAction(final Creature creature, final Communicator comm, final long subject, final long target, final short action) {
        TaskProfile taskProfile = getSelectedProfile();
        if(Serfs.blacklist.contains(action) || (Serfs.whitelist.size() > 0 && !Serfs.whitelist.contains(action))) {
            comm.sendNormalServerMessage("Serfs are not allowed to do that.");
            return;
        }
        try {
            taskProfile.getSelectedQueue().addTask(taskProfile, action, target);
        } catch (NullPointerException npe) {
            comm.sendNormalServerMessage("Could not add task");
        }
    }

    public TaskProfile getSelectedProfile() {
        return ListUtil.findOrNull(taskProfiles, TaskProfile::isSelected);
    }

    public Item getSelectedItem(TaskProfile taskProfile) {
        if (taskProfile.getActiveItemTemplate() == null)
            return null;
        return taskProfile.getFirstSerf().getInventory().findItem(taskProfile.getActiveItemTemplate().getTemplateId());
    }

    public ArrayList<TaskQueue> getQueues() {
        ArrayList<TaskQueue> toReturn = new ArrayList<>();
        toReturn.addAll(taskGroups);
        toReturn.addAll(taskAreas);
        for (Serf serf : serfs)
            toReturn.add(serf.taskQueue);
        return toReturn;
    }

    public TaskQueue getQueue(long queueId) {
        return ListUtil.findOrNull(getQueues(), taskQueue -> taskQueue.queueId == queueId);
    }

    public void removeSerfFromAll(Serf serf) {
        while(!serf.taskQueue.containers.isEmpty())
         serf.taskQueue.removeContainer(serf.taskQueue.containers.get(0).getWurmId());
        while(!serf.taskQueue.queue.isEmpty())
         serf.taskQueue.removeTask(serf.taskQueue.queue.get(0));
        for (TaskQueue queue : taskGroups)
            queue.removeSerf(serf);
        for (TaskQueue queue : taskAreas)
            queue.removeSerf(serf);
        if(serf.citizenVillage != null)
            serf.citizenVillage.removeCitizen(serf);
        serfs.remove(serf);
    }

    public void addSerf(Serf serf) {
        serfs.add(serf);
    }
}