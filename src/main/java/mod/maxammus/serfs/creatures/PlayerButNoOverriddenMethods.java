package mod.maxammus.serfs.creatures;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;

//Through use of reflection PlayerButNoOverriddenMethods actually extends Player during runtime
//But replaces methods Player overrode from Creature with calls to the Creature version

//Using this class between Serf and Player so super calls from Serf go to this
//and then to Creature instead of directly to Player
//And so hot reloads of Serf aren't rejected by the IDE complaining about hierarchy changes.
public class PlayerButNoOverriddenMethods extends Creature {
    public PlayerButNoOverriddenMethods(long aId) throws Exception {
        super(aId);
    }

    public PlayerButNoOverriddenMethods(CreatureTemplate aTemplate) throws Exception {
        super(aTemplate);
    }
}
