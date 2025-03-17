package mod.maxammus.serfs;

import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import mod.maxammus.serfs.actions.AddContainerToQueueAction;
import mod.maxammus.serfs.actions.ContractAction;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.actions.ManagerAction;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.items.SerfContract;
import mod.maxammus.serfs.items.SerfInstructor;
import mod.maxammus.serfs.tasks.TaskHandler;
import mod.maxammus.serfs.tasks.TaskProfile;
import mod.maxammus.serfs.util.DBUtil;
import mod.maxammus.serfs.util.ReflectionUtility;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

//TODO: make hatchet a tool (type 38)
//TODO: Current pathfinding only checks tile position, not layer.  Make cross-layer pathfinder
//TODO: Handle giving instructions while serf is in combat
public class Serfs implements WurmServerMod, Configurable, Initable, PreInitable, ServerStartedListener, ItemTemplatesCreatedListener, ServerPollListener, PlayerLoginListener {
    private static final Logger logger = Logger.getLogger(Serfs.class.getName());
    private static ClassPool classPool;
    private static final String version = "0.0.2";
    public static boolean debug = true;

    public static float startingSkillLevel = -1;
    public static int maxActiveSerfs = -1;
    public static int serfContractPrice = 50000;
    public static int maxAreaSize = 100;
    public static boolean tradeableSerfs = false;
    private static boolean addToTraders = true;
    public static boolean alwaysOn = false;
    public static final List<Short> whitelist = new ArrayList<>();
    public static final List<Short> blacklist = new ArrayList<>();
    public static final List<Short> autoDropWhenCannotCarryActions = new ArrayList<>();
    public static float expShare = 0;
    public static boolean hivemind = false;
    private static boolean serverStarted = false;

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
            expShare = Float.parseFloat(properties.getProperty("expShare", Float.toString(expShare)));
            hivemind = Boolean.parseBoolean(properties.getProperty("hivemind", Boolean.toString(hivemind)));

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
            logger.info("expShare: " + expShare);
            logger.info("hivemind: " + hivemind);
        } catch (Exception e) {
            logger.severe("Error while reading serf mod configuration.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preInit() {
        logger.info("Serfs version " + getVersion());
        ModActions.init();
        classPool = HookManager.getInstance().getClassPool();
    }

    @Override
    public void init() {
        try {
            ModCreatures.init();
            ReflectionUtility.init();
            
            logger.info("Adding hook into requestAction to display actions available to serfs");
            hookSerfRequestActionList();

            logger.info("Editing reallyHandle_CMD_ACTION to intercept actions to be sent to serfs");
            editSerfInstructionAction();

            if(startingSkillLevel > 0) {
                logger.info("startingSkillLevel enabled, editing code");
                classPool.getMethod("com.wurmonline.server.skills.Skills", "addTempSkills")
                        .insertAt(755, ReflectionUtility.convertToFullClassNames(
                                "if(Players.getInstance().getPlayerOrNull(id) instanceof Serf) initialTempValue = Serfs.startingSkillLevel;"));
            }

            if(addToTraders) {
                logger.info("Adding serf contracts to traders.");
                classPool.getMethod("com.wurmonline.server.creatures.TradeHandler", "addItemsToTrade")
                        .insertBefore(ReflectionUtility.convertToFullClassNames(
                                "if (trade != null && !shop.isPersonal() && creature.getInventory().findItem(SerfContract.templateId) == null)" +
                                        "creature.getInventory().insertItem("+
                                        "   Creature.createItem(" +
                                        "       SerfContract.templateId," +
                                        "       10f + com.wurmonline.server.Server.rand.nextInt(80)));"));
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
                            "encryptRandom = new java.util.Random(1L);" +
                            "decryptRandom = new java.util.Random(1L);}");

            //Might not be needed but seems good to do
            logger.info("Making SteamHandler.EndAuthSession() ignore serfs");
            classPool.getMethod("com.wurmonline.server.steam.SteamHandler", "EndAuthSession")
                    .insertBefore("if($1.equals(\"0\")) return;");

            logger.info("Allowing the \"Serf \" prefix to pass name checks");
            ReflectionUtility.replaceMethodCall("com.wurmonline.server.LoginHandler", "handleLogin", null, "checkName",
                    "$_ = steamIDAsString.equals(\"0\") || $proceed($$);");

            logger.info("Creating Serf instead of Player when a serf logs in");
            ReflectionUtility.replaceNewCall("com.wurmonline.server.LoginHandler", "handleLogin", null, "Player", null,
                    "if(steamIDAsString.equals(\"0\")) $_ = new Serf($$); else $_ = $proceed($$);");
            ReflectionUtility.replaceMethodCall("com.wurmonline.server.LoginHandler", "handleLogin", null, "doNewPlayer",
                    "if(steamIDAsString.equals(\"0\")) $_ = new Serf($$); else $_ = $proceed($$);");

            CtClass playerClass = classPool.getCtClass("com.wurmonline.server.players.Player");
            logger.info("Giving serfs SerfCommunicators during Player constructor");
            for(CtConstructor ctConstructor : playerClass.getDeclaredConstructors()) {
                ctConstructor.instrument(ReflectionUtility.getNewCallReplacer("PlayerCommunicator", null,"if(this instanceof Serf) $_ = new SerfCommunicator($$); else $_ = $proceed($$);") );
                ctConstructor.instrument(ReflectionUtility.getNewCallReplacer("PlayerCommunicatorQueued", null, "if(this instanceof Serf) $_ = new SerfCommunicator($$); else $_ = $proceed($$);") );
            }

            logger.info("Removing serfs from player count");
            CtClass playersClass = classPool.getCtClass("com.wurmonline.server.Players");
            playersClass.getDeclaredMethod("numberOfPlayers")
                    .insertAfter(ReflectionUtility.convertToFullClassNames(
                            "$_ -= Serf.numSerfsOnline();"));
            playersClass.getDeclaredMethod("getPlayerNames")
                    .setBody(ReflectionUtility.convertToFullClassNames(
                "{ java.util.ArrayList names = new java.util.ArrayList();" +
                "Player[] players = Players.getInstance().getPlayers();" +
                "for(int i = 0; i < players.length; i++)" +
                "    if(!(players[i] instanceof Serf))" +
                "        names.add(players[i].getName());" +
                "return ($r)names.toArray(new java.lang.String[0]); }"));

            logger.info("Making Serf.setFullyLoaded call Player.setFullyLoaded");
            classPool.getMethod("mod.maxammus.serfs.creatures.Serf", "setFullyLoaded")
                    .insertBefore("super.setFullyLoaded();");

            //Needed to avoid concurrent modification exception when logging serfs in using onPlayerLogin
            logger.info("Adding custom player login hook");
            playersClass.getDeclaredMethod("addToGroups")
                    .insertBefore(ReflectionUtility.convertToFullClassNames(
                            "if(!Serfs.alwaysOn && !(player instanceof Serf))" +
                            "   TaskHandler.getTaskHandler(player.getWurmId()).loginSerfs();"));

            if(expShare > 0 || hivemind) {
                //Add field to quickly access owner's copy of the skill
                CtClass skillClass = classPool.getCtClass("com.wurmonline.server.skills.Skill");
                skillClass.addField(new CtField(skillClass, "ownerSkill", skillClass));
                //Setting initial ownerSkill for non-serf skills
                for(CtConstructor ctConstructor : skillClass.getDeclaredConstructors())
                    ctConstructor.insertAfter("ownerSkill = this;");

                //set ownerSkill when serf learns a new skill after load
                classPool.getCtClass("com.wurmonline.server.skills.Skills")
                        .getMethod("learn", "(IFZ)Lcom/wurmonline/server/skills/Skill;")
                        .insertAfter(ReflectionUtility.convertToFullClassNames(
                                "if(Players.getInstance().getPlayerOrNull(id) instanceof Serf && $_ != null) {" +
                                    "Skills ownerSkills = TaskHandler.getTaskHandler(((Serf)Players.getInstance().getPlayer(id)).ownerId).ownerSkills;" +
                                    "$_.ownerSkill = ownerSkills.getSkillOrLearn($1);" +
                                        "if($_.ownerSkill.isTemporary())" +
                                            "$_.ownerSkill = ownerSkills.learn($$);" +
                                "}"));

                if(expShare > 0) {
                    logger.info("Setting up expShare");
                    classPool.getCtClass("com.wurmonline.server.skills.Skill")
                            .getMethod("alterSkill", "(DZFZD)V")
                            .insertBefore(ReflectionUtility.convertToFullClassNames(
                                    "if(Players.getInstance().getPlayer(parent.getId()) instanceof Serf)" +
                                            "   ownerSkill.alterSkill($1 * " + expShare + ", $2, $3, $4, $5);"
                            ));
                }
                if(hivemind) {
                    logger.info("Setting up hivemind");
                    //Replace all fields but parent with the owner's
                    ExprEditor ownerKnowledge = new ExprEditor() {
                        @Override
                        public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                            if(!fieldAccess.getFieldName().equals("parent")
                                    && fieldAccess.getClassName().equals(skillClass.getName())
                                    && !fieldAccess.isStatic())
                                if(fieldAccess.isReader())
                                    fieldAccess.replace("$_ = ownerSkill." + fieldAccess.getFieldName() + ";");
                                else if(fieldAccess.isWriter())
                                    fieldAccess.replace("ownerSkill." + fieldAccess.getFieldName() + " = $1;");
                        }
                    };
                    for(CtMethod ctMethod : skillClass.getDeclaredMethods())
                        ctMethod.instrument(ownerKnowledge);
                    //Get player's skills from TaskHandler in case online serfs already have the owner's skilltree loaded
                    ReflectionUtility.replaceMethodCall("com.wurmonline.server.skills.DbSkills", "load", null,
                            "getPlayerDbCon", "if(Players.getInstance().getPlayerOrNull(id) instanceof Serf) return;" +
                                    "else if(TaskHandler.getTaskHandler(id).ownerSkills != null) {" +
                                    "   skills = TaskHandler.getSkillMapFor(id);" +
                                    "   return;" +
                                    "}" +
                                    "else $_ = $proceed();");
                }
            }
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
                            m.replace("if(mod.maxammus.serfs.Serfs.shouldSendActionToSerf(this.player, this, subject, targets[0], action)) {" +
                                    "   mod.maxammus.serfs.tasks.TaskHandler.getTaskHandler(this.player.getWurmId()).receiveAction(this.player, this, subject, targets[0], action);" +
                                    "} " +
                                    "else" +
                                    "   $_ = $proceed($$);");
                        else if(m.getMethodName().equals("action") && m.getSignature().equals("(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Communicator;JJS)V"))
                            m.replace("if(mod.maxammus.serfs.Serfs.shouldSendActionToSerf(this.player, this, subject, targets[x], action)) {" +
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
                TaskHandler.requestSerfActions(comm, requestId, target, serf, itemId, true);
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
        if((Creature)player instanceof Serf) {
            Serf serf = Serf.fromPlayer(player);
            TaskHandler.getTaskHandler(serf.ownerId).removeSerf(serf);
        }
        else if(!alwaysOn)
            TaskHandler.getTaskHandler(player.getWurmId()).logoutSerfs();
    }
}
