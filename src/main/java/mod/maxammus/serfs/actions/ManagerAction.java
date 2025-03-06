package mod.maxammus.serfs.actions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import mod.maxammus.serfs.items.SerfInstructor;
import mod.maxammus.serfs.questions.ManageSerfQuestion;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ManagerAction implements ModAction {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final String actionString = "Manage serfs";
    final short actionId;
    public final ActionEntry actionEntry;

    @SuppressWarnings("unused")
    public ManagerAction() {
        actionId = (short) ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, actionString, "Managing", new int[]{ 0 });
        ModActions.registerAction(actionEntry);
        logger.info("Creating action " + actionString + " - ID: " + actionId);
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target)
            {
                return this.getBehavioursFor(performer, target);
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
                List<ActionEntry> toReturn = new ArrayList<>();
                if(performer instanceof Player && target.getTemplateId() == SerfInstructor.templateId)
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
                return this.action(act, performer, target, action, counter);
            }
            @Override
            public boolean action(Action act, Creature performer, Item target, short action, float counter) {
                try {
                    if (performer instanceof Player && target.getTemplateId() == SerfInstructor.templateId) {
                        ManageSerfQuestion.create(performer, target.getWurmId());
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
    }
}