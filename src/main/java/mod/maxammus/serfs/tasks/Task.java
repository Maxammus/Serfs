package mod.maxammus.serfs.tasks;

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
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.util.ListUtil;
import mod.maxammus.serfs.util.MiscUtil;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.wurmonline.server.MiscConstants.NOID;
import static com.wurmonline.server.behaviours.Actions.*;

public class Task implements CounterTypes {
    final Logger logger = Logger.getLogger(this.getClass().getName());
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
                //Pile of items
                if (item.getTemplateId() == 177) {
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

    public void startTask()  {
        if(assigned.getStatus().getHunger() > 60000)
            assigned.getStatus().modifyHunger(50000 - assigned.getStatus().getHunger(), Math.max(0, assigned.getStatus().getNutritionlevel() - 3));
        assigned.getStatus().modifyStamina2(100);
        assigned.failedToCarry = false;
        receivedActionTimer = false;
        assigned.log.add("starting task: " + getActionName());
        started = true;
        assigned.turnTowardsPoint(pos.x, pos.y);
        if(action == TAKE || isDropTask()) {
            handleMoveItems();
            return;
        }
        repeatedCount++;
        if(!isTileTask() && (!exactTarget || action == IMPROVE))
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
        } catch (FailedException | NoSuchItemException | NoSuchWallException | NoSuchPlayerException |
                 NoSuchCreatureException | NoSuchBehaviourException e) {
            //TODO: Higher priority channel for messages like these?
            finishTask("Couldn't start action - " + e.getMessage());
        }
    }

    private void handleMoveItems() {
        ArrayList<Long> itemIds = new ArrayList<>();
        Set<Item> targetItems = new HashSet<>();
        long targetId = -1;

        if(action == TAKE) {
            if(exactTarget) {
                Item item = Items.getItemOptional(target).orElse(null);
                if(item == null) {
                    finishTask("Target item doesn't exist");
                    return;
                }
                targetItems.add(item);
                Item parent = item.getTopParentOrNull();
                //Add watcher to parent to prevent an exception
                if (parent != null && parent.isHollow())
                    parent.addWatcher(parent.getWurmId(), assigned);
            }
            //Take from ground
            else if(getTakeContainerWithItem(targetItemTemplate) == null) {
                for (VolaTile vt : Zones.getTilesSurrounding((int) pos.x / 4, (int) pos.y / 4, isOnSurface(), 1)) {
                    Item[] items = vt.getItems();
                    for (Item i : items) {
                        Item parent = i.getTopParentOrNull();
                        //Add watcher to parent to prevent an exception
                        if (parent != null && parent.isHollow()) {
                            parent.addWatcher(parent.getWurmId(), assigned);
                            break;
                        }
                    }
                    targetItems.addAll(Arrays.asList(items));
                }
            }
            else
                targetItems = getTakeContainerWithItem(targetItemTemplate).getItems();
        }
        else {
            if(dropContainerId != -10)
                targetId = dropContainerId;
            if(exactTarget) {
                Item item = Items.getItemOptional(target).orElse(null);
                if(item == null) {
                    finishTask("Target item doesn't exist");
                    return;
                }
                targetItems.add(item);
                Item parent = item.getTopParentOrNull();
                //Add watcher to parent to prevent an exception
                if (parent != null && parent.isHollow())
                    parent.addWatcher(parent.getWurmId(), assigned);
            }
            else
                targetItems = assigned.getInventory().getItems();
        }

        if(action == DropAllNonToolItems.actionId) {
            //Hatchet, smelting pot
            List<Integer> toolIds = Arrays.asList(7, 788);
            itemIds.addAll(targetItems.stream()
                    .filter(item -> (!item.isTool() && !toolIds.contains(item.getTemplateId())) && (getDropContainer() == null || !getDropContainer().isBulkContainer() || item.isBulk()))
                    .map(Item::getWurmId)
                    .collect(Collectors.toList()));
            repeatedCount++;
        }
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
                    if (action == TAKE && getTakeContainerWithItem(targetItemTemplate) != null && getTakeContainerWithItem(targetItemTemplate).isBulkContainer()) {
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
                BehaviourDispatcher.action(assigned, assigned.getCommunicator(), -1, targets, DROP_AS_PILE);
            } catch (FailedException | NoSuchBehaviourException | NoSuchPlayerException | NoSuchCreatureException |
                     NoSuchItemException e) {
                logger.warning("Failed to dispatch action in handleMoveItems - " + e.getMessage());
                finishTask("Couldn't drop items");
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
            logger.warning("Failed to invoke reallyHandle_CMD_MOVE_INVENTORY");
            finishTask("Couldn't move items");
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
        if(isInventoryAction())
            return true;
        target = NOID;
        if(targetItemTemplate != null) {
            if(action == IMPROVE) {
                //Get the lowest quality item
                Item targetItem = getNearbyItemsWithTemplate(targetItemTemplate).stream()
                            .filter(item ->
                                    !item.isNewbieItem() && !item.isChallengeNewbieItem()
                                    && (!item.isMetal() || item.isNoTake() || item.getTemperature() > 3500))
                            .min((a, b) -> (int) (a.getQualityLevel() * 100 - b.getQualityLevel() * 100))
                            .orElse(null);
                if(targetItem == null) {
                    finishTask("Target not found");
                    return false;
                }
                target = targetItem.getWurmId();
                exactTarget = true;

                if(targetItem.getDamage() > 0.0f) {
                    Task repairTask = insertTask(REPAIR);
                    repairTask.target = targetItem.getWurmId();
                    repairTask.exactTarget = true;
                }
                try {
                    //improved by adding material
                    if(targetItem.creationState == 0) {
                        activeItemTemplate = ItemTemplateFactory.getInstance().getTemplate(
                                MethodsItems.getImproveTemplateId(targetItem));
                        Predicate<Item> canImp = item -> item.getTemplate() == activeItemTemplate
                                && (!activeItemTemplate.isMetalLump() || item.getTemperature() > 3500)
                                && (Materials.isLeather(item.getMaterial()) || item.getQualityLevel() > targetItem.getQualityLevel());
                        Item active = getActiveItem();
                        if(active == null || !canImp.test(active)) {
                            //can't use active item, drop it
                            if(active != null) {
                                Task dropTask = insertTask(DROP);
                                dropTask.targetItemTemplate = activeItemTemplate;
                                dropTask.target = active.getWurmId();
                                dropTask.exactTarget = true;
                            }
                            Item newActive = null;
                            if(!takeContainerGroup.isEmpty() || takeContainerId != -10) {
                                Item takeContainer = getTakeContainerWithItem(activeItemTemplate);
                                if (takeContainer != null)
                                    //Get the lowest quality material that can be used to improve
                                    newActive = Arrays.stream(takeContainer.getAllItems(true))
                                            .filter(canImp)
                                            .min((a, b) -> (int) (a.getQualityLevel() * 100 - b.getQualityLevel() * 100))
                                            .orElse(null);
                            }
                            else
                                newActive = getNearbyItemsWithTemplate(activeItemTemplate).stream()
                                        .filter(canImp)
                                        .min((a, b) -> (int) (a.getQualityLevel() * 100 - b.getQualityLevel() * 100))
                                        .orElse(null);
                            if(newActive != null) {
                                //Get new material
                                Task takeTask = insertTask(TAKE);
                                takeTask.target = newActive.getWurmId();
                                takeTask.targetItemTemplate = activeItemTemplate;
                                takeTask.exactTarget = true;
                                return false;
                            }
                            else {
                                finishTask("Couldn't find suitable material to improve with");
                                return false;
                            }
                        }
                    }
                    else
                        activeItemTemplate = ItemTemplateFactory.getInstance().getTemplate(
                                MethodsItems.getItemForImprovement(targetItem.getMaterial(), targetItem.creationState));
                    //Water
                    if(activeItemTemplate.getTemplateId() == 128) {
                        if(getActiveItem() == null) {
                            Item waterHolder = null;
                            for(Item item : assigned.getAllItems())
                                if(item.isContainerLiquid() && item.isEmpty(true)) {
                                    waterHolder = item;
                                    break;
                                }
                            if(waterHolder == null) {
                                finishTask("No empty container to fill with water");
                                return false;
                            }
                            Item newTarget = null;
                            if(!takeContainerGroup.isEmpty() || takeContainerId != -10) {
                                Item takeContainer = getTakeContainerWithItem(activeItemTemplate);
                                if (takeContainer != null)
                                    //Get the lowest quality material that can be used to improve
                                    newTarget = takeContainer.findItem(activeItemTemplate.getTemplateId());
                            }
                            else
                                newTarget = getNearbyItemsWithTemplate(activeItemTemplate).stream()
                                        .filter(item -> item.getTemplate() == activeItemTemplate)
                                        .findFirst()
                                        .orElse(null);
                            if(newTarget != null) {
                                //Get new material
                                Task fillTask = insertTask(FILL);
                                //TODO: This can get another item with the same template, add activeItemId?
                                fillTask.activeItemTemplate = waterHolder.getTemplate();
                                fillTask.target = newTarget.getWurmId();
                                fillTask.targetItemTemplate = activeItemTemplate;
                                fillTask.exactTarget = true;
                                return false;
                            }
                            finishTask("Couldn't find water");
                            return false;
                        }
                    }
                } catch (NoSuchTemplateException e) {
                    logger.warning("Missing improving tool template.");
                }
            }
            else
                getNearbyItemsWithTemplate(targetItemTemplate).stream()
                    .findFirst()
                    .ifPresent(targetItem -> target = targetItem.getWurmId());
//            if(target == NOID) {
//                VolaTile[] tiles = Zones.getTilesSurrounding((int) pos.x / 4, (int) pos.y / 4, layer >= 0, 1);
//                for (VolaTile tile : tiles) {
//                    Item item = ListUtil.findOrNull(tile.getItems(),
//                            tileItem -> tileItem.getTemplate() == targetItemTemplate);
//                    if (item != null) {
//                        target = item.getWurmId();
//                        break;
//                    }
//                }
//            }
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

    private @Nullable Item findCreationMaterial() {
        List<Item> items = Collections.emptyList();
        if(!takeContainerGroup.isEmpty() || takeContainerId != -10) {
            Item takeContainer = getTakeContainerWithItem(targetItemTemplate);
            if (takeContainer != null)
                items = Arrays.stream(takeContainer.getAllItems(true))
                        .collect(Collectors.toList());
        }
        else
            items = getNearbyItemsWithTemplate(targetItemTemplate);
        if(items.isEmpty())
            return null;
        Item toRet = items.get(0);
        //Creation actions
        if(action > 10000 && activeItemTemplate != null && targetItemTemplate != null) {
            try {
                CreationEntry creation = CreationMatrix.getInstance().getCreationEntry(
                        activeItemTemplate.getTemplateId(),
                        targetItemTemplate.getTemplateId(),
                        action - 10000);
                ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(creation.getObjectCreated());
                if (activeItemTemplate.getTemplateId() == creation.getObjectTarget() && targetItemTemplate.getTemplateId() == creation.getObjectSource()) {
                    assigned.log.add("Active and target item are reversed, can't choose target");
                    return toRet;
                }
                Item realSource = getActiveItem();
                if(realSource == null)
                    return toRet;

                Method checkSaneAmounts;
                try {
                    checkSaneAmounts = ReflectionUtil.getMethod(CreationEntry.class, "checkSaneAmounts");
                } catch (NoSuchMethodException e) {
                    logger.warning("Couldn't find CreationEntry.checkSaneAmounts");
                    return null;
                }
                //Filter any items that don't have enough material for the crafted item
                Predicate<Item> filter = targetItem -> {
                    try {
                        int sourceWeightToRemove = creation.getSourceWeightToRemove(realSource, targetItem, template, creation.isAdvanced());
                        int targetWeightToRemove = creation.getTargetWeightToRemove(realSource, targetItem, template, creation.isAdvanced());
                        if (creation.getObjectCreated() == 1272 || creation.getObjectCreated() == 1347) {
                            targetWeightToRemove = targetItemTemplate.getWeightGrams();
                        }
                        ReflectionUtil.callPrivateMethod(creation, checkSaneAmounts, realSource, sourceWeightToRemove, targetItem, targetWeightToRemove, template, assigned, creation.isAdvanced());
                    } catch (IllegalAccessException e) {
                        logger.warning("Couldn't run CreationEntry.checkSaneAmounts - " + e.getMessage());
                        return false;
                    } catch (InvocationTargetException e) {
                        //checkSaneAmounts fails by throwing an exception
                        return false;
                    }
                    return true;
                };
                //Filter metal creation where the item isn't glowing
                if (targetItemTemplate.isMetal() && activeItemTemplate.isMetal())
                    filter = filter.and(targetItem -> targetItem.getTemperature() > 3500);
                return ListUtil.findOrNull(items, filter);
            } catch (NoSuchEntryException | NoSuchTemplateException e) {
                logger.info("Error getting creation target for serf task");
                return null;
            }
        }
        return toRet;
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
        assigned.getStatus().setPath(null);
        assigned.stopCurrentAction();
        assigned = null;
        TaskQueue parent = getParent();
        if(parent != null) {
            parent.reAddOrDelete(this);
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
        if(assigned.getStatus().getPath() == null) {
            //Update target position in case the creature moved
            if(!started && WurmId.getType(target) == CounterTypes.COUNTER_TYPE_CREATURES) {
                Creature creature = Creatures.getInstance().getCreatureOrNull(target);
                if(creature != null)
                    pos = creature.getPos3f();
                else
                    finishTask("Target creature no longer exists.");
            }
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
                Vector3f finalPos = getPosWithinTaskRange();
                if(finalPos == null)
                    return;
                assigned.startPathingToTile(new PathTile(finalPos.x, finalPos.y, Zones.getTileIntForTile((int) finalPos.x / 4, (int) finalPos.y / 4, layer), layer >= 0, getFloor()));
            }
        }
    }

    private boolean isAssignedInRange() {
        float range = 2f; //action < Actions.actionEntrys.length ? Actions.actionEntrys[action].getRange() : 4f;
        if(WurmId.getType(target) == COUNTER_TYPE_CAVETILES) {
            int tilex = Tiles.decodeTileX(target);
            int tiley = Tiles.decodeTileY(target);
            final MeshIO mesh2 = Server.caveMesh;
            int tile = mesh2.getTile(tilex, tiley);
            int heightOffset = (int) (Tiles.decodeHeight(tile) / 10.0f);
            return assigned.isWithinTileDistanceTo(tilex, tiley, heightOffset, (int) Math.ceil(range / 4));
        }
        return assigned.getPos3f().distanceSquared(new Vector3f(pos.x, pos.y, pos.z)) < range * range;
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
        if(isInventoryAction())
            return prepareInventoryTasks();
        if(isTileTask() && !targetTileStructureExists())
            return false;
        Item activeItem = getActiveItem();
        long activeItemId = activeItem != null ? activeItem.getWurmId() : -1;
        TaskHandler.requestSerfActions(serf.getCommunicator(), (byte)-1, target, serf, activeItemId, fromTargetPosition);
        if(serf.lastAvailableActions == null)
            return false;
        return serf.lastAvailableActions.stream().anyMatch(actionEntry -> actionEntry.getNumber() == action);
    }

    private boolean isInventoryAction() {
        return action == DropAllNonToolItems.actionId ||
         action == TAKE ||
         action == DROP;
    }

    public boolean isPossibleForSerf(Serf serf) {
        if(isItemTask() && action == TAKE) {
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
            if (!dropContainerGroup.isEmpty()) {
                Item targetContainer = getClosestContainerWithSpaceInGroup(dropContainerGroup);
                if (targetContainer != null) {
                    setDropContainer(targetContainer);
                    pos = targetContainer.getPos3f();
                    layer = targetContainer.isOnSurface() ? 0 : -1;
                    return true;
                } else {
                    finishTask("No valid drop container with space");
                    return false;
                }
            }
            //Check current container
            if (getDropContainer() != null) {
                Item dropContainer = getDropContainer();
                if (dropContainer.testInsertItem(targetItem)) {
                    pos = dropContainer.getPos3f();
                    layer = dropContainer.isOnSurface() ? 0 : -1;
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
                        if (t.getNumberOfItems(t.getDropFloorLevel(assigned.getFloorLevel())) > 99)
                            return false;
                    pos = assigned.getPos3f();
                    layer = assigned.getLayer();
                    return true;
                } catch (NoSuchZoneException e){
                    finishTask("Error");
                    return false;
                }
            }
        } else if (action == TAKE) {
            if(getNumItemsCanTake(targetItemTemplate) == 0) {
                finishTask("Cannot carry item.");
                return false;
            }
            //Check if there's a container group
            if (!takeContainerGroup.isEmpty()) {
                Item container = getClosestContainerWithItemInGroup(targetItemTemplate, takeContainerGroup);
                if (container == null) {
                    finishTask("No take container with target item");
                    return false;
                }
                setTakeContainer(container);
            }
            //Check current container
            if(takeContainerId != -10) {
                Item container = getTakeContainerWithItem(targetItemTemplate);
                if(container == null)
                    return false;
                pos = container.getPos3f();
                layer = container.isOnSurface() ? 0 : -1;
                if (container.isBulkContainer())
                    return container.getItems().stream()
                            .anyMatch(item1 -> item1.getRealTemplate() == targetItemTemplate);
                if (container.findItem(targetItemTemplate.getTemplateId()) != null)
                    return true;
                finishTask("No take container with target item");
                return false;
            }
            //taking from ground
            else {
                Item targetItem;
                if(exactTarget)
                    targetItem = Items.getItemOptional(target).orElse(null);
                else {
                    List<Item> nearbyItems = getNearbyItemsWithTemplate(targetItemTemplate);
                    targetItem = nearbyItems.get(0);
                }
                if (targetItem!= null) {
                    this.target = targetItem.getWurmId();
                    pos = targetItem.getPos3f();
                    layer = targetItem.isOnSurface() ? 0 : -1;
                    return true;
                }
                finishTask("No target item nearby on the ground");
                return false;
            }
        }
        return true;
    }

    private boolean isDropTask() {
        return action == DROP || action == DROP_AS_PILE || action == DropAllNonToolItems.actionId;
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
        if(Serfs.autoDropWhenCannotCarryActions.contains(action) && assigned.getInventory().getNumItemsNotCoins() >= 100) {
            insertTask(DropAllNonToolItems.actionId);
            return false;
        }
        initialized = true;
        try {
            if(isInventoryAction())
                return prepareInventoryTasks();
            //TODO: Handle advanced creation item retrieval
            //Recipe and creation actions where target needs to be in inventory - Make a take action to get it
            if(action >= 8000 && targetItemTemplate != null && !targetItemTemplate.isNoTake() && !targetItemTemplate.isUseOnGroundOnly()) {
                if(assigned.getInventory().findItem(targetItemTemplate.getTemplateId()) == null) {
                    //Check if a take task can get the item
                    Task takeTask = insertTask(TAKE);
                    takeTask.targetItemTemplate = targetItemTemplate;
                    //If not taking from a bulk container make sure the target can actually be used for creation
                    if(getTakeContainerWithItem(targetItemTemplate) == null || !getTakeContainerWithItem(targetItemTemplate).isBulkContainer()) {
                        Item targetItem = findCreationMaterial();
                        if(targetItem != null) {
                            takeTask.target = targetItem.getWurmId();
                            takeTask.exactTarget = true;
                        }
                    }
                    if(!takeTask.initialize()) {
                        takeTask.finishTask("Couldn't initialize.");
                        finishTask("Couldn't get missing item.");
                    }
                    return false;
                }
            }
            if(target == NOID)
                return true;
            if(isItemTask()) {
                if(action == UNEQUIP)
                    exactTarget = true;
                if(exactTarget) {
                    Item item = Items.getItemOptional(target).orElse(null);
                    if(item == null) {
                        finishTask("Exact target couldn't be found.");
                        return false;
                    }
                    layer = item.isOnSurface() ? 0 : -1;
                    pos = item.getPos3f();
                }
                else {
                    layer = assigned.isOnSurface() ? 0 : -1;
                    pos = assigned.getPos3f();
                }
            }
            else if(isCreatureTask()) {
                Creature creature = Creatures.getInstance().getCreature(target);
                layer = creature.getLayer();
                pos = creature.getPos3f();
            }
            else if(isTileTask()) {
                layer = Tiles.decodeLayer(target);
                pos.x = Tiles.decodeTileX(target) * 4;
                pos.y = Tiles.decodeTileY(target) * 4;
                try {
                    pos.z = Zones.calculateHeight(pos.x, pos.y, layer >= 0);
                } catch (NoSuchZoneException ignored) { }
            }
            //TODO: handle other target types that might need it
            else {
                pos = assigned.getPos3f();
                layer = assigned.getLayer();
            }
            return true;
        } catch (NoSuchCreatureException e) {
            finishTask("Target doesn't exist.");
        }
        return false;
    }

    public Task insertTask(short taskAction) {
        Task newTask = new Task(this);
        newTask.action = taskAction;
        newTask.doTimes = 1;
        newTask.whileTimerShows = false;
        newTask.whileActionAvailable = false;
        newTask.reAdd = false;
        newTask.exactTarget = false;
        newTask.setParent(assigned.taskQueue);
        newTask.setAssigned(assigned);
        //reset for later
        initialized = started = false;
        //Put the new task first in line
        assigned.taskQueue.addTask(0, newTask);
        assigned.log.add("Pausing " + getActionName() + " to " + newTask.getActionName());
        return newTask;
    }

    private Item getClosestContainerWithItemInGroup(ItemTemplate itemTemplate, String group) {
        TaskQueue parent = getParent();
        if(parent != null) {
            //TODO: Does this work with bulk items?
            return parent.getContainerGroup(group).stream()
                    .filter(item -> item.findItem(itemTemplate.getTemplateId()) != null)
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
                return "ERROR";
            }
        }
        else if(action > 8000) {
            Recipe recipe = Recipes.getRecipeByActionId(action);
            if(recipe != null)
                return recipe.getName();
        }
        else if(action < actionEntrys.length)
            return actionEntrys[action].getActionString();
        return "";
    }


    private List<Item> getNearbyItemsWithTemplate(ItemTemplate targetItemTemplate) {
        List<Item> toRet = new ArrayList<>();
        for(Item item : assigned.getAllItems())
            if(item.getTemplate() == targetItemTemplate)
                toRet.add(item);
        if(!toRet.isEmpty())
            return toRet;
        Set<VolaTile> tiles = assigned.currentTile.getThisAndSurroundingTiles(1);
        for (VolaTile tile : tiles) {
            for(Item item : tile.getItems()) {
                if(item.getTemplate() == targetItemTemplate)
                    toRet.add(item);
                for(Item item2 : item.getAllItems(true))
                    if(item2.getTemplate() == targetItemTemplate)
                        toRet.add(item2);
            }
        }
        return toRet;
    }

    public Vector3f getPosWithinTaskRange() {
        Vector3f toRet = new Vector3f(pos);
        float range = /*this.action < Actions.actionEntrys.length ? Actions.actionEntrys[action].getRange() / 2f : */1.5f;
        Vector3f heading = pos.subtract(assigned.getPos3f());
        if(isTileTask()) {
            if(WurmId.getType(target) == CounterTypes.COUNTER_TYPE_CAVETILES) {
                //Set to center of tile
                Vector3f pos3f = pos.add(2, 2, 0);
                int tilex = Tiles.decodeTileX(target);
                int tiley = Tiles.decodeTileY(target);
                final MeshIO mesh = Server.caveMesh;
                //if target tile is a cave wall check cardinal directions
                if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex, tiley)))) toRet.set(pos3f.x, pos3f.y, pos.z);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex + 1, tiley)))) toRet.set(pos3f.x + 4, pos3f.y, pos.z);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex - 1, tiley)))) toRet.set(pos3f.x - 4, pos3f.y, pos.z);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex, tiley + 1)))) toRet.set(pos3f.x, pos3f.y + 4, pos.z);
                else if(!Tiles.isSolidCave(Tiles.decodeType(mesh.getTile(tilex, tiley - 1)))) toRet.set(pos3f.x, pos3f.y - 4, pos.z);
                //all surrounding tiles are solid
                else {
                    finishTask("Impossible to reach target tile.");
                    return null;
                }
            }
            //standing too close prevents finishing fences
            //TODO: figure out a better way for this
            else if(action == CONTINUE_BUILDING_FENCE)
                toRet = pos.add(3, 3, 0);
            else
                toRet = pos.add(1, 1, 0);
        }
        //Don't move if within range
        else {
            float distance = assigned.getPos3f().distance(pos);
            if(distance > range)
                //set position a few units closer to serf instead of on top of target
                toRet = pos.subtract(heading.normalize().mult(Math.min(distance, range)));
        }
        try {
            toRet.setZ(Math.max(0.0F, Zones.calculateHeight(pos.x, pos.y, isOnSurface())));
        } catch (NoSuchZoneException e) {
            logger.warning("No zone found when setting serf target height.");
        }
        return toRet;
    }

    public void save(long priority, long queueId) {

        try(Connection con = ModSupportDb.getModSupportDb();
            PreparedStatement ps = con.prepareStatement("UPDATE Tasks SET PRIORITY=?,QUEUEID=?,POSX=?,POSY=?,POSZ=?,LAYER=?,ASSIGNED=?,TARGET=?,REPEATEDCOUNT=?,STARTED=?,INITIALIZED=?,TAKECONTAINER=?,DROPCONTAINER=? WHERE TASKID=?"))
        {
            int id = 1;
            ps.setLong(id++, priority);
            ps.setLong(id++, queueId);
            ps.setFloat(id++, pos.x);
            ps.setFloat(id++, pos.y);
            ps.setFloat(id++, pos.z);
            ps.setInt(id++, layer);
            ps.setLong(id++, assigned != null ? assigned.getWurmId() : -10);
            ps.setLong(id++, target);
            ps.setInt(id++, repeatedCount);
            ps.setBoolean(id++, started);
            ps.setBoolean(id++, initialized);
            ps.setLong(id++, takeContainerId);
            ps.setLong(id++, dropContainerId);
            ps.setLong(id++, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Exception while saving task: " + e.getMessage());
        }
    }

    //should be called as it gets added to parent queue
    public void addToDB(long priority, long queueId) {
        if(inDb) {
            logger.warning("Task called addToDB while already in database");
            return;
        }
        if(getParent() == null) {
            logger.warning("Task called addToDB while parent is null");
            return;
        }
        taskId = ++TaskHandler.taskIdCounter;

        try(Connection con = ModSupportDb.getModSupportDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO Tasks " +
                    "(PRIORITY,TASKID,QUEUEID,ACTION,POSX,POSY,POSZ,LAYER,ASSIGNED,TARGET,DOTIMES,WHILETIMERSHOWS,WHILEACTIONAVAILABLE,READD,ACTIVEITEMTEMPLATE,TARGETITEMTEMPLATE,TARGETCREATURETEMPLATE,PARENT,REPEATEDCOUNT,STARTED,INITIALIZED,TAKECONTAINER,DROPCONTAINER,TAKECONTAINERGROUP,DROPCONTAINERGROUP,EXACTTARGET) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"))
        {
            int id = 1;
            ps.setLong(id++, priority);
            ps.setLong(id++, taskId);
            ps.setLong(id++, queueId);
            ps.setShort(id++, action);
            ps.setFloat(id++, pos.x);
            ps.setFloat(id++, pos.y);
            ps.setFloat(id++, pos.z);
            ps.setInt(id++, layer);
            ps.setLong(id++, assigned != null ? assigned.getWurmId() : -10);
            ps.setLong(id++, target);
            ps.setInt(id++, doTimes);
            ps.setBoolean(id++, whileTimerShows);
            ps.setBoolean(id++, whileActionAvailable);
            ps.setBoolean(id++, reAdd);
            ps.setInt(id++, activeItemTemplate != null ? activeItemTemplate.getTemplateId() : -10);
            ps.setInt(id++, targetItemTemplate != null ? targetItemTemplate.getTemplateId() : -10);
            ps.setInt(id++, targetCreatureTemplate != null ? targetCreatureTemplate.getTemplateId() :-10);
            ps.setLong(id++, parentId);
            ps.setInt(id++, repeatedCount);
            ps.setBoolean(id++, started);
            ps.setBoolean(id++, initialized);
            ps.setLong(id++, takeContainerId);
            ps.setLong(id++, dropContainerId);
            ps.setString(id++, takeContainerGroup);
            ps.setString(id++, dropContainerGroup);
            ps.setBoolean(id++, exactTarget);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Exception while adding task to database: " + e.getMessage());
        }
        inDb = true;
    }

    public boolean deleteFromDb() {
        try(Connection con = ModSupportDb.getModSupportDb();
            PreparedStatement ps = con.prepareStatement("DELETE FROM Tasks WHERE TASKID=?"))
        {
            int id = 1;
            ps.setLong(id++, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Exception while adding task to database: " + e.getMessage());
            return false;
        }
        return true;
    }

    public TaskQueue getParent() {
        return TaskHandler.taskQueues.get(parentId);
    }

    public void setParent(TaskQueue parent) {
        this.parentId = parent.queueId;
    }

    public Item getTakeContainerWithItem(ItemTemplate template) {
        if(!takeContainerGroup.isEmpty()) {
            Item takeContainer = getClosestContainerWithItemInGroup(template, takeContainerGroup);
            if(takeContainer != null) {
                takeContainerId = takeContainer.getWurmId();
            }
            return takeContainer;
        }
        else if(takeContainerId != NOID)
            return Items.getItemOptional(takeContainerId).orElse(null);
        return null;
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
