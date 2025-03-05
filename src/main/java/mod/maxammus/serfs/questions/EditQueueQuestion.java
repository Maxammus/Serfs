package mod.maxammus.serfs.questions;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.utils.BMLBuilder;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.tasks.TaskQueue;
import mod.maxammus.serfs.tasks.*;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

import static com.wurmonline.server.utils.BMLBuilder.*;
import static mod.maxammus.serfs.util.BMLUtil.openBracket;

public class EditQueueQuestion implements ModQuestion {
    static Logger logger = Logger.getLogger(EditQueueQuestion.class.getName());
    int height = 300;
    long queueId;
    TaskHandler taskHandler;
    BMLBuilder bmlBuilder;
    TaskQueue queue;

    @SuppressWarnings("unused")
    public EditQueueQuestion(long queueId, Creature player) {
        this.queueId = queueId;
        taskHandler = TaskHandler.getTaskHandler(player.getWurmId());
        bmlBuilder = createVertArrayNode(false);
        queue = taskHandler.getQueue(queueId);
    }

    public static void create(Creature player, long target, long queueId) {
        ModQuestions.createQuestion(player, "Edit Queue", "", target, new EditQueueQuestion(queueId, player)).sendQuestion();
    }
    
    @Override
    public void sendQuestion(Question question) {
        String queueIdentity = queue.getIdentity();

        openBracket(bmlBuilder,"harray")
                .addLabel(queueIdentity, null, BMLBuilder.TextType.BOLD, Color.white)
                .addButton("save", "Save")
                .addButton("back", "Back")
                .addText("")
                .addButton("togglePause", queue.paused ? "Unpause" : "Pause");
        if(queue instanceof TaskGroup)
            bmlBuilder
                    .addText("")
                    .addCheckbox("group-wide", "Group-wide tasks", ((TaskGroup)queue).groupwide);
        if(queue instanceof TaskArea)
            bmlBuilder
                    .addText("")
                    .addCheckbox("chainedTasks", "Chained tasks", ((TaskArea)queue).chainedTasks);
        bmlBuilder.closeBracket();
        bmlBuilder.addLabel("");

        if(!queueIdentity.startsWith("Serf "))
            addWorkingSerfs(bmlBuilder, queue);

        addInstructionList(bmlBuilder, queue);

        if(queue instanceof TaskGroup)
            addGroupInstructions(bmlBuilder, (TaskGroup) queue);

        if(queue instanceof TaskArea)
            addGroupList(bmlBuilder, (TaskArea) queue, taskHandler);

        addContainers(bmlBuilder, queue);

        if(!queueIdentity.startsWith("Serf "))
            addSerfList(taskHandler, bmlBuilder, queue);
        BMLBuilder rightSide = createVertArrayNode(false);

        if(queueIdentity.startsWith("Serf ")) {
            rightSide.addLabel("Log:", null, BMLBuilder.TextType.BOLD, Color.white);
            Iterator<String> it = TaskQueue.getSerf(queue.assignedSerfs.get(0)).log.descendingIterator();
            while(it.hasNext()) {
                String logMessage = it.next();
                rightSide.addLabel(logMessage);
            }
        }
        String content = ModQuestions.getBmlHeaderNoQuestion(question) +
                createTable(2).addString(createRightAlignedNode(bmlBuilder).toString())
                        .addString(rightSide.toString()) +
                //Close the header without a button that all the ending methods give
                "}};null;null;}";
        question.getResponder().getCommunicator().sendBml(700, height , true, true, content, 200, 200, 200, question.getTitle());
    }

    private static void addSerfList(TaskHandler taskHandler, BMLBuilder bmlBuilder, TaskQueue queue) {
        bmlBuilder
                .addLabel("Serfs:", null, BMLBuilder.TextType.BOLD, Color.white);
        BMLBuilder serfTable = createTable(1);
        for(Serf serf : taskHandler.serfs) {
            String name = serf.getName();
            serfTable
                    .addCheckbox("serf." + serf.getWurmId(), name, queue.assignedSerfs.contains(serf.getWurmId()));
        }
        bmlBuilder
                .addString(serfTable.toString())
                .addLabel("");
    }

    private void addContainers(BMLBuilder bmlBuilder, TaskQueue queue) {
        BMLBuilder containerTable = createTable(4);
        for (long containerId : queue.containers) {
            Item container = Items.getItemOptional(containerId).orElse(null);
            if(container == null) {
                queue.removeContainer(containerId);
                continue;
            }
            String name;
            if(!container.getDescription().isEmpty())
                name = container.getActualName() + "(" + container.getDescription() + ")";
            else
                name = container.getName();

            containerTable
                    .addLabel(name)
                    .addLabel( " - Free space: ")
                    .addLabel( String.format("%.02f%%", (double)container.getFreeVolume() / container.getContainerVolume() * 100))
                    .addButton("removeContainer." + container.getWurmId(), "X ", 20, 16, true);
            height += 16;
        }
        bmlBuilder
                .addLabel("Containers:", null, BMLBuilder.TextType.BOLD, Color.white)
                .addString(containerTable.toString())
                .addLabel("");
    }

    private void addGroupList(BMLBuilder bmlBuilder, TaskArea queue, TaskHandler taskHandler) {
        BMLBuilder groupTable = createTable(1);
        for (TaskGroup taskGroup : taskHandler.taskGroups) {
            String name = taskGroup.getIdentity();
            groupTable
                    .addCheckbox("group." + name, name, queue.assignedGroups.contains(taskGroup));
            height += 16;
        }
        bmlBuilder
                .addLabel("Groups:", null, BMLBuilder.TextType.BOLD, Color.white)
                .addString(groupTable.toString())
                .addLabel("");
    }

    private void addGroupInstructions(BMLBuilder bmlBuilder, TaskGroup queue) {
        openBracket(bmlBuilder, "harray")
            .addLabel("Group-wide Instruction list:", null, BMLBuilder.TextType.BOLD, Color.white)
            .addText("")
            .addButton("clearGroup-wide.", "Clear")
            .closeBracket();
        bmlBuilder.addLabel("");
        BMLBuilder groupTaskTable = createTable(13);
        Map<Task, ArrayList<Long>> groupwideTaskMap = queue.getTaskMapForMenu();
        for(Task task : groupwideTaskMap.keySet())
        {
            groupTaskTable
                    .addLabel("Serfs with this task: ")
                    .addLabel("" + groupwideTaskMap.get(task).size(), null, BMLBuilder.TextType.BOLD, Color.white);
            addTaskToTable(groupTaskTable, task, "Group-wide");
            height += 16;
        }
        bmlBuilder
                .addString(groupTaskTable.toString())
                .addLabel("");
    }

    private static void addInstructionList(BMLBuilder bmlBuilder, TaskQueue queue) {
        openBracket(bmlBuilder, "harray")
                .addLabel("Instruction list:", null, BMLBuilder.TextType.BOLD, Color.white)
                .addText("")
                .addButton("clear.", "Clear")
                .closeBracket();
        bmlBuilder.addLabel("");
        BMLBuilder taskTable = createTable(11);
        for(Task task : queue.queue)
            addTaskToTable(taskTable, task, "");
        bmlBuilder
                .addString(taskTable.toString())
                .addLabel("");
    }

    private static void addWorkingSerfs(BMLBuilder bmlBuilder, TaskQueue queue) {
        openBracket(bmlBuilder, "harray")
                .addLabel("Working serfs:", null, BMLBuilder.TextType.BOLD, Color.white)
                .addText("")
                .addButton("stopAll.", "Stop all")
                .closeBracket();
        bmlBuilder.addLabel("");

        BMLBuilder activeTaskTable = createTable(8);
        for (Task task : queue.getActiveTasks()) {
            if (task.assigned == null)
                continue;
            activeTaskTable
                    .addLabel(task.assigned.getName() + ": ", null, BMLBuilder.TextType.BOLD, Color.white)
                    .addLabel("using ")
                    .addLabel(task.getActiveItemTemplateName(), null, BMLBuilder.TextType.BOLD, Color.white)
                    .addLabel(" to ")
                    .addLabel(task.getActionName(), null, BMLBuilder.TextType.BOLD, Color.white)
                    .addLabel(": ")
                    .addLabel(task.getTargetName(), null, BMLBuilder.TextType.BOLD, Color.white)
                    .addButton("stop." + task.taskId, "Stop");
        }
        bmlBuilder
                .addString(activeTaskTable.toString())
                .addLabel("");
    }

    private static void addTaskToTable(BMLBuilder groupTaskTable, Task task, String id) {
        String repeat;
        if(task.whileTimerShows) repeat = "While timer shows";
        else if(task.whileActionAvailable) repeat = "While action available";
        else repeat = String.valueOf(task.doTimes - 1);
        groupTaskTable
                .addLabel("use ")
                .addLabel(task.getActiveItemTemplateName(), null, BMLBuilder.TextType.BOLD, Color.white)
                .addLabel(" to ")
                .addLabel(task.getActionName(), null, BMLBuilder.TextType.BOLD, Color.white)
                .addLabel(": ")
                .addLabel(task.getTargetName(), null, BMLBuilder.TextType.BOLD, Color.white)
                .addLabel(" repeat: ")
                .addLabel(repeat, null, BMLBuilder.TextType.BOLD, Color.white)
                .addLabel(" re-add: ")
                .addLabel(task.reAdd ? "Yes" : "No", null, BMLBuilder.TextType.BOLD, Color.white)
                .addButton("remove"+ id + "." + task.taskId, "X ", 20, 16, true);
    }

    @Override
    public void answer(Question question, Properties answers) {
        for (String key : answers.stringPropertyNames()) {
            String answer = answers.getProperty(key);
            //Continue until pressed button is found
            if(!answer.equalsIgnoreCase("true"))
                continue;
            //Buttons with id should use the style "name.id"
            int subStringIndex = key.indexOf('.') + 1;
            String id = "";
            if(subStringIndex > 0)
                id = key.substring(subStringIndex);

            try {
                if (key.startsWith("back")) {
                    ManageSerfQuestion.create(question.getResponder(), question.getTarget());
                    return;
                } else if (key.startsWith("save")) {
                    saveQueue(answers);
                } else if (key.startsWith("togglePause")) {
                    queue.paused = !queue.paused;
                    queue.save();
                } else if (key.startsWith("remove.")) {
                    queue.removeTask(Integer.parseInt(id));
                } else if (key.startsWith("removeContainer.")) {
                    queue.removeContainer(Long.parseLong(id));
                } else if (key.startsWith("stop.")) {
                    queue.stop(Integer.parseInt(id));
                } else if (key.startsWith("stopAll.")) {
                    for(Task task : queue.getActiveTasks())
                        task.finishTask("Told to stop all");
                } else if (key.startsWith("clear.")) {
                    while(queue.queue.size() > 0)
                        queue.removeTask(queue.queue.get(0));
                } else if (key.startsWith("clearGroup-wide.")) {
                    for(List<Task> tasks : ((TaskGroup)queue).groupwideTasks.values())
                        tasks.clear();
                } else if (key.startsWith("removeGroup-wide.")) {
                    ((TaskGroup)queue).removeGroupwideTask(Integer.parseInt(id));
                }
            } catch (NumberFormatException e) {
                question.getResponder().getCommunicator().sendNormalServerMessage("Invalid number.");
            }
        }
        EditQueueQuestion.create(question.getResponder(), question.getTarget(), queueId);
    }

    private void saveQueue(Properties answers) {
        ArrayList<TaskGroup> newGroups = new ArrayList<>();
        String queueIdentity = queue.getIdentity();
        if(!queueIdentity.startsWith("Serf ")){
            ArrayList<Long> newSerfs = new ArrayList<>();
            for (Serf serf : taskHandler.serfs)
                //update serfs like this rather than rebuilding the list to keep group-wide tasks correct
                if (Boolean.parseBoolean(answers.getProperty("serf." + serf.getWurmId())))
                    newSerfs.add(serf.getWurmId());
            queue.updateSerfs(newSerfs);
        }
        if (queue instanceof TaskArea) {
            for (TaskGroup group : taskHandler.taskGroups)
                if (Boolean.parseBoolean(answers.getProperty("group." + group.getIdentity())))
                    newGroups.add(group);
            ((TaskArea) queue).updateGroups(newGroups);
        }
        if(queue instanceof TaskGroup)
            ((TaskGroup) queue).groupwide = Boolean.parseBoolean(answers.getProperty("group-wide"));
        if(queue instanceof TaskArea)
            ((TaskArea) queue).chainedTasks = Boolean.parseBoolean(answers.getProperty("chainedTasks"));
        queue.save();
    }
}
