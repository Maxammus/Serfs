package mod.maxammus.serfs.creatures;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.server.LoginHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Recipe;
import com.wurmonline.server.kingdom.Appointment;
import com.wurmonline.server.players.*;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.steam.SteamId;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.VirtualZone;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Set;
@SuppressWarnings("unused")
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
    public Set<MapAnnotation> getAllianceAnnotations() { throw new NotImplementedException(); }
    private void checkMaySteal() { throw new NotImplementedException(); }
    public void closePM(String St0) { throw new NotImplementedException(); }
    public void setAddFriendTimout(int in0, Friend.Category Fr0) { throw new NotImplementedException(); }
    public boolean isBlockingPvP() { throw new NotImplementedException(); }
    public PlayerVote getPlayerVote(int in0) { throw new NotImplementedException(); }
    public void setTeleportCounter(int in0) { throw new NotImplementedException(); }
    public void sendToWorld() { throw new NotImplementedException(); }
    public boolean isNearCave() { throw new NotImplementedException(); }
    public void setMarkedByOrb(boolean bo0) { throw new NotImplementedException(); }
    public boolean respondMGMTTab(String St0, String St1) { throw new NotImplementedException(); }
    public boolean hasGlobalEffect(long lo0) { throw new NotImplementedException(); }
    public void setQuestion(Question Qu0) { throw new NotImplementedException(); }
    public boolean mayHearMgmtTalk() { throw new NotImplementedException(); }
    public void removeDuellist(Creature Cr0) { throw new NotImplementedException(); }
    public void spawn(byte by0) { throw new NotImplementedException(); }
    public void setIsWritingRecipe(boolean bo0) { throw new NotImplementedException(); }
    public void setFrozen(boolean bo0) { throw new NotImplementedException(); }
    public void sleep() { throw new NotImplementedException(); }
    private void contactLoginServerForAwards(boolean bo0) { throw new NotImplementedException(); }
    public Friend getFriend(long lo0) { throw new NotImplementedException(); }
    public boolean isTradeChannel() { throw new NotImplementedException(); }
    public void checkBattleTitle(int in0, int in1) { throw new NotImplementedException(); }
    public MapAnnotation getAnnotation(long lo0, byte by0) { throw new NotImplementedException(); }
    public long getLastLogin() { throw new NotImplementedException(); }
    private void checkSendLinkStatus() { throw new NotImplementedException(); }
    public void addGlobalEffect(Long Lo0, int in0) { throw new NotImplementedException(); }
    public void gotVotes(boolean bo0) { throw new NotImplementedException(); }
    public boolean hasCrownEffect() { throw new NotImplementedException(); }
    public void checkAffinity() { throw new NotImplementedException(); }
    public boolean canSignIn() { throw new NotImplementedException(); }
    public void addPlotCourseCooldown(long lo0) { throw new NotImplementedException(); }
    public boolean showTradeStartMessage() { throw new NotImplementedException(); }
    public Question getCurrentQuestion() { throw new NotImplementedException(); }
    public void sendKarma() { throw new NotImplementedException(); }
    public boolean isGlobalChat() { throw new NotImplementedException(); }
    public String waitingForFriend() { throw new NotImplementedException(); }
    public void sendScenarioKarma() { throw new NotImplementedException(); }
    public void sendAllMapAnnotations() { throw new NotImplementedException(); }
    public void setLogout() { throw new NotImplementedException(); }
    public void sendClearVillageMapAnnotations() { throw new NotImplementedException(); }
    public void setCreationWindowOpen(boolean bo0) { throw new NotImplementedException(); }
    public void setLastChangedPath(long lo0) { throw new NotImplementedException(); }
    public boolean sendPM(String St0, long lo0, String St1) { throw new NotImplementedException(); }
    public void getVotes() { throw new NotImplementedException(); }
    private void pollAlcohol() { throw new NotImplementedException(); }
    public boolean shouldGiveAffinity(int in0, boolean bo0) { throw new NotImplementedException(); }
    private void sendFriendTimedOut() { throw new NotImplementedException(); }
    public void maybeTriggerAchievement(int in0, boolean bo0) { throw new NotImplementedException(); }
    public
    SteamId getSteamId() { throw new NotImplementedException(); }
    public String getEigcId() { throw new NotImplementedException(); }
    public boolean isSendExtraBytes() { throw new NotImplementedException(); }
    public void setViewingRecipe(Recipe Re0) { throw new NotImplementedException(); }
    public boolean isVillageChatShowing() { throw new NotImplementedException(); }
    private void sendNewPhantasm(boolean bo0) { throw new NotImplementedException(); }
    public boolean togglePlayerAssistantWindow(boolean bo0) { throw new NotImplementedException(); }
    public boolean canUseFreeVillageTeleport() { throw new NotImplementedException(); }
    public Recipe getViewingRecipe() { throw new NotImplementedException(); }
    public boolean isKingdomChat() { throw new NotImplementedException(); }
    void access$000(Player Pl0, boolean bo0) { throw new NotImplementedException(); }
    public boolean isAllowedToEditVillageMap() { throw new NotImplementedException(); }
    public void removeMapPOI(MapAnnotation Ma0) { throw new NotImplementedException(); }
    public int getChampionPoints() { throw new NotImplementedException(); }
    public boolean isCreationWindowOpen() { throw new NotImplementedException(); }
    private void checkItemsWatched() { throw new NotImplementedException(); }
    public void setPlayerAssistant(boolean bo0) { throw new NotImplementedException(); }
    public String getClientVersion() { throw new NotImplementedException(); }
    private void nutcase(Cultist Cu0) { throw new NotImplementedException(); }
    public void setAFK(boolean bo0) { throw new NotImplementedException(); }
    public void setNextActionRarity(byte by0) { throw new NotImplementedException(); }
    public boolean showGlobalKingdomStartMessage() { throw new NotImplementedException(); }
    public void closeBank() { throw new NotImplementedException(); }
    public boolean pollDead() { throw new NotImplementedException(); }
    public void setSignedIn(boolean bo0) { throw new NotImplementedException(); }
    public void resetInactivity(boolean bo0) { throw new NotImplementedException(); }
    private boolean addPrivateMapPOI(MapAnnotation Ma0) { throw new NotImplementedException(); }
    public void receivedCmd(int in0) { throw new NotImplementedException(); }
    public void fillVotes() { throw new NotImplementedException(); }
    public Ban getBan() { throw new NotImplementedException(); }
    public void clearRespondTo() { throw new NotImplementedException(); }
    public boolean isWritingRecipe() { throw new NotImplementedException(); }
    public boolean isReimbursed() { throw new NotImplementedException(); }
    public boolean sendPM(byte by0, String St0, long lo0, boolean bo0, String St1, boolean bo1, byte by1, int in0, boolean bo2) { throw new NotImplementedException(); }
    private void addPvPDeath() { throw new NotImplementedException(); }
    private void sendServerMessage(String St0, int in0, int in1, int in2) { throw new NotImplementedException(); }
    public byte remoteAddFriend(String St0, byte by0, byte by1, boolean bo0, boolean bo1) { throw new NotImplementedException(); }
    public void reimburse() { throw new NotImplementedException(); }
    public boolean checkTileInvulnerability() { throw new NotImplementedException(); }
    public void setRarityShader(byte by0) { throw new NotImplementedException(); }
    public void setBlood(byte by0) { throw new NotImplementedException(); }
    private boolean calculateMissionSpawnPoint() { throw new NotImplementedException(); }
    public void sendAddChampionPoints() { throw new NotImplementedException(); }
    public Player doNewPlayer(int in0, SocketConnection So0) { throw new NotImplementedException(); }
    public float[] findRandomSpawnX(boolean bo0, boolean bo1) { throw new NotImplementedException(); }
    public long getLastChangedPath() { throw new NotImplementedException(); }
    public void sendPM(String St0, String St1, boolean bo0, boolean bo1) { throw new NotImplementedException(); }
    public boolean maySeeGVHelpWindow() { throw new NotImplementedException(); }
    public boolean containsPlayerVote(int in0) { throw new NotImplementedException(); }
    public long getLastLogout() { throw new NotImplementedException(); }
    private MapAnnotation getVillageAnnotationById(long lo0) { throw new NotImplementedException(); }
    private MapAnnotation getAllianceAnnotationById(long lo0) { throw new NotImplementedException(); }
    public void chattedLocal() { throw new NotImplementedException(); }
    public void setClientVersion(String St0) { throw new NotImplementedException(); }
    public void logout() { throw new NotImplementedException(); }
    public long getSleepBonusInactivity() { throw new NotImplementedException(); }
    public boolean showAllianceMessage() { throw new NotImplementedException(); }
    public void checkInitialBattleTitles() { throw new NotImplementedException(); }
    public boolean allowIncomingPMs(String St0, byte by0, long lo0, boolean bo0, byte by1, int in0) { throw new NotImplementedException(); }
    public LoginHandler getLoginhandler() { throw new NotImplementedException(); }
    public void sendMarkedByOrb() { throw new NotImplementedException(); }
    public void setAskFriend(String St0, Friend.Category Fr0) { throw new NotImplementedException(); }
    public void createSomeItems(float fl0, boolean bo0) { throw new NotImplementedException(); }
    public void setLoginStep(int in0) { throw new NotImplementedException(); }
    public boolean isActiveInLocalChat() { throw new NotImplementedException(); }
    public boolean hasPlantedSign() { throw new NotImplementedException(); }
    public boolean hasVoted(int in0) { throw new NotImplementedException(); }
    public void checkLantern() { throw new NotImplementedException(); }
    public long getVersion() { throw new NotImplementedException(); }
    public long getRespondTo() { throw new NotImplementedException(); }
    public boolean mayAppointPlayerAssistant() { throw new NotImplementedException(); }
    private Item[] getItemsWatched() { throw new NotImplementedException(); }
    public boolean startBank(Village Vi0) { throw new NotImplementedException(); }
    public void removeFriend(long lo0) { throw new NotImplementedException(); }
    private void spreadCrownInfluence() { throw new NotImplementedException(); }
    public void removePlayerVote(int in0) { throw new NotImplementedException(); }
    public void sendClearAllianceMapAnnotations() { throw new NotImplementedException(); }
    public void reimbAnniversaryGift(boolean bo0) { throw new NotImplementedException(); }
    public boolean showVillageMessage() { throw new NotImplementedException(); }
    public String getWarningStats(long lo0) { throw new NotImplementedException(); }
    public void setEigcClientId(String St0) { throw new NotImplementedException(); }
    public void updateFriendData(long lo0, byte by0, String St0) { throw new NotImplementedException(); }
    public boolean isNew() { throw new NotImplementedException(); }
    public String getAFKMessage() { throw new NotImplementedException(); }
    public void checkPaymentUpdate() { throw new NotImplementedException(); }
    public void sendSpellResistances() { throw new NotImplementedException(); }
    public long getAlcoholAddiction() { throw new NotImplementedException(); }
    public boolean seesGVHelpWindow() { throw new NotImplementedException(); }
    public void setUndeadType(byte by0) { throw new NotImplementedException(); }
    public PlayerInfo getSaveFile() { throw new NotImplementedException(); }
    public void openBank() { throw new NotImplementedException(); }
    public boolean removeIgnored(long lo0) { throw new NotImplementedException(); }
    public boolean isSignedIn() { throw new NotImplementedException(); }
    public void setPower(byte by0) { throw new NotImplementedException(); }
    public void setFullyLoaded() { throw new NotImplementedException(); }
    public void reimbursePacks(boolean bo0) { throw new NotImplementedException(); }
    public int getLoginStep() { throw new NotImplementedException(); }
    public void setLastTrigger(int in0) { throw new NotImplementedException(); }
    private void pollStealAttack() { throw new NotImplementedException(); }
    private short[] getSpawnPointOutside(Village Vi0) { throw new NotImplementedException(); }
    public int getSecondsPlayedSinceLinkLoss() { throw new NotImplementedException(); }
    public void showPMWarn(String St0, String St1) { throw new NotImplementedException(); }
    public Set<MapAnnotation> getVillageAnnotations() { throw new NotImplementedException(); }
    private void sendNormalServerMessage(String St0) { throw new NotImplementedException(); }
    public boolean isAllianceChatShowing() { throw new NotImplementedException(); }
    public Set<MapAnnotation> getAllMapAnnotations() { throw new NotImplementedException(); }
    public void addAppointment(Appointment Ap0, Creature Cr0) { throw new NotImplementedException(); }
    public void setUsedFreeVillageTeleport() { throw new NotImplementedException(); }
    public boolean askingFriend() { throw new NotImplementedException(); }
    public void initialisePlayer(PlayerInfo Pl0) { throw new NotImplementedException(); }
    public void createNewMapPOI(String St0, byte by0, int in0, int in1, String St1, byte by1) { throw new NotImplementedException(); }
    private byte getPseudoPower() { throw new NotImplementedException(); }
    public void setLoginHandler(LoginHandler Lo0) { throw new NotImplementedException(); }
    public boolean showKingdomStartMessage() { throw new NotImplementedException(); }
    private MapAnnotation getPrivateAnnotationById(long lo0) { throw new NotImplementedException(); }
    public String getClientSystem() { throw new NotImplementedException(); }
    private void setPremStuff() { throw new NotImplementedException(); }
    public void plantSign() { throw new NotImplementedException(); }
    public void setVotes(PlayerVote[] Pl0) { throw new NotImplementedException(); }
    public void sendSpawnQuestion() { throw new NotImplementedException(); }
    public void removeMeFromFriendsList(long lo0, String St0) { throw new NotImplementedException(); }
    public long getNicotineAddiction() { throw new NotImplementedException(); }
    public void addMapPOI(MapAnnotation Ma0, boolean bo0) { throw new NotImplementedException(); }
    private void setDead(boolean bo0) { throw new NotImplementedException(); }
    private void clearChallengeScores() { throw new NotImplementedException(); }
    public void addPlayerVote(PlayerVote Pl0) { throw new NotImplementedException(); }
    public void sendLantern(VirtualZone Vi0) { throw new NotImplementedException(); }
    public void setLastChangedVillage(long lo0) { throw new NotImplementedException(); }
    public void checkKingdom() { throw new NotImplementedException(); }
    public Titles.Title[] getTitles() { throw new NotImplementedException(); }
    public boolean addIgnored(long lo0) { throw new NotImplementedException(); }
    private void setMissionDeathEffects() { throw new NotImplementedException(); }
    public void setMayAppointPlayerAssistant(boolean bo0) { throw new NotImplementedException(); }
    public void setIsViewingCookbook() { throw new NotImplementedException(); }
    public int getRoyalLevels() { throw new NotImplementedException(); }
    public void checkInitialTitles() { throw new NotImplementedException(); }
    public long removeFriend(String St0) { throw new NotImplementedException(); }
    public void setAffString(String St0) { throw new NotImplementedException(); }
    public void setLink(boolean bo0) { throw new NotImplementedException(); }
    public void setAlcohol(float fl0) { throw new NotImplementedException(); }
    public void checkFaithTitles() { throw new NotImplementedException(); }
    public long getChangeKingdomLimit() { throw new NotImplementedException(); }
    public void calculateSpawnPoints() { throw new NotImplementedException(); }
    public String checkCourseRestrictions() { throw new NotImplementedException(); }
    public void pollActions() { throw new NotImplementedException(); }
    private void pollPayment() { throw new NotImplementedException(); }
    public boolean hasFreeTransfer() { throw new NotImplementedException(); }
    public boolean isFriend(long lo0) { throw new NotImplementedException(); }
    public void showPM(String St0, String St1, String St2, boolean bo0) { throw new NotImplementedException(); }
    public void setNicotine(float fl0) { throw new NotImplementedException(); }
    public long[] getIgnored() { throw new NotImplementedException(); }
    private void removePrivatePOI(MapAnnotation Ma0) { throw new NotImplementedException(); }
    public long calculatePhantasmId(int in0, int in1, int in2) { throw new NotImplementedException(); }
    PlayerInfo access$100(Player Pl0) { throw new NotImplementedException(); }
    public void sendPM(byte by0, String St0, long lo0, String St1, boolean bo0, boolean bo1) { throw new NotImplementedException(); }
    public void addFriend(long lo0, byte by0, String St0) { throw new NotImplementedException(); }
    public void addItemEffect(long lo0, int in0, int in1, float fl0) { throw new NotImplementedException(); }
    public boolean toggleGVHelpWindow(boolean bo0) { throw new NotImplementedException(); }
    public void setPrivateMapPOIList(Set<MapAnnotation> Se0) { throw new NotImplementedException(); }
    public void setRank(int in0) { throw new NotImplementedException(); }
    public void setSteamID(SteamId St0) { throw new NotImplementedException(); }
    public void addSparrer(Creature Cr0) { throw new NotImplementedException(); }
    public int getMaxRank() { throw new NotImplementedException(); }
    public float getAlcohol() { throw new NotImplementedException(); }
    public void disableKosPopups(int in0) { throw new NotImplementedException(); }
    public boolean isViewingCookbook() { throw new NotImplementedException(); }
    public boolean isAFK() { throw new NotImplementedException(); }
    public boolean isSBIdleOffEnabled() { throw new NotImplementedException(); }
    public long getInactivity() { throw new NotImplementedException(); }
    private void pollGlobalEffects() { throw new NotImplementedException(); }
    public int getStudied() { throw new NotImplementedException(); }
    public boolean respondGMTab(String St0, String St1) { throw new NotImplementedException(); }
    public int getWarnings() { throw new NotImplementedException(); }
    public void setNewPlayer(boolean bo0) { throw new NotImplementedException(); }
    public void chatted() { throw new NotImplementedException(); }
    public String getVillageName() { throw new NotImplementedException(); }
    public void setStudied(int in0) { throw new NotImplementedException(); }
    public Friend[] getFriends() { throw new NotImplementedException(); }
    public void checkBodyInventoryConsistency() { throw new NotImplementedException(); }
    public float getNicotine() { throw new NotImplementedException(); }
    public long getPlotCourseCooldown() { throw new NotImplementedException(); }
    public void ban(String St0, long lo0) { throw new NotImplementedException(); }
    public void setIsTransferring(boolean bo0) { throw new NotImplementedException(); }
    private void setPersonalSeed() { throw new NotImplementedException(); }
    public long getLastWarned() { throw new NotImplementedException(); }
    public boolean isQAAccount() { throw new NotImplementedException(); }
    public boolean isAspiringKing() { throw new NotImplementedException(); }
    public boolean isActiveInChat() { throw new NotImplementedException(); }
    public Player doNewPlayer(int in0) { throw new NotImplementedException(); }
    public void setSaveFile(PlayerInfo Pl0) { throw new NotImplementedException(); }
    public void setIpaddress(String St0) { throw new NotImplementedException(); }
    public long getPaymentExpire() { throw new NotImplementedException(); }
    public int getRank() { throw new NotImplementedException(); }
    public void setSendExtraBytes(boolean bo0) { throw new NotImplementedException(); }
    public void setClientSystem(String St0) { throw new NotImplementedException(); }
    public void logoutIn(int in0, String St0) { throw new NotImplementedException(); }
    public boolean isAllowedToEditAllianceMap() { throw new NotImplementedException(); }
    public void sendReligion() { throw new NotImplementedException(); }
    public void checkCanVote() { throw new NotImplementedException(); }
    public void toggleGMLight() { throw new NotImplementedException(); }
    public boolean acceptsKosPopups(int in0) { throw new NotImplementedException(); }
    public boolean mayHearDevTalk() { throw new NotImplementedException(); }
    public void setArcheryMode(boolean bo0) { throw new NotImplementedException(); }
    public void addDuellist(Creature Cr0) { throw new NotImplementedException(); }
    public boolean isMarkedByOrb() { throw new NotImplementedException(); }
    private void checkMayAttack() { throw new NotImplementedException(); }
    public void checkJournalAchievements() { throw new NotImplementedException(); }
    public void setAFKMessage(String St0) { throw new NotImplementedException(); }
    public boolean isFullyLoaded() { throw new NotImplementedException(); }
    public void sendPopup(String St0, String St1) { throw new NotImplementedException(); }
    public void setPaymentExpire(long lo0) { throw new NotImplementedException(); }
    public boolean sendLastMissionInformation() { throw new NotImplementedException(); }
    public boolean isNewTutorial() { throw new NotImplementedException(); }
    public void removeSparrer(Creature Cr0) { throw new NotImplementedException(); }
    public Set<MapAnnotation> getPrivateMapAnnotations() { throw new NotImplementedException(); }
    private void checkVehicleSpeeds() { throw new NotImplementedException(); }
}
