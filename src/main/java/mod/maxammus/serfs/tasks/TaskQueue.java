package mod.maxammus.serfs.tasks;

import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.DBUtil;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TaskQueue {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public ArrayList<Task> queue = new ArrayList<>();
    public ArrayList<Long> containers = new ArrayList<>();
    public ArrayList<Serf> assignedSerfs = new ArrayList<>();
    public boolean paused = false;
    public String name;
    public long queueId;
    public long playerId;

    public TaskQueue() {
    }
    public TaskQueue(long playerId, String name) {
        this.playerId = playerId;
        this.name = name;
        queueId = ++TaskHandler.queueIdCounter;
        TaskHandler.taskQueues.put(queueId, this);
    }

    public TaskQueue(ResultSet rs) throws SQLException {
        queueId = rs.getLong("QUEUEID");
        name = rs.getString("NAME");
        paused = rs.getBoolean("PAUSED");
        playerId = rs.getLong("PLAYERID");
        TaskHandler.taskQueues.put(queueId, this);
    }

    public List<Task> getAvailableTasks(Serf serf)
    {
        List<Task> toRet = new ArrayList<>();
        Task newTask = queue.get(0);
        toRet.add(newTask);
        queue.remove(newTask);
        newTask.setAssigned(serf);
        serf.log.add("Got task from " + getIdentity() + " - " + newTask.getActionName());
        return toRet;
    }

    public void addTask(TaskProfile taskProfile, short action, long target) {
        Task task = new Task(taskProfile, action, target);
        task.addToDB(queue.size());
        queue.add(task);

    }

    public void addTask(int index, Task task) {
        if(index == -1)
            index = queue.size();
        queue.add(index, task);
        if(task.inDb)
            task.save(index, queueId);
        else
            task.addToDB(index);
    }
    public void addTask(Task task) {
        addTask(-1, task);
    }

    public void removeTask(Task task)
    {
        queue.remove(task);
        task.deleteFromDb();
    }

    public void removeTask(int taskId) {
        for(Task task: queue) {
            if (task.taskId == taskId) {
                removeTask(task);
                return;
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasTasksFor(Serf serf) {
        return !paused && queue.size() > 0 && assignedSerfs.contains(serf);
    }

    public boolean givesTasksTo(Serf serf) {
        return assignedSerfs.contains(serf);
    }

    public String getIdentity() {
        return name;
    }

    public void stop(int id) {
        for(Task task: getActiveTasks()) {
            if(task.taskId == id) {
                task.finishTask("Told to stop");
                break;
            }
        }
    }

    public List<Task> getActiveTasks() {
        List<Task> toRet = new ArrayList<>();
        for(Serf serf : assignedSerfs) {
            ArrayList<Task> tasks = serf.taskQueue.queue;
            if (tasks.size() > 0 && tasks.get(0).parentId == queueId)
                toRet.add(tasks.get(0));
        }
        return toRet;
    }

    public List<Item> getContainerGroup(String group) {
        group = group.replace("Group: ", "");
        ArrayList<Item> toRet = new ArrayList<>();
        for(Item item : getContainers())
            if(item.getDescription().equals(group))
                toRet.add(item);
        return toRet;
    }


    public void addSerf(Serf serf) {
        if(!assignedSerfs.contains(serf)) {
            DBUtil.executeSingleStatement("INSERT INTO SerfAssignments (SERFID,QUEUEID) VALUES (?,?)", serf.getWurmId(), queueId);
            assignedSerfs.add(serf);
        }
    }

    public void removeSerf(Serf serf) {
        DBUtil.executeSingleStatement("DELETE FROM SerfAssignments WHERE SERFID=? AND QUEUEID=?", serf.getWurmId(), queueId);
        assignedSerfs.remove(serf);
    }

    public void reAddOrDelete(Task task) {
        if(task.reAdd) {
            task.repeatedCount = 0;
            task.started = false;
            task.initialized = false;
            addTask(task);
        }
        else
            task.deleteFromDb();
    }

    //Not ideal to have subclasses call this and then their own
    //but can't seem to update two tables in one execute()
    public void save() {
        DBUtil.executeSingleStatement("UPDATE TaskQueues SET PAUSED=? WHERE QUEUEID=?", paused, queueId);
    }

    public void updateSerfs(ArrayList<Serf> newSerfs) {
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("DELETE FROM SerfAssignments WHERE SERFID=? AND QUEUEID=?");
             PreparedStatement ps2 = dbcon.prepareStatement("INSERT INTO SerfAssignments (SERFID,QUEUEID) VALUES (?,?)")) {
            for(int i = 0; i < assignedSerfs.size() ; i++) {
                Serf serf = assignedSerfs.get(i);
                if (!newSerfs.contains(serf)) {
                    ps.setLong(1, serf.getWurmId());
                    ps.setLong(2, queueId);
                    ps.addBatch();
                    removeSerf(serf);
                    i--;
                }
            }
            ps.executeBatch();
            for(Serf serf : newSerfs)
                if(!assignedSerfs.contains(serf)) {
                    ps2.setLong(1, serf.getWurmId());
                    ps2.setLong(2, queueId);
                    ps2.addBatch();
                    addSerf(serf);
                }
            ps2.executeBatch();
        } catch (SQLException e) {
            logger.warning("Exception while editing assigned serfs: " + e.getMessage());
        }
    }

    public void addToDb(long playerId) {
        DBUtil.executeSingleStatement("INSERT INTO TaskQueues (QUEUEID, PLAYERID, NAME, PAUSED) VALUES (?,?,?,?)", queueId, playerId, this.name, paused);
    }

    public boolean deleteFromDb() {
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             Statement statement = dbcon.createStatement();
             PreparedStatement ps = dbcon.prepareStatement("DELETE FROM TaskQueues WHERE QUEUEID=?")) {
            //Turn foreign keys on for this connection to allow the delete cascade to trigger and clean up
            //entries in other tables with this queue's id
            statement.execute("PRAGMA foreign_keys = ON;");

            ps.setLong(1, queueId);
            ps.execute();
            TaskHandler.taskQueues.remove(this.queueId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning("Exception when deleting queue " + queueId + " - " + e.getMessage());
        }
        return false;
    }

    public void removeContainer(long containerId) {
        containers.removeIf(id -> id == containerId);
        DBUtil.executeSingleStatement("DELETE FROM TaskContainerAssignments WHERE QUEUEID=? AND ITEMID=?", queueId, containerId);
    }

    public void updateTaskPositions() {
        //TODO: Probably add a cooldown to this
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("UPDATE Tasks SET QUEUEPOSITION=? WHERE TASKID=?")) {
            for(int i = 0; i < queue.size(); i++) {
                int id = 1;
                ps.setInt(id++, i);
                ps.setLong(id++, queue.get(i).taskId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            logger.warning("Exception while saving task: " + e.getMessage());
        }
    }

    public void addContainer(long target) {
        if(!containers.contains(target)) {
            DBUtil.executeSingleStatement("INSERT INTO TaskContainerAssignments (ITEMID, QUEUEID) VALUES (?,?)", target, queueId);
            containers.add(target);
        }
    }

    public ArrayList<Item> getContainers() {
        ArrayList<Item> toRet = new ArrayList<>(containers.size());
        for(int i = 0; i < containers.size(); i++) {
            Item item = Items.getItemOptional(containers.get(i)).orElse(null);
            if(item == null) {
                removeContainer(containers.get(i));
                continue;
            }
            toRet.add(item);
        }
        return toRet;
    }
}
