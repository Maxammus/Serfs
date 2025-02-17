package mod.maxammus.serfs.actions;

import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.AutoEquipMethods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.NoSuchRoleException;
import com.wurmonline.server.villages.VillageRole;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.creatures.SerfTemplate;
import mod.maxammus.serfs.items.SerfContract;
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
    private static String actionString = "Call serf";
    short actionId;
    public ActionEntry actionEntry;

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
                if (target.isTraded() || (target.lastOwner > 0 && !target.isOwner(performer)) || target.getTopParentOrNull() != performer.getInventory())
                {
                    performer.getCommunicator().sendNormalServerMessage("You cannot do that.");
                    return true;
                }
                if (performer instanceof Player && target.getTemplateId() == SerfContract.templateId) {
                    TaskHandler taskHandler = TaskHandler.getTaskHandler(performer.getWurmId());
                    if(Serfs.maxActiveSerfs != -1 && taskHandler.serfs.size() >= Serfs.maxActiveSerfs) {
                        performer.getCommunicator().sendNormalServerMessage("You cannot handle managing any more serfs at one time.");
                        return true;
                    }
                    Serf serf;
                    try {
                        serf = (Serf) Creatures.getInstance().getCreatureOrNull(target.getData());
                        if(serf != null)
                            serf.spawnFromContract(performer);
                        else if(target.getData() == -1) {
                            serf = (Serf) Creature.doNew(SerfTemplate.templateId,
                                    performer.getPosX(), performer.getPosY(), performer.getStatus().getRotation(),
                                    performer.getLayer(), "Serf", (byte) 0);
                            //DoNew does an auto equip so undo that
                            for (final Item equipment : serf.getBody().getContainersAndWornItems())
                                    AutoEquipMethods.unequip(equipment, serf);
                        }
                        else {
                            performer.getCommunicator().sendNormalServerMessage("Serf id " + target.getData() + " not found in creature list");
                            return true;
                        }
                        serf.setupQueue(performer.getWurmId());
                        performer.getCommunicator().sendNormalServerMessage("You call " + serf.getName());
                        Items.destroyItem(target.getWurmId());
                        try {
                            //TODO: Check if this is the right role.
                            VillageRole role =  performer.getCitizenVillage().getRoleForStatus((byte) 3);
                            //TODO: change serf village when owner changes?
                            performer.getCitizenVillage().addCitizen(serf, role);
                        } catch (NoSuchRoleException | IOException ignored) { }

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