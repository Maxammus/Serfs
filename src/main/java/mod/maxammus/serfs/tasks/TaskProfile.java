package mod.maxammus.serfs.tasks;

import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.DBUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class TaskProfile {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private TaskQueue selectedQueue = null;
    private ItemTemplate activeItemTemplate;
    private int repeat = 0;
    private boolean whileTimerShows = false;
    private boolean reAdd = false;
    private boolean selected = false;
    public long takeContainerId;
    public long dropContainerId;
    private String takeContainerGroup = "";
    private String dropContainerGroup = "";
    private boolean whileActionAvailable;
    public long profileId;
    boolean changed = false;

    public TaskProfile(long playerId) {
        profileId = ++TaskHandler.profileIdCounter;
        DBUtil.executeSingleStatement("INSERT INTO TaskProfiles " +
                "(PROFILEID, PLAYERID, SELECTEDQUEUE, REPEAT, WHILETIMERSHOWS, WHILEACTIONAVAILABLE, READD, ACTIVEITEMTEMPLATE, TAKECONTAINER, DROPCONTAINER, TAKECONTAINERGROUP, DROPCONTAINERGROUP) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                profileId, playerId, -10, repeat, whileTimerShows, whileActionAvailable, reAdd, -10, takeContainerId, dropContainerId, takeContainerGroup, dropContainerGroup);
    }

    public TaskProfile(ResultSet rs) throws SQLException {
        profileId = rs.getLong("PROFILEID");
        takeContainerId = rs.getLong("TAKECONTAINER");
        dropContainerId = rs.getLong("DROPCONTAINER");
        setRepeat(rs.getInt("REPEAT"));
        setWhileTimerShows(rs.getBoolean("WHILETIMERSHOWS"));
        setWhileActionAvailable(rs.getBoolean("WHILEACTIONAVAILABLE"));
        setReAdd(rs.getBoolean("READD"));
        setTakeContainerGroup(rs.getString("TAKECONTAINERGROUP"));
        setDropContainerGroup(rs.getString("DROPCONTAINERGROUP"));
        setSelectedQueue(TaskHandler.taskQueues.get(rs.getLong("SELECTEDQUEUE")));
        setActiveItemTemplate(ItemTemplateFactory.getInstance().getTemplateOrNull(rs.getInt("ACTIVEITEMTEMPLATE")));
        if(rs.wasNull())
            activeItemTemplate = null;
    }

    public String getActiveItemTemplateName() {
        return getActiveItemTemplate() == null ? "Nothing" : getActiveItemTemplate().getName();
    }

    public String getSelectedQueueIdentity() {
        return getSelectedQueue() == null ? "None" : getSelectedQueue().getIdentity();
    }

    public String getTakeContainerName() {
        return getTakeContainer() == null ? "Ground" : getTakeContainer().getActualName();
    }

    public String getDropContainerName() {
        return getDropContainer() == null ? "Ground" : getDropContainer().getActualName();
    }

    public Serf getFirstSerf() {
        try {
            if(getSelectedQueue().assignedSerfs == null || getSelectedQueue().assignedSerfs.size() == 0) {
                if (getSelectedQueue() instanceof TaskArea) {
                    for (TaskGroup group : ((TaskArea) getSelectedQueue()).assignedGroups)
                        if (group.assignedSerfs.size() > 0)
                            return group.assignedSerfs.get(0);
                }
            }
            else
                return getSelectedQueue().assignedSerfs.get(0);
        }
        //taskProfile or selectedQueue are null
        catch(NullPointerException ignored) {
        }
        return null;
    }

    public TaskQueue getSelectedQueue() {
        return selectedQueue;
    }

    public void setSelectedQueue(TaskQueue selectedQueue) {
        if(!changed)
            changed = this.selectedQueue != selectedQueue;
        this.selectedQueue = selectedQueue;
    }

    public ItemTemplate getActiveItemTemplate() {
        return activeItemTemplate;
    }

    public void setActiveItemTemplate(ItemTemplate activeItemTemplate) {
        if(!changed)
            changed = this.activeItemTemplate != activeItemTemplate;
        this.activeItemTemplate = activeItemTemplate;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        if(!changed)
            changed = this.repeat != repeat;
        this.repeat = repeat;
    }

    public boolean isWhileTimerShows() {
        return whileTimerShows;
    }

    public void setWhileTimerShows(boolean whileTimerShows) {
        if(!changed)
            changed = this.whileTimerShows != whileTimerShows;
        this.whileTimerShows = whileTimerShows;
    }

    public boolean isReAdd() {
        return reAdd;
    }

    public void setReAdd(boolean reAdd) {
        if(!changed)
            changed = this.reAdd != reAdd;
        this.reAdd = reAdd;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if(!changed)
            changed = this.selected != selected;
        this.selected = selected;
    }


    public Item getTakeContainer() {
        return Items.getItemOptional(takeContainerId).orElse(null);
    }

    public Item getDropContainer() {
        return Items.getItemOptional(dropContainerId).orElse(null);
    }

    public String getTakeContainerGroup() {
        return takeContainerGroup;
    }

    public void setTakeContainerGroup(String takeContainerGroup) {
        if(!changed)
            changed = !this.takeContainerGroup.equals(takeContainerGroup);
        this.takeContainerGroup = takeContainerGroup;
    }

    public String getDropContainerGroup() {
        return dropContainerGroup;
    }

    public void setDropContainerGroup(String dropContainerGroup) {
        if(!changed)
            changed = !this.dropContainerGroup.equals(dropContainerGroup);
        this.dropContainerGroup = dropContainerGroup;
    }

    public boolean isWhileActionAvailable() {
        return whileActionAvailable;
    }

    public void setWhileActionAvailable(boolean whileActionAvailable) {
        if(!changed)
            changed = this.whileActionAvailable != whileActionAvailable;
        this.whileActionAvailable = whileActionAvailable;
    }

    public void maybeSave() {
        if(!changed)
            return;
        changed = false;
        DBUtil.executeSingleStatement("UPDATE TaskProfiles SET " +
                "SELECTEDQUEUE=?,TAKECONTAINER=?,DROPCONTAINER=?,ACTIVEITEMTEMPLATE=?,REPEAT=?,READD=?,WHILETIMERSHOWS=?,WHILEACTIONAVAILABLE=?,TAKECONTAINERGROUP=?,DROPCONTAINERGROUP=? WHERE PROFILEID=?",
                selectedQueue != null ? selectedQueue.queueId : -10, takeContainerId, dropContainerId, activeItemTemplate != null ? activeItemTemplate.getTemplateId() : null, repeat, reAdd, whileTimerShows, whileActionAvailable, takeContainerGroup, dropContainerGroup, profileId);
    }

    public boolean deleteFromDb() {
        return DBUtil.executeSingleStatement("DELETE FROM TaskProfiles WHERE PROFILEID=?", profileId);
    }
}
