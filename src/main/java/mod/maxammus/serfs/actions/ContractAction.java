package mod.maxammus.serfs.actions;

import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.items.SerfContract;
import mod.maxammus.serfs.questions.SerfContractQuestion;
import mod.maxammus.serfs.tasks.TaskHandler;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ContractAction implements ModAction {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private static final String actionString = "Call serf";
    final short actionId;
    public final ActionEntry actionEntry;

    public ContractAction() {
        actionId = (short) ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, actionString, "Calling", new int[]{ 0 });
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
                if(target.getTemplateId() == SerfContract.templateId)
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
                if (target.getTopParentOrNull() != performer.getInventory())
                {
                    performer.getCommunicator().sendNormalServerMessage("You must be holding the contract to use it.");
                    return true;
                }
                if (performer instanceof Player && target.getTemplateId() == SerfContract.templateId) {
                    TaskHandler taskHandler = TaskHandler.getTaskHandler(performer.getWurmId());
                    if(Serfs.maxActiveSerfs != -1 && taskHandler.onlineSerfs.size() >= Serfs.maxActiveSerfs) {
                        performer.getCommunicator().sendNormalServerMessage("You cannot handle managing any more serfs at one time.");
                        return true;
                    }
                    try {
                        if(target.getData() == -1)
                            SerfContractQuestion.create(performer, target.getWurmId(), true);
                        else {
                            String name = target.getDescription();
                            final PlayerInfo file = PlayerInfoFactory.createPlayerInfo(name);
                            try {
                                file.load();
                            } catch (IOException ignored) { }
                            if(file.wurmId != target.getData()) {
                                logger.warning(String.format("Error calling serf %d", target.getData()));
                                performer.getCommunicator().sendNormalServerMessage("Error calling serf.");
                                return true;
                            }

                            Serf serf = Serf.createSerf(name, performer.getWurmId());
                            serf.calledBy(performer);
                            Items.destroyItem(target.getWurmId());
                        }

                    } catch (ClassCastException e) {
                        logger.warning("Serf contract has non-serf id stored somehow.");
                    } catch (Exception e) {
                        logger.warning("Failed to redeem serf contract - " + e.getMessage());
                    }
                }
                return true;
            }
        };
    }

}