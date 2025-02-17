package mod.maxammus.serfs.creatures;

import com.wurmonline.server.Servers;
import com.wurmonline.server.bodys.BodyTemplate;
import com.wurmonline.server.items.Materials;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modsupport.CreatureTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreature;

import java.util.logging.Logger;

//TODO  public final void addTempSkills() {final float initialTempValue = (WurmId.getType(this.id) == 0) ? Servers.localServer.getSkilloverallval() :
//                                                                                                                   1.0f;
//edit that to have serfs use
public class SerfTemplate  implements ModCreature {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public static int templateId;
    public static String name = "Serf";

    @Override
    public CreatureTemplateBuilder createCreateTemplateBuilder() {
        //TODO: Look into these more
        int[] types = {
                12, //CreatureTypes.C_TYPE_SWIMMING,
                17, //CreatureTypes.C_TYPE_HUMAN,
                //4, CreatureTypes.C_TYPE_INVULNERABLE,
                62, //CreatureTypes.C_TYPE_NO_REBIRTH,
                45 //CreatureTypes.C_TYPE_OPENDOORS
        };
        final float start = Servers.localServer.getSkillbasicval();
        CreatureTemplateBuilder builder = new CreatureTemplateBuilder("mod.serf", "Serf",
                "A hard working serf", "model.creature.humanoid.human.player",
                types, BodyTemplate.TYPE_HUMAN, (short) 5, (byte) 0, (short) 180, (short) 20, (short) 35, "sound.death.male", "sound.death.female", "sound.combat.hit.male", "sound.combat.hit.female", 1.0f, 1.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.8f, 0, new int[0], 3, 0, Materials.MATERIAL_MEAT_HUMAN);

        builder.skill(SkillList.BODY_STRENGTH, start);
        builder.skill(SkillList.BODY_STAMINA, start);
        builder.skill(SkillList.BODY_CONTROL, Servers.localServer.getSkillbcval());
        builder.skill(SkillList.MIND_LOGICAL, Servers.localServer.getSkillmindval());
        builder.skill(SkillList.MIND_SPEED, start);
        builder.skill(SkillList.SOUL_STRENGTH, start);
        builder.skill(SkillList.SOUL_DEPTH, start);
        builder.skill(SkillList.GROUP_FIGHTING, Servers.localServer.getSkillfightval());



        builder.baseCombatRating(4);
        builder.childTemplate(66);
        builder.maxGroupAttackSize(7);
        builder.hasHands(true);

        templateId = builder.getTemplateId();
        logger.info("Creating creature template " + name + " - ID: " + templateId);

        return builder;
    }

    @Override
    public void addEncounters() {
    }
}