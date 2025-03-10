package mod.maxammus.serfs.creatures;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.math.Vector2f;
import com.wurmonline.math.Vector3f;
import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.creatures.ai.NoPathException;
import com.wurmonline.server.creatures.ai.Path;
import com.wurmonline.server.creatures.ai.PathFinder;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.players.*;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.villages.NoSuchRoleException;
import com.wurmonline.server.villages.VillageRole;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.items.SerfContract;
import mod.maxammus.serfs.tasks.Task;
import mod.maxammus.serfs.tasks.TaskHandler;
import mod.maxammus.serfs.tasks.TaskQueue;
import mod.maxammus.serfs.util.DBUtil;
import mod.maxammus.serfs.util.ListUtil;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;

//TODO: Set relevant hasFlags for serf
public class Serf extends CustomPlayerClass implements MiscConstants {
    private static final Logger logger = Logger.getLogger(Serf.class.getName());
    public List<ActionEntry> lastAvailableActions = new ArrayList<>();
    public long ownerId = NOID;
    public TaskQueue taskQueue;
    public int numItemsToTake;
    long nextActionTime = 0;
    public boolean failedToCarry = false;
    public final Deque<String> log = new ArrayDeque<String>() {
        @Override
        public boolean add(String o) {
            //If over max size remove oldest.
            if(size() > 30)
                pollFirst();
            return super.add(o);
        }
    };

    //Create new serf
    public Serf(int id, SocketConnection connection) throws Exception {
        super(id, connection);
    }

    //Load existing serf
    public Serf(PlayerInfo info, SocketConnection connection) throws Exception {
        super(info, connection);
    }

    public static Serf fromId(long target) {
        return (Serf)(Creature) Players.getInstance().getPlayerOrNull(target);
    }
    public static Serf fromPlayer(Player player) {
        return (Serf)(Creature) player;
    }

    public void pollTasks() {
        if(nextActionTime > System.currentTimeMillis())
            return;
        nextActionTime = System.currentTimeMillis() + 1000;
        if (taskQueue.queue.size() == 0)
            lookForTask();
        if (taskQueue.queue.size() > 0 && !taskQueue.paused) {
            Task task = taskQueue.queue.get(0);
            if(task.assigned == null) {
                task.setAssigned(this);
                log.add("Got task from " + taskQueue.getIdentity() + " - " + task.getActionName());
            }
            task.poll();
        }
    }

    //Check all queues containing this serf for waiting tasks
    void lookForTask() {
        TaskHandler taskHandler = TaskHandler.getTaskHandler(ownerId);
        getTasksFrom(taskHandler.taskGroups);
        if(taskQueue.queue.size() == 0)
            getTasksFrom(taskHandler.taskAreas);
    }

    private void getTasksFrom(List<? extends TaskQueue> taskQueues) {
        for (TaskQueue queue : taskQueues) {
            if (!queue.hasTasksFor(this))
                continue;

            for (Task availableTask : queue.getAvailableTasks(this))
                taskQueue.addTask(availableTask);
            return;
        }
    }

    //Called by actions to send action name/timer to player client
    //seems to be the most universal way to see when an action actually went through
    @Override
    public void sendActionControl(final String actionString, final boolean start, final int timeLeft) {
        if(taskQueue != null && !taskQueue.queue.isEmpty() && start)
            taskQueue.queue.get(0).receivedActionTimer = true;
        VolaTile playerCurrentTile = this.getCurrentTile();
        this.sendToLoggers("Action string " + actionString + ", starting=" + start + ", time left " + timeLeft);
        if (playerCurrentTile != null) {
            playerCurrentTile.sendActionControl(this, actionString, start, timeLeft);
        }
    }

    //Return null when path size is 0 to avoid killing the pathfinding thread.
    @Override
    public Path findPath(final int targetX, final int targetY, final PathFinder pathfinder) {
        try {
            Path path;
            final PathFinder pf = (pathfinder != null) ? pathfinder : new PathFinder();
            setPathfindcounter(getPathfindCounter() + 1);
            path = pf.findPath(this, getTileX(), getTileY(), targetX, targetY, isOnSurface(), 20);
            if (path != null && path.getSize() != 0) {
                setPathfindcounter(0);
                return path;
            }
            else {
                maybeDoLastMovementToTask();
                return null;
            }
        }
        catch (NoPathException ignored) {
            setPathing(false, true);
            if(taskQueue.queue.size() > 0)
                //pathing is done in another thread so to avoid having to mess with synchronize
                // just set a flag to finish next poll
                taskQueue.queue.get(0).finishReason = "No path to target.";
        }
        return null;
    }

    public void handleOwnerQuestionResponse(Properties properties) {
        Question question = toPlayer().question;
        question.answer(properties);
        Questions.removeQuestion(question);
    }

    private @NotNull Player toPlayer() {
        return (Player) (Creature) this;
    }

    //Change serf speed
    @Override
    public float getSpeed() {
        return super.getSpeed() * 2;
    }

    @Override
    public boolean poll() throws Exception {
        if (status.getPath() != null) {
            this.checkMove();
            this.startUsingPath();
        }
        return super.poll();
    }

    //TODO: look into how death is handled for players
    public void setDeathEffects(final boolean freeDeath, final int dtilex, final int dtiley) {
        removeWoundMod();
        modifyFightSkill(dtilex, dtiley);
        setDestroyed();
        try {
            status.setDead(true);
        } catch (final IOException e) {
            logger.warning("setDead failed while " + getName() + " is dying  - " + e.getMessage());
        }
        getStatus().setStunned(0.0f, false);
        trimAttackers(true);
        //TODO: another way to respawn
        setTeleportPoints(getPosX(), getPosY(), getLayer(), getFloorLevel());
        startTeleporting();
        respawn();
    }

    public boolean turnIntoContract() {
        Creature owner = Players.getInstance().getPlayerOrNull(ownerId);
        if(owner == null) {
            logger.warning("Couldn't find owner while tokenizing " + getWurmId());
            return false;
        }
        Item serfContract = ItemFactory.createItemOptional(SerfContract.templateId, 99, owner.getName()).orElse(null);
        if(serfContract == null) {
            logger.warning("Couldn't create token while tokenizing " + getWurmId());
            return false;
        }
        serfContract.setData(getWurmId());
        serfContract.setLastOwnerId(owner.getWurmId());
        serfContract.setDescription(name);
        owner.getInventory().insertItem(serfContract, true);

        toPlayer().setLink(false);
        log.clear();
        owner.getCommunicator().sendNormalServerMessage("You get the contract for " + getName() + "." );
        return true;
    }

    private void maybeDoLastMovementToTask() {
        if (!taskQueue.queue.isEmpty() && taskQueue.queue.get(0).assigned != null) {
            try {
                setPathfindcounter(0);
                Vector2f pathPos = taskQueue.queue.get(0).getXYWithinTaskRange();
                Vector3f oldPos = getPos3f();
                Vector3f pathPos3f = new Vector3f(pathPos.x, pathPos.y, Zones.calculateHeight(pathPos.x, pathPos.y, isOnSurface()));
                Vector3f diff = pathPos3f.subtract(oldPos);
                if((int) pathPos.x / 4 == (int) oldPos.x / 4 && (int) pathPos.y / 4 == (int) oldPos.y / 4) {
                        getStatus().setPositionXYZ(pathPos.x, pathPos.y, pathPos3f.z);
                        int diffTileX = (int) pathPos.x / 4 - (int) oldPos.x / 4;
                        int diffTileY = (int) pathPos.y / 4 - (int) oldPos.y / 4;
                        moved(diff.x, diff.y, diff.z, diffTileX, diffTileY);
                        setPathing(false, true);
                }
            } catch (NoSuchZoneException ignored) { }
        }
    }

    public boolean canCarry(final int weight) {
        failedToCarry = !super.canCarry(weight);
        Task activeTask = taskQueue.queue.get(0);
        if(failedToCarry && activeTask != null && Serfs.autoDropWhenCannotCarryActions.stream().anyMatch(action -> action == activeTask.action)) {
            Task dropTask = new Task(activeTask);
            dropTask.action = DropAllNonToolItems.actionId;
            dropTask.doTimes = 1;
            dropTask.whileTimerShows = false;
            dropTask.whileActionAvailable = false;
            dropTask.reAdd = false;
            dropTask.setParent(taskQueue);
            dropTask.setAssigned(this);
            if(dropTask.taskActionIsAvailable(this, true)) {
                //reset for later
                activeTask.initialized = false;
                activeTask.started = false;
                //Put the new task first in line
                taskQueue.addTask(0, dropTask);
                log.add("Cannot carry more - pausing " + activeTask.getActionName() + " to drop all non tools.");
                return false;
            }
        }
        return !failedToCarry;
    }

    public void setupQueue(long ownerId) {
        this.ownerId = ownerId;
        TaskHandler taskHandler = TaskHandler.getTaskHandler(ownerId);
        taskHandler.addSerf(this);
        //Queue may already exist and serf is just logging in
        taskQueue = ListUtil.findOrNull(taskHandler.serfQueues, tq -> tq.name.equals(name));
        if(taskQueue == null) {
            taskQueue = new TaskQueue(ownerId, name);
            taskQueue.addSerf(getWurmId());
            taskQueue.addToDb(ownerId);
            TaskHandler.taskQueues.put(taskQueue.queueId, taskQueue);
        }
        else if(taskQueue.playerId != ownerId){
            taskQueue.playerId = ownerId;
            DBUtil.executeSingleStatement("UPDATE TaskQueues SET PLAYERID=? WHERE QUEUEID=?", ownerId, taskQueue.queueId);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        TaskHandler.getTaskHandler(ownerId).removeSerfFromAll(this);
        if(!taskQueue.deleteFromDb())
            logger.warning("Couldn't delete serf " + getWurmId() + " from TaskQueue database when destroying");
    }

    public void handleTeleport() {
        ByteBuffer bb = ByteBuffer.allocate(4).putInt(getTeleportCounter());
        bb.flip();
        try {
            Method method = ReflectionUtil.getMethod(Communicator.class, "reallyHandle_CMD_TELEPORT");
            ReflectionUtil.callPrivateMethod(getCommunicator(), method, bb);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.warning("Failed to call Communicator.reallyHandle_CMD_TELEPORT");
        }
    }

    @Override
    public int getSecondsToLogout() {
        return 0;
    }

    @SuppressWarnings("unused")
    public static int numSerfsOnline() {
        int count = 0;
        for(Creature player : Players.getInstance().getPlayers())
            if(player instanceof Serf)
                count++;
        return count;
    }

    @SuppressWarnings("unused")
    public void setFullyLoaded() {
        //added during runtime:
        //super.setFullyLoaded()

        //simulate client sending a teleport command after log in to finish some loading that is handled there
        handleTeleport();
    }

    public void calledBy(Creature owner) {
        setTeleportPoints(owner.getPosX(), owner.getPosY(), owner.getLayer(), owner.getFloorLevel());
        startTeleporting();
//        getStatus().setPositionXYZ(owner.getPosX(), owner.getPosY(), owner.getPositionZ());
//        getStatus().setLayer(owner.getLayer());
        owner.getCommunicator().sendNormalServerMessage("You call " + getName());
        try {
            setKingdomId(owner.getKingdomId());
        } catch (IOException e) {
            owner.getCommunicator().sendNormalServerMessage("Couldn't set kingdom for " + name);
            logger.warning("Couldn't set kingdom for " + name);
        }
        try {
            if(owner.getCitizenVillage() != null) {
                //TODO: Check if this is the right role.
                VillageRole role = owner.getCitizenVillage().getRoleForStatus((byte) 3);
                //TODO: change serf village when owner changes?
                owner.getCitizenVillage().addCitizen(this, role);
            }
        } catch (NoSuchRoleException | IOException e) {
            owner.getCommunicator().sendNormalServerMessage("Couldn't add " + name + "to village");
            logger.warning("Couldn't add " + name + "to village");
        }
    }

    public static Serf createSerf(String name, long ownerId) {
        Serf serf = CustomPlayerClass.doLogIn(name);
        if(serf == null)
            return null;
        serf.ownerId = ownerId;
        serf.setupQueue(ownerId);
        TaskHandler.getTaskHandler(ownerId).addSerf(serf);
        return serf;
    }
}