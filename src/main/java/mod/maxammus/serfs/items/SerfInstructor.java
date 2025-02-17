package mod.maxammus.serfs.items;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.*;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import java.io.IOException;
import java.util.logging.Logger;

import static com.wurmonline.server.items.ItemTypes.ITEM_TYPE_REPAIRABLE;
import static com.wurmonline.server.items.ItemTypes.ITEM_TYPE_WOOD;


public class SerfInstructor {
    static Logger logger = Logger.getLogger(SerfInstructor.class.getName());
    public static int templateId;
    public static String name = "serf instructor";

    public static void createTemplate() {
        ItemTemplateBuilder itemBuilder = new ItemTemplateBuilder("serf.instructor");
        itemBuilder.itemTypes(new short[]{ITEM_TYPE_WOOD, ITEM_TYPE_REPAIRABLE});
        itemBuilder.name(name, name + "s", "A thin stick, great for pointing at things.");
        itemBuilder.descriptions("new", "fancy", "ok", "old");
        itemBuilder.imageNumber((short) 1255);
        itemBuilder.behaviourType((short) 1);
        itemBuilder.combatDamage(0);
        itemBuilder.decayTime(Long.MAX_VALUE);
        itemBuilder.dimensions(1, 1, 20);
        itemBuilder.primarySkill(SkillList.CARPENTRY);
        itemBuilder.bodySpaces(MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY);
        itemBuilder.modelName("model.arrow.shaft.");
        itemBuilder.difficulty(5.0f);
        itemBuilder.weightGrams(50);
        itemBuilder.material(Materials.MATERIAL_WOOD_BIRCH);
        itemBuilder.value(1);
        ItemTemplate template;
        try {
            template = itemBuilder.build();
        } catch (IOException e) {
            logger.severe("Failed to create template for " + name);
            throw new RuntimeException(e);
        }
        templateId = template.getTemplateId();
        logger.info("Adding template for " + name + " - ID: " + templateId);
    }

    public static void initCreationEntry() {
        logger.info("Adding creation entry for " + name);
        if(templateId > 0) {
            CreationEntryCreator.createSimpleEntry(SkillList.CARPENTRY,
                    ItemList.knifeCarving, ItemList.shaft, templateId,
                    false, true, 0.0F, false, false, CreationCategories.TOOLS);
            CreationEntryCreator.createSimpleEntry(SkillList.CARPENTRY,
                    ItemList.crudeKnife, ItemList.shaft, templateId,
                    false, true, 0.0F, false, false, CreationCategories.TOOLS);
        }
    }
}

