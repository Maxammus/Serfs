package com.wurmonline.server.creatures;

import com.wurmonline.server.Message;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.questions.RemoveItemQuestion;
import com.wurmonline.server.sounds.Sound;
import com.wurmonline.server.structures.Door;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.server.zones.VolaTile;
import mod.maxammus.serfs.creatures.Serf;
import mod.maxammus.serfs.questions.SerfQuestionQuestion;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class SerfCommunicator extends Communicator {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    public Serf serf;

    public SerfCommunicator(final Creature serf) {
        this.serf = (Serf) serf;
    }

    @Override
    public void sendServerMessage(final String message, final int r, final int g, final int b, final byte messageType) {
        serf.log.add(message);
    }
    @Override
    public void sendServerMessage(final String message, final int r, final int g, final int b) {
        sendServerMessage(message, r, g, b, (byte)0);
    }

    @Override
    public void sendNormalServerMessage(final String message) {
        sendServerMessage(message, 255, 255, 255);
    }
    @Override
    public void sendNormalServerMessage(final String message, final byte messageType) {
        this.sendServerMessage(message, 255, 255, 255, messageType);
    }

    public void sendBml(short id, int width, int height, float xLoc, float yLoc, boolean resizeable, boolean closeable, String content, int r, int g, int b, String title) {
        Properties properties = new Properties();
        //Handle specific questions
        if(serf.question instanceof RemoveItemQuestion) {

            properties.put("numstext", Integer.toString(serf.numItemsToTake));
            properties.put("items", "None");
            serf.question.answer(properties);
            Questions.removeQuestion(serf.question);
            serf.question = null;
            return;
        }
        //Send the rest to the player
        try {
            Player owner = Players.getInstance().getPlayer(serf.ownerId);
            SerfQuestionQuestion.create(owner, serf.getWurmId(), width, height, xLoc, yLoc, resizeable, content, r, g, b);
        } catch (NoSuchPlayerException e) {
            e.printStackTrace();
        }
    }
        @Override
    public void disconnect() {
    }

    @Override
    public void sendMessage(final Message message) {
        this.sendServerMessage(message.getMessage(), 255, 255, 255, (byte) 0);
    }

    @Override
    public void sendSafeServerMessage(final String message) {
        this.sendServerMessage(message, 255, 255, 255, (byte) 0);
    }

    @Override
    public void sendAlertServerMessage(final String message) {
        this.sendServerMessage(message, 255, 255, 255, (byte) 0);
    }

    @Override
    public void sendUpdateInventoryItem(final Item item) {
    }

    @Override
    public void sendRemoveFromInventory(final Item item) {
    }

    @Override
    public void sendAddToInventory(final Item item, final long inventoryWindow, final long rootid, final int price) {
    }

    @Override

    public void sendStartTrading(final Creature opponent) {
    }

    @Override
    public void sendCloseTradeWindow() {
    }

    @Override

    public void sendTradeChanged(final int id) {
    }

    @Override

    public void sendTradeAgree(final Creature agreer, final boolean agree) {
    }

    @Override
    public void sendUpdateInventoryItem(final Item item, final long inventoryWindow, final int price) {
    }

    @Override
    public void sendRemoveFromInventory(final Item item, final long inventoryWindow) {
    }

    @Override
    public void sendNewCreature(final long id, final String name, final String model, final float x, final float y, final float z, final long onBridge, final float rot, final byte layer, final boolean onGround, final boolean floating, final boolean isSolid, final byte kingdomId, final long face, final byte blood, final boolean isUndead, final boolean isCopy, final byte modtype) {
    }

    @Override
    public void sendMoveCreature(final long id, final float x, final float y, final int rot, final boolean isMoving) {
    }

    @Override
    public void sendMoveCreatureAndSetZ(final long id, final float x, final float y, final float z, final int rot) {
    }

    @Override
    public void sendDeleteCreature(final long id) {
    }

    @Override
    public void sendTileStripFar(final short xStart, final short yStart, final int width, final int height) {
    }

    @Override
    public void sendTileStrip(final short xStart, final short yStart, final int width, final int height) {
    }

    @Override
    public void sendCaveStrip(final short xStart, final short yStart, final int width, final int height) {
    }

    @Override
    public void sendAvailableActions(final byte requestId, final List<ActionEntry> availableActions, final String helpstring) {
        serf.lastAvailableActions = availableActions;
    }

    @Override
    public void sendItem(final Item item, final long creatureId, final boolean onGroundLevel) {
    }

    @Override
    public void sendRemoveItem(final Item item) {
    }

    @Override
    public void sendAddSkill(final int id, final int parentSkillId, final String name, final float value, final float maxValue, final int affinities) {
    }

    @Override
    public void sendUpdateSkill(final int id, final float value, final int affinities) {
    }

    @Override
    public void sendAddEffect(final long id, final short type, final float x, final float y, final float z, final byte layer) {
    }

    @Override
    public void sendRemoveEffect(final long id) {

    }

    public void sendStamina(final int stamina, final int damage) {

    }

    public void sendThirst(final int thirst) {
    }

    @Override
    public void sendHunger(final int hunger, final float nutrition, final float calories, final float carbs, final float fats, final float proteins) {

    }

    public void sendWeight(final byte weight) {

    }

    public void sendSpeedModifier(final float speedModifier) {

    }

    public void sendTimeLeft(final short tenthOfSeconds) {
    }

    @Override
    public void sendSingleBuildMarker(final long structureId, final int tilex, final int tiley, final byte layer) {

    }

    @Override
    public void sendMultipleBuildMarkers(final long structureId, final VolaTile[] tiles, final byte layer) {
    }

    @Override
    public void sendAddStructure(final String name, final short centerTilex, final short centerTiley, final long structureId, final byte structureType, final byte layer) {
    }

    @Override
    public void sendRemoveStructure(final long structureId) {
    }

    @Override
    public void sendAddWall(final long structureId, final Wall wall) {
    }

    @Override
    public void sendPassable(final boolean passable, final Door door) {
    }

    @Override
    public void sendOpenDoor(final Door door) {
    }

    @Override
    public void sendCloseDoor(final Door door) {
    }

    @Override
    public void sendChangeStructureName(final long structureId, final String newName) {
    }

    @Override
    public void sendTeleport(final boolean aLocal) {
        this.sendTeleport(aLocal, true, (byte) 0);
    }

    @Override
    public void sendTeleport(final boolean aLocal, final boolean disembark, final byte commandType) {
    }

    @Override
    public void sendOpenInventoryWindow(final long inventoryWindow, final String title) {
    }

    @Override
    public boolean sendCloseInventoryWindow(final long inventoryWindow) {
        return true;
    }

    @Override
    public void sendAddFence(final Fence fence) {
    }

    @Override
    public void sendRemoveFence(final Fence fence) {
    }

    @Override
    public void sendRename(final Item item, final String newName, final String newModelName) {
    }

    @Override
    public void sendOpenFence(final Fence fence, final boolean passable, final boolean changePassable) {
    }

    @Override
    public void sendCloseFence(final Fence fence, final boolean passable, final boolean changePassable) {
    }

    @Override
    public void sendSound(final Sound sound) {
    }

    @Override
    public void sendMusic(final Sound sound) {
    }

    public void sendStatus(final String status) {
    }

    @Override
    public void sendAddWound(final Wound wound, final Item bodyPart) {
    }

    @Override
    public void sendRemoveWound(final Wound wound) {
    }

    @Override
    public void sendUpdateWound(final Wound wound, final Item bodyPart) {
    }

    @Override
    public void sendAddFriend(final String name, final long wurmid) {
    }

    @Override
    public void sendRemoveFriend(final String name) {
    }

    @Override
    public void sendAddVillager(final String name, final long wurmid) {
    }

    @Override
    public void sendRemoveVillager(final String name) {
    }

    @Override
    public void sendAddGm(final String name, final long wurmid) {
    }

    @Override
    public void sendRemoveGm(final String name) {
    }

    @Override
    public void changeAttitude(final long creatureId, final byte status) {
    }

    @Override
    public void sendAddLocal(final String name, final long wurmid) {
    }

    @Override
    public void sendRemoveLocal(final String name) {
    }

    @Override
    public void sendDead() {
    }

    @Override
    public void sendClimb(final boolean climbing) {
    }

    @Override
    public void sendReconnect(final String ip, final int port, final String session) {
    }

    public void sendHasMoreItems(final long inventoryId, final long wurmid) {
    }

    @Override
    public void sendIsEmpty(final long inventoryId, final long wurmid) {
    }

    @Override
    public void sendCreatureChangedLayer(final long wurmid, final byte newlayer) {
    }

    public void sendCompass(final Item item) {
    }

    @Override
    public void sendServerTime() {
    }

    @Override
    public void sendAttachEffect(final long targetId, final byte effectType, final byte data0, final byte data1, final byte data2, final byte dimension) {
    }

    @Override
    public void sendRemoveEffect(final long targetId, final byte effectType) {
    }

    @Override
    public void sendWieldItem(final long creatureId, final byte slot, final String modelname, final byte rarity, final int colorRed, final int colorGreen, final int colorBlue, final int secondaryColorRed, final int secondaryColorGreen, final int secondaryColorBlue) {
    }

    @Override
    public void sendUseItem(final long creatureId, final String modelname, final byte rarity, final int colorRed, final int colorGreen, final int colorBlue, final int secondaryColorRed, final int secondaryColorGreen, final int secondaryColorBlue) {
    }

    @Override
    public void sendStopUseItem(final long creatureId) {
    }

    @Override
    public void sendRepaint(final long wurmid, final byte r, final byte g, final byte b, final byte alpha, final byte paintType) {
    }

    @Override
    public void sendResize(final long wurmid, final byte xscaleMod, final byte yscaleMod, final byte zscaleMod) {
    }

    @Override
    public void sendNewMovingItem(final long id, final String name, final String model, final float x, final float y, final float z, final long onBridge, final float rot, final byte layer, final boolean onGround, final boolean floating, final boolean isSolid, final byte material, final byte rarity) {
    }

    @Override
    public void sendMoveMovingItem(final long id, final float x, final float y, final int rot) {
    }

    @Override
    public void sendMoveMovingItemAndSetZ(final long id, final float x, final float y, final float z, final int rot) {
    }

    @Override
    public void sendMovingItemChangedLayer(final long wurmid, final byte newlayer) {
    }

    @Override
    public void sendDeleteMovingItem(final long id) {
    }

    @Override
    public void sendShutDown(final String reason, final boolean requested) {
    }

    @Override
    public void attachCreature(final long source, final long target, final float offx, final float offy, final float offz, final int seatId) {
    }

    @Override
    public void setVehicleController(final long playerId, final long targetId, final float offx, final float offy, final float offz, final float maxDepth, final float maxHeight, final float maxHeightDiff, final float vehicleRotation, final int seatId) {
    }

    @Override
    public void sendAnimation(final long creatureId, final String animationName, final boolean looping, final boolean freezeAtFinish) {
    }

    @Override
    public void sendAnimation(final long creatureId, final String animationName, final boolean looping, final boolean freezeAtFinish, final long targetId) {
    }

    @Override
    public void sendCombatOptions(final byte[] options, final short tenthsOfSeconds) {
    }

    public void sendCombatStatus(final float distanceToTarget, final float footing, final byte stance) {
    }

    @Override
    public void sendCombatNormalMessage(final String message) {
    }

    @Override
    public void sendCombatAlertMessage(final String message) {
    }

    @Override
    public void sendCombatSafeMessage(final String message) {
    }

    @Override
    public void sendCombatServerMessage(final String message, final byte r, final byte g, final byte b) {
    }

    public void sendStunned(final boolean stunned) {
    }

    @Override
    public void sendSpecialMove(final short move, final String movename) {
    }

    public void sendTarget(final long id) {
    }

    @Override
    public void sendToggleShield(final boolean on) {
    }

    public void sendFightStyle(final byte style) {
    }

    @Override
    public void setCreatureDamage(final long wurmid, final float damagePercent) {
    }

    @Override
    public void sendWindImpact(final byte windimpact) {
    }

    @Override
    public void sendRotate(final long itemId, final float rotation) {
    }

    @Override
    public void sendWeather() {
    }

    @Override
    public void sendTileDoor(final short tilex, final short tiley, final boolean openHole) throws IOException {
    }

    @Override
    public void sendAddPa(final String name, final long wurmid) {
    }

    @Override
    public void sendRemovePa(final String name) {
    }

    @Override
    public void sendAddSpellEffect(final long id, final String name, final byte type, final byte effectType, final byte influence, final int duration, final float power) {
    }

    @Override
    public void sendAck(final float xpos, final float ypos) {
    }

    @Override
    public void sendAddTeam(final String name, final long wurmid) {
    }

    @Override
    public void sendDamageState(final long wurmid, final byte damage) {
    }

    @Override
    public void sendRemoveTeam(final String name) {
    }

    @Override
    public void sendAddAreaSpellEffect(final int tilex, final int tiley, final int layer, final byte type, final int floorLevel, final int heightOffset, final boolean loop) {
    }

    @Override
    public void sendRemoveAreaSpellEffect(final int tilex, final int tiley, final int layer) {
    }

    @Override
    public void sendMissionState(final long wurmId, final String name, final String description, final String creator, final float state, final long start, final long endDate, final long expires, final boolean restartable, final byte difficulty, final String rewards) {
    }

    @Override
    public void sendRemoveMissionState(final long wurmId) {
    }

    @Override
    public void sendProjectile(final long id, final byte type, final String modelName, final String name, final byte material, final float startX, final float startY, final float startH, final float rot, final byte layer, final float endX, final float endY, final float endH, final long sourceId, final long targetId, final float projectedSecondsInAir, final float actualSecondsInAir) {
    }

    @Override
    public void sendBridgeId(final long creatureId, final long bridgeId) {
    }

    @Override
    public void sendTargetStatus(final long targetId, final byte kingdom, final float conquerLevel) {
    }
}
