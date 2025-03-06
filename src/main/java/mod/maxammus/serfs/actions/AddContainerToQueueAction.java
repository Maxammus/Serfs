package mod.maxammus.serfs.actions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.tasks.TaskHandler;
import mod.maxammus.serfs.tasks.TaskProfile;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AddContainerToQueueAction implements ModAction {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public static short actionId;
    public final ActionEntry actionEntry;
    static final String actionString = "Add container to queue";
    public AddContainerToQueueAction() {
        actionId = (short) ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, actionString, "adding", new int[]{ 0 });
        ModActions.registerAction(actionEntry);
        logger.info("Creating action " + actionString + " - ID: " + actionId);
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target)
            {
                List<ActionEntry> toReturn = new ArrayList<>();
                try {
                    if ((target.isHollow() || target.isVehicle()) &&
                            (performer instanceof Serf || TaskHandler.getTaskHandler(performer.getWurmId()).getSelectedProfile().getSelectedQueue() != null))
                        toReturn.add(actionEntry);
                }
                catch (NullPointerException ignored) { }
                return toReturn;
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {

                List<ActionEntry> toReturn = new ArrayList<>();
                try {
                    if (target.getContainerVolume() > 0 && performer instanceof Serf)
                        toReturn.add(actionEntry);
                }
                catch (NullPointerException ignored) { }
                return toReturn;
            }
        };
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override
            public short getActionId() {
                return actionId;
            }
            @Override
            public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter) {
                return this.action(act, performer, target, action, counter);
            }
            @Override
            public boolean action(Action act, Creature performer, Item target, short action, float counter) {
                try {
                    if(target.getContainerVolume() > 0)
                    {
                        TaskProfile taskProfile = TaskHandler.getTaskHandler(performer.getWurmId()).getSelectedProfile();
                        if(taskProfile != null && taskProfile.getSelectedQueue() != null)
                            taskProfile.getSelectedQueue().addContainer(target.getWurmId());
                        else
                            performer.getCommunicator().sendNormalServerMessage("No task target selected.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
    }
}