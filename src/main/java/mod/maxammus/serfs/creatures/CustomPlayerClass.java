package mod.maxammus.serfs.creatures;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.kingdom.Appointment;
import com.wurmonline.server.players.*;
import com.wurmonline.server.steam.SteamId;
import com.wurmonline.server.villages.Village;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.Set;
@SuppressWarnings("unused")
//Through use of reflection CustomPlayerClass actually extends Player during runtime
//But replaces methods Player overrode from Creature with calls to the Creature version

//Using this class between Serf and Player so super calls from Serf go to this
//and then to Creature instead of directly to Player
//And so hot reloads of Serf aren't rejected by the IDE complaining about hierarchy changes.
public class CustomPlayerClass extends Creature {
    public CustomPlayerClass(long aId) throws Exception {
        super(aId);
    }

    public CustomPlayerClass(CreatureTemplate aTemplate) throws Exception {
        super(aTemplate);
    }

    //Altered methods
    public boolean pollDead() { return isDead(); }
    private void setDead(boolean bo0) {
        try {
            status.setDead(bo0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public long getLastChangedPath() { return 0; } //think serfs will always be able to change paths with this but oh well

    //Seems OK to do nothing when called for serfs
    public void initialisePlayer(PlayerInfo Pl0) { }
    public void sendReligion() { }
    public void sendKarma() { }
    public void sendScenarioKarma() { }
    public void setSaveFile(PlayerInfo Pl0) { }
    public void openBank() { }
    public void closeBank() { }
    private void pollPayment() { }
    private void nutcase(Cultist Cu0) { }
    private void sendNewPhantasm(boolean bo0) { }
    public void resetInactivity(boolean bo0) { }
    private void setPremStuff() { }
    private void contactLoginServerForAwards(boolean bo0) { }
    public void setAddFriendTimout(int in0, Friend.Category Fr0) { }
    public void addFriend(long lo0, byte by0, String St0) { }
    public void removeFriend(long lo0) { }
    public void removeMeFromFriendsList(long lo0, String St0) { }
    public void updateFriendData(long lo0, byte by0, String St0) { }
    public void setAskFriend(String St0, Friend.Category Fr0) { }
    private void sendFriendTimedOut() { }
    public byte remoteAddFriend(String St0, byte by0, byte by1, boolean bo0, boolean bo1) { return 7;}
    public boolean isFriend(long lo0) { return false; }
    public boolean askingFriend() { return false; }
    public Friend[] getFriends() { return new Friend[0]; }
    public long removeFriend(String St0) { return -10; }
    public String waitingForFriend() { return ""; }
    public Friend getFriend(long lo0) { return null; }
    public boolean addIgnored(long lo0) { return false; }
    public boolean removeIgnored(long lo0) { return false; }
    public long[] getIgnored() { return new long[0]; }
    public void setPaymentExpire(long lo0) { }
    public long getPaymentExpire() { return System.currentTimeMillis() + 29030400000L; }
    public void setPower(byte by0) { }
    public void setRank(int in0) { }
    public int getRoyalLevels() { return 0;}
    public boolean sendLastMissionInformation() { return false; }
    public void setLastTrigger(int in0) { }
    public boolean isReimbursed() { return true; }
    public boolean isNewTutorial() { return true; }
    public boolean hasPlantedSign() { return true; }
    public boolean mayHearDevTalk() { return false; }
    public boolean mayHearMgmtTalk() { return false; }
    public boolean startBank(Village Vi0) { return false; }
    private void addPvPDeath() { }
    public long getLastLogin() { return 0; }
    public long getLastLogout() { return 0; }
    public int getWarnings() { return 0; }
    public long getLastWarned() { return 0; }
    public String getWarningStats(long lo0) { return ""; }
    public void sendAddChampionPoints() { }
    public int getChampionPoints() { return 0; }
    public long getAlcoholAddiction() { return 0; }
    public long getNicotineAddiction() { return 0; }
    public int getRank() { return 0; }
    public int getMaxRank() { return 0; }
    public float getAlcohol() { return 0; }
    public float getNicotine() { return 0; }
    public void setAlcohol(float fl0) {  }
    private void pollAlcohol() {  }
    public void setNicotine(float fl0) {  }
    public void setLastChangedVillage(long lo0) {  }
    public boolean isAspiringKing() { return false; }
    public void addDuellist(Creature Cr0) {  }
    public void addSparrer(Creature Cr0) {  }
    public void removeDuellist(Creature Cr0) {  }
    public void removeSparrer(Creature Cr0) {  }
    public void addAppointment(Appointment Ap0, Creature Cr0) {  }
    public boolean mayAppointPlayerAssistant() { return false; }
    public void setPlayerAssistant(boolean bo0) { }
    public void setMayAppointPlayerAssistant(boolean bo0) {  }
    public boolean maySeeGVHelpWindow() { return false; }
    public boolean seesGVHelpWindow() { return false; }
    public boolean togglePlayerAssistantWindow(boolean bo0) { return false; }
    public boolean toggleGVHelpWindow(boolean bo0) { return false; }
    public boolean hasFreeTransfer() { return false; }
    public boolean allowIncomingPMs(String St0, byte by0, long lo0, boolean bo0, byte by1, int in0) { return false; }
    public boolean respondMGMTTab(String St0, String St1) { return true; }
    public boolean respondGMTab(String St0, String St1) { return true; }
    public boolean sendPM(String St0, long lo0, String St1) { return false; }
    public boolean sendPM(byte by0, String St0, long lo0, boolean bo0, String St1, boolean bo1, byte by1, int in0, boolean bo2) { return false; }
    public void sendPM(String St0, String St1, boolean bo0, boolean bo1) {  }
    public void sendPM(byte by0, String St0, long lo0, String St1, boolean bo0, boolean bo1) {  }
    public void showPM(String St0, String St1, String St2, boolean bo0) {  }
    public void showPMWarn(String St0, String St1) {  }
    public void closePM(String St0) {  }
    private byte getPseudoPower() { return 0; }
    public boolean isSignedIn() { return true; }
    public void setSignedIn(boolean bo0) {  }
    public boolean canSignIn() { return false; }
    public boolean isAFK() { return false; }
    public void setAFK(boolean bo0) {  }
    public void setSendExtraBytes(boolean bo0) {  }
    public boolean canUseFreeVillageTeleport() { return false; }
    public void setUsedFreeVillageTeleport() {  }
    public boolean isCreationWindowOpen() { return false; }
    public void setCreationWindowOpen(boolean bo0) {  }
    public boolean isAllowedToEditVillageMap() { return false; }
    public void removeMapPOI(MapAnnotation Ma0) {  }
    public void createNewMapPOI(String St0, byte by0, int in0, int in1, String St1, byte by1) {  }
    public void addMapPOI(MapAnnotation Ma0, boolean bo0) {  }
    public void setPrivateMapPOIList(Set<MapAnnotation> Se0) {  }
    private boolean addPrivateMapPOI(MapAnnotation Ma0) { return false; }
    public void sendClearAllianceMapAnnotations() {  }
    private void removePrivatePOI(MapAnnotation Ma0) {  }
    public boolean isAllowedToEditAllianceMap() { return false; }
    public void sendClearVillageMapAnnotations() {  }
    public PlayerVote getPlayerVote(int in0) { return null; }
    public void gotVotes(boolean bo0) {  }
    public void getVotes() {  }
    public void fillVotes() {  }
    public boolean containsPlayerVote(int in0) { return false; }
    public boolean hasVoted(int in0) { return false; }
    public void setVotes(PlayerVote[] Pl0) {  }
    public void checkCanVote() {  }
    public void sendSpellResistances() {  }
    public void setUndeadType(byte by0) {  }
    public void setIpaddress(String St0) {  }
    public SteamId getSteamId() { return null; }
    public void setSteamID(SteamId St0) {  }
    public void plantSign() {  }
    public Ban getBan() { return null; }
    public void ban(String St0, long lo0) {  }


    //Could be implemented later
    public void setIsTransferring(boolean bo0) { }
    public void sleep() { }
    public void setLogout() { }
    public void logout() { }
    public void logoutIn(int in0, String St0) { }
    public void checkBattleTitle(int in0, int in1) { }
    public void checkInitialBattleTitles() { }
    public void checkFaithTitles() { }
    public void maybeTriggerAchievement(int in0, boolean bo0) { }
    public void checkJournalAchievements() { }
    public void checkInitialTitles() { }
    public long getVersion() { return 0; }
    public Titles.Title[] getTitles() { return new Titles.Title[0]; }
    public void checkAffinity() { }
    public void setBlood(byte by0) {  }

    //Something probably went wrong if these ever get called
    public Player doNewPlayer(int in0, SocketConnection So0) { throw new NotImplementedException(); }
    public Player doNewPlayer(int in0) { throw new NotImplementedException(); }


    //Probably ok to let Player handle but didn't fully confirm
//    public void calculateSpawnPoints() { throw new NotImplementedException(); }
//    private void setMissionDeathEffects() { throw new NotImplementedException(); }
//    private void clearChallengeScores() { throw new NotImplementedException(); }
//    public void addPlotCourseCooldown(long lo0) { throw new NotImplementedException(); }
//    public long getPlotCourseCooldown() { throw new NotImplementedException(); }

    //Let Player handle it
//    public Set<MapAnnotation> getAllianceAnnotations() { throw new NotImplementedException(); }
//    private void checkMaySteal() { throw new NotImplementedException(); }
//    public boolean isBlockingPvP() { throw new NotImplementedException(); }
//    public void setTeleportCounter(int in0) { throw new NotImplementedException(); }
//    public void sendToWorld() { throw new NotImplementedException(); }
//    public boolean isNearCave() { throw new NotImplementedException(); }
//    public void setMarkedByOrb(boolean bo0) { throw new NotImplementedException(); }
//    public boolean hasGlobalEffect(long lo0) { throw new NotImplementedException(); }
//    public void setQuestion(Question Qu0) { throw new NotImplementedException(); }
//    public void spawn(byte by0) { throw new NotImplementedException(); }
//    public void setIsWritingRecipe(boolean bo0) { throw new NotImplementedException(); }
//    public void setFrozen(boolean bo0) { throw new NotImplementedException(); }
//    public boolean isTradeChannel() { throw new NotImplementedException(); }
//    public MapAnnotation getAnnotation(long lo0, byte by0) { throw new NotImplementedException(); }
//    private void checkSendLinkStatus() { throw new NotImplementedException(); }
//    public void addGlobalEffect(Long Lo0, int in0) { throw new NotImplementedException(); }
//    public boolean hasCrownEffect() { throw new NotImplementedException(); }
//    public boolean showTradeStartMessage() { throw new NotImplementedException(); }
//    public Question getCurrentQuestion() { throw new NotImplementedException(); }
//    public boolean isGlobalChat() { throw new NotImplementedException(); }
//    public void sendAllMapAnnotations() { throw new NotImplementedException(); }
//    public void setLastChangedPath(long lo0) { throw new NotImplementedException(); }
//    public boolean shouldGiveAffinity(int in0, boolean bo0) { throw new NotImplementedException(); }
//    public String getEigcId() { throw new NotImplementedException(); }
//    public boolean isSendExtraBytes() { throw new NotImplementedException(); }
//    public void setViewingRecipe(Recipe Re0) { throw new NotImplementedException(); }
//    public boolean isVillageChatShowing() { throw new NotImplementedException(); }
//    public Recipe getViewingRecipe() { throw new NotImplementedException(); }
//    public boolean isKingdomChat() { throw new NotImplementedException(); }
//    private void checkItemsWatched() { throw new NotImplementedException(); }
//    public String getClientVersion() { throw new NotImplementedException(); }
//    public void setNextActionRarity(byte by0) { throw new NotImplementedException(); }
//    public boolean showGlobalKingdomStartMessage() { throw new NotImplementedException(); }
//    public void receivedCmd(int in0) { throw new NotImplementedException(); }
//    public void clearRespondTo() { throw new NotImplementedException(); }
//    public boolean isWritingRecipe() { throw new NotImplementedException(); }}
//    private void sendServerMessage(String St0, int in0, int in1, int in2) { throw new NotImplementedException(); }
//    public void reimburse() { throw new NotImplementedException(); }
//    public boolean checkTileInvulnerability() { throw new NotImplementedException(); }
//    public void setRarityShader(byte by0) { throw new NotImplementedException(); }
//    private boolean calculateMissionSpawnPoint() { throw new NotImplementedException(); }
//    public float[] findRandomSpawnX(boolean bo0, boolean bo1) { throw new NotImplementedException(); }
//    private MapAnnotation getVillageAnnotationById(long lo0) { throw new NotImplementedException(); }
//    private MapAnnotation getAllianceAnnotationById(long lo0) { throw new NotImplementedException(); }
//    public void chattedLocal() { throw new NotImplementedException(); }
//    public void setClientVersion(String St0) { throw new NotImplementedException(); }
//    public long getSleepBonusInactivity() { throw new NotImplementedException(); }
//    public boolean showAllianceMessage() { throw new NotImplementedException(); }
//    public boolean allowIncomingPMs(String St0, byte by0, long lo0, boolean bo0, byte by1, int in0) { throw new NotImplementedException(); }
//    public LoginHandler getLoginhandler() { throw new NotImplementedException(); }
//    public void sendMarkedByOrb() { throw new NotImplementedException(); }
//    public void createSomeItems(float fl0, boolean bo0) { throw new NotImplementedException(); }
//    public void setLoginStep(int in0) { throw new NotImplementedException(); }
//    public boolean isActiveInLocalChat() { throw new NotImplementedException(); }
//    public void checkLantern() { throw new NotImplementedException(); }
//    public long getRespondTo() { throw new NotImplementedException(); }
//    private Item[] getItemsWatched() { throw new NotImplementedException(); }
//    private void spreadCrownInfluence() { throw new NotImplementedException(); }
//    public void removePlayerVote(int in0) { throw new NotImplementedException(); }
//    public void reimbAnniversaryGift(boolean bo0) { throw new NotImplementedException(); }
//    public boolean showVillageMessage() { throw new NotImplementedException(); }
//    public void setEigcClientId(String St0) { throw new NotImplementedException(); }
//    public boolean isNew() { throw new NotImplementedException(); }
//    public String getAFKMessage() { throw new NotImplementedException(); }
//    public void checkPaymentUpdate() { throw new NotImplementedException(); }
//    public PlayerInfo getSaveFile() { throw new NotImplementedException(); }
//    public void setFullyLoaded() { throw new NotImplementedException(); }
//    public void reimbursePacks(boolean bo0) { throw new NotImplementedException(); }
//    public int getLoginStep() { throw new NotImplementedException(); }
//    private void pollStealAttack() { throw new NotImplementedException(); }
//    private short[] getSpawnPointOutside(Village Vi0) { throw new NotImplementedException(); }
//    public int getSecondsPlayedSinceLinkLoss() { throw new NotImplementedException(); }
//    public Set<MapAnnotation> getVillageAnnotations() { throw new NotImplementedException(); }
//    private void sendNormalServerMessage(String St0) { throw new NotImplementedException(); }
//    public boolean isAllianceChatShowing() { throw new NotImplementedException(); }
//    public Set<MapAnnotation> getAllMapAnnotations() { throw new NotImplementedException(); }
//    public void setLoginHandler(LoginHandler Lo0) { throw new NotImplementedException(); }
//    public boolean showKingdomStartMessage() { throw new NotImplementedException(); }
//    private MapAnnotation getPrivateAnnotationById(long lo0) { throw new NotImplementedException(); }
//    public String getClientSystem() { throw new NotImplementedException(); }
//    public void sendSpawnQuestion() { throw new NotImplementedException(); }
//    public void addPlayerVote(PlayerVote Pl0) { throw new NotImplementedException(); }
//    public void sendLantern(VirtualZone Vi0) { throw new NotImplementedException(); }
//    public void checkKingdom() { throw new NotImplementedException(); }
//    public void setIsViewingCookbook() { throw new NotImplementedException(); }
//    public void setAffString(String St0) { throw new NotImplementedException(); }
//    public void setLink(boolean bo0) { throw new NotImplementedException(); }
//    public long getChangeKingdomLimit() { throw new NotImplementedException(); }
//    public String checkCourseRestrictions() { throw new NotImplementedException(); }
//    public void pollActions() { throw new NotImplementedException(); }
//    public long calculatePhantasmId(int in0, int in1, int in2) { throw new NotImplementedException(); }
//    public void addItemEffect(long lo0, int in0, int in1, float fl0) { throw new NotImplementedException(); }
//    public void disableKosPopups(int in0) { throw new NotImplementedException(); }
//    public boolean isViewingCookbook() { throw new NotImplementedException(); }
//    public boolean isSBIdleOffEnabled() { throw new NotImplementedException(); }
//    public long getInactivity() { throw new NotImplementedException(); }
//    private void pollGlobalEffects() { throw new NotImplementedException(); }
//    public int getStudied() { throw new NotImplementedException(); }
//    public void setNewPlayer(boolean bo0) { throw new NotImplementedException(); }
//    public void chatted() { throw new NotImplementedException(); }
//    public String getVillageName() { throw new NotImplementedException(); }
//    public void setStudied(int in0) { throw new NotImplementedException(); }
//    public void checkBodyInventoryConsistency() { throw new NotImplementedException(); }
//    private void setPersonalSeed() { throw new NotImplementedException(); }
//    public boolean isQAAccount() { throw new NotImplementedException(); }
//    public boolean isActiveInChat() { throw new NotImplementedException(); }
//    public void setClientSystem(String St0) { throw new NotImplementedException(); }
//    public void toggleGMLight() { throw new NotImplementedException(); }
//    public boolean acceptsKosPopups(int in0) { throw new NotImplementedException(); }
//    public void setArcheryMode(boolean bo0) { throw new NotImplementedException(); }
//    public boolean isMarkedByOrb() { throw new NotImplementedException(); }
//    private void checkMayAttack() { throw new NotImplementedException(); }
//    public void setAFKMessage(String St0) { throw new NotImplementedException(); }
//    public boolean isFullyLoaded() { throw new NotImplementedException(); }
//    public void sendPopup(String St0, String St1) { throw new NotImplementedException(); }
//    public Set<MapAnnotation> getPrivateMapAnnotations() { throw new NotImplementedException(); }
//    private void checkVehicleSpeeds() { throw new NotImplementedException(); }
}
