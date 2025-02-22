package mod.maxammus.serfs;

import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import mod.maxammus.serfs.actions.AddContainerToQueueAction;
import mod.maxammus.serfs.actions.ContractAction;
import mod.maxammus.serfs.actions.DropAllNonToolItems;
import mod.maxammus.serfs.actions.ManagerAction;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.creatures.SerfTemplate;
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

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static mod.maxammus.serfs.tasks.TaskHandler.requestSerfActions;

//TODO: serfs still show tool after finishing action.
//TODO: make hatchet a tool (type 38)
//TODO: Current pathfinding only checks tile position, not layer.  Make cross-layer pathfinder
//TODO: Check for world edge bugs in TaskArea etc
public class Serfs implements WurmServerMod, Configurable, Initable, PreInitable, ServerStartedListener, ItemTemplatesCreatedListener, ServerPollListener {
    private static final Logger logger = Logger.getLogger(Serfs.class.getName());
    private final String version = "0.0.1";
    public static float startingSkillLevel = -1;
    public static int maxActiveSerfs = -1;
    public static int serfContractPrice = 50000;
    public static int maxAreaSize = 100;
    public static boolean tradeableSerfs = false;
    private static boolean addToTraders;
    public static List<Short> whitelist = new ArrayList<>();
    public static List<Short> blacklist = new ArrayList<>();
    public static List<Short> autoDropWhenCannotCarryActions = new ArrayList<>();


    private static ClassPool classPool;
    private static boolean serverStarted = false;

    public static MethodHandle[] originalCreatureMethods;
    public static List<CtMethod> playerOverriddenMethodsToPatch = new ArrayList<>(750);
    @Override
    public void configure(Properties properties) {
        System.out.println("serf mod configure: ");

        try {
            startingSkillLevel = Float.parseFloat(properties.getProperty("startingSkillLevel", Float.toString(startingSkillLevel)));
            maxActiveSerfs = Integer.parseInt(properties.getProperty("maxActiveSerfs", Integer.toString(maxActiveSerfs)));
            serfContractPrice = Integer.parseInt(properties.getProperty("serfContractPrice", Integer.toString(serfContractPrice)));
            maxAreaSize = Integer.parseInt(properties.getProperty("maxAreaSize", Integer.toString(maxAreaSize)));
            tradeableSerfs = Boolean.parseBoolean(properties.getProperty("tradeableSerfs", Boolean.toString(tradeableSerfs)));
            addToTraders = Boolean.parseBoolean(properties.getProperty("addToTraders", Boolean.toString(addToTraders)));
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

            String creatures = "com.wurmonline.server.creatures.Creatures";
            String creature = "com.wurmonline.server.creatures.Creature";
            String communicator = "com.wurmonline.server.creatures.Communicator";
            logger.info("Making creatures with Serf template create Serfs instead of Creature when loading from database");
            ReflectionUtility.replaceNewCall(creatures, "loadAllCreatures", "", creature, "" +
                    "if(templateName.equalsIgnoreCase(mod.maxammus.serfs.creatures.SerfTemplate.name)) {" +
                    "   $_ = new mod.maxammus.serfs.creatures.Serf(rs.getLong(\"WURMID\"));" +
                    "}" +
                    "else $_ = $proceed($$);");

            logger.info("Making creatures with Serf template create Serfs instead of Creature when calling doNew");
            ReflectionUtility.replaceNewCall(creature, "doNew", "(IZFFFILjava/lang/String;BBBZBI)Lcom/wurmonline/server/creatures/Creature;", creature, "" +
                    "if (templateid == mod.maxammus.serfs.creatures.SerfTemplate.templateId) {" +
                    "   $_ = new mod.maxammus.serfs.creatures.Serf($$);" +
                    "}" +
                    "else $_ = $proceed($$);");

            logger.info("Changing some argument in sendNewCreature to make the client render serf model correctly");
            ReflectionUtility.replaceMethodCall("com.wurmonline.server.zones.VirtualZone","addCreature", "(JZJFFF)Z","sendNewCreature","" +
                    "$18 = $18 || creature.getTemplateId() == mod.maxammus.serfs.creatures.SerfTemplate.templateId;" +
                    "$proceed($$);");

            logger.info("Changing temp skills to work with serfs, and possibly used configured starting level");
            String initialTempValue = "initialTempValue = mod.maxammus.serfs.Serfs.startingSkillLevel;";
            if(startingSkillLevel > 0)
                initialTempValue = "initialTempValue = " + startingSkillLevel + ";";
            classPool.getMethod("com.wurmonline.server.skills.Skills", "addTempSkills")
                    .insertAt(755, "" +
                            "if(com.wurmonline.server.creatures.Creatures.getInstance().getCreatureOrNull(id) instanceof mod.maxammus.serfs.creatures.Serf) " +
                            initialTempValue);

            logger.info("Replacing Communicator.player field with SerfCommunicator.serf for serfs in multiple methods");
            String replacePlayer = "" +
                    "if(this instanceof com.wurmonline.server.creatures.SerfCommunicator)" +
                    "   $_ = ((com.wurmonline.server.creatures.SerfCommunicator)this).serf;" +
                    "else $_ = $proceed();";
            ReflectionUtility.replaceFieldAccess(communicator, "reallyHandle_CMD_MOVE_INVENTORY", "", "player", replacePlayer);
            ReflectionUtility.replaceFieldAccess(communicator, "reallyHandle_CMD_REQUEST_ACTIONS", "", "player", replacePlayer);
            ReflectionUtility.replaceFieldAccess(communicator, "setInvulnerable", "", "player", replacePlayer);

            CtClass communicatorClass = classPool.getCtClass(communicator);
            //For interacting with inventories
            ReflectionUtility.replacePlayerWithCreatureInMethod(communicatorClass, "reallyHandle_CMD_MOVE_INVENTORY");
            //For getting right click menu actions
            ReflectionUtility.replacePlayerWithCreatureInMethod(communicatorClass, "reallyHandle_CMD_REQUEST_ACTIONS");
            ReflectionUtility.replacePlayerWithCreatureInMethod(communicatorClass, "setInvulnerable");

            //needed to move items into an inventory
            ExprEditor isPlayerOrSerf = ReflectionUtility.getMethodCallReplacer("isPlayer",
                    "$_ = $proceed($$) || $0 instanceof mod.maxammus.serfs.creatures.Serf;");

            logger.info("Allowing Serfs to move items (into their inventory?)");
            classPool.getMethod(communicator, "equipCreatureCheck")
                    .instrument(isPlayerOrSerf);
            classPool.getMethod(communicator, "creatureWearableRestrictions")
                    .instrument(isPlayerOrSerf);

            logger.info("Giving serfs a real inventory");
            CtClass possessions = classPool.get("com.wurmonline.server.items.Possessions");
            for(CtConstructor ctConstructor : possessions.getConstructors())
                ctConstructor.instrument(isPlayerOrSerf);

            //also avoids an exception from attempting to cast Serf to Player
            logger.info("Allowing Serfs to handle questions");
            classPool.getMethod("com.wurmonline.server.questions.Questions", "addQuestion")
                    .insertBefore("" +
                            "if(question.getResponder() instanceof mod.maxammus.serfs.creatures.Serf) {" +
                            "    mod.maxammus.serfs.creatures.Serf serf = (mod.maxammus.serfs.creatures.Serf)question.getResponder();" +
                            "    if (serf.question != null)" +
                            "        serf.question.timedOut();" +
                            "    serf.question = question;" +
                            "    return;" +
                            "}");

            //Makes moved call movementScheme.move which moves dragged carts along with the serf
            //also needed to move visionArea around which sounds important, idk.
            logger.info("Making carts move when being dragged by serfs");
            ReflectionUtility.replaceMethodCall(creature, "moved", "", "isWagoner",
                            "$_ = $proceed($$) || $0 instanceof mod.maxammus.serfs.creatures.Serf;");

//            //Modify a check to let serfs stay offline
//            ReflectionUtility.replaceMethodCall(creatures, "pollOfflineCreatures", "", "add","" +
//                    "if(offline instanceof mod.maxammus.serfs.creatures.Serf)" +
//                    "   $_ = true;" +
//                    "else $_ = $proceed($$);");

            logger.info("Making tools show when being used by a serf");
            ExprEditor getType = ReflectionUtility.getMethodCallReplacer("getType", "" +
                    "if(com.wurmonline.server.creatures.Creatures.getInstance().getCreatureOrNull(creatureId) instanceof mod.maxammus.serfs.creatures.Serf)" +
                    "    $_ = 0;" +
                    "else $_ = $proceed($$);");
            communicatorClass.getMethod("sendUseItem", "(JLjava/lang/String;BIIIIII)V")
                    .instrument(getType);

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
            CtClass serfClass = classPool.getCtClass("mod.maxammus.serfs.creatures.Serf");
            CtClass playerClass = classPool.getCtClass("com.wurmonline.server.players.Player");
            CtClass creatureClass = classPool.getCtClass(creature);

            //Add constructors Player is missing
            CtClass lng = classPool.getCtClass("long");
            CtClass template = classPool.getCtClass("com.wurmonline.server.creatures.CreatureTemplate");
            CtConstructor jv = new CtConstructor(new CtClass[]{lng}, playerClass);
            CtConstructor ctv = new CtConstructor(new CtClass[]{template}, playerClass);
            CtConstructor v = new CtConstructor(new CtClass[]{}, playerClass);
            jv.setBody("super($1);");
            ctv.setBody("super($1);");
            v.setBody("super();");
            playerClass.addConstructor(jv);
            playerClass.addConstructor(ctv);
            playerClass.addConstructor(v);

            //Make a dummy class extend Player, then Serf extend the dummy to make
            //calling super methods in Serf properly call Creature after editing methods later
            playerClass.setModifiers(playerClass.getModifiers() ^ Modifier.FINAL);
            CtClass dummyClass = classPool.makeClass(serfClass.getName()+"_", playerClass);
            serfClass.setSuperclass(dummyClass);


            //Return all Creature methods that Player overrides back to the Creature method
            Map<String, CtMethod> dummyMethods = new HashMap<>(1500);
            Map<String, CtMethod> creatureMethods = new HashMap<>(1250);
            Map<String, CtMethod> newPlayerMethods = new HashMap<>(750);
            for(CtMethod ctMethod : dummyClass.getMethods())
                dummyMethods.put(ctMethod.getName() + ctMethod.getSignature(), ctMethod);
            for(CtMethod ctMethod : creatureClass.getDeclaredMethods())
                creatureMethods.put(ctMethod.getName() + ctMethod.getSignature(), ctMethod);
            for(CtMethod ctMethod : playerClass.getDeclaredMethods())
                if(creatureMethods.containsKey(ctMethod.getName() + ctMethod.getSignature()))
                    playerOverriddenMethodsToPatch.add(ctMethod);
                else
                    newPlayerMethods.put(ctMethod.getName() + ctMethod.getSignature(), ctMethod);

            //Remove any methods  serf already has
            for(CtMethod ctMethod : dummyClass.getDeclaredMethods())
                playerOverriddenMethodsToPatch.remove(ctMethod);

            originalCreatureMethods = new MethodHandle[playerOverriddenMethodsToPatch.size()];
            ConstPool constPool = dummyClass.getClassFile().getConstPool();
            int playerIndex = constPool.addClassInfo(playerClass);
            int creatureIndex = constPool.addClassInfo(creatureClass);
            for(String methodSignature : newPlayerMethods.keySet()) {
                CtMethod playerMethod = newPlayerMethods.get(methodSignature);
                //Turn method not final
                playerMethod.setModifiers(playerMethod.getModifiers() & ~(Modifier.FINAL));
                        CtMethod dummyMethod = new CtMethod(playerMethod.getReturnType(), playerMethod.getName(), playerMethod.getParameterTypes(), dummyClass);
                dummyMethod.setBody("throw new sun.reflect.generics.reflectiveObjects.NotImplementedException();");
                dummyClass.addMethod(dummyMethod);
            }
            for(int i = 0; i < playerOverriddenMethodsToPatch.size(); ++i) {
                CtMethod playerMethod = playerOverriddenMethodsToPatch.get(i);
                String methodSignature = playerMethod.getName() + playerMethod.getSignature();
                //Turn method not final
                playerMethod.setModifiers(playerMethod.getModifiers() & ~(Modifier.FINAL));
                CtMethod dummyMethod = new CtMethod(playerMethod.getReturnType(), playerMethod.getName(), playerMethod.getParameterTypes(), dummyClass);
                System.out.println(dummyMethod.getName());

                if(dummyMethod.getParameterTypes().length > 0)
                    dummyMethod.setBody("return ($r) mod.maxammus.serfs.Serfs.originalCreatureMethods[" + i + "].bindTo(this).invokeWithArguments($args);");
                else
                    dummyMethod.setBody("return ($r) mod.maxammus.serfs.Serfs.originalCreatureMethods[" + i + "].invokeWithArguments(new Object[] { this });");
//                dummyMethod.setBody("return ($r) invokeHelper(" + i + ", $args);");

//                int nameAndType = constPool.addNameAndTypeInfo(playerMethod.getName(), playerMethod.getSignature());
//                int creatureMethodIndex = constPool.addMethodrefInfo(creatureIndex, nameAndType);
//                int playerMethodIndex = constPool.addMethodrefInfo(playerIndex, nameAndType);
//
//                ByteBuffer search = ByteBuffer.allocate(3)
//                        .put((byte)Opcode.INVOKESPECIAL)
//                        .putShort((short)playerMethodIndex);
//                ByteBuffer replace = ByteBuffer.allocate(3)
//                        .put((byte)Opcode.INVOKESPECIAL)
//                        .putShort((short)creatureMethodIndex);
//                CodeReplacer codeReplacer = new CodeReplacer(dummyMethod.getMethodInfo().getCodeAttribute());
//                constPool.print();
//                System.out.println(Arrays.toString(search.array()));
//                System.out.println(Arrays.toString(dummyMethod.getMethodInfo().getCodeAttribute().getCode()));
//                InstructionPrinter.print(dummyMethod, System.out);
//                try {
//                    codeReplacer.replaceCode(search.array(), replace.array());
//                } catch (BadBytecode e) {
//                    throw new RuntimeException(e);
//                }
//                InstructionPrinter.print(dummyMethod, System.out);
//
                dummyClass.addMethod(dummyMethod);
//                dummyMethod = dummyClass.getMethod(playerMethod.getName(), playerMethod.getSignature());
//                InstructionPrinter.print(dummyMethod, System.out);
//
            }

//            dummyClass.getClassFile().getConstPool().renameClass(playerClass.getName(), creatureClass.getName());
//            dummyClass.getClassFile().renameClass();

//            //Find overridden methodrefs in const pool, replace the class they reference with Creature
//            for(int j = 1; j < constPool.getSize(); ++j) {
//                if(constPool.getTag(j) == ConstPool.CONST_Methodref) {
//                    int nameAndType = constPool.getMethodrefNameAndType(j);
//                    String name = constPool.getMethodrefName(j);
//                    String desc = constPool.getMethodrefType(j);
//                    MethodTypeInfo
//                    if(playerOverriddenMethods.containsKey(name + desc)) {
//                        constPool.
//                    }
//                }
//            }


        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException(e);
        }
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
}
