package mod.maxammus.serfs.items;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.Materials;
import mod.maxammus.serfs.Serfs;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import java.io.IOException;
import java.util.logging.Logger;

import static com.wurmonline.server.items.ItemTypes.*;


public class SerfContract {
    static Logger logger = Logger.getLogger(SerfContract.class.getName());
    public static int templateId;
    public static String name = "serf contract";

    public static void createTemplate() {
        ItemTemplateBuilder itemBuilder = new ItemTemplateBuilder("serf.contract");

        //TODO: ITEM_TYPE_PASS_FULLDATA??ITEM_TYPE_HAS_EXTRA_DATA
        short[] types = { ITEM_TYPE_INDESTRUCTIBLE,
                ITEM_TYPE_FULLPRICE, ITEM_TYPE_NOSELLBACK, ITEM_TYPE_HASDATA, ITEM_TYPE_NORENAME,
                ITEM_TYPE_FLOATING, ITEM_TYPE_NAMED, ITEM_TYPE_SERVERBOUND,
                ITEM_TYPE_NOBANK, ITEM_TYPE_NODISCARD,
                ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION, ITEM_TYPE_NO_IMPROVE,
                ITEM_TYPE_NODROP, ITEM_TYPE_NOTRADE };
        if(Serfs.tradeableSerfs)
            types = new short[] { ITEM_TYPE_INDESTRUCTIBLE,
                    ITEM_TYPE_FULLPRICE, ITEM_TYPE_HASDATA, ITEM_TYPE_NORENAME,
                    ITEM_TYPE_FLOATING, ITEM_TYPE_NAMED, ITEM_TYPE_SERVERBOUND,
                    ITEM_TYPE_NOBANK, ITEM_TYPE_NODISCARD,
                    ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION, ITEM_TYPE_NO_IMPROVE};
        itemBuilder.itemTypes(types);

        itemBuilder.name(name, name + "s", "A contract for working with a serf");
        itemBuilder.descriptions("new", "fancy", "ok", "old");
        itemBuilder.imageNumber((short) 340);
        itemBuilder.behaviourType((short) 1);
        itemBuilder.combatDamage(0);
        itemBuilder.decayTime(Long.MAX_VALUE);
        itemBuilder.dimensions(0, 0, 0);
        itemBuilder.primarySkill((int) MiscConstants.NOID);
        itemBuilder.bodySpaces(MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY);
        itemBuilder.modelName("model.writ.");
        itemBuilder.difficulty(5.0f);
        itemBuilder.weightGrams(5);
        itemBuilder.material(Materials.MATERIAL_PAPER);
        itemBuilder.value(Serfs.serfContractPrice);
        itemBuilder.isTraded(Serfs.tradeableSerfs);
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
}

