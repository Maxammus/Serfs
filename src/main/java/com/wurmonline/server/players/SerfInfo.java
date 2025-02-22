package com.wurmonline.server.players;
import com.wurmonline.server.deities.Deity;
import com.wurmonline.server.steam.SteamId;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;

//Empty PlayerInfo class.  Probably not needed since all Player methods that would use this
//should have been reflected away
public class SerfInfo extends PlayerInfo {

    public SerfInfo(String aname) {
        super(aname);
    }

    void setLastVehicle(long _lastvehicle) { }
    public void setPower(byte b) throws IOException { }
    public void setPaymentExpire(long l) throws IOException { }
    public void setPaymentExpire(long l, boolean b) throws IOException { }
    public void setBanned(boolean b, String s, long l) throws IOException { }
    public void resetWarnings() throws IOException { }
    public void setMuted(boolean b, String s, long l) { }
    void setFatigueSecs(int i, long l) { }
    void setCheated(String s) { }
    public void updatePassword(String s) throws IOException { }
    public void setRealDeath(byte b) throws IOException { }
    public void setFavor(float v) throws IOException { }
    public void setFaith(float v) throws IOException { }
    void setDeity(Deity deity) throws IOException { }
    void setAlignment(float v) throws IOException { }
    void setGod(Deity deity) throws IOException { }
    public void load() throws IOException { }
    public void warn() throws IOException { }
    public void save() throws IOException { }
    public void setLastTrigger(int i) { }
    void setIpaddress(String s) throws IOException { }
    void setSteamId(SteamId steamId) throws IOException { }
    public void setRank(int i) throws IOException { }
    public void setReimbursed(boolean b) throws IOException { }
    void setPlantedSign() throws IOException { }
    void setChangedDeity() throws IOException { }
    public String getIpaddress() { return "0.0.0.0"; }
    void setDead(boolean b) { }
    public void setSessionKey(String s, long l) throws IOException { }
    void setName(String s) throws IOException { }
    public void setVersion(long l) throws IOException { }
    void saveFriend(long l, long l1, byte b, String s) throws IOException { }
    void updateFriend(long l, long l1, byte b, String s) throws IOException { }
    void deleteFriend(long l, long l1) throws IOException { }
    void saveEnemy(long l, long l1) throws IOException { }
    void deleteEnemy(long l, long l1) throws IOException { }
    void saveIgnored(long l, long l1) throws IOException { }
    void deleteIgnored(long l, long l1) throws IOException { }
    public void setNumFaith(byte b, long l) throws IOException { }
    long getFlagLong() { return 0; }
    long getFlag2Long() { return 0; }
    public void setMoney(long l) throws IOException { }
    void setSex(byte b) throws IOException { }
    void setClimbing(boolean b) throws IOException { }
    void setChangedKingdom(byte b, boolean b1) throws IOException { }
    public void setFace(long l) throws IOException { }
    boolean addTitle(Titles.Title title) { return false; }
    boolean removeTitle(Titles.Title title) { return false; }
    void setAlcohol(float v) { }
    void setPet(long l) { }
    public void setNicotineTime(long l) { }
    public boolean setAlcoholTime(long l) { return false; }
    void setNicotine(float v) { }
    public void setMayMute(boolean b) { }
    public void setEmailAddress(String s) { }
    void setPriest(boolean b) { }
    public void setOverRideShop(boolean b) { }
    public void setReferedby(long l) { }
    public void setBed(long l) { }
    void setLastChangedVillage(long l) { }
    void setSleep(int i) { }
    void setTheftwarned(boolean b) { }
    public void setHasNoReimbursementLeft(boolean b) { }
    void setDeathProtected(boolean b) { }
    public void setCurrentServer(int i) { }
    public void setDevTalk(boolean b) { }
    public void transferDeity(@Nullable Deity deity) throws IOException { }
    void saveSwitchFatigue() { }
    void saveFightMode(byte b) { }
    void setNextAffinity(long l) { }
    public void saveAppointments() { }
    void setTutorialLevel(int i) { }
    void setAutofight(boolean b) { }
    public void setIsPlayerAssistant(boolean b) { }
    public void setMayAppointPlayerAssistant(boolean b) { }
    public boolean togglePlayerAssistantWindow(boolean b) { return b; }
    public void setLastTaggedTerr(byte b) { }
    public void setNewPriestType(byte b, long l) { }
    public void setChangedJoat() { }
    public void setMovedInventory(boolean b) { }
    public void setFreeTransfer(boolean b) { }
    public boolean setHasSkillGain(boolean b) { return true;}
    public void loadIgnored(long l) { }
    public void loadTitles(long l) { }
    public void loadFriends(long l) { }
    public void loadHistorySteamIds(long l) { }
    public void loadHistoryIPs(long l) { }
    public void loadHistoryEmails(long l) { }
    public boolean setChampionPoints(short i) { return true; }
    public void setChangedKingdom() { }
    public void setChampionTimeStamp() { }
    public void setChampChanneling(float v) { }
    public void setMuteTimes(short i) { }
    public void setVotedKing(boolean b) { }
    public void setEpicLocation(byte b, int i) { }
    public void setChaosKingdom(byte b) { }
    public void setHotaWins(short i) { }
    public void setSpamMode(boolean b) { }
    public void setKarma(int i) { }
    public void setScenarioKarma(int i) { }
    public void setBlood(byte b) { }
    public void setFlag(int i, boolean b) { }
    public void setFlagBits(long l) { }
    public void setFlag2Bits(long l) { }
    public void forceFlagsUpdate() { }
    public void setAbility(int i, boolean b) { }
    public void setCurrentAbilityTitle(int i) { }
    public void setUndeadData() { }
    public void setModelName(String s) { }
    public void addMoneyEarnedBySellingEver(long l) { }
    public void setPointsForChamp() { }
    public void switchChamp() { }
    public void setPassRetrieval(String s, String s1) throws IOException { }
    public void setReputation(int i) { }
}
