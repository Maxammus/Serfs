package mod.maxammus.serfs;

import com.wurmonline.server.Items;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import mod.maxammus.serfs.actions.AddContainerToQueueAction;
import mod.maxammus.serfs.actions.ContractAction;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.actions.ManagerAction;
import mod.maxammus.serfs.creatures.CustomPlayerClass;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.creatures.SerfTemplate;
import mod.maxammus.serfs.items.SerfContract;
import mod.maxammus.serfs.items.SerfInstructor;
import mod.maxammus.serfs.tasks.TaskHandler;
import mod.maxammus.serfs.tasks.TaskProfile;
import mod.maxammus.serfs.tasks.TaskQueue;
import mod.maxammus.serfs.util.DBUtil;
import mod.maxammus.serfs.util.ReflectionUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static mod.maxammus.serfs.tasks.TaskHandler.requestSerfActions;

//TODO: serfs still show tool after finishing action.
//TODO: make hatchet a tool (type 38)
//TODO: Current pathfinding only checks tile position, not layer.  Make cross-layer pathfinder
//TODO: Check for world edge bugs in TaskArea etc
public class Serfs implements WurmServerMod, Configurable, Initable, PreInitable, ServerStartedListener, ItemTemplatesCreatedListener, ServerPollListener, PlayerLoginListener {
    private static final Logger logger = Logger.getLogger(Serfs.class.getName());
    private static final Log log = LogFactory.getLog(Serfs.class);
    private final String version = "0.0.1";
    public static float startingSkillLevel = -1;
    public static int maxActiveSerfs = -1;
    public static int serfContractPrice = 50000;
    public static int maxAreaSize = 100;
    public static boolean tradeableSerfs = false;
    private static boolean addToTraders = true;
    public static boolean alwaysOn = false;
    public static List<Short> whitelist = new ArrayList<>();
    public static List<Short> blacklist = new ArrayList<>();
    public static List<Short> autoDropWhenCannotCarryActions = new ArrayList<>();


    private static ClassPool classPool;
    private static boolean serverStarted = false;

    public static MethodHandle[] originalCreatureMethods;
    public static List<CtMethod> playerOverriddenMethodsToPatch = new ArrayList<>(750);
    @Override
    public void configure(Properties properties) {

        try {
            startingSkillLevel = Float.parseFloat(properties.getProperty("startingSkillLevel", Float.toString(startingSkillLevel)));
            maxActiveSerfs = Integer.parseInt(properties.getProperty("maxActiveSerfs", Integer.toString(maxActiveSerfs)));
            serfContractPrice = Integer.parseInt(properties.getProperty("serfContractPrice", Integer.toString(serfContractPrice)));
            maxAreaSize = Integer.parseInt(properties.getProperty("maxAreaSize", Integer.toString(maxAreaSize)));
            tradeableSerfs = Boolean.parseBoolean(properties.getProperty("tradeableSerfs", Boolean.toString(tradeableSerfs)));
            addToTraders = Boolean.parseBoolean(properties.getProperty("addToTraders", Boolean.toString(addToTraders)));
            alwaysOn = Boolean.parseBoolean(properties.getProperty("alwaysOn", Boolean.toString(alwaysOn)));
            for(String s : properties.getProperty("whitelist", "").split(","))
                if (!s.isEmpty()) whitelist.add(Short.parseShort(s));
            for(String s : properties.getProperty("blacklist", "").split(","))
                if (!s.isEmpty()) blacklist.add(Short.parseShort(s));
            for(String s : properties.getProperty("autoDropWhenCannotCarryActions", "").split(","))
                if (!s.isEmpty()) autoDropWhenCannotCarryActions.add(Short.parseShort(s));

            logger.info("startingSkillLevel: " + startingSkillLevel);
            logger.info("maxActiveSerfs: " + maxActiveSerfs);
            logger.info("serfContractPrice: " + serfContractPrice);
            logger.info("maxAreaSize: " + maxAreaSize);
            logger.info("tradeableSerfs: " + tradeableSerfs);
            logger.info("addToTraders: " + addToTraders);
            logger.info("alwaysOn: " + alwaysOn);
            logger.info("whitelist: " + whitelist);
            logger.info("blacklist: " + blacklist);
            logger.info("autoDropWhenCannotCarryActions: " + autoDropWhenCannotCarryActions);
        } catch (Exception e) {
            logger.severe("Error while reading serf mod configuration.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preInit() {
        logger.info("Serfs version " + version);
        ModActions.init();
        classPool = HookManager.getInstance().getClassPool();
    }

    @Override
    public void init() {
        try {
            ModCreatures.init();
            ModCreatures.addCreature(new SerfTemplate());
            ReflectionUtility.init();

            logger.info("Adding hook into requestAction to display actions available to serfs");
            hookSerfRequestActionList();

            logger.info("Editing reallyHandle_CMD_ACTION to intercept actions to be sent to serfs");
            editSerfInstructionAction();

            logger.info("Changing temp skills to use configured starting level");
            String initialTempValue = "initialTempValue = mod.maxammus.serfs.Serfs.startingSkillLevel;";
            if(startingSkillLevel > 0)
                initialTempValue = "initialTempValue = " + startingSkillLevel + ";";
            classPool.getMethod("com.wurmonline.server.skills.Skills", "addTempSkills")
                    .insertAt(755, "" +
                            "if(com.wurmonline.server.Players.getInstance().getPlayerOrNull(id) instanceof mod.maxammus.serfs.creatures.Serf) " +
                            initialTempValue);

            if(addToTraders) {
                logger.info("Adding serf contracts to traders.");
                classPool.getMethod("com.wurmonline.server.creatures.TradeHandler", "addItemsToTrade")
                        .insertBefore(""+
                                "if (trade != null && !shop.isPersonal() && creature.getInventory().findItem(mod.maxammus.serfs.items.SerfContract.templateId) == null)" +
                                        "creature.getInventory().insertItem("+
                                        "   com.wurmonline.server.creatures.Creature.createItem(" +
                                        "       mod.maxammus.serfs.items.SerfContract.templateId," +
                                        "       10f + com.wurmonline.server.Server.rand.nextInt(80)));");
            }


            logger.info("Making Serf class extend Player and patching any overridden methods to call their Creature versions.");
            makeSerfExtendPlayer();

            logger.info("Setting up constructors for DummySocketConnection");
            //Add empty () constructor in SocketConnection for DummySocketConnection to use
            CtClass socketConnectionClass = classPool.getCtClass("com.wurmonline.communication.SocketConnection");
            CtConstructor socketConnectionConstructor = new CtConstructor(null, socketConnectionClass);
            socketConnectionConstructor.setBody("{}");
            socketConnectionClass.addConstructor(socketConnectionConstructor);

            CtClass dummySocketConnectionClass = classPool.getCtClass("mod.maxammus.serfs.workarounds.DummySocketConnection");
            dummySocketConnectionClass.getDeclaredConstructors()[0].setBody(
                    "{ super();" +
                            "byteBuffer = java.nio.ByteBuffer.allocate(65534);" +
                            "encryptRandom = new java.util.Random(105773331L);" +
                            "decryptRandom = new java.util.Random(105773331L);}");

            //Might not be needed but seems good to do
            logger.info("Making SteamHandler.EndAuthSession() ignore serfs");
            classPool.getMethod("com.wurmonline.server.steam.SteamHandler", "EndAuthSession")
                    .insertBefore("if($1.equals(\"0\")) return;");

            logger.info("Allowing the \"Serf \" prefix to pass name checks");
            ReflectionUtility.replaceMethodCall("com.wurmonline.server.LoginHandler", "handleLogin", null, "checkName",
                    "$_ = steamIDAsString.equals(\"0\") || $proceed($$);");

            logger.info("Creating Serf instead of Player when a serf logs in");
            ReflectionUtility.replaceNewCall("com.wurmonline.server.LoginHandler", "handleLogin", null, "Player", null,
                    "if(steamIDAsString.equals(\"0\")) $_ = new mod.maxammus.serfs.creatures.Serf($$); else $_ = $proceed($$);");
            ReflectionUtility.replaceMethodCall("com.wurmonline.server.LoginHandler", "handleLogin", null, "doNewPlayer",
                    "if(steamIDAsString.equals(\"0\")) $_ = new mod.maxammus.serfs.creatures.Serf($$); else $_ = $proceed($$);");

            CtClass playerClass = classPool.getCtClass("com.wurmonline.server.players.Player");
            logger.info("Giving serfs SerfCommunicators during Player constructor");
            for(CtConstructor ctConstructor : playerClass.getDeclaredConstructors()) {
                ctConstructor.instrument(ReflectionUtility.getNewCallReplacer("PlayerCommunicator", null,"if(this instanceof mod.maxammus.serfs.creatures.Serf) $_ = new com.wurmonline.server.creatures.SerfCommunicator($$); else $_ = $proceed($$);") );
                ctConstructor.instrument(ReflectionUtility.getNewCallReplacer("PlayerCommunicatorQueued", null, "if(this instanceof mod.maxammus.serfs.creatures.Serf) $_ = new com.wurmonline.server.creatures.SerfCommunicator($$); else $_ = $proceed($$);") );
            }

            logger.info("Removing serfs from player count");
            classPool.getMethod("com.wurmonline.server.Players", "numberOfPlayers")
                    .insertAfter("$_ -= mod.maxammus.serfs.creatures.Serf.numSerfsOnline();");
            classPool.getMethod("com.wurmonline.server.Players", "getPlayerNames")
                    .setBody(
                "{ java.util.ArrayList names = new java.util.ArrayList();" +
                "com.wurmonline.server.players.Player[] players = com.wurmonline.server.Players.getInstance().getPlayers();" +
                "for(int i = 0; i < players.length; i++)" +
                "    if(!(players[i] instanceof mod.maxammus.serfs.creatures.Serf))" +
                "        names.add(players[i].getName());" +
                "return ($r)names.toArray(new java.lang.String[0]); }");

            logger.info("Making Serf.setFullyLoaded call Player.setFullyLoaded");
            classPool.getMethod("mod.maxammus.serfs.creatures.Serf", "setFullyLoaded")
                    .insertBefore("super.setFullyLoaded();");

            //Needed to avoid concurrent modification exception when logging serfs in using onPlayerLogin
            logger.info("Adding custom player login hook");
            classPool.getMethod("com.wurmonline.server.Players", "addToGroups")
                    .insertBefore("if(!mod.maxammus.serfs.Serfs.alwaysOn && !(player instanceof mod.maxammus.serfs.creatures.Serf))" +
                            "   mod.maxammus.serfs.tasks.TaskHandler.getTaskHandler(player.getWurmId()).loginSerfs();");

        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    private static void makeSerfExtendPlayer() throws NotFoundException, CannotCompileException {
        CtClass customPlayerClass = classPool.getCtClass("mod.maxammus.serfs.creatures.CustomPlayerClass");
        CtClass playerClass = classPool.getCtClass("com.wurmonline.server.players.Player");

        playerClass.setModifiers(playerClass.getModifiers() ^ Modifier.FINAL);
        customPlayerClass.setSuperclass(playerClass);

        //Set Player constructors to public
        for(CtConstructor ctConstructor : playerClass.getDeclaredConstructors())
            ctConstructor.setModifiers(Modifier.setPublic(ctConstructor.getModifiers()));

        //Add calls to super that couldn't be done during compile
        customPlayerClass.getConstructor("(Lcom/wurmonline/server/players/PlayerInfo;Lcom/wurmonline/communication/SocketConnection;)V")
                .setBody("{ super($$); }");
        customPlayerClass.getConstructor("(ILcom/wurmonline/communication/SocketConnection;)V")
                .setBody("{ super($$); }");
    }

    @SuppressWarnings("unused")
    public static boolean shouldSendActionToSerf(Creature creature, Communicator communicator, long subject, long target, short action) {
        Item subjectItem = null;
        Item targetItem = null;
        if(subject > 0)
            subjectItem = Items.getItemOptional(subject).orElse(null);
        if(target > 0)
            targetItem = Items.getItemOptional(target).orElse(null);
        return subjectItem != null && subjectItem.getTemplateId() == SerfInstructor.templateId &&
                (targetItem == null || targetItem.getTemplateId() != SerfInstructor.templateId) &&
                action != AddContainerToQueueAction.actionId && action != 3 && action >= 0;
    }


    @Override
    public void onItemTemplatesCreated() {
        SerfContract.createTemplate();
        SerfInstructor.createTemplate();
    }
    private static void editSerfInstructionAction() throws NotFoundException, CannotCompileException {
        classPool
                .getCtClass("com.wurmonline.server.creatures.Communicator")
                .getMethod("reallyHandle_CMD_ACTION", "(Ljava/nio/ByteBuffer;)V")
                .instrument(new ExprEditor() {
                    @Override
                        public void edit(MethodCall m) throws CannotCompileException {
                        if(m.getMethodName().equals("action") && m.getSignature().equals("(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Communicator;J[JS)V"))
                            //TODO: Maybe implement multi target actions, for now just send it with the first target
                            m.replace("" +
                                    "if(mod.maxammus.serfs.Serfs.shouldSendActionToSerf(this.player, this, subject, targets[0], action)) {" +
                                    "   mod.maxammus.serfs.tasks.TaskHandler.getTaskHandler(this.player.getWurmId()).receiveAction(this.player, this, subject, targets[0], action);" +
                                    "} " +
                                    "else" +
                                    "   $_ = $proceed($$);");
                        else if(m.getMethodName().equals("action") && m.getSignature().equals("(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Communicator;JJS)V"))
                            m.replace("" +
                                    "if(mod.maxammus.serfs.Serfs.shouldSendActionToSerf(this.player, this, subject, targets[x], action)) {" +
                                    "   mod.maxammus.serfs.tasks.TaskHandler.getTaskHandler(this.player.getWurmId()).receiveAction(this.player, this, subject, targets[x], action);" +
                                    "}"+
                                    "else $proceed($$);");
                        }
                    }
                );
    }
    private static void hookSerfRequestActionList(){
        HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.BehaviourDispatcher", "requestActions",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Communicator;BJJ)V",
                () -> (object, method, args) -> {
//            Creature creature, Communicator comm, byte requestId, long subject, long target)
            Creature performer = (Creature) args[0];
            Communicator comm = (Communicator) args[1];
            byte requestId = (byte) args[2];
            long subjectId = (long) args[3];
            long target = (long) args[4];

            Item subject = null;
            if(subjectId != -1)
                subject = Items.getItem(subjectId);
            if(subject != null && subject.getTemplateId() == SerfInstructor.templateId && subjectId != target) {
                TaskHandler taskHandler = TaskHandler.getTaskHandler(performer.getWurmId());
                TaskProfile selectedProfile = taskHandler.getSelectedProfile();
                Serf serf = null;
                if(selectedProfile != null)
                    serf = selectedProfile.getFirstSerf();
                if(serf == null)
                    return method.invoke(object, args);
                Item activeItem = taskHandler.getSelectedItem(taskHandler.getSelectedProfile());
                long itemId = activeItem == null ? -1 : activeItem.getWurmId();
                requestSerfActions(comm, requestId, target, serf, itemId, true);
                return null;
            }
            else
                return method.invoke(object, args);
        });
    }


    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new ContractAction());
        ModActions.registerAction(new ManagerAction());
        ModActions.registerAction(new AddContainerToQueueAction());
        ModActions.registerAction(new DropAllNonToolItems());
        SerfInstructor.initCreationEntry();

        //thrown runtime exceptions will get caught by the modloader and not end the program
        //so let's just manually shut it down
        try {
            DBUtil.createDBs();
            TaskHandler.init();
        } catch (RuntimeException e) {
            Server.getInstance().shutDown("Serf mod failed to load", e);
        }
        serverStarted = true;

    }

    @Override
    public void onServerPoll() {
        //Why does polling start before onServerStarted >:(
        if(!serverStarted)
            return;
        TaskHandler.poll();
    }

    @Override
    public void onPlayerLogin(Player player) { }

    @Override
    public void onPlayerLogout(Player player) {
        if(!alwaysOn)
            TaskHandler.getTaskHandler(player.getWurmId()).logoutSerfs();
    }
}
