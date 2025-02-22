package mod.maxammus.serfs.creatures;

import com.wurmonline.math.Vector2f;
import com.wurmonline.math.Vector3f;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.creatures.ai.NoPathException;
import com.wurmonline.server.creatures.ai.Path;
import com.wurmonline.server.creatures.ai.PathFinder;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.players.SerfInfo;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import javassist.*;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.items.SerfContract;
import mod.maxammus.serfs.tasks.Task;
import mod.maxammus.serfs.tasks.TaskHandler;
import mod.maxammus.serfs.tasks.TaskQueue;
import mod.maxammus.serfs.util.DBUtil;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

//Through use of reflection Serf actually extends Player during runtime
//But doesn't use any of the methods Player overrode from Creature
public class Serf extends Creature implements MiscConstants {
    private static final Logger logger = Logger.getLogger(Serf.class.getName());
    public List<ActionEntry> lastAvailableActions = new ArrayList<>();
    public long ownerId = NOID;
    public TaskQueue taskQueue;
    public Question question;
    public int numItemsToTake;
    long nextActionTime = 0;
    public boolean failedToCarry = false;
    public static int serfCount = 0;
    private SerfInfo saveFile;

    public Deque<String> log = new ArrayDeque<String>() {
        public boolean add(String o) {
            //If over max size remove oldest.
            if(size() > 30)
                pollFirst();
            return super.add(o);
        }
    };

    @SuppressWarnings("unused")
    public Serf(CreatureTemplate aTemplate) throws Exception {
        super(aTemplate);
        communicator = new SerfCommunicator(this);
    }

    @SuppressWarnings("unused")
    public Serf(long aId) throws Exception {
        super(aId);
        saveFile = new SerfInfo("Serf");
        serfCount++;
        communicator = new SerfCommunicator(this);
    }

    @Override
    public long createPossessions() {
        long ret = -10;
        try {
            ret = super.createPossessions();
            Item inventory = getInventory();
            if (inventory.findItem(7, true) == null)
                inventory.insertItem(createItem(7, 30.0f));
            if (inventory.findItem(20, true) == null)
                inventory.insertItem(createItem(20, 30.0f));
            if (inventory.findItem(25, true) == null)
                inventory.insertItem(createItem(25, 30.0f));
        } catch (Exception e) {
            logger.warning("Exception in createPossions for " + getWurmId() +" - " + e.getMessage());
        }
        return ret;
    }

    @Override
    public long getFace() {
        return new Random(getWurmId()).nextLong();
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

    @Override
    public int getPower() {
        return 0;
    }

    //Called by actions to send action name/timer to player client
    //seems to be the most universal way to see when an action actually went through
    @Override
    public void sendActionControl(final String actionString, final boolean start, final int timeLeft) {
        if (taskQueue.queue.size() != 0 && start)
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
        catch (NoPathException ignored) { }
        return null;
    }

    public void handleOwnerQuestionResponse(Properties properties) {
        question.answer(properties);
        Questions.removeQuestion(question);
    }

    //Change serf speed
    @Override
    public float getSpeed() {
        return super.getSpeed() * 2;
    }

    @Override
    public boolean poll() throws Exception {
        //Have to set this every poll, or they randomly walk around
        if (/*taskQueue.queue.size() == 0 && */status.getPath() == null)
            shouldStandStill = true;
        return super.poll();
    }

    @Override
    public boolean isRespawn() {
        return true;
    }

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
        respawn();
    }

    //override to avoid messing with skill loss
    @Override
    public void respawn() {
        if (this.getVisionArea() == null) {
            try {
                this.status.setDead(false);
                this.setDisease((byte) 0);
                this.getStatus().removeWounds();
                this.getStatus().modifyStamina(65535.0f);
                this.getStatus().refresh(0.5f, false);
                this.createVisionArea();
                final Zone zone = Zones.getZone(this.getTileX(), this.getTileY(), this.isOnSurface());
                zone.addCreature(this.getWurmId());
                this.savePosition(zone.getId());
            } catch (Exception e) {
                logger.severe("Error while spawning serf " + getWurmId() + " from contract: " + e.getMessage());
            }
        } else
            logger.warning(getName() + " already has a visionarea.");
        Server.getInstance().broadCastAction(this.getNameWithGenus() + " has arrived.", this, 10);
    }

    //Allow serfs to ride vehicles
    public boolean isClimbing() {
        return false;
    }

    //Handle teleporting here
    //TODO: Move it somewhere more general if it's needed for more than vehicles
    public void setVehicle(final long vehicle, final boolean teleport, final byte seatType, int tilex, int tiley) {
        super.setVehicle(vehicle, teleport, seatType, tilex, tiley);
        if (isTeleporting())
            teleport();
        if (getMovementScheme().isIntraTeleporting()) {
            if (getMovementScheme().removeIntraTeleport(getTeleportCounter())) {
                if (getVehicle() == -10L) {
                    getMovementScheme().setMooredMod(false);
                    getMovementScheme().addWindImpact((byte) 0);
                    calcBaseMoveMod();
                }
                setTeleporting(false);
            }
        }
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

//        Creatures.getInstance().setCreatureOffline(this);
        //Call all the relevant commands from die() to prepare for storage
        removeIllusion();
        try {
            if (getSpellEffects() != null)
                ReflectionUtil.callPrivateMethod(getCombatHandler(), ReflectionUtil.getMethod(CombatHandler.class, "clearMoveStack"));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.warning("Couldn't clear combat move stack while while tokenizing " + getWurmId());
        }
        getCommunicator().setGroundOffset(0, true);
        setDoLavaDamage(false);
        setDoAreaEffect(false);
        combatRound = 0;
        if (getDraggedItem() != null)
            MethodsItems.stopDragging(this, getDraggedItem());
        stopLeading();
        clearLinks();
        disableLink();
        disembark(false);
        Creatures.getInstance().setCreatureDead(this);
        Players.getInstance().setCreatureDead(this);
        try {
            if (getSpellEffects() != null)
                ReflectionUtil.callPrivateMethod(getSpellEffects(), ReflectionUtil.getMethod(SpellEffects.class, "destroy"), true);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.warning("Couldn't clear spell effects while tokenizing " + getWurmId());
        }
        if (currentVillage != null)
            currentVillage.removeTarget(getWurmId(), true);
        setOpponent(null);
        target = -10L;
        try {
            getCurrentAction().stop(false);
        } catch (NoSuchActionException ignored) {
        }
        actions.clear();
        setBridgeId(-10L);
        removeWoundMod();
        setDestroyed();
        try {
            status.setDead(true);
        } catch (IOException e) {
            logger.warning("setDead failed while tokenizing " + getWurmId() +" - " + e.getMessage());
        }
        getStatus().setStunned(0.0f, false);
        trimAttackers(true);
        owner.getCommunicator().sendNormalServerMessage("You get the contract for " + getName() + "." );
        return true;
    }

    public void spawnFromContract(Creature owner) {
//        try {
//            Creatures.getInstance().loadOfflineCreature(getWurmId());
//        } catch (NoSuchCreatureException e) {
//            logger.severe("Couldn't find serf" + getWurmId() + " to load from contract for owner " + ownerId);
//        }
        ownerId = owner.getWurmId();
        status.setPositionXYZ(owner.getPosX(), owner.getPosY(), owner.getPositionZ());
        getStatus().setLayer(owner.getLayer());
        respawn();
    }

    private void maybeDoLastMovementToTask() {
        if (taskQueue.queue.size() != 0 && taskQueue.queue.get(0).assigned != null) {
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
        if(taskQueue == null) {
            taskQueue = new TaskQueue(ownerId, name);
            taskQueue.addSerf(this);
            taskQueue.addToDb(ownerId);
            TaskHandler.taskQueues.put(taskQueue.queueId, taskQueue);
        }
        else
            DBUtil.executeSingleStatement("UPDATE TaskQueues SET PLAYERID=? WHERE QUEUEID=?", ownerId, taskQueue.queueId);
        TaskHandler.getTaskHandler(ownerId).addSerf(this);
    }

    @Override
    public void destroy() {
        super.destroy();
        TaskHandler.getTaskHandler(ownerId).removeSerfFromAll(this);
        if(!taskQueue.deleteFromDb())
            logger.warning("Couldn't delete serf " + getWurmId() + " from TaskQueue database when destroying");
    }


    public Object invokeHelper(int methodIndex, Object... args) {
        try {
            if(args.length > 0)
                return Serfs.originalCreatureMethods[methodIndex].bindTo(this).invokeWithArguments(args);
//                return Serfs.originalCreatureMethods[methodIndex].asFixedArity();
            return Serfs.originalCreatureMethods[methodIndex].bindTo(this).invoke();
        } catch (Throwable e) {
            logger.severe("Serf.invokeHelper exception - " + e.getMessage());
        }
        return null;
    }

    static {
        //Populate Serfs.originalCreatureMethods
        try {
            //Class.getDeclaredMethod doesn't check return types so get all methods and manually check return type
            Method[] creatureDeclaredMethods = Creature.class.getDeclaredMethods();

            //Turn CtClass.getName()'s "Class[]" and "Class[][]" into Class.getName()'s "[LClass;" and "[[LClass;"
            String regex = "(\\w+)(\\[+)]*(\\[*)]*";
            String replacement = "$3$2L$1;";
            for(int i = 0; i < Serfs.playerOverriddenMethodsToPatch.size(); i++) {
                CtMethod ctMethod = Serfs.playerOverriddenMethodsToPatch.get(i);
                String[] params = Arrays.stream(ctMethod.getParameterTypes())
                        .map(ctClass ->
                                ctClass.getName().replaceAll(regex, replacement))
                        .toArray(String[]::new);


                String returnType = ctMethod.getReturnType().getName().replaceAll(regex, replacement);
                for(Method method : creatureDeclaredMethods) {
                    String[] params2 = Arrays.stream(method.getParameterTypes())
                            .map(Class::getName)
                            .toArray(String[]::new);

                    if (method.getName().equals(ctMethod.getName()) &&
                            Arrays.equals(params2, params) &&
                            method.getReturnType().getName().equals(returnType)) {
                        //2 days of the JVM ruining every attempt, going as far as to ignore my directly changing the
                        //bytecode to INVOKESPECIAL Creature methods, and still calling Player methods
                        //Asked our new AI overlords for this hack:

                        //Hackily bypass access checks somehow
                        Constructor<MethodHandles.Lookup> lookupConstructor =
                                MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                        lookupConstructor.setAccessible(true);
                        MethodHandles.Lookup aLookup = lookupConstructor.newInstance(Creature.class);
                        //get MethodHandle to directly call Creature class from Serf, ignoring that super class is Player.
                        Serfs.originalCreatureMethods[i] = aLookup.findSpecial(Creature.class, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), Creature.class);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}