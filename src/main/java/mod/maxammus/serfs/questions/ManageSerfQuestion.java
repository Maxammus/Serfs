package mod.maxammus.serfs.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.utils.BMLBuilder;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.tasks.TaskQueue;
import mod.maxammus.serfs.tasks.*;
import mod.maxammus.serfs.util.ListUtil;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.wurmonline.server.utils.BMLBuilder.*;
import static mod.maxammus.serfs.util.BMLUtil.openBracket;

public class ManageSerfQuestion implements ModQuestion {

    static Logger logger = Logger.getLogger(ManageSerfQuestion.class.getName());

    //Store the list so a serf's inventory changing doesn't mess with the index of a selected item
    Map<String, ArrayList<ItemTemplate>> itemTemplateMap = new HashMap<>();
    Map<String, List<String>> containerDropdownMap = new HashMap<>();
    Map<String, Integer> groupSize = new HashMap<>();

    TaskHandler taskHandler;

    BMLBuilder bmlBuilder;

    public static void create(Creature player, long target) {
        ModQuestions.createQuestion(player, "Manage Tasks", "", target, new ManageSerfQuestion()).sendQuestion();
    }

    @Override
    public void sendQuestion(Question question) {

        taskHandler = TaskHandler.getTaskHandler(question.getResponder().getWurmId());

        bmlBuilder = createGenericBuilder();

        addProfiles();

        addGroups();

        addAreas();

        addSerfs();

        String content = ModQuestions.getBmlHeader(question) + bmlBuilder +
                //Close the header without a button that all the ending methods give
                "}};null;null;}";
        int height = 256 + (taskHandler.taskProfiles.size() + taskHandler.taskGroups.size() + taskHandler.taskAreas.size()) * 16;
        question.getResponder().getCommunicator().sendBml(1000, height, true, true, content, 200, 200, 200, question.getTitle());
    }

    private void addSerfs() {
        bmlBuilder.addLabel("Serfs:", null, TextType.BOLD, Color.white);
        BMLBuilder serfTable = createTable(5);
        taskHandler.serfs.forEach(serf ->
                serfTable
                       .addButton("editQueue." + serf.taskQueue.queueId, "Manage", 45, 16, true)
                       .addLabel(serf.getName())
                       .addLabel("Instructions: " + (serf.taskQueue.queue.size() + serf.taskQueue.queue.size()))
   //                    .addLabel("At:" + taskQueue.queue.get(0).pos.x + " | " + taskQueue.queue.get(0).pos.y)
   //                    .addLabel("From: " + taskQueue.queue.get(0).parent.getIdentity())
                       .addLabel("Items: " + serf.getInventory().getNumItemsNotCoins())
                       //.addButton("stop." + serf.taskQueue.queue.get(0).id + "." + serf.taskQueue.getIdentity(), "Stop")
                       .addButton("getContract." + serf.getWurmId(), "Recall", 42, 16, true));

        bmlBuilder
                .addString(serfTable.toString())
                .addLabel("");
    }

    private void addAreas() {
        bmlBuilder.addLabel("Areas:", null, TextType.BOLD, Color.white);
        BMLBuilder areaTable = createTable(4);
        for (TaskArea taskArea : taskHandler.taskAreas) {
            areaTable
                    .addButton("editQueue." + taskArea.queueId, "Edit")
                    .addLabel("Name: " + taskArea.name)
                    .addLabel("Count: " + taskArea.counter + "/" + taskArea.getResetCount())
                    .addButton("removeArea." + taskArea.queueId, "X ", 20, 16, true);
        }
        bmlBuilder.addString(areaTable.toString());
        openBracket(bmlBuilder,"harray")
                .addButton("addArea", "Add")
                .addInput("addAreaName", null, true, null, 0,0, null, null, null, 70, 16)
                .addLabel("Length:")
                .addInput("addAreaLength", null, true, "10", 0,0, null, null, null, 35, 16)
                        .addLabel("Width:")
                .addInput("addAreaWidth", null, true, "10", 0,0, null, null, null, 35, 16)
                .closeBracket();
        bmlBuilder.addLabel("");
    }

    private void addGroups() {
        bmlBuilder.addLabel("Groups:", null, TextType.BOLD, Color.white);
        BMLBuilder groupTable = createTable(4);
        for (TaskGroup group : taskHandler.taskGroups) {
            groupTable
                    .addButton("editQueue." + group.queueId, "Edit")
                    .addLabel("Name: " + group.name)
                    .addLabel("Queued: " + group.queue.size())
                    .addButton("removeGroup." + group.queueId, "X ", 20, 14, true);
        }
        bmlBuilder.addString(groupTable.toString());
        openBracket(bmlBuilder,"harray")
                .addButton("addGroup", "Add")
                .addInput("addGroupName", null, true, null, 0,0, null, null, null, 70, 16)
                .closeBracket();
        bmlBuilder.addLabel("");
    }

    private void addProfiles() {
        ArrayList<TaskProfile> taskProfiles = taskHandler.taskProfiles;
        ArrayList<TaskQueue> queues = taskHandler.getQueues();
        List<String> queueDropdown = queues.stream()
                .map(TaskQueue::getIdentity)
                .collect(Collectors.toList());
        queueDropdown.add(0, "None");

        bmlBuilder.addLabel("Profiles:", null, BMLBuilder.TextType.BOLD, Color.white);
        BMLBuilder profileTable = createTable(15);
        for (int i = 0; i < taskProfiles.size(); ++i) {
            TaskProfile profile = taskProfiles.get(i);
            ArrayList<Item> containers = new ArrayList<>();
            if (profile.getSelectedQueue() != null) {
                containers = profile.getSelectedQueue().containers;
                getDropdowns(profile);
            }
            String queueIdentity = profile.getSelectedQueueIdentity();
            List<String> itemDropdown = itemTemplateMap
                    .getOrDefault(queueIdentity, new ArrayList<>()).stream()
                    .map(ItemTemplate::getName)
                    .collect(Collectors.toList());
            List<String> containerDropdown = containerDropdownMap.getOrDefault(queueIdentity, new ArrayList<>());
            itemDropdown.add(0, "Nothing");
            containerDropdown.add(0, "Ground");

            int groups = 0;
            if(!queueIdentity.equals("None"))
                groups = groupSize.get(queueIdentity);
            int takeContainerIndex = containers.indexOf(profile.getTakeContainer()) + groups + 1;
            int dropContainerIndex = containers.indexOf(profile.getDropContainer()) + groups + 1;
            //if indexOf returned -1
            if(takeContainerIndex == groups)
                takeContainerIndex = Math.max(0, containerDropdown.indexOf(profile.getTakeContainerGroup()));
            if(dropContainerIndex == groups)
                dropContainerIndex = Math.max(0, containerDropdown.indexOf(profile.getDropContainerGroup()));

            int activeItemIndex = Math.max(0, itemDropdown.indexOf(profile.getActiveItemTemplateName()));
            profileTable
                    .addButton("selected." + i, profile.isSelected() ? "Selected" : "Select", 44, 16, !profile.isSelected())
                    .addLabel("Queue:")
                    .addDropdown("selectQueue." + i, Integer.toString(queues.indexOf(profile.getSelectedQueue()) + 1), queueDropdown.toArray(new String[0]))
                    .addLabel("Item:")
                    .addDropdown("activeItem." + i, Integer.toString(activeItemIndex), itemDropdown.toArray(new String[0]))
                    .addLabel("Take from:")
                    .addDropdown("takeContainer." + i, Integer.toString(takeContainerIndex), containerDropdown.toArray(new String[0]))
                    .addLabel("Drop to:")
                    .addDropdown("dropContainer." + i, Integer.toString(dropContainerIndex), containerDropdown.toArray(new String[0]))
                    .addLabel("Repeat:")
                    .addInput("repeatNum." + i, null, true, Integer.toString(profile.getRepeat()),3,1, null, null, null, 35, 16)
                    .addCheckbox("whileTimerShows." + i, "Until no timer shows", profile.isWhileTimerShows())
                    .addCheckbox("whileActionAvailable." + i, "Until not in menu", profile.isWhileActionAvailable())
                    .addCheckbox("addToBottom." + i, "Re-add to queue when done", profile.isReAdd())
                    .addButton("removeProfile." + i, "X ", 20, 16, true);
        }
        bmlBuilder
                .addString(profileTable.toString());
        openBracket(bmlBuilder,"harray")
                .addButton("save", "Save")
                .addButton("addProfile", "Add")
                .closeBracket();
        bmlBuilder
                .addLabel("");
    }

    private void getDropdowns(TaskProfile taskProfile) {
        if(taskProfile.getSelectedQueue() == null)
            return;
        String identity = taskProfile.getSelectedQueue().getIdentity();
        if(containerDropdownMap.containsKey(identity))
            return;
        ArrayList<ItemTemplate> templates = new ArrayList<>();
        Serf serf = taskProfile.getFirstSerf();
        if (serf != null)
            serf.getInventory()
                .getItems()
                .forEach(item -> {
                    if(!templates.contains(item.getTemplate()))
                        templates.add(item.getTemplate());
                });
        itemTemplateMap.put(identity, templates);

        Map<String, Integer> groups = new HashMap<>();
        ArrayList<String> containers = new ArrayList<>();
        taskProfile.getSelectedQueue().containers.forEach(item ->  {
            if(!item.getDescription().equals("")) {
                String group = "Group: " + item.getDescription();
                groups.put(group, groups.getOrDefault(group, 0) + 1);
                containers.add(item.getActualName() + "(" + item.getDescription() + ")");
            } else
                containers.add(item.getName());
        });
        List<String> tooSmall = new ArrayList<>();
        //Only want groups with multiple members
        for(String group : groups.keySet())
            if(groups.get(group) < 2)
                tooSmall.add(group);
        for(String smallGroup : tooSmall)
            groups.remove(smallGroup);

        groupSize.put(identity, groups.size());
        //Put groups at the start of the dropdown
        containers.addAll(0, groups.keySet());
        containerDropdownMap.put(identity, containers);
    }

    @Override
    public void answer(Question question, Properties answers) {
        Creature responder = question.getResponder();
        try {
            for (String key : answers.stringPropertyNames()) {
                String answer = answers.getProperty(key);
                //Continue until pressed button is found
                if (!answer.equalsIgnoreCase("true"))
                    continue;
                //Buttons with id should use the style "name.id"
                int subStringIndex = key.indexOf('.') + 1;
                String id = "";
                if (subStringIndex > 0)
                    id = key.substring(subStringIndex);

                if (key.startsWith("addProfile"))
                    taskHandler.addProfile();
                else if (key.startsWith("addGroup"))
                    taskHandler.addGroup(answers.getProperty("addGroupName"));
                else if (key.startsWith("addArea")) {
                    String name = answers.getProperty("addAreaName");
                    int length = Integer.parseInt(answers.getProperty("addAreaLength"));
                    int width = Integer.parseInt(answers.getProperty("addAreaWidth"));
                    if(width > Serfs.maxAreaSize || length > Serfs.maxAreaSize)
                        responder.getCommunicator().sendNormalServerMessage("Max size is " + Serfs.maxAreaSize);
                    else
                        taskHandler.addArea(name, responder.getTileX(), responder.getTileY(), responder.getLayer(),
                            responder.getFloorLevel(), length, width, responder.getStatus().getRotation());
                } else if (key.startsWith("removeProfile.")) {
                    int index = Integer.parseInt(id);
                    taskHandler.removeProfile(index);
                } else if (key.startsWith("removeGroup.")) {
                    taskHandler.removeGroup(Long.parseLong(id));
                } else if (key.startsWith("removeArea.")) {
                    taskHandler.removeArea(Long.parseLong(id));
                } else if (key.startsWith("selected.")) {
                    if (taskHandler.getSelectedProfile() != null)
                        taskHandler.getSelectedProfile().setSelected(false);
                    taskHandler.taskProfiles.get(Integer.parseInt(id)).setSelected(true);
                    //Don't reopen window if made selection.
                    return;
                } else if (key.startsWith("save")) {
                    for (int index = 0; answers.getProperty("selectQueue." + index) != null; ++index) {
                        TaskProfile profile = taskHandler.taskProfiles.get(index);
                        saveProfile(answers, taskHandler, index, profile);
                    }
                } else if (key.startsWith("editQueue.")) {
                    EditQueueQuestion.create(responder, question.getTarget(), Long.parseLong(id));
                    return;
                } else if (key.startsWith("getContract.")) {
                    Serf serf;
                    try {
                        serf = (Serf) Creatures.getInstance().getCreatureOrNull(Long.parseLong(id));
                        if (serf != null) {
                            if (serf.ownerId != responder.getWurmId())
                                logger.severe(responder.getName() + " tried to get the contract of someone else's serf!");
                            else if (serf.turnIntoContract())
                                taskHandler.removeSerfFromAll(serf);
                            else
                                question.getResponder().getCommunicator().sendNormalServerMessage("Couldn't turn serf into contract");
                        } else
                            question.getResponder().getCommunicator().sendNormalServerMessage("Couldn't find serf.");
                    } catch (ClassCastException e) {
                        logger.severe(responder.getName() + " tried to get the contract of a non-serf creature");
                        return;
                    }
                }
                //Not one of our buttons, probably a checked checkbox, skip
                else
                    continue;
                //Re-open window
                create(responder, question.getTarget());
            }
        } catch (NumberFormatException e) {
            responder.getCommunicator().sendNormalServerMessage("Not a number.");
            create(responder, question.getTarget());
        } catch (ArrayIndexOutOfBoundsException ignored) { }
    }

    private void saveProfile(Properties answers, TaskHandler taskHandler, int index, TaskProfile profile) {
        //Reduce index by 1 for the added default option
        int dropdownIndex = Integer.parseInt(answers.getProperty("selectQueue." + index)) - 1;
        TaskQueue temp = ListUtil.getOrNull(taskHandler.getQueues(), dropdownIndex);
        if(profile.getSelectedQueue() != temp) {
            profile.setSelectedQueue(temp);
        }
        else if(profile.getSelectedQueue() != null) {
            List<String> containerDropdown = containerDropdownMap.get(profile.getSelectedQueueIdentity());

            int groups = this.groupSize.get(profile.getSelectedQueueIdentity());
            dropdownIndex = Integer.parseInt(answers.getProperty("activeItem." + index)) - 1;
            profile.setActiveItemTemplate(ListUtil.getOrNull(itemTemplateMap.get(profile.getSelectedQueueIdentity()), dropdownIndex));
            dropdownIndex = Integer.parseInt(answers.getProperty("takeContainer." + index));
            //Not ground (0) or anything after container groups in the dropdown
            if(dropdownIndex != 0 && dropdownIndex <= groups) {
                profile.setTakeContainerGroup(containerDropdown.get(dropdownIndex));
                profile.setTakeContainer(null);
            }
            else {
                profile.setTakeContainerGroup("");
                profile.setTakeContainer(ListUtil.getOrNull(profile.getSelectedQueue().containers, dropdownIndex - groups - 1));
            }
            dropdownIndex = Integer.parseInt(answers.getProperty("dropContainer." + index));
            if(dropdownIndex != 0 && dropdownIndex <= groups) {
                profile.setDropContainerGroup(containerDropdown.get(dropdownIndex));
                profile.setDropContainer(null);
            }
            else {
                profile.setDropContainerGroup("");
                profile.setDropContainer(ListUtil.getOrNull(profile.getSelectedQueue().containers, dropdownIndex - groups - 1));
            }
            profile.setRepeat(Integer.parseInt(answers.getProperty("repeatNum." + index)));
            profile.setWhileTimerShows(Boolean.parseBoolean(answers.getProperty("whileTimerShows." + index)));
            profile.setWhileActionAvailable(Boolean.parseBoolean(answers.getProperty("whileActionAvailable." + index)));
            profile.setReAdd(Boolean.parseBoolean(answers.getProperty("addToBottom." + index)));
        }
        else {
            profile.setTakeContainer(null);
            profile.setDropContainer(null);
            profile.setTakeContainerGroup("");
            profile.setDropContainerGroup("");
            profile.setActiveItemTemplate(null);
        }
        profile.maybeSave();
    }
}