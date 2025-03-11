package mod.maxammus.serfs.tasks;

import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.DBUtil;
import mod.maxammus.serfs.util.ListUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TaskGroup extends TaskQueue {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public boolean groupwide = false;
    public final Map<Long, List<Task>> groupwideTasks = new LinkedHashMap<>();

    public TaskGroup(long playerId, String name) {
        super(playerId, name);
        addToDb(playerId);
    }

    public TaskGroup(ResultSet rs) throws SQLException {
        super(rs);
        groupwide = rs.getBoolean("GROUPWIDE");
    }

    @Override
    public String getIdentity() {
        return "Group: " + name;
    }

    @Override
    public void addTask(TaskProfile taskProfile, short action, long target) {
        if(groupwide) {
            //Add the same task to all here so the map can be inverted later and keyed by task
            Task task = new Task(taskProfile, action, target);
            for(long serf : groupwideTasks.keySet())
                groupwideTasks.get(serf).add(task);
        }
        else
            super.addTask(taskProfile, action, target);
    }

    public List<Task> getAvailableTasks(Serf serf)
    {
        List<Task> toRet = new ArrayList<>();
        List<Task> taskList = groupwideTasks.get(serf.getWurmId());
        if(taskList != null && !taskList.isEmpty()) {
            //Copy into a new task to give to serf
            Task templateTask = taskList.get(0);
            Task groupTask = new Task(templateTask);
            groupTask.templateTask = templateTask;
            taskList.remove(0);
            toRet.add(groupTask);
            groupTask.setAssigned(serf);
            serf.log.add("Got task from " + getIdentity() + " - " + groupTask.getActionName());
            return toRet;
        }

        Task newTask = queue.get(0);
        newTask.setAssigned(serf);
        toRet.add(newTask);
        queue.remove(newTask);
        serf.log.add("Got task from " + getIdentity() + " - " + newTask.getActionName());
        return toRet;
    }

    @Override
    public boolean hasTasksFor(Serf serf) {
        if(super.hasTasksFor(serf))
            return true;
        if(paused || groupwideTasks.get(serf.getWurmId()) == null)
            return false;
        return !groupwideTasks.get(serf.getWurmId()).isEmpty();
    }

    //Invert groupwideTasks map to handle it by Task for more easily displaying in the menu
    public Map<Task, ArrayList<Long>> getTaskMapForMenu() {
        Map<Task, ArrayList<Long>> map = new LinkedHashMap<>();
        for(Long serfId : groupwideTasks.keySet())
            for(Task task : groupwideTasks.get(serfId)) {
                map.putIfAbsent(task, new ArrayList<>());
                map.get(task).add(serfId);
            }
        return  map;
    }

    public void removeGroupwideTask(int id) {
        Task task = null;
        for(List<Task> list : groupwideTasks.values()) {
            task = ListUtil.findOrNull(list, t -> t.taskId == id);
            if(task != null)
                break;
        }
        if(task == null)
            return;
        for(List<Task> list: groupwideTasks.values())
            list.remove(task);
    }

    @Override
    public void addSerf(long serfId, boolean database) {
        super.addSerf(serfId, database);
        groupwideTasks.putIfAbsent(serfId, new ArrayList<>());
    }

    @Override
    public void removeSerf(long serfId, boolean database) {
        super.removeSerf(serfId, database);
        groupwideTasks.remove(serfId);
    }

    public void reAddOrDelete(Task task) {
        if(task.reAdd) {
            //TODO: This will probably mess up if groupwide is toggled around.
            if (groupwide)
                //add the task's template back to keep all tasks using the same template together
                groupwideTasks.get(task.assigned.getWurmId())
                        .add(task.templateTask);
            else
                queue.add(task);
            task.repeatedCount = 0;
            task.started = false;
        }
        else
            task.deleteFromDb();
    }

    @Override
    public void save() {
        super.save();
        DBUtil.executeSingleStatement("UPDATE TaskGroups SET GROUPWIDE=? WHERE QUEUEID=?", groupwide, queueId);
    }

    @Override
    public boolean addToDb(long playerId) {
        if(super.addToDb(playerId)
                && DBUtil.executeSingleStatement("INSERT INTO TaskGroups (QUEUEID, GROUPWIDE) VALUES (?,?)", queueId, groupwide))
            return true;
        return false;
    }
}
