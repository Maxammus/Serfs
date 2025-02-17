package mod.maxammus.serfs.actions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.maxammus.serfs.creatures.Serf;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DropAllNonToolItems implements ModAction {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public static short actionId;
    private static String actionString = "Drop all non-tools";
    public ActionEntry actionEntry;

    @SuppressWarnings("unused")
    public DropAllNonToolItems() {
        actionId = (short) ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, actionString, "Dropping", new int[]{ 0 });
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
                //Only show when players are using the instruction tool thus using a serf to get actions
                if (performer instanceof Serf)
                    toReturn.add(actionEntry);
                return toReturn;
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {

                List<ActionEntry> toReturn = new ArrayList<>();
                //Only show when players are using the instruction tool thus using a serf to get actions
                if (performer instanceof Serf)
                    toReturn.add(actionEntry);
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
                return true;
            }
            @Override
            public boolean action(Action act, Creature performer, Item target, short action, float counter) {
                return true;
            }
        };
    }
}