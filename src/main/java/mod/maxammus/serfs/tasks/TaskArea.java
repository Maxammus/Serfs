package mod.maxammus.serfs.tasks;

import com.wurmonline.math.Vector2f;
import com.wurmonline.math.Vector3f;
import com.wurmonline.server.WurmId;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.CounterTypes;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.DBUtil;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.wurmonline.server.MiscConstants.NOID;

//Takes the outline of a task and creates the tasks for serfs to accept
public class TaskArea extends TaskQueue implements CounterTypes {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    int tileX;
    int tileY;
    public int length;
    public int width;
    int layer;
    int floor;
    public int counter = 0;
    float rotation;
    public boolean precheckTasks = true;
    public boolean chainedTasks = true;
    public ArrayList<TaskGroup> assignedGroups = new ArrayList<>();
    ArrayList<Item> targetItems = new ArrayList<>();
    ArrayList<Creature> targetCreatures = new ArrayList<>();

    public TaskArea(long playerId, String name, int tileX, int tileY, int layer, int floor, int length, int width, float rotation) {
        super(playerId, name);
        this.tileX = tileX;
        this.tileY = tileY;
        this.layer = layer;
        this.floor = floor;
        //Adjust angle to be more intuitive, round to nearest 90.
        this.rotation = Math.round((rotation % 360) / 90) * 90;
        //Convert to radians
        this.rotation *= (float) (Math.PI / 180);
        Vector2f offset = new Vector2f(width, length);
        offset.rotateAroundOrigin(this.rotation, true);

        //Limit size to world border
        this.length = Math.abs(Zones.safeTileX((int) (tileX + offset.x)) - tileX);
        this.width =  Math.abs(Zones.safeTileY((int) (tileY - offset.y)) - tileY);
        TaskHandler.taskQueues.put(queueId, this);

        addToDb(playerId);
    }

    public TaskArea(ResultSet rs) throws SQLException {
        super(rs);
        tileX = rs.getInt("TILEX");
        tileY = rs.getInt("TILEY");
        length = rs.getInt("LENGTH");
        width = rs.getInt("WIDTH");
        floor = rs.getInt("FLOOR");
        layer = rs.getInt("LAYER");
        counter = rs.getInt("COUNTER");
        rotation = rs.getFloat("ROTATION");
        chainedTasks = rs.getBoolean("CHAINEDTASKS");
        precheckTasks = rs.getBoolean("PRECHECKTASKS");
    }

    public void addTask(TaskProfile taskProfile, short action, long target) {
        Task task = new Task(taskProfile, action, target);
        if(     task.isItemTask() ||
                task.isCreatureTask() ||
                task.isTileTask()) {
            super.addTask(task);

            if(queue.size() == 1)
                populateAreaTargets();
            //Add task to any serf currently working
            //nvm gets confusing if area queue is empty
//            if(chainedTasks) {
//                Set<Serf> serfs = new HashSet<>();
//                for(Task t : taskQueue.queue())
//                    serfs.add(t.assigned);
//                for(Serf serf : serfs) {
//                    List<Task> taskQueue.queue = serf.taskQueue.queue;
//                    if(taskQueue.queue.size() > 0 && taskQueue.queue.get(0).parent == this) {
//                        Task t = createSubtask(taskQueue.queue.get(0), task);
//                        t.setAssigned(serf);
//                        taskQueue.queue.add(t);
//                    }
//                }
//            }
        }
    }
    @Override
    public List<Task> getAvailableTasks(Serf serf) {
        ArrayList<Task> toRet = new ArrayList<>();
        if(queue.size() == 0 || checkAndHandleIfDone())
            return toRet;
        Task task = new Task(queue.get(0));
        Vector3f targetPos = new Vector3f();
        int type = WurmId.getType(task.target);
        long targetId = NOID;

        do {
            if (Task.isCreatureType(type)) {
                Creature targetCreature = targetCreatures.get(counter);
                targetPos = targetCreature.getPos3f();
                targetId = targetCreature.getWurmId();
            } else if (Task.isItemType(type)) {
                Item targetItem = targetItems.get(counter);
                targetPos = targetItem.getPos3f();
                targetId = targetItem.getWurmId();
            } else if (Task.isTileType(type)) {
                Vector2f tilePos = getTaskTilePos(counter);
                targetPos.x = tilePos.x * 4;
                targetPos.y = tilePos.y * 4;
                try {
                    targetPos.z = Zones.calculateHeight(targetPos.x, targetPos.y, layer >= 0);
                } catch (NoSuchZoneException ignored) {}
                targetId = encodeTargetXY(task.target, (short) tilePos.x, (short) tilePos.y);
            }
            //TODO:Maybe do something about the serf not being able to do the task
            //If serf can't do task then don't increase counter and discard this task
            if(!task.isPossibleForSerf(serf))
                return toRet;
            task.target = targetId;
            task.setAssigned(serf);
            task.pos = targetPos;
        }
        //pre-check task if enabled, keep looking if check fails
        while(++counter < getResetCount() && (precheckTasks && !task.taskActionIsAvailable(serf, true)));
        if(targetId == NOID || (precheckTasks && !task.taskActionIsAvailable(serf, true)))
            return toRet;
        toRet.add(task);

        if(chainedTasks)
        {
            for(int i = 1; i < queue.size(); ++i) {
                toRet.add(createSubtask(task, queue.get(i)));
            }
        }
        checkAndHandleIfDone();
        serf.log.add("Got " + toRet.size() + " task(s) from " + getIdentity());
        return toRet;
    }

    //Create a subtask from a template with the main task's position
    private Task createSubtask(Task task, Task subTaskTemplate) {
        long targetId = NOID;
        Task subTask = new Task(subTaskTemplate);
        subTask.pos = new Vector3f(task.pos);
        subTask.setAssigned(task.assigned);
        int subType = WurmId.getType(subTask.target);
        if(Task.isTileType(subType))
            //Use the subtask's target to make sure type, border, data stays intact
            targetId = encodeTargetXY(subTaskTemplate.target, (short) (task.pos.x / 4), (short) (task.pos.y / 4));
        subTask.target = targetId;
        return subTask;
    }

    private boolean checkAndHandleIfDone() {
        if(counter >= getResetCount())
        {
            counter = 0;
            int numRemove = 1;
            if(chainedTasks)
                numRemove = queue.size();
            for(int i = 0; i < numRemove; i++) {
                if(queue.get(0).reAdd) {
                    //Add to back of queue
                    queue.add(queue.get(0));
                    //Remove from front
                    queue.remove(0);
                }
                else
                    removeTask(queue.get(0));
            }
            if(queue.size() > 0)
                populateAreaTargets();
            return true;
        }
        return false;
    }

    public int getResetCount() {
        if(queue.size() == 0)
            return 0;
        int type = WurmId.getType(queue.get(0).target);
        if(Task.isCreatureType(type))
            return targetCreatures.size();
        if(Task.isItemType(type))
            return targetItems.size();
        return width * length;
    }

    private Vector2f getTaskTilePos(int count) {
        int taskX = count % width;
        int taskY = count / width;
        //Reverse direction every other row to prevent the serf walking to the other side to start
        if(taskY % 2 == 1)
            taskX = width - 1 - taskX;
        Vector2f offset = new Vector2f(taskX, taskY);
        offset.rotateAroundOrigin(rotation, true);
        return new Vector2f((int) (tileX + offset.x), (int) (tileY - offset.y));
    }

    private void populateAreaTargets() {
        Task task = queue.get(0);
        int type = WurmId.getType(task.target);
        if(type != COUNTER_TYPE_ITEMS && type != COUNTER_TYPE_TEMPITEMS && type != COUNTER_TYPE_CREATURES)
            return;
        targetItems = new ArrayList<>();
        targetCreatures = new ArrayList<>();
        for (int i = 0; i < width * length; i++){
            Vector2f tile = getTaskTilePos(i);
            final VolaTile t = Zones.getTileOrNull((int) tile.x, (int) tile.y, layer >= 0);
            if(t == null)
                continue;
            if(Task.isItemType(type)) {
                List<Item> tileItems = Arrays.stream(t.getItems())
                        .filter(item -> item.getTemplate() == task.targetItemTemplate)
                        .collect(Collectors.toList());
                //add as many tasks as it takes to move the items
                for(int index = 0; index < tileItems.size(); index += task.whileTimerShows || task.whileActionAvailable ? 100 : task.doTimes)
                    targetItems.add(tileItems.get(index));
            }
            if(Task.isCreatureType(type))
                targetCreatures.addAll(Arrays.stream(t.getCreatures())
                        .filter(creature -> creature.getTemplate() == task.targetCreatureTemplate)
                        .collect(Collectors.toList()));
        }
    }

    //Encode the tile x/y into the target ID
    private long encodeTargetXY(long target, short offsetX, short offsetY) {
        long toRet;
        //Zero out the original x/y bits while keeping the rest
        toRet = target & 0xFFFF00000000FFFFL;
        //Insert the new location
        toRet += ((long)offsetX << 32) + ((long)offsetY << 16);
        return toRet;
    }

    @Override
    public boolean hasTasksFor(Serf serf) {
        if(queue.size() == 0 || paused)
            return false;
        if(assignedSerfs.contains(serf.getWurmId()))
            return true;
        for(TaskQueue taskQueue: assignedGroups)
            if(taskQueue.givesTasksTo(serf))
                return true;
        return false;
    }

    @Override
    public String getIdentity() {
        return "Area: " + name;
    }

    @Override
    public List<Task> getActiveTasks(boolean onlyFirst) {
        List<Task> toRet = super.getActiveTasks(onlyFirst);
        for(TaskGroup group : assignedGroups)
            for(long serfId : group.assignedSerfs) {
                ArrayList<Task> tasks = Serf.fromId((serfId)).taskQueue.queue;
                for(Task task : tasks) {
                    if (task.parentId == queueId)
                        toRet.add(tasks.get(0));
                    if(onlyFirst)
                        break;
                }
            }
        return toRet;
    }

    public void reAddOrDelete(Task task) {
        task.deleteFromDb();
    }

    @Override
    public void save() {
        super.save();
        DBUtil.executeSingleStatement("UPDATE TaskAreas SET COUNTER=?,CHAINEDTASKS=?,PRECHECKTASKS=? WHERE QUEUEID=?", counter, chainedTasks, precheckTasks, queueId);
    }

    public void updateGroups(ArrayList<TaskGroup> newGroups) {
        try (Connection dbcon = ModSupportDb.getModSupportDb();
             PreparedStatement ps = dbcon.prepareStatement("DELETE FROM TaskGroupAssignments WHERE GROUPID=? AND QUEUEID=?");
             PreparedStatement ps2 = dbcon.prepareStatement("INSERT INTO TaskGroupAssignments (GROUPID, QUEUEID) VALUES (?,?)")) {
            for (TaskGroup taskGroup : assignedGroups)
                if (!newGroups.contains(taskGroup)) {
                    ps.setLong(1, taskGroup.queueId);
                    ps.setLong(2, queueId);
                    ps.addBatch();
                }
            ps.executeBatch();
            for (TaskGroup taskGroup : newGroups)
                if (!assignedGroups.contains(taskGroup)) {
                    ps2.setLong(1, taskGroup.queueId);
                    ps2.setLong(2, queueId);
                    ps2.addBatch();
                }
            ps2.executeBatch();
            assignedGroups = newGroups;
        } catch (SQLException e) {
            logger.warning("Exception while editing task area groups: " + e.getMessage());
        }
    }

    @Override
    public boolean addToDb(long playerId) {
        if(super.addToDb(playerId)
                && DBUtil.executeSingleStatement( "INSERT INTO TaskAreas (" +
                "QUEUEID, TILEX, TILEY, LENGTH, WIDTH, FLOOR, LAYER, COUNTER, ROTATION, CHAINEDTASKS, PRECHECKTASKS) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                queueId, tileX, tileY, length, width, floor, layer, counter, rotation, chainedTasks, precheckTasks))
            return true;
        return false;
    }
}
