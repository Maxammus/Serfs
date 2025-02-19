Add serfs to Wurm to do your bidding.  Configurable from a single serf mining to a horde of serfs capable of nearly anything a player can do shaping the land to your will.

## CURRENTLY IN ALPHA TESTING - Bugs will be present, stability not guaranteed.


### **How to use:** 
* Get a serf contract and call the serf to your service.
* Make a serf instructor with a knife on a shaft for giving instructions to your serfs.
* Open the "Manage serfs" menu on the instructor and add a profile.
* Set up the profile for the task your want your serf to do.
* Activating the instructor and right-clicking will show the actions that the serf can do.
* Selecting an action with the instructor activated will send the action to the selected queue

### **Features:**

**Target containers:** With the instructor activated and a profile set up you can add containers to be used by your selected queue.  If multiple containers have been renamed to the same thing they will create a container group which when selected will allow the serf to choose the closest container in that group (that has the target item for take actions or the space to hold the target item for drop actions).

**Groups:** Create a group and add serfs to make organization easier.  Sending an action to a group will have the first free serf take the action or if the group is set to group-wide ALL serfs will have the action added to a queue

**Areas:** Create an area from where you're facing.  Actions are sent to the entire area, depending on the type of action.  Item/creature actions will make a list of all targets in the area with that template (Felled trees, cows, etc).  Tile actions will do that action on every tile of the area where that action applies.

_(Experimental)_ Chained area tasks take the first action in an area's queue as the "Main" action for the rest of the queue to be chained together with.  For example: Cut down tree -> chop up felled tree -> take logs -> drop logs (Drop container set to cart being dragged) will make that chain for every tree tile in the area

**Automatic actions:** Certain actions that would be tedious to manage have been made automatic.
* When crafting (Multi-step not currently supported) if the target item is not in the serf's inventory the serf will attempt to take it from the take-from container.
* When doing actions that add items to the serfs inventory (Digging, harvesting, etc) if the serf cannot carry the item it will attempt to drop all non-tool items to the drop-to container.  Supported actions can be changed in serfs.properties
*When tools reach 10 damage the serf will repair it.

**Configuration:** Many options to get exactly what you want from the mod.  Action blacklist, whitelist, max serfs per player, tradeable serfs, and more.

Known issues:
Cannot use items in your inventory to instruct serfs (Because the instructor only sees what serfs can do, and serfs cannot do things to your inventory items).
