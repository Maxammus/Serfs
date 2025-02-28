package mod.maxammus.serfs.tasks;

import com.wurmonline.math.TilePos;
import com.wurmonline.math.Vector2f;
import com.wurmonline.math.Vector3f;
import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.creatures.ai.PathTile;
import com.wurmonline.server.items.*;
import com.wurmonline.server.structures.NoSuchWallException;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.CounterTypes;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.DBUtil;
import mod.maxammus.serfs.util.ListUtil;
import mod.maxammus.serfs.util.MiscUtil;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.wurmonline.server.MiscConstants.NOID;
import static com.wurmonline.server.behaviours.Actions.REPAIR;

public class Task implements CounterTypes {
    Logger logger = Logger.getLogger(this.getClass().getName());
    public short action;
    public Vector3f pos = new Vector3f();
    public Serf assigned;
    public long target;
    public int layer;
    public int doTimes;
    public boolean whileTimerShows;
    public boolean whileActionAvailable;
    public boolean reAdd;
    public ItemTemplate activeItemTemplate;
    public ItemTemplate targetItemTemplate;
    public CreatureTemplate targetCreatureTemplate;
    protected long parentId;
    public int repeatedCount = 0;
    public boolean receivedActionTimer = false;
    public boolean started = false;
    public boolean initialized = false;
    public long taskId;
    Task templateTask;
    private long takeContainerId;
    private long dropContainerId;
    String takeContainerGroup;
    String dropContainerGroup;
    public boolean exactTarget = false;
    public boolean inDb = false;
    public String finishReason = "";
    public Task(TaskProfile taskProfile, short action, long target) {
        this.action = action;
        this.target = target;
        doTimes = taskProfile.getRepeat() + 1;
        whileTimerShows = taskProfile.isWhileTimerShows();
        whileActionAvailable = taskProfile.isWhileActionAvailable();
        reAdd = taskProfile.isReAdd();
        activeItemTemplate = taskProfile.getActiveItemTemplate();
        setParent(taskProfile.getSelectedQueue());
        setTakeContainer(taskProfile.getTakeContainer());
        setDropContainer(taskProfile.getDropContainer());
        takeContainerGroup = taskProfile.getTakeContainerGroup();
        dropContainerGroup = taskProfile.getDropContainerGroup();
        targetItemTemplate = null;
        targetCreatureTemplate = null;
        templateTask = null;
        taskId = ++TaskHandler.taskIdCounter;
        try {
            if(isItemTask()) {
                Item item = Items.getItem(target);
                if(item.isBulkItem())
                    targetItemTemplate = item.getRealTemplate();
                else
                    targetItemTemplate = item.getTemplate();
                if (item.isTopParentPile()) {
                    Item[] items = item.getItemsAsArray();
                    //TODO: handle piles with multiple item types
                    if(items.length > 0)
                        targetItemTemplate = items[0].getTemplate();
                }
            }
            else if(isCreatureTask()) {
                Creature creature = Creatures.getInstance().getCreature(target);
                targetCreatureTemplate = creature.getTemplate();
            }
        } catch (NoSuchItemException | NoSuchCreatureException e) {
            logger.info("Task has no such target during creation during creation.");
        }
    }

    //likely to be changed from the template after calling, should addToDb then
    @SuppressWarnings("CopyConstructorMissesField")
    public Task(Task templateTask) {
        action = templateTask.action;
        activeItemTemplate = templateTask.activeItemTemplate;
        target = templateTask.target;
        pos = new Vector3f(templateTask.pos);
        layer = templateTask.layer;
        doTimes = templateTask.doTimes;
        whileTimerShows = templateTask.whileTimerShows;
        whileActionAvailable = templateTask.whileActionAvailable;
        reAdd = templateTask.reAdd;
        parentId = templateTask.parentId;
        targetItemTemplate = templateTask.targetItemTemplate;
        targetCreatureTemplate = templateTask.targetCreatureTemplate;
        takeContainerId = templateTask.takeContainerId;
        dropContainerId = templateTask.dropContainerId;
        takeContainerGroup = templateTask.takeContainerGroup;
        dropContainerGroup = templateTask.dropContainerGroup;
    }

    public Task(ResultSet rs) throws SQLException {
        inDb = true;
        taskId = rs.getLong("TASKID");
        assigned = (Serf)Creatures.getInstance().getCreatureOrNull(rs.getLong("ASSIGNED"));
        parentId = rs.getLong("QUEUEID");
        target = rs.getLong("TARGET");
        takeContainerId =  rs.getLong("TAKECONTAINER");
        dropContainerId = rs.getLong("DROPCONTAINER");
        doTimes = rs.getInt("DOTIMES");
        whileTimerShows = rs.getBoolean("WHILETIMERSHOWS");
        whileActionAvailable = rs.getBoolean("WHILEACTIONAVAILABLE");
        reAdd = rs.getBoolean("READD");
        takeContainerGroup = rs.getString("TAKECONTAINERGROUP");
        dropContainerGroup = rs.getString("DROPCONTAINERGROUP");
        action = (short) rs.getInt("ACTION");
        layer = rs.getInt("LAYER");
        repeatedCount = rs.getInt("REPEATEDCOUNT");
        pos.x = rs.getFloat("POSX");
        pos.y = rs.getFloat("POSY");
        pos.z = rs.getFloat("POSZ");
        started = rs.getBoolean("STARTED");
        initialized = rs.getBoolean("INITIALIZED");
        exactTarget = rs.getBoolean("EXACTTARGET");
        takeContainerGroup = rs.getString("TAKECONTAINERGROUP");
        dropContainerGroup = rs.getString("DROPCONTAINERGROUP");
        activeItemTemplate = ItemTemplateFactory.getInstance().getTemplateOrNull(rs.getInt("ACTIVEITEMTEMPLATE"));
        if(rs.wasNull())
            activeItemTemplate = null;
        targetItemTemplate = ItemTemplateFactory.getInstance().getTemplateOrNull(rs.getInt("TARGETITEMTEMPLATE"));
        if(rs.wasNull())
            targetItemTemplate = null;
        try {
            targetCreatureTemplate = CreatureTemplateFactory.getInstance().getTemplate(rs.getInt("TARGETCREATURETEMPLATE"));
            if(rs.wasNull())
                targetCreatureTemplate = null;
        } catch (NoSuchCreatureTemplateException ignored) {
            targetCreatureTemplate = null;
        }
    }

    public static boolean isTileType(int type) {
        return type == COUNTER_TYPE_TILES || type == COUNTER_TYPE_TILEBORDER || type == COUNTER_TYPE_CAVETILES || type == COUNTER_TYPE_TILECORNER || type == COUNTER_TYPE_FLOORS || type == COUNTER_TYPE_FENCES || type == COUNTER_TYPE_WALLS || type == COUNTER_TYPE_STRUCTURES;
    }
    public static boolean isItemType(int type) {
        return type == COUNTER_TYPE_ITEMS || type == CounterTypes.COUNTER_TYPE_TEMPITEMS;
    }
    public static boolean isCreatureType(int type) {
        return type == COUNTER_TYPE_CREATURES;
    }
    public boolean isTileTask() {
        int type = WurmId.getType(target);
        return isTileType(type);
    }
    public boolean isItemTask() {
        int type = WurmId.getType(target);
        return isItemType(type) || targetItemTemplate != null;
    }
    public boolean isCreatureTask() {
        int type = WurmId.getType(target);
        return isCreatureType(type) || targetCreatureTemplate != null;
    }

    public void setAssigned(Serf creature)
    {
        assigned = creature;
    }

    public void startTask()
    {
        assigned.getStatus().modifyStamina2(100);
        assigned.failedToCarry = false;
        receivedActionTimer = false;
        assigned.log.add("starting task: " + getActionName());
        started = true;
        assigned.turnTowardsPoint(pos.x, pos.y);
        if(action == Actions.TAKE || isDropTask()) {
            handleMoveItems();
            return;
        }
        if(!isTileTask() && !exactTarget)
            if(!findNearbyTargetFromTemplate())
                return;
        try {
            long activeItemId = -1;
            if(activeItemTemplate != null) {
                Item tool = getActiveItem();
                if (tool == null) {
                    finishTask("Tool couldn't be found");
                    return;
                }
                if (checkAndRepairItem(getActiveItem()))
                    assigned.log.add("Repairing active item.");
                activeItemId = tool.getWurmId();
            }
            BehaviourDispatcher.action(assigned, assigned.getCommunicator(), activeItemId, target, action);
        } catch (FailedException | NoSuchItemException | NoSuchWallException  e) {
            //TODO: Higher priority channel for messages like these?
            finishTask("Couldn't start action - " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            repeatedCount++;
        }
    }

    private void handleMoveItems() {
        ArrayList<Long> itemIds = new ArrayList<>();
        Set<Item> targetItems = new HashSet<>();
        long targetId = -1;

        if(action == Actions.TAKE) {
            //Take from ground
            if(getTakeContainer() == null)
                for (VolaTile vt : Zones.getTilesSurrounding((int)pos.x / 4, (int)pos.y / 4, isOnSurface(), 1)) {
                    Item[] items = vt.getItems();
                    if(items.length > 0 )
                        for(Item i : items) {
                            Item parent = i.getTopParentOrNull();
                            //Add watcher to parent to prevent an exception
                            if (parent != null && parent.isHollow()) {
                                parent.addWatcher(parent.getWurmId(), assigned);
                                break;
                            }
                        }
                    targetItems.addAll(Arrays.asList(items));
                }
            else
                targetItems = getTakeContainer().getItems();
        }
        else {
            if(getDropContainer() != null)
                targetId = getDropContainer().getWurmId();
            targetItems = assigned.getInventory().getItems();
        }

        if(action == DropAllNonToolItems.actionId)
            itemIds.addAll(targetItems.stream()
                    .filter(item -> !item.isTool() && (getDropContainer() == null || !getDropContainer().isBulkContainer() || item.isBulk()))
                    .map(Item::getWurmId)
                    .collect(Collectors.toList()));
        else {
            for (Item item : targetItems) {
                ItemTemplate itemTemplate = item.getTemplate();
                //Skip dropping non-bulk items into a bulk container.
                if(isDropTask() && getDropContainer() != null && getDropContainer().isBulkContainer() && !itemTemplate.isBulk())
                    continue;
                if (item.isBulkItem())
                    itemTemplate = item.getRealTemplate();
                if (itemTemplate == targetItemTemplate) {
                    itemIds.add(item.getWurmId());
                    //Amount of items to take from bulk is handled in a later question, just need the one
                    if (action == Actions.TAKE && getTakeContainer() != null && getTakeContainer().isBulkContainer()) {
                        assigned.numItemsToTake = Math.min(item.getBulkNums(), getNumItemsCanTake(itemTemplate));
                        //No more repeating
                        repeatedCount = doTimes;
                        break;
                    } else if (!whileTimerShows && !whileActionAvailable && ++repeatedCount >= doTimes)
                        break;
                }
            }
        }
        if(itemIds.size() == 0) {
            repeatedCount = doTimes;
            return;
        }
        //If dropping to ground
        if(isDropTask() && getDropContainer() == null) {
            long[] targets = new long[itemIds.size()];
            for (int i = 0; i < itemIds.size(); ++i)
                targets[i] = itemIds.get(i);
            try {
                BehaviourDispatcher.action(assigned, assigned.getCommunicator(), -1, targets, Actions.DROP_AS_PILE);
            } catch (FailedException | NoSuchBehaviourException | NoSuchPlayerException | NoSuchCreatureException |
                     NoSuchItemException e) {
                e.printStackTrace();
            }
            return;
        }

        //moving items around inventories
        short nums = (short) itemIds.size();
        ByteBuffer byteBuffer = ByteBuffer.allocate(10 + nums * 8);
        byteBuffer.putShort(nums);
        for(long l : itemIds)
            byteBuffer.putLong(l);
        byteBuffer.putLong(targetId);
        byteBuffer.position(0);
        try {
            ReflectionUtil.callPrivateMethod(assigned.getCommunicator(), ReflectionUtil.getMethod(Communicator.class, "reallyHandle_CMD_MOVE_INVENTORY"), byteBuffer);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private int getNumItemsCanTake(ItemTemplate itemTemplate) {
        int min = MiscUtil.min(
                whileTimerShows || whileActionAvailable ? 99 : doTimes,
                assigned.getCarryCapacityFor(itemTemplate.getWeightGrams()),
                100 - assigned.getInventory().getNumItemsNotCoins());
        return Math.max(min, 0);
    }

    private boolean findNearbyTargetFromTemplate() {
        //inventory tasks just need templates not ids
        if(action == Actions.TAKE || isDropTask())
            return true;
        target = NOID;
        if(targetItemTemplate != null) {
            for (Item item : assigned.getInventory().getItems())
                if (item.getTemplate() == targetItemTemplate) {
                    target = item.getWurmId();
                    break;
                }
            if(target == NOID) {
                VolaTile[] tiles = Zones.getTilesSurrounding((int) pos.x / 4, (int) pos.y / 4, layer >= 0, 1);
                for (VolaTile tile : tiles) {
                    Item item = ListUtil.findOrNull(tile.getItems(),
                            tileItem -> tileItem.getTemplate() == targetItemTemplate);
                    if (item != null) {
                        target = item.getWurmId();
                        break;
                    }
                }
            }
        }
        if(targetCreatureTemplate != null) {
            VolaTile[] tiles = Zones.getTilesSurrounding((int) pos.x / 4, (int) pos.y / 4, layer >= 0, 1);
            for (VolaTile tile : tiles) {
                Creature creature = ListUtil.findOrNull(tile.getCreatures(), c -> c.getTemplate() == targetCreatureTemplate);
                if (creature != null) {
                    target = creature.getWurmId();
                    break;
                }
            }
        }
        if(target != NOID)
            assigned.log.add("Found nearby target");
        else
            finishTask("Couldn't find nearby target.");
        return target != NOID;
    }

    private Item getActiveItem() {
        if(activeItemTemplate == null)
            return null;
        return ListUtil.findOrNull(assigned.getAllItems(), item -> item.getTemplate() == activeItemTemplate);
    }

    public boolean shouldRepeat() {
        return isPossibleForSerf(assigned) && ((repeatedCount < doTimes) || (whileTimerShows && receivedActionTimer) || (whileActionAvailable && taskActionIsAvailable()));
    }

    public void finishTask(String reason) {
        finishReason = "";
        //finishTask could have been called during an earlier check with a more specific reason
        if(assigned == null)
            return;
        assigned.log.add("finishing task: " + reason);
        assigned.taskQueue.queue.remove(this);
        assigned = null;
        TaskQueue parent = getParent();
        if(parent != null) {
            parent.reAddOrDelete(this);
            parent.updateTaskPositions();
        }
    }
    public void poll() {
        if(!finishReason.isEmpty()) {
            finishTask(finishReason);
            return;
        }
        if(!initialized)
            if(!initialize())
                return;
        if(/*!assigned.isMoving() && */assigned.getStatus().getPath() == null) {
            //Update target position in case the creature moved
            if(!started && WurmId.getType(target) == CounterTypes.COUNTER_TYPE_CREATURES) {
                Creature creature = Creatures.getInstance().getCreatureOrNull(target);
                if(creature != null)
                    pos = creature.getPos3f();
                else
                    finishTask("Target creature no longer exists.");
            }
//            if(assigned.getPos2f().distance(getXYWithinTaskRange()) < .1) {
//            if(assigned.getPos2f().distance(getXYWithinTaskRange()) < .1) {
            if(isAssignedInRange()) {
                if(!started)
                    startTask();
                else if(getAction() == null) {
                    if (shouldRepeat()) {
                        assigned.log.add("Repeating");
                        startTask();
                    }
                    else
                        finishTask("Done.");
                }
            }
            else {
                Vector2f finalPos = getXYWithinTaskRange();
                if(finalPos == null)
                    return;
                assigned.startPathingToTile(new PathTile(finalPos.x, finalPos.y, Zones.getTileIntForTile((int) finalPos.x / 4, (int) finalPos.y / 4, layer), layer >= 0, getFloor()));
                assigned.log.add("Walking to task.");
            }
        }
    }

    private boolean isAssignedInRange() {
        float range = action < Actions.actionEntrys.length ? Actions.actionEntrys[action].getRange() : 8f;
        if(WurmId.getType(target) == COUNTER_TYPE_CAVETILES) {
            int tilex = Tiles.decodeTileX(target);
            int tiley = Tiles.decodeTileY(target);
            final MeshIO mesh2 = Server.caveMesh;
            int tile = mesh2.getTile(tilex, tiley);
            int heightOffset = (int) (Tiles.decodeHeight(tile) / 10.0f);
            if(action >= 8000 || action < 2000)
                return assigned.isWithinTileDistanceTo(tilex, tiley, heightOffset, (int) (range / 4));
        }
        return assigned.getPos2f().distance(getXYWithinTaskRange()) < range;
    }

    private Action getAction() {
        try {
            return assigned.getCurrentAction();
        } catch (NoSuchActionException ignored) {
        }
        return null;
    }

    //TODO: Verify math checks out
    public int getFloor() {
        try {
            float groundHeight;
            groundHeight = Math.max(0.0F, Zones.calculateHeight(pos.x, pos.y, isOnSurface()));
            float posZ = Math.max(0.0F, pos.z - groundHeight + 0.5F);
            return (int)posZ / 3;
        } catch (NoSuchZoneException e) {
            return 0;
        }

    }

    private boolean isOnSurface() {
        return layer >= 0;
    }

    private boolean taskActionIsAvailable() {
        return taskActionIsAvailable(assigned, false);
    }

    //TODO: Check if needed item is in inventory for continue actions/advanced creation
    public boolean taskActionIsAvailable(Serf serf, boolean fromTargetPosition) {
        if(!prepareInventoryTasks())
            return false;
        if(isTileTask() && !targetTileStructureExists())
            return false;
        Item activeItem = null;
        if(activeItemTemplate != null)
            activeItem = serf.getInventory().findItem(activeItemTemplate.getTemplateId());
        long activeItemId = activeItem != null ? activeItem.getWurmId() : -1;
        TaskHandler.requestSerfActions(serf.getCommunicator(), (byte)-1, target, serf, activeItemId, fromTargetPosition);
        if(serf.lastAvailableActions == null)
            return false;
        return serf.lastAvailableActions.stream().anyMatch(actionEntry -> actionEntry.getNumber() == action);
    }

    public boolean isPossibleForSerf(Serf serf) {
        if(isItemTask() && action == Actions.TAKE) {
            int carryAmt = MiscUtil.min(
                    whileTimerShows || whileActionAvailable ? 99 : doTimes,
                    serf.getCarryCapacityFor(targetItemTemplate.getWeightGrams()),
                    100 - serf.getInventory().getNumItemsNotCoins());
            return carryAmt > 0;
        }
        return true;
    }

    private boolean prepareInventoryTasks() {
        //If dropping all non tools set one as the target template to use for the rest of the checks
        if (action == DropAllNonToolItems.actionId) {
            Item nonTool = ListUtil.findOrNull(assigned.getInventory().getItems(),
                    item1 -> !item1.isTool() && (getDropContainer() == null || !getDropContainer().isBulkContainer() || item1.isBulk()));
            if (nonTool == null) {
                finishTask("No non-tools to drop");
                return false;
            }
            targetItemTemplate = nonTool.getTemplate();
        }
        if (isDropTask()) {
            Item targetItem = assigned.getInventory().findItem(targetItemTemplate.getTemplateId());
            if(targetItem == null) {
                finishTask("No target items in inventory");
                return false;
            }
                //check container group
            if (!dropContainerGroup.equals("")) {
                Item targetContainer = getClosestContainerWithSpaceInGroup(dropContainerGroup);
                if (targetContainer != null) {
                    setDropContainer(targetContainer);
                    pos = getDropContainer().getPos3f();
                    return true;
                } else {
                    finishTask("No valid drop container with space");
                    return false;
                }
            }
            //Check current container
            if (getDropContainer() != null) {
                if (getDropContainer().testInsertItem(targetItem)) {
                    pos = getDropContainer().getPos3f();
                    return true;
                } else {
                   finishTask("No space in drop container");
                   return false;
                }
            }
            //dropping to ground
            else {
                final int[] tilecoords;
                try {
                    tilecoords = Item.getDropTile(assigned);
                    final VolaTile t = Zones.getTileOrNull(tilecoords[0], tilecoords[1], assigned.isOnSurface());
                    if (t != null)
                        if (t.getNumberOfItems(t.getDropFloorLevel(assigned.getFloorLevel())) < 99)
                            return true;
                } catch (NoSuchZoneException e){
                    finishTask("Error");
                    return false;
                }
            }
        } else if (action == Actions.TAKE && getNumItemsCanTake(targetItemTemplate) > 0) {
            //Check if there's a container group
            if (!takeContainerGroup.equals("")) {
                Item target = getClosestContainerWithItemInGroup(dropContainerGroup);
                if (target != null) {
                    setTakeContainer(target);
                    pos = getTakeContainer().getPos3f();
                    return true;
                }
                finishTask("No take container with target item");
                return false;
            }
            //Check current container
            if (getTakeContainer() != null) {
                pos = getTakeContainer().getPos3f();
                if (getTakeContainer().isBulkContainer())
                    return getTakeContainer().getItems().stream()
                            .anyMatch(item1 -> item1.getRealTemplate() == targetItemTemplate);
                if (getTakeContainer().findItem(targetItemTemplate.getTemplateId()) != null)
                    return true;
                finishTask("No take container with target item");
                return false;
            }
            //taking from ground
            else {
                Set<Item> groundItems = getNearbyGroundItems();
                Item first = ListUtil.findOrNull(groundItems, item1 -> item1.getTemplate() == targetItemTemplate);
                if (first!= null) {
                    target = first.getWurmId();
                    return true;
                }
                finishTask("No target item nearby on the ground");
                return false;
            }
        }
        return true;
    }

    private boolean isDropTask() {
        return action == Actions.DROP || action == Actions.DROP_AS_PILE || action == DropAllNonToolItems.actionId;
    }

    private boolean targetTileStructureExists() {
        int x = Tiles.decodeTileX(target);
        int y = Tiles.decodeTileY(target);
        int heightOffset = Tiles.decodeHeightOffset(target);
        VolaTile tile = Zones.getTileOrNull(x, y, layer >= 0);
        int type = WurmId.getType(target);
        if(type == COUNTER_TYPE_TILES || type == COUNTER_TYPE_CAVETILES || type == COUNTER_TYPE_TILEBORDER || type == COUNTER_TYPE_TILECORNER)
            return true;
        if (tile != null)
            switch (type) {
                case COUNTER_TYPE_FENCES: return tile.getFence(target) != null;
                case COUNTER_TYPE_FLOORS: return Zones.getFloorsAtTile(x, y, heightOffset, heightOffset, layer) != null;
                case COUNTER_TYPE_WALLS: return Arrays.stream(tile.getWallsForLevel(getFloor())).anyMatch(wall -> wall.getId() == target);
                //TODO: What uses structure type
//                case COUNTER_TYPE_STRUCTURES:
                //TODO:Bridges?
//                case COUNTER_TYPE_BRIDGE_PARTS:
            }
        return false;
    }

    public String getTargetName() {
        if(isItemTask() && targetItemTemplate != null)
            return targetItemTemplate.getName();
        if(isCreatureTask() && targetCreatureTemplate != null)
            return targetCreatureTemplate.getName();
        if(isTileTask())
            return "Tile";
        return "Unknown";
    }
    public String getActiveItemTemplateName() {
        return activeItemTemplate == null ? "Nothing" : activeItemTemplate.getName();
    }

    //Set anything that needs to know the state of things as the task begins, e.g. serf's position after last action
    boolean initialize() {
        initialized = true;
        try {
            if(action == Actions.TAKE || isDropTask())
                return prepareInventoryTasks();
            //TODO: Handle advanced creation item retrieval
            //Recipe and creation actions where target needs to be in inventory - Make a take action to get it
            if(action >= 8000 && targetItemTemplate != null && !targetItemTemplate.isNoTake() && !targetItemTemplate.isUseOnGroundOnly()) {
                if(assigned.getInventory().findItem(targetItemTemplate.getTemplateId()) == null) {
                    //Check if a take task can get the item
                    Task takeTask = new Task(this);
                    takeTask.action = Actions.TAKE;
                    takeTask.doTimes = 1;
                    takeTask.whileTimerShows = false;
                    takeTask.whileActionAvailable = false;
                    takeTask.reAdd = false;
                    takeTask.setParent(assigned.taskQueue);
                    takeTask.setAssigned(assigned);
                    if (takeTask.prepareInventoryTasks()) {
                        //reset for later
                        initialized = started = false;
                        //Put the new task first in line
                        assigned.taskQueue.addTask(0, takeTask);
                        assigned.log.add("Pausing " + getActionName() + " to get a " + targetItemTemplate.getName());
                        return false;
                    }
                }
            }
            if(target == NOID)
                return true;
            if(isItemTask()) {
                Item item = Items.getItem(target);
                layer = item.isOnSurface() ? 0 : -1;
                pos = item.getPos3f();
            }
            else if(isCreatureTask()) {
                Creature creature = Creatures.getInstance().getCreature(target);
                layer = creature.getLayer();
                pos = creature.getPos3f();
            }
            else if(isTileTask()) {
                pos.x = Tiles.decodeTileX(target) * 4;
                pos.y = Tiles.decodeTileY(target) * 4;
                pos.z = Tiles.decodeHeightOffset(target) * 4;
                layer = Tiles.decodeLayer(target);
            }
            //TODO: handle other target types that might need it
            else {
                pos = assigned.getPos3f();
                layer = assigned.getLayer();
            }
            return true;
        } catch (NoSuchItemException | NoSuchCreatureException e) {
            finishTask("Target doesn't exist.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Item getClosestContainerWithItemInGroup(String group) {
        TaskQueue parent = getParent();
        if(parent != null) {
            //TODO: Does this work with bulk items?
            return parent.getContainerGroup(group).stream()
                    .filter(item -> item.findItem(targetItemTemplate.getTemplateId()) != null)
                    //Sort and pick closest
                    .min((o1, o2) -> (int) (o1.getPos3f().distance(pos) - o2.getPos3f().distance(pos)))
                    .orElse(null);
        }
        return null;
    }

    //TODO: Maybe use the target item with the smallest volume to filter.
    public Item getClosestContainerWithSpaceInGroup(String group) {
        Item targetItem = assigned.getInventory().findItem(targetItemTemplate.getTemplateId());
        if(targetItem == null)
            return null;
        TaskQueue parent = getParent();
        if(parent != null) {
            return parent.getContainerGroup(group).stream()
                    .filter(item -> item.testInsertItem(targetItem))
                    .min((o1, o2) -> (int) (o1.getPos3f().distance(assigned.getPos3f()) - o2.getPos3f().distance(assigned.getPos3f())))
                    .orElse(null);
        }
        return null;
    }

    public String getActionName() {
        if(action > 10000 && activeItemTemplate != null && targetItemTemplate != null){
            try {
                CreationEntry creation = CreationMatrix.getInstance().getCreationEntry(activeItemTemplate.getTemplateId(), targetItemTemplate.getTemplateId(), action - 10000);

                ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(creation.getObjectCreated());
                return "create " + template.getName();
            } catch (NoSuchEntryException | NoSuchTemplateException e) {
                e.printStackTrace();
            }
        }
        else if(action > 8000) {
            Recipe recipe = Recipes.getRecipeByActionId(action);
            if(recipe != null)
                return recipe.getName();
        }
        else if(action < Actions.actionEntrys.length)
            return Actions.actionEntrys[action].getActionString();
        return "";
    }


    private Set<Item> getNearbyGroundItems() {
        Set<Item> toRet = new HashSet<>();
        Set<VolaTile> tiles = assigned.currentTile.getThisAndSurroundingTiles(1);
        for (VolaTile tile : tiles) {
            Item[] items = tile.getItems();
            toRet.addAll(Arrays.asList(items));
        }
        return toRet;
    }

    public Vector2f getXYWithinTaskRange() {
        Vector2f pos2f = new Vector2f(pos.x, pos.y);
        Vector2f toRet = pos2f;
        float range = this.action < Actions.actionEntrys.length ? Actions.actionEntrys[action].getRange() / 1.25f : 1.5f;
        Vector2f heading = pos2f.subtract(assigned.getPos2f());
        if(isTileTask()) {
            if(WurmId.getType(target) == CounterTypes.COUNTER_TYPE_CAVETILES) {
                pos = pos.add(2, 2, 0);
                int tilex = Tiles.decodeTileX(target);
                int tiley = Tiles.decodeTileY(target);
                final MeshIO mesh = Server.caveMesh;
                //if target tile is a cave wall check cardinal directions
                if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex, tiley)))) toRet.set(pos.x, pos.y );
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex + 1, tiley)))) toRet.set(pos.x + 4, pos.y);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex - 1, tiley)))) toRet.set(pos.x - 4, pos.y);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex, tiley + 1)))) toRet.set(pos.x, pos.y + 4);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex, tiley - 1)))) toRet.set(pos.x, pos.y - 4);
                //all surrounding tiles are solid
                else {
                    finishTask("Impossible to reach target tile.");
                    return null;
                }
            }
            //standing too close prevents finishing fences
            //TODO: figure out a better way for this
            else if(action == Actions.CONTINUE_BUILDING_FENCE)
                toRet = pos2f.add(3, 3);
            else
                toRet = pos2f.add(1, 1);
        }
        //Don't move if within range
        else if(assigned.getPos2f().distance(pos2f) > range)
            //set position a few units closer to serf instead of on top of target
            toRet = pos2f.subtract(heading.normalize().mult(range));
        return toRet;
    }

    public void save(int position, long queueId) {
        DBUtil.executeSingleStatement("UPDATE Tasks SET QUEUEPOSITION=?,QUEUEID=?,POSX=?,POSY=?,POSZ=?,LAYER=?,ASSIGNED=?,TARGET=?,REPEATEDCOUNT=?,STARTED=?,INITIALIZED=?,TAKECONTAINER=?,DROPCONTAINER=? WHERE TASKID=?", position, queueId, pos.x, pos.y, pos.z, layer, assigned != null ? assigned.getWurmId() : null, target, repeatedCount, started, initialized, takeContainerId, dropContainerId, taskId);
    }
    public void addToUpdateBatch(PreparedStatement ps, int position, long queueId) throws SQLException {
        DBUtil.addStatementToBatch(ps, position, queueId, pos.x, pos.y, pos.z, layer, assigned != null ? assigned.getWurmId() : null, target, repeatedCount, started, initialized, takeContainerId, dropContainerId, taskId);
    }

    //should be called as it gets added to parent queue
    public void addToDB(int position) {
        if(inDb) {
            logger.warning("Task called addToDB while already in database");
            return;
        }
        if(getParent() == null) {
            logger.warning("Task called addToDB while parent is null");
            return;
        }
        taskId = ++TaskHandler.taskIdCounter;
        DBUtil.executeSingleStatement("INSERT INTO Tasks " +
                        "(QUEUEPOSITION,TASKID,QUEUEID,ACTION,POSX,POSY,POSZ,LAYER,ASSIGNED,TARGET,DOTIMES,WHILETIMERSHOWS,WHILEACTIONAVAILABLE,READD,ACTIVEITEMTEMPLATE,TARGETITEMTEMPLATE,TARGETCREATURETEMPLATE,PARENT,REPEATEDCOUNT,STARTED,INITIALIZED,TAKECONTAINER,DROPCONTAINER,TAKECONTAINERGROUP,DROPCONTAINERGROUP,EXACTTARGET) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                position, taskId, parentId, action, pos.x, pos.y, pos.z, layer, assigned != null ? assigned.getWurmId() : null, target, doTimes, whileTimerShows, whileActionAvailable, reAdd, activeItemTemplate != null ? activeItemTemplate.getTemplateId() : null, targetItemTemplate != null ? targetItemTemplate.getTemplateId() : null, targetCreatureTemplate != null ? targetCreatureTemplate.getTemplateId() : null, parentId, repeatedCount, started, initialized, takeContainerId, dropContainerId, takeContainerGroup, dropContainerGroup, exactTarget);
        inDb = true;
    }

    public boolean deleteFromDb() {
        return DBUtil.executeSingleStatement("DELETE FROM Tasks WHERE TASKID=?", taskId);
    }

    public TaskQueue getParent() {
        return TaskHandler.taskQueues.get(parentId);
    }

    public void setParent(TaskQueue parent) {
        this.parentId = parent.queueId;
    }

    public Item getTakeContainer() {
        return Items.getItemOptional(takeContainerId).orElse(null);
    }

    public void setTakeContainer(Item takeContainerId) {
        this.takeContainerId = takeContainerId != null ? takeContainerId.getWurmId() : NOID;
    }

    public Item getDropContainer() {
        return Items.getItemOptional(dropContainerId).orElse(null);
    }

    public void setDropContainer(Item dropContainerId) {
        this.dropContainerId = dropContainerId != null ? dropContainerId.getWurmId() : NOID;
    }

    public boolean checkAndRepairItem(Item tool) {
        if (tool == null)
            return false;
        if (tool.getDamage() > 10.0f && tool.isRepairable()) {
            try {
                BehaviourDispatcher.action(assigned, assigned.getCommunicator(), NOID, tool.getWurmId(), REPAIR);
                return true;
            } catch (Exception e) {
                logger.warning("Exception when trying to repair serf tool - " + e.getMessage());
            }
        }
        return false;
    }
}
