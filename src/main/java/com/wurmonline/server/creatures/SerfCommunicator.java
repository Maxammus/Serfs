package com.wurmonline.server.creatures;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.server.Message;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.epic.ValreiMapData;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Achievements;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.Titles;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.questions.RemoveItemQuestion;
import com.wurmonline.server.questions.SelectSpawnQuestion;
import com.wurmonline.server.sounds.Sound;
import com.wurmonline.server.structures.Door;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.constants.PlayerOnlineStatus;
import mod.maxammus.serfs.Serfs;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.questions.SerfQuestionQuestion;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class SerfCommunicator extends Communicator {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public final Serf serf;

    @SuppressWarnings("unused")
    public SerfCommunicator(Player serf, SocketConnection connection) {
        super(serf, connection);
        this.serf = (Serf)(Creature) serf;
        setInvulnerable(false);
    }

    @Override
    public void sendAvailableActions(byte requestId, List<ActionEntry> availableActions, String helpstring) {
        serf.lastAvailableActions = availableActions;
    }

    @Override
    public void sendBml(short id, int width, int height, float xLoc, float yLoc, boolean resizeable, boolean closeable, String content, int r, int g, int b, String title) {
        Properties properties = new Properties();
        //Handle specific questions
        if(player.question instanceof RemoveItemQuestion) {

            properties.put("numstext", Integer.toString(serf.numItemsToTake));
            properties.put("items", "None");
            player.question.answer(properties);
            Questions.removeQuestion(player.question);
            player.question = null;
            return;
        }
        else if(player.question instanceof SelectSpawnQuestion) {
            Players.getInstance().sendConnectInfo(player, " has logged in.", player.getLastLogin(), PlayerOnlineStatus.ONLINE, true);
            Players.getInstance().addToGroups(player);
            Server.getInstance().startSendingFinals(player);
            player.setSex((byte)Server.rand.nextInt(2), false);
            player.getCommunicator().sendMapInfo();
            Achievements.sendAchievementList(player);
            Players.loadAllPrivatePOIForPlayer(player);
            player.sendAllMapAnnotations();
            ValreiMapData.sendAllMapData(player);
            player.resetLastSentToolbelt();
            //Skip tutorial
            player.addTitle(Titles.Title.Educated);
            return;
        }
        //Send the rest to the player
        Player owner = Players.getInstance().getPlayerOrNull(serf.ownerId);
        if(owner != null)
            SerfQuestionQuestion.create(owner, serf.getWurmId(), width, height, xLoc, yLoc, resizeable, content, r, g, b);
    }

    @Override
    public void sendServerMessage(String message, int r, int g, int b, byte messageType) {
        //Messages are sent during Player construction before log is initialized
        try {
            serf.log.add(message);
        } catch (NullPointerException ignored) {}
    }
    public void sendUpdateSkill(int id, float value, int affinities) {
        //Send skill ticks to owner
        if(Serfs.hivemind) {
            Player owner = Players.getInstance().getPlayerOrNull(serf.ownerId);
            if(owner != null)
                owner.getCommunicator().sendUpdateSkill(id, value, affinities);
        }
    }

    public void sendServerMessage(String message, int r, int g, int b) { sendServerMessage(message, r, g, b, (byte)0); }
    public void sendNormalServerMessage(String message) { sendServerMessage(message, 255, 255, 255); }
    public void sendNormalServerMessage(String message, byte messageType) { this.sendServerMessage(message, 255, 255, 255, messageType); }
    public void sendSafeServerMessage(String message) { this.sendServerMessage(message, 255, 255, 255, (byte) 0); }
    public void sendAlertServerMessage(String message) { this.sendServerMessage(message, 255, 255, 255, (byte) 0); }
    public void sendAlertServerMessage(String message, byte messageType) { this.sendServerMessage(message, 255, 255, 255, (byte) 0); }
    public void disconnect() {}
    public void sendTeleport(boolean aLocal) { serf.handleTeleport(); }
    public void sendTeleport(boolean aLocal, boolean disembark, byte commandType) { serf.handleTeleport(); }
    public boolean sendCloseInventoryWindow(long inventoryWindow) { return true; }

    //Dummy methods
    public void sendMessage(Message message) { }
    public void sendUpdateInventoryItem(Item item) {}
    public void sendRemoveFromInventory(Item item) {}
    public void sendAddToInventory(Item item, long inventoryWindow, long rootid, int price) {}
    public void sendStartTrading(Creature opponent) {}
    public void sendCloseTradeWindow() {}
    public void sendTradeChanged(int id) {}
    public void sendTradeAgree(Creature agreer, boolean agree) {}
    public void sendUpdateInventoryItem(Item item, long inventoryWindow, int price) {}
    public void sendRemoveFromInventory(Item item, long inventoryWindow) {}
    public void sendNewCreature(long id, String name, String model, float x, float y, float z, long onBridge, float rot, byte layer, boolean onGround, boolean floating, boolean isSolid, byte kingdomId, long face, byte blood, boolean isUndead, boolean isCopy, byte modtype) {}
    public void sendMoveCreature(long id, float x, float y, int rot, boolean isMoving) {}
    public void sendMoveCreatureAndSetZ(long id, float x, float y, float z, int rot) {}
    public void sendDeleteCreature(long id) {}
    public void sendTileStripFar(short xStart, short yStart, int width, int height) {}
    public void sendTileStrip(short xStart, short yStart, int width, int height) {}
    public void sendCaveStrip(short xStart, short yStart, int width, int height) {}
    public void sendItem(Item item, long creatureId, boolean onGroundLevel) {}
    public void sendRemoveItem(Item item) {}
    public void sendAddSkill(int id, int parentSkillId, String name, float value, float maxValue, int affinities) {}
    public void sendAddEffect(long id, short type, float x, float y, float z, byte layer) {}
    public void sendRemoveEffect(long id) {}
    public void sendStamina(int stamina, int damage) {}
    public void sendThirst(int thirst) {}
    public void sendHunger(int hunger, float nutrition, float calories, float carbs, float fats, float proteins) {}
    public void sendWeight(byte weight) {}
    public void sendSpeedModifier(float speedModifier) {}
    public void sendTimeLeft(short tenthOfSeconds) {}
    public void sendSingleBuildMarker(long structureId, int tilex, int tiley, byte layer) {}
    public void sendMultipleBuildMarkers(long structureId, VolaTile[] tiles, byte layer) {}
    public void sendAddStructure(String name, short centerTilex, short centerTiley, long structureId, byte structureType, byte layer) {}
    public void sendRemoveStructure(long structureId) {}
    public void sendAddWall(long structureId, Wall wall) {}
    public void sendPassable(boolean passable, Door door) {}
    public void sendOpenDoor(Door door) {}
    public void sendCloseDoor(Door door) {}
    public void sendChangeStructureName(long structureId, String newName) {}
    public void sendOpenInventoryWindow(long inventoryWindow, String title) {}
    public void sendAddFence(Fence fence) {}
    public void sendRemoveFence(Fence fence) {}
    public void sendRename(Item item, String newName, String newModelName) {}
    public void sendOpenFence(Fence fence, boolean passable, boolean changePassable) {}
    public void sendCloseFence(Fence fence, boolean passable, boolean changePassable) {}
    public void sendSound(Sound sound) {}
    public void sendMusic(Sound sound) {}
    public void sendStatus(String status) {}
    public void sendAddWound(Wound wound, Item bodyPart) {}
    public void sendRemoveWound(Wound wound) {}
    public void sendUpdateWound(Wound wound, Item bodyPart) {}
    public void sendAddFriend(String name, long wurmid) {}
    public void sendRemoveFriend(String name) {}
    public void sendAddVillager(String name, long wurmid) {}
    public void sendRemoveVillager(String name) {}
    public void sendAddGm(String name, long wurmid) {}
    public void sendRemoveGm(String name) {}
    public void changeAttitude(long creatureId, byte status) {}
    public void sendAddLocal(String name, long wurmid) {}
    public void sendRemoveLocal(String name) {}
    public void sendDead() {}
    public void sendClimb(boolean climbing) {}
    public void sendReconnect(String ip, int port, String session) {}
    public void sendHasMoreItems(long inventoryId, long wurmid) {}
    public void sendIsEmpty(long inventoryId, long wurmid) {}
    public void sendCreatureChangedLayer(long wurmid, byte newlayer) {}
    public void sendCompass(Item item) {}
    public void sendServerTime() {}
    public void sendAttachEffect(long targetId, byte effectType, byte data0, byte data1, byte data2, byte dimension) {}
    public void sendRemoveEffect(long targetId, byte effectType) {}
    public void sendWieldItem(long creatureId, byte slot, String modelname, byte rarity, int colorRed, int colorGreen, int colorBlue, int secondaryColorRed, int secondaryColorGreen, int secondaryColorBlue) {}
    public void sendUseItem(long creatureId, String modelname, byte rarity, int colorRed, int colorGreen, int colorBlue, int secondaryColorRed, int secondaryColorGreen, int secondaryColorBlue) {}
    public void sendStopUseItem(long creatureId) {}
    public void sendRepaint(long wurmid, byte r, byte g, byte b, byte alpha, byte paintType) {}
    public void sendResize(long wurmid, byte xscaleMod, byte yscaleMod, byte zscaleMod) {}
    public void sendNewMovingItem(long id, String name, String model, float x, float y, float z, long onBridge, float rot, byte layer, boolean onGround, boolean floating, boolean isSolid, byte material, byte rarity) {}
    public void sendMoveMovingItem(long id, float x, float y, int rot) {}
    public void sendMoveMovingItemAndSetZ(long id, float x, float y, float z, int rot) {}
    public void sendMovingItemChangedLayer(long wurmid, byte newlayer) {}
    public void sendDeleteMovingItem(long id) {}
    public void sendShutDown(String reason, boolean requested) {}
    public void attachCreature(long source, long target, float offx, float offy, float offz, int seatId) {}
    public void setVehicleController(long playerId, long targetId, float offx, float offy, float offz, float maxDepth, float maxHeight, float maxHeightDiff, float vehicleRotation, int seatId) {}
    public void sendAnimation(long creatureId, String animationName, boolean looping, boolean freezeAtFinish) {}
    public void sendAnimation(long creatureId, String animationName, boolean looping, boolean freezeAtFinish, long targetId) {}
    public void sendCombatOptions(byte[] options, short tenthsOfSeconds) {}
    public void sendCombatStatus(float distanceToTarget, float footing, byte stance) {}
    public void sendCombatNormalMessage(String message) {}
    public void sendCombatAlertMessage(String message) {}
    public void sendCombatSafeMessage(String message) {}
    public void sendCombatServerMessage(String message, byte r, byte g, byte b) {}
    public void sendStunned(boolean stunned) {}
    public void sendSpecialMove(short move, String movename) {}
    public void sendTarget(long id) {}
    public void sendToggleShield(boolean on) {}
    public void sendFightStyle(byte style) {}
    public void setCreatureDamage(long wurmid, float damagePercent) {}
    public void sendWindImpact(byte windimpact) {}
    public void sendRotate(long itemId, float rotation) {}
    public void sendWeather() {}
    public void sendTileDoor(short tilex, short tiley, boolean openHole) {}
    public void sendAddPa(String name, long wurmid) {}
    public void sendRemovePa(String name) {}
    public void sendAddSpellEffect(long id, String name, byte type, byte effectType, byte influence, int duration, float power) {}
    public void sendAck(float xpos, float ypos) {}
    public void sendAddTeam(String name, long wurmid) {}
    public void sendDamageState(long wurmid, byte damage) {}
    public void sendRemoveTeam(String name) {}
    public void sendAddAreaSpellEffect(int tilex, int tiley, int layer, byte type, int floorLevel, int heightOffset, boolean loop) {}
    public void sendRemoveAreaSpellEffect(int tilex, int tiley, int layer) {}
    public void sendMissionState(long wurmId, String name, String description, String creator, float state, long start, long endDate, long expires, boolean restartable, byte difficulty, String rewards) {}
    public void sendRemoveMissionState(long wurmId) {}
    public void sendProjectile(long id, byte type, String modelName, String name, byte material, float startX, float startY, float startH, float rot, byte layer, float endX, float endY, float endH, long sourceId, long targetId, float projectedSecondsInAir, float actualSecondsInAir) {}
    public void sendBridgeId(long creatureId, long bridgeId) {}
    public void sendTargetStatus(long targetId, byte kingdom, float conquerLevel) {}
}
