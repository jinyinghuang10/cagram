/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Base64;
import android.util.SparseArray;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;

public class UserConfig extends BaseController {

    public static int selectedAccount;
    //public final static int MAX_ACCOUNT_DEFAULT_COUNT = 16;
    //public final static int MAX_ACCOUNT_COUNT = 4;

    private final Object sync = new Object();
    private boolean configLoaded;
    private TLRPC.User currentUser;
    public boolean registeredForPush;
    public int lastSendMessageId = -210000;
    public int lastBroadcastId = -1;
    public int contactsSavedCount;
    public long clientUserId;
    public int lastContactsSyncTime;
    public int lastHintsSyncTime;
    public boolean draftsLoaded;
    public boolean unreadDialogsLoaded = true;
    public TLRPC.TL_account_tmpPassword tmpPassword;
    public int ratingLoadTime;
    public int botRatingLoadTime;
    public boolean contactsReimported;
    public boolean hasValidDialogLoadIds;
    public int migrateOffsetId = -1;
    public int migrateOffsetDate = -1;
    public long migrateOffsetUserId = -1;
    public long migrateOffsetChatId = -1;
    public long migrateOffsetChannelId = -1;
    public long migrateOffsetAccess = -1;
    public boolean filtersLoaded;

    public int sharingMyLocationUntil;
    public int lastMyLocationShareTime;

    public boolean notificationsSettingsLoaded;
    public boolean notificationsSignUpSettingsLoaded;
    public boolean syncContacts = true;
    public boolean suggestContacts = true;
    public boolean hasSecureData;
    public int loginTime;
    public TLRPC.TL_help_termsOfService unacceptedTermsOfService;
    public long autoDownloadConfigLoadTime;
    public boolean official;
    public boolean deviceInfo;

    public List<String> awaitBillingProductIds = new ArrayList<>();
    public TLRPC.InputStorePaymentPurpose billingPaymentPurpose;

    public String premiumGiftsStickerPack;
    public long lastUpdatedPremiumGiftsStickerPack;

    public volatile byte[] savedPasswordHash;
    public volatile byte[] savedSaltedPassword;
    public volatile long savedPasswordTime;

    private static SparseArray<UserConfig> Instance = new SparseArray<>();

    public static UserConfig getInstance(int num) {
        UserConfig localInstance = Instance.get(num);
        if (localInstance == null) {
            synchronized (UserConfig.class) {
                localInstance = Instance.get(num);
                if (localInstance == null) {
                    Instance.put(num, localInstance = new UserConfig(num));
                }
            }
        }
        return localInstance;
    }

    public static int getActivatedAccountsCount() {
        int count = 0;
        for (int a : SharedConfig.activeAccounts) {
            if (getInstance(a).isClientActivated()) {
                count++;
            }
        }
        return count;
    }

    public UserConfig(int instance) {
        super(instance);
    }

    public static boolean hasPremiumOnAccounts() {
        for (int a : SharedConfig.activeAccounts)  {
            if (AccountInstance.getInstance(a).getUserConfig().isClientActivated() && AccountInstance.getInstance(a).getUserConfig().getUserConfig().isPremium()) {
                return true;
            }
        }
        return false;
    }

    public static int getMaxAccountCount() {
        return hasPremiumOnAccounts() ? 5 : 3;
    }

    public int getNewMessageId() {
        int id;
        synchronized (sync) {
            id = lastSendMessageId;
            lastSendMessageId--;
        }
        return id;
    }

    public void saveConfig(boolean withFile) {
        NotificationCenter.getInstance(currentAccount).doOnIdle(() -> {
            synchronized (sync) {
                try {
                    SharedPreferences.Editor editor = getPreferences().edit();
                    if (currentAccount == 0) {
                        editor.putInt("selectedAccount", selectedAccount);
                    }
                    editor.putBoolean("registeredForPush", registeredForPush);
                    editor.putInt("lastSendMessageId", lastSendMessageId);
                    editor.putInt("contactsSavedCount", contactsSavedCount);
                    editor.putInt("lastBroadcastId", lastBroadcastId);
                    editor.putInt("lastContactsSyncTime", lastContactsSyncTime);
                    editor.putInt("lastHintsSyncTime", lastHintsSyncTime);
                    editor.putBoolean("draftsLoaded", draftsLoaded);
                    editor.putBoolean("unreadDialogsLoaded", unreadDialogsLoaded);
                    editor.putInt("ratingLoadTime", ratingLoadTime);
                    editor.putInt("botRatingLoadTime", botRatingLoadTime);
                    editor.putBoolean("contactsReimported", contactsReimported);
                    editor.putInt("loginTime", loginTime);
                    editor.putBoolean("syncContacts", syncContacts);
                    editor.putBoolean("suggestContacts", suggestContacts);
                    editor.putBoolean("hasSecureData", hasSecureData);
                    editor.putBoolean("notificationsSettingsLoaded3", notificationsSettingsLoaded);
                    editor.putBoolean("notificationsSignUpSettingsLoaded", notificationsSignUpSettingsLoaded);
                    editor.putLong("autoDownloadConfigLoadTime", autoDownloadConfigLoadTime);
                    editor.putBoolean("hasValidDialogLoadIds", hasValidDialogLoadIds);
                    editor.putInt("sharingMyLocationUntil", sharingMyLocationUntil);
                    editor.putInt("lastMyLocationShareTime", lastMyLocationShareTime);
                    editor.putBoolean("official", official);
                    editor.putBoolean("deviceInfo", deviceInfo);

                    editor.putBoolean("filtersLoaded", filtersLoaded);
                    editor.putStringSet("awaitBillingProductIds", new HashSet<>(awaitBillingProductIds));
                    if (billingPaymentPurpose != null) {
                        SerializedData data = new SerializedData(billingPaymentPurpose.getObjectSize());
                        billingPaymentPurpose.serializeToStream(data);
                        editor.putString("billingPaymentPurpose", Base64.encodeToString(data.toByteArray(), Base64.DEFAULT));
                        data.cleanup();
                    } else {
                        editor.remove("billingPaymentPurpose");
                    }
                    editor.putString("premiumGiftsStickerPack", premiumGiftsStickerPack);
                    editor.putLong("lastUpdatedPremiumGiftsStickerPack", lastUpdatedPremiumGiftsStickerPack);

                    editor.putInt("6migrateOffsetId", migrateOffsetId);
                    if (migrateOffsetId != -1) {
                        editor.putInt("6migrateOffsetDate", migrateOffsetDate);
                        editor.putLong("6migrateOffsetUserId", migrateOffsetUserId);
                        editor.putLong("6migrateOffsetChatId", migrateOffsetChatId);
                        editor.putLong("6migrateOffsetChannelId", migrateOffsetChannelId);
                        editor.putLong("6migrateOffsetAccess", migrateOffsetAccess);
                    }

                    if (unacceptedTermsOfService != null) {
                        try {
                            SerializedData data = new SerializedData(unacceptedTermsOfService.getObjectSize());
                            unacceptedTermsOfService.serializeToStream(data);
                            editor.putString("terms", Base64.encodeToString(data.toByteArray(), Base64.DEFAULT));
                            data.cleanup();
                        } catch (Exception ignore) {

                        }
                    } else {
                        editor.remove("terms");
                    }

                    SharedConfig.saveConfig();

                    if (tmpPassword != null) {
                        SerializedData data = new SerializedData();
                        tmpPassword.serializeToStream(data);
                        String string = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT);
                        editor.putString("tmpPassword", string);
                        data.cleanup();
                    } else {
                        editor.remove("tmpPassword");
                    }

                    if (currentUser != null) {
                        if (withFile) {
                            SerializedData data = new SerializedData();
                            currentUser.serializeToStream(data);
                            String string = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT);
                            editor.putString("user", string);
                            data.cleanup();
                        }
                    } else {
                        editor.remove("user");
                    }

                    editor.apply();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
    }

    public static boolean isValidAccount(int num) {
        return num >= 0 && SharedConfig.activeAccounts.contains(num) && getInstance(num).isClientActivated();
    }

    public boolean isClientActivated() {
        synchronized (sync) {
            return currentUser != null;
        }
    }

    public long getClientUserId() {
        synchronized (sync) {
            return currentUser != null ? currentUser.id : 0;
        }
    }

    public String getClientPhone() {
        synchronized (sync) {
            return currentUser != null && currentUser.phone != null ? currentUser.phone : "";
        }
    }

    public TLRPC.User getCurrentUser() {
        synchronized (sync) {
            return currentUser;
        }
    }

    public void setCurrentUser(TLRPC.User user) {
        synchronized (sync) {
            TLRPC.User oldUser = currentUser;
            currentUser = user;
            clientUserId = user.id;
            checkPremium(oldUser, user);
        }
    }

    private void checkPremium(TLRPC.User oldUser, TLRPC.User newUser) {
        if (oldUser == null || (newUser != null && oldUser.premium != newUser.premium)) {
            AndroidUtilities.runOnUIThread(() -> {
                getMessagesController().updatePremium(newUser.premium || NekoConfig.localPremium.Bool());
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.premiumStatusChangedGlobal);

                getMediaDataController().loadPremiumPromo(false);
            });
        }
    }

    public void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }
            SharedPreferences preferences = getPreferences();
            if (currentAccount == 0) {
                selectedAccount = preferences.getInt("selectedAccount", 0);
            }
            registeredForPush = preferences.getBoolean("registeredForPush", false);
            lastSendMessageId = preferences.getInt("lastSendMessageId", -210000);
            contactsSavedCount = preferences.getInt("contactsSavedCount", 0);
            lastBroadcastId = preferences.getInt("lastBroadcastId", -1);
            lastContactsSyncTime = preferences.getInt("lastContactsSyncTime", (int) (System.currentTimeMillis() / 1000) - 23 * 60 * 60);
            lastHintsSyncTime = preferences.getInt("lastHintsSyncTime", (int) (System.currentTimeMillis() / 1000) - 25 * 60 * 60);
            draftsLoaded = preferences.getBoolean("draftsLoaded", false);
            unreadDialogsLoaded = preferences.getBoolean("unreadDialogsLoaded", false);
            contactsReimported = preferences.getBoolean("contactsReimported", false);
            ratingLoadTime = preferences.getInt("ratingLoadTime", 0);
            botRatingLoadTime = preferences.getInt("botRatingLoadTime", 0);
            loginTime = preferences.getInt("loginTime", currentAccount);
            syncContacts = preferences.getBoolean("syncContacts", true);
            suggestContacts = preferences.getBoolean("suggestContacts", true);
            hasSecureData = preferences.getBoolean("hasSecureData", false);
            notificationsSettingsLoaded = preferences.getBoolean("notificationsSettingsLoaded3", false);
            notificationsSignUpSettingsLoaded = preferences.getBoolean("notificationsSignUpSettingsLoaded", false);
            autoDownloadConfigLoadTime = preferences.getLong("autoDownloadConfigLoadTime", 0);
            hasValidDialogLoadIds = preferences.contains("2dialogsLoadOffsetId") || preferences.getBoolean("hasValidDialogLoadIds", false);
            official = preferences.getBoolean("official", false);
            deviceInfo = preferences.getBoolean("deviceInfo", true);

            sharingMyLocationUntil = preferences.getInt("sharingMyLocationUntil", 0);
            lastMyLocationShareTime = preferences.getInt("lastMyLocationShareTime", 0);
            filtersLoaded = preferences.getBoolean("filtersLoaded", false);
            awaitBillingProductIds = new ArrayList<>(preferences.getStringSet("awaitBillingProductIds", Collections.emptySet()));
            if (preferences.contains("billingPaymentPurpose")) {
                String purpose = preferences.getString("billingPaymentPurpose", null);
                if (purpose != null) {
                    byte[] arr = Base64.decode(purpose, Base64.DEFAULT);
                    if (arr != null) {
                        SerializedData data = new SerializedData();
                        billingPaymentPurpose = TLRPC.InputStorePaymentPurpose.TLdeserialize(data, data.readInt32(false), false);
                        data.cleanup();
                    }
                }
            }
            premiumGiftsStickerPack = preferences.getString("premiumGiftsStickerPack", null);
            lastUpdatedPremiumGiftsStickerPack = preferences.getLong("lastUpdatedPremiumGiftsStickerPack", 0);

            try {
                String terms = preferences.getString("terms", null);
                if (terms != null) {
                    byte[] arr = Base64.decode(terms, Base64.DEFAULT);
                    if (arr != null) {
                        SerializedData data = new SerializedData(arr);
                        unacceptedTermsOfService = TLRPC.TL_help_termsOfService.TLdeserialize(data, data.readInt32(false), false);
                        data.cleanup();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }

            migrateOffsetId = preferences.getInt("6migrateOffsetId", 0);
            if (migrateOffsetId != -1) {
                migrateOffsetDate = preferences.getInt("6migrateOffsetDate", 0);
                migrateOffsetUserId = AndroidUtilities.getPrefIntOrLong(preferences, "6migrateOffsetUserId", 0);
                migrateOffsetChatId = AndroidUtilities.getPrefIntOrLong(preferences, "6migrateOffsetChatId", 0);
                migrateOffsetChannelId = AndroidUtilities.getPrefIntOrLong(preferences, "6migrateOffsetChannelId", 0);
                migrateOffsetAccess = preferences.getLong("6migrateOffsetAccess", 0);
            }

            String string = preferences.getString("tmpPassword", null);
            if (string != null) {
                byte[] bytes = Base64.decode(string, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    tmpPassword = TLRPC.TL_account_tmpPassword.TLdeserialize(data, data.readInt32(false), false);
                    data.cleanup();
                }
            }

            string = preferences.getString("user", null);
            if (string != null) {
                byte[] bytes = Base64.decode(string, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    currentUser = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                    data.cleanup();
                }
            }
            if (currentUser != null) {
                checkPremium(null, currentUser);
                clientUserId = currentUser.id;
            }
            configLoaded = true;
        }
    }

    public boolean isConfigLoaded() {
        return configLoaded;
    }

    public void savePassword(byte[] hash, byte[] salted) {
        savedPasswordTime = SystemClock.elapsedRealtime();
        savedPasswordHash = hash;
        savedSaltedPassword = salted;
    }

    public void checkSavedPassword() {
        if (savedSaltedPassword == null && savedPasswordHash == null || Math.abs(SystemClock.elapsedRealtime() - savedPasswordTime) < 30 * 60 * 1000) {
            return;
        }
        resetSavedPassword();
    }

    public void resetSavedPassword() {
        savedPasswordTime = 0;
        if (savedPasswordHash != null) {
            Arrays.fill(savedPasswordHash, (byte) 0);
            savedPasswordHash = null;
        }
        if (savedSaltedPassword != null) {
            Arrays.fill(savedSaltedPassword, (byte) 0);
            savedSaltedPassword = null;
        }
    }

    private SharedPreferences getPreferences() {
        if (currentAccount == 0) {
            return ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
        } else {
            return ApplicationLoader.applicationContext.getSharedPreferences("userconfig" + currentAccount, Context.MODE_PRIVATE);
        }
    }

    public void clearConfig() {
        getPreferences().edit().clear().apply();

        sharingMyLocationUntil = 0;
        lastMyLocationShareTime = 0;
        currentUser = null;
        clientUserId = 0;
        registeredForPush = false;
        contactsSavedCount = 0;
        lastSendMessageId = -210000;
        lastBroadcastId = -1;
        notificationsSettingsLoaded = false;
        notificationsSignUpSettingsLoaded = false;
        migrateOffsetId = -1;
        migrateOffsetDate = -1;
        migrateOffsetUserId = -1;
        migrateOffsetChatId = -1;
        migrateOffsetChannelId = -1;
        migrateOffsetAccess = -1;
        ratingLoadTime = 0;
        botRatingLoadTime = 0;
        draftsLoaded = false;
        contactsReimported = true;
        syncContacts = true;
        suggestContacts = true;
        unreadDialogsLoaded = true;
        hasValidDialogLoadIds = true;
        unacceptedTermsOfService = null;
        filtersLoaded = false;
        hasSecureData = false;
        loginTime = (int) (System.currentTimeMillis() / 1000);
        lastContactsSyncTime = (int) (System.currentTimeMillis() / 1000) - 23 * 60 * 60;
        lastHintsSyncTime = (int) (System.currentTimeMillis() / 1000) - 25 * 60 * 60;
        resetSavedPassword();
        boolean hasActivated = false;
        for (int a : SharedConfig.activeAccounts) {
            if (AccountInstance.getInstance(a).getUserConfig().isClientActivated()) {
                hasActivated = true;
                break;
            }
        }
        if (!hasActivated) {
            SharedConfig.clearConfig();
        }
        saveConfig(true);
    }

    public boolean isPinnedDialogsLoaded(int folderId) {
        return getPreferences().getBoolean("2pinnedDialogsLoaded" + folderId, false);
    }

    public void setPinnedDialogsLoaded(int folderId, boolean loaded) {
        getPreferences().edit().putBoolean("2pinnedDialogsLoaded" + folderId, loaded).commit();
    }

    public static final int i_dialogsLoadOffsetId = 0;
    public static final int i_dialogsLoadOffsetDate = 1;
    public static final int i_dialogsLoadOffsetUserId = 2;
    public static final int i_dialogsLoadOffsetChatId = 3;
    public static final int i_dialogsLoadOffsetChannelId = 4;
    public static final int i_dialogsLoadOffsetAccess = 5;

    public int getTotalDialogsCount(int folderId) {
        return getPreferences().getInt("2totalDialogsLoadCount" + (folderId == 0 ? "" : folderId), 0);
    }

    public void setTotalDialogsCount(int folderId, int totalDialogsLoadCount) {
        getPreferences().edit().putInt("2totalDialogsLoadCount" + (folderId == 0 ? "" : folderId), totalDialogsLoadCount).commit();
    }

    public long[] getDialogLoadOffsets(int folderId) {
        SharedPreferences preferences = getPreferences();
        int dialogsLoadOffsetId = preferences.getInt("2dialogsLoadOffsetId" + (folderId == 0 ? "" : folderId), hasValidDialogLoadIds ? 0 : -1);
        int dialogsLoadOffsetDate = preferences.getInt("2dialogsLoadOffsetDate" + (folderId == 0 ? "" : folderId), hasValidDialogLoadIds ? 0 : -1);
        long dialogsLoadOffsetUserId = AndroidUtilities.getPrefIntOrLong(preferences, "2dialogsLoadOffsetUserId" + (folderId == 0 ? "" : folderId), hasValidDialogLoadIds ? 0 : -1);
        long dialogsLoadOffsetChatId = AndroidUtilities.getPrefIntOrLong(preferences, "2dialogsLoadOffsetChatId" + (folderId == 0 ? "" : folderId), hasValidDialogLoadIds ? 0 : -1);
        long dialogsLoadOffsetChannelId = AndroidUtilities.getPrefIntOrLong(preferences, "2dialogsLoadOffsetChannelId" + (folderId == 0 ? "" : folderId), hasValidDialogLoadIds ? 0 : -1);
        long dialogsLoadOffsetAccess = preferences.getLong("2dialogsLoadOffsetAccess" + (folderId == 0 ? "" : folderId), hasValidDialogLoadIds ? 0 : -1);
        return new long[]{dialogsLoadOffsetId, dialogsLoadOffsetDate, dialogsLoadOffsetUserId, dialogsLoadOffsetChatId, dialogsLoadOffsetChannelId, dialogsLoadOffsetAccess};
    }

    public void setDialogsLoadOffset(int folderId, int dialogsLoadOffsetId, int dialogsLoadOffsetDate, long dialogsLoadOffsetUserId, long dialogsLoadOffsetChatId, long dialogsLoadOffsetChannelId, long dialogsLoadOffsetAccess) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt("2dialogsLoadOffsetId" + (folderId == 0 ? "" : folderId), dialogsLoadOffsetId);
        editor.putInt("2dialogsLoadOffsetDate" + (folderId == 0 ? "" : folderId), dialogsLoadOffsetDate);
        editor.putLong("2dialogsLoadOffsetUserId" + (folderId == 0 ? "" : folderId), dialogsLoadOffsetUserId);
        editor.putLong("2dialogsLoadOffsetChatId" + (folderId == 0 ? "" : folderId), dialogsLoadOffsetChatId);
        editor.putLong("2dialogsLoadOffsetChannelId" + (folderId == 0 ? "" : folderId), dialogsLoadOffsetChannelId);
        editor.putLong("2dialogsLoadOffsetAccess" + (folderId == 0 ? "" : folderId), dialogsLoadOffsetAccess);
        editor.putBoolean("hasValidDialogLoadIds", true);
        editor.commit();
    }

    public boolean isPremium() {
        if (currentUser == null) {
            return false;
        }
        return currentUser.premium || NekoConfig.localPremium.Bool();
    }
}
