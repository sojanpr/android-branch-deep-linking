package io.branch.referral;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.branch.indexing.BranchUniversalObject;
import io.branch.indexing.ContentDiscoverer;
import io.branch.referral.network.BranchRemoteInterface;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CommerceEvent;
import io.branch.referral.util.LinkProperties;

/**
 * <p>
 * The core object required when using Branch SDK. You should declare an object of this type at
 * the class-level of each Activity or Fragment that you wish to use Branch functionality within.
 * </p>
 * <p>
 * Normal instantiation of this object would look like this:
 * </p>
 * <!--
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 * Branch.getInstance(this.getApplicationContext()) // from an Activity
 * Branch.getInstance(getActivity().getApplicationContext())    // from a Fragment
 * </pre>
 * -->
 */
public class Branch implements BranchViewHandler.IBranchViewEvents, SystemObserver.GAdsParamsFetchEvents, InstallListener.IInstallReferrerEvents {
    
    private static final String TAG = "BranchSDK";
    
    /**
     * Hard-coded {@link String} that denotes a {@link BranchLinkData#tags}; applies to links that
     * are shared with others directly as a user action, via social media for instance.
     */
    public static final String FEATURE_TAG_SHARE = "share";
    
    /**
     * Hard-coded {@link String} that denotes a 'referral' tag; applies to links that are associated
     * with a referral program, incentivized or not.
     */
    public static final String FEATURE_TAG_REFERRAL = "referral";
    
    /**
     * Hard-coded {@link String} that denotes a 'referral' tag; applies to links that are sent as
     * referral actions by users of an app using an 'invite contacts' feature for instance.
     */
    public static final String FEATURE_TAG_INVITE = "invite";
    
    /**
     * Hard-coded {@link String} that denotes a link that is part of a commercial 'deal' or offer.
     */
    public static final String FEATURE_TAG_DEAL = "deal";
    
    /**
     * Hard-coded {@link String} that denotes a link tagged as a gift action within a service or
     * product.
     */
    public static final String FEATURE_TAG_GIFT = "gift";
    
    /**
     * The code to be passed as part of a deal or gift; retrieved from the Branch object as a
     * tag upon initialisation. Of {@link String} format.
     */
    public static final String REDEEM_CODE = "$redeem_code";
    
    /**
     * <p>Default value of referral bucket; referral buckets contain credits that are used when users
     * are referred to your apps. These can be viewed in the Branch dashboard under Referrals.</p>
     */
    public static final String REFERRAL_BUCKET_DEFAULT = "default";
    
    /**
     * <p>Hard-coded value for referral code type. Referral codes will always result on "credit" actions.
     * Even if they are of 0 value.</p>
     */
    public static final String REFERRAL_CODE_TYPE = "credit";
    
    /**
     * Branch SDK version for the current release of the Branch SDK.
     */
    public static final int REFERRAL_CREATION_SOURCE_SDK = 2;
    
    /**
     * Key value for referral code as a parameter.
     */
    public static final String REFERRAL_CODE = "referral_code";
    
    /**
     * The redirect URL provided when the link is handled by a desktop client.
     */
    public static final String REDIRECT_DESKTOP_URL = "$desktop_url";
    
    /**
     * The redirect URL provided when the link is handled by an Android device.
     */
    public static final String REDIRECT_ANDROID_URL = "$android_url";
    
    /**
     * The redirect URL provided when the link is handled by an iOS device.
     */
    public static final String REDIRECT_IOS_URL = "$ios_url";
    
    /**
     * The redirect URL provided when the link is handled by a large form-factor iOS device such as
     * an iPad.
     */
    public static final String REDIRECT_IPAD_URL = "$ipad_url";
    
    /**
     * The redirect URL provided when the link is handled by an Amazon Fire device.
     */
    public static final String REDIRECT_FIRE_URL = "$fire_url";
    
    /**
     * The redirect URL provided when the link is handled by a Blackberry device.
     */
    public static final String REDIRECT_BLACKBERRY_URL = "$blackberry_url";
    
    /**
     * The redirect URL provided when the link is handled by a Windows Phone device.
     */
    public static final String REDIRECT_WINDOWS_PHONE_URL = "$windows_phone_url";
    
    /**
     * Open Graph: The title of your object as it should appear within the graph, e.g., "The Rock".
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_TITLE = "$og_title";
    
    /**
     * The description of the object to appear in social media feeds that use
     * Facebook's Open Graph specification.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_DESC = "$og_description";
    
    /**
     * An image URL which should represent your object to appear in social media feeds that use
     * Facebook's Open Graph specification.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_IMAGE_URL = "$og_image_url";
    
    /**
     * A URL to a video file that complements this object.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_VIDEO = "$og_video";
    
    /**
     * The canonical URL of your object that will be used as its permanent ID in the graph.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_URL = "$og_url";
    
    /**
     * Unique identifier for the app in use.
     */
    public static final String OG_APP_ID = "$og_app_id";
    
    /**
     * {@link String} value denoting the deep link path to override Branch's default one. By
     * default, Branch will use yourapp://open?link_click_id=12345. If you specify this key/value,
     * Branch will use yourapp://'$deeplink_path'?link_click_id=12345
     */
    public static final String DEEPLINK_PATH = "$deeplink_path";
    
    /**
     * {@link String} value indicating whether the link should always initiate a deep link action.
     * By default, unless overridden on the dashboard, Branch will only open the app if they are
     * 100% sure the app is installed. This setting will cause the link to always open the app.
     * Possible values are "true" or "false"
     */
    public static final String ALWAYS_DEEPLINK = "$always_deeplink";
    
    /**
     * An {@link Integer} value indicating the user to reward for applying a referral code. In this
     * case, the user applying the referral code receives credit.
     */
    public static final int REFERRAL_CODE_LOCATION_REFERREE = 0;
    
    /**
     * An {@link Integer} value indicating the user to reward for applying a referral code. In this
     * case, the user who created the referral code receives credit.
     */
    public static final int REFERRAL_CODE_LOCATION_REFERRING_USER = 2;
    
    /**
     * An {@link Integer} value indicating the user to reward for applying a referral code. In this
     * case, both the creator and applicant receive credit
     */
    public static final int REFERRAL_CODE_LOCATION_BOTH = 3;
    
    /**
     * An {@link Integer} value indicating the calculation type of the referral code. In this case,
     * the referral code can be applied continually.
     */
    public static final int REFERRAL_CODE_AWARD_UNLIMITED = 1;
    
    /**
     * An {@link Integer} value indicating the calculation type of the referral code. In this case,
     * a user can only apply a specific referral code once.
     */
    public static final int REFERRAL_CODE_AWARD_UNIQUE = 0;
    
    /**
     * An {@link Integer} value indicating the link type. In this case, the link can be used an
     * unlimited number of times.
     */
    public static final int LINK_TYPE_UNLIMITED_USE = 0;
    
    /**
     * An {@link Integer} value indicating the link type. In this case, the link can be used only
     * once. After initial use, subsequent attempts will not validate.
     */
    public static final int LINK_TYPE_ONE_TIME_USE = 1;
    
    /**
     * <p>An {@link Integer} variable specifying the amount of time in milliseconds to keep a
     * connection alive before assuming a timeout condition.</p>
     *
     * @see <a href="http://developer.android.com/reference/java/util/Timer.html#schedule(java.util.TimerTask, long)">
     * Timer.schedule (TimerTask task, long delay)</a>
     */
    private static final int SESSION_KEEPALIVE = 2000;
    
    /**
     * <p>An {@link Integer} value defining the timeout period in milliseconds to wait during a
     * looping task before triggering an actual connection close during a session close action.</p>
     */
    private static final int PREVENT_CLOSE_TIMEOUT = 500;
    
    /* Json object containing key-value pairs for debugging deep linking */
    private JSONObject deeplinkDebugParams_;
    
    private static boolean disableDeviceIDFetch_;
    
    private boolean enableFacebookAppLinkCheck_ = false;
    
    static boolean isSimulatingInstalls_;
    
    static Boolean isLogging_ = null;
    
    static boolean checkInstallReferrer_ = true;
    private static long playStoreReferrerFetchTime = 1500;
    public static final long NO_PLAY_STORE_REFERRER_WAIT = 0;
    
    /**
     * <p>A {@link Branch} object that is instantiated on init and holds the singleton instance of
     * the class during application runtime.</p>
     */
    private static Branch branchReferral_;
    
    private BranchRemoteInterface branchRemoteInterface_;
    private PrefHelper prefHelper_;
    private final SystemObserver systemObserver_;
    private Context context_;

    final Object lock;
    
    private Semaphore serverSema_;
    
    private final ServerRequestQueue requestQueue_;
    
    private int networkCount_;
    
    private boolean hasNetwork_;
    
    private Map<BranchLinkData, String> linkCache_;
    
    
    /* Set to true when application is instantiating {@BranchApp} by extending or adding manifest entry. */
    private static boolean isAutoSessionMode_ = false;
    
    /* Set to true when {@link Activity} life cycle callbacks are registered. */
    private static boolean isActivityLifeCycleCallbackRegistered_ = false;
    
    
    /* Enumeration for defining session initialisation state. */
    private enum SESSION_STATE {
        INITIALISED, INITIALISING, UNINITIALISED
    }
    
    
    private enum INTENT_STATE {
        PENDING,
        READY
    }
    
    private INTENT_STATE intentState_ = INTENT_STATE.PENDING;
    private boolean handleDelayedNewIntents_ = false;
    
    /* Holds the current Session state. Default is set to UNINITIALISED. */
    private SESSION_STATE initState_ = SESSION_STATE.UNINITIALISED;
    
    /* Instance  of share link manager to share links automatically with third party applications. */
    private ShareLinkManager shareLinkManager_;
    
    /* The current activity instance for the application.*/
    WeakReference<Activity> currentActivityReference_;
    
    /* Specifies the choice of user for isReferrable setting. used to determine the link click is referrable or not. See getAutoSession for usage */
    private enum CUSTOM_REFERRABLE_SETTINGS {
        USE_DEFAULT, REFERRABLE, NON_REFERRABLE
    }
    
    /* By default assume user want to use the default settings. Update this option when user specify custom referrable settings */
    private static CUSTOM_REFERRABLE_SETTINGS customReferrableSettings_ = CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT;
    
    /* Key to indicate whether the Activity was launched by Branch or not. */
    private static final String AUTO_DEEP_LINKED = "io.branch.sdk.auto_linked";
    
    /* Key for Auto Deep link param. The activities which need to automatically deep linked should define in this in the activity metadata. */
    private static final String AUTO_DEEP_LINK_KEY = "io.branch.sdk.auto_link_keys";
    
    /* Path for $deeplink_path or $android_deeplink_path to auto deep link. The activities which need to automatically deep linked should define in this in the activity metadata. */
    private static final String AUTO_DEEP_LINK_PATH = "io.branch.sdk.auto_link_path";
    
    /* Key for disabling auto deep link feature. Setting this to true in manifest will disable auto deep linking feature. */
    private static final String AUTO_DEEP_LINK_DISABLE = "io.branch.sdk.auto_link_disable";
    
    /*Key for defining a request code for an activity. should be added as a metadata for an activity. This is used as a request code for launching a an activity on auto deep link. */
    private static final String AUTO_DEEP_LINK_REQ_CODE = "io.branch.sdk.auto_link_request_code";
    
    /* Request code  used to launch and activity on auto deep linking unless DEF_AUTO_DEEP_LINK_REQ_CODE is not specified for teh activity in manifest.*/
    private static final int DEF_AUTO_DEEP_LINK_REQ_CODE = 1501;
    
    /* Sets to true when the init session params are reported to the app though call back.*/
    boolean isInitReportedThroughCallBack = false;
    
    private final ConcurrentHashMap<String, String> instrumentationExtraData_;
    
    /* Name of the key for getting Fabric Branch API key from string resource */
    private static final String FABRIC_BRANCH_API_KEY = "io.branch.apiKey";
    
    private boolean isGAParamsFetchInProgress_ = false;
    
    String sessionReferredLink_; // Link which opened this application session if opened by a link click.
    
    private static String cookieBasedMatchDomain_ = "app.link"; // Domain name used for cookie based matching.
    
    private static int LATCH_WAIT_UNTIL = 2500; //used for getLatestReferringParamsSync and getFirstReferringParamsSync, fail after this many milliseconds
    
    /* List of keys whose values are collected from the Intent Extra.*/
    private static final String[] EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST = new String[]{
            "extra_launch_uri",   // Key for embedded uri in FB ads triggered intents
            "branch_intent"       // A boolean that specifies if this intent is originated by Branch
    };
    
    private CountDownLatch getFirstReferringParamsLatch = null;
    private CountDownLatch getLatestReferringParamsLatch = null;
    
    /* Flag for checking of Strong matching is waiting on GAID fetch */
    private boolean performCookieBasedStrongMatchingOnGAIDAvailable = false;
    
    boolean isInstantDeepLinkPossible = false;
    /* Flag to find if the activity is launched from stack (incase of  single top) or created fresh and launched */
    private boolean isActivityCreatedAndLaunched = false;
    /* Flag to turn on or off instant deeplinking feature. IDL is disabled by default */
    private static boolean disableInstantDeepLinking = true;
    private final TrackingController trackingController;
    
    /**
     * <p>The main constructor of the Branch class is private because the class uses the Singleton
     * pattern.</p>     *
     * <p>Use {@link #getInstance(Context) getInstance} method when instantiating.</p>
     *
     * @param context A {@link Context} from which this call was made.
     */
    private Branch(@NonNull Context context) {
        prefHelper_ = PrefHelper.getInstance(context);
        trackingController = new TrackingController(context);
        branchRemoteInterface_ = BranchRemoteInterface.getDefaultBranchRemoteInterface(context);
        systemObserver_ = new SystemObserver(context);
        requestQueue_ = ServerRequestQueue.getInstance(context);
        serverSema_ = new Semaphore(1);
        lock = new Object();
        networkCount_ = 0;
        hasNetwork_ = true;
        linkCache_ = new HashMap<>();
        instrumentationExtraData_ = new ConcurrentHashMap<>();
        if (!trackingController.isTrackingDisabled()) { // Do not get GAID when tracking is disabled
            isGAParamsFetchInProgress_ = systemObserver_.prefetchGAdsParams(this);
        }
        // newIntent() delayed issue is only with Android M+ devices. So need to handle android M and above
        // PRS: Since this seem more reliable and not causing any integration issues adding this to all supported SDK versions
        if (android.os.Build.VERSION.SDK_INT >= 15) {
            handleDelayedNewIntents_ = true;
            intentState_ = INTENT_STATE.PENDING;
        } else {
            handleDelayedNewIntents_ = false;
            intentState_ = INTENT_STATE.READY;
        }
    }


    public Context getApplicationContext() {
        return context_;
    }

    /**
     * Sets a custom Branch Remote interface for handling RESTful requests. Call this for implementing a custom network layer for handling communication between
     * Branch SDK and remote Branch server
     *
     * @param remoteInterface A instance of class extending {@link BranchRemoteInterface} with implementation for abstract RESTful GET or POST methods
     */
    public void setBranchRemoteInterface(BranchRemoteInterface remoteInterface) {
        branchRemoteInterface_ = remoteInterface;
    }
    
    /**
     * <p>
     * Enables/Disables the test mode for the SDK. This will use the Branch Test Keys.
     * This will also enable debug logs.
     * Note: This is same as setting "io.branch.sdk.TestMode" to "True" in Manifest file
     * </p>
     */
    public static void enableTestMode() {
        BranchUtil.isCustomDebugEnabled_ = true;
    }
    
    public static void disableTestMode() {
        BranchUtil.isCustomDebugEnabled_ = false;
    }
    
    public void setDebug() {
        enableTestMode();
    }

    /**
     * Method to change the Tracking state. If disabled SDK will not track any user data or state. SDK will not send any network calls except for deep linking when tracking is disabled
     */
    public void disableTracking(boolean disableTracking) {
        trackingController.disableTracking(context_, disableTracking);
    }
    
    /**
     * Checks if tracking is disabled. See {@link #disableTracking(boolean)}
     *
     * @return {@code true} if tracking is disabled
     */
    public boolean isTrackingDisabled() {
        return trackingController.isTrackingDisabled();
    }
    
    /**
     * @deprecated This method is deprecated since play store referrer is enabled by default from v2.9.1.
     * Please use {@link #setPlayStoreReferrerCheckTimeout(long)} instead.
     */
    public static void enablePlayStoreReferrer(long delay) {
        setPlayStoreReferrerCheckTimeout(delay);
    }
    
    /**
     * Since play store referrer broadcast from google play is few millisecond delayed Branch will delay the collecting deep link data on app install by {@link #playStoreReferrerFetchTime} millisecond
     * This will allow branch to provide for more accurate tracking and attribution. This will delay branch init only the first time user open the app.
     * This method allows to override the maximum wait time for play store referrer to arrive. Set it to {@link Branch#NO_PLAY_STORE_REFERRER_WAIT} if you don't want to wait for play store referrer
     * <p>
     * Note:  as of our testing 4/2017  a 1500 milli sec wait time is enough to capture more than 90% of the install referrer case
     *
     * @param delay {@link Long} Maximum wait time for install referrer broadcast in milli seconds. Set to {@link Branch#NO_PLAY_STORE_REFERRER_WAIT} if you don't want to wait for play store referrer
     */
    public static void setPlayStoreReferrerCheckTimeout(long delay) {
        checkInstallReferrer_ = delay > 0;
        playStoreReferrerFetchTime = delay;
    }
    
    /**
     * <p>
     * Disables or enables the instant deep link functionality.
     * </p>
     *
     * @param disableIDL Value {@code true} disables the  instant deep linking. Value {@code false} enables the  instant deep linking.
     */
    public static void disableInstantDeepLinking(boolean disableIDL) {
        disableInstantDeepLinking = disableIDL;
    }
    
    /**
     * <p>Singleton method to return the pre-initialised object of the type {@link Branch}.
     * Make sure your app is instantiating {@link BranchApp} before calling this method
     * or you have created an instance of Branch already by calling getInstance(Context ctx).</p>
     *
     * @return An initialised singleton {@link Branch} object
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getInstance() {
        /* Check if BranchApp is instantiated. */
        if (branchReferral_ == null) {
            Log.e("BranchSDK", "Branch instance is not created yet. Make sure you have initialised Branch. [Consider Calling getInstance(Context ctx) if you still have issue.]");
        } else if (isAutoSessionMode_) {
            /* Check if Activity life cycle callbacks are set if in auto session mode. */
            if (!isActivityLifeCycleCallbackRegistered_) {
                Log.e("BranchSDK", "Branch instance is not properly initialised. Make sure your Application class is extending BranchApp class. " +
                        "If you are not extending BranchApp class make sure you are initialising Branch in your Applications onCreate()");
            }
        }
        return branchReferral_;
    }
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     *
     * @param context   A {@link Context} from which this call was made.
     * @param branchKey Your Branch key as a {@link String}.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     * @see <a href="https://github.com/BranchMetrics/Branch-Android-SDK/blob/05e234855f983ae022633eb01989adb05775532e/README.md#add-your-app-key-to-your-project">
     * Adding your app key to your project</a>
     */
    public static Branch getInstance(@NonNull Context context, @NonNull String branchKey) {
        if (branchReferral_ == null) {
            branchReferral_ = Branch.initInstance(context);
        }
        branchReferral_.context_ = context.getApplicationContext();
        if (branchKey.startsWith("key_")) {
            boolean isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }
        } else {
            Log.e("BranchSDK", "Branch Key is invalid.Please check your BranchKey");
        }
        return branchReferral_;
    }
    
    private static Branch getBranchInstance(@NonNull Context context, boolean isLive, String branchKey) {
        if (branchReferral_ == null) {
            branchReferral_ = Branch.initInstance(context);
            
            // If a Branch key is passed already use it. Else read the key
            if (TextUtils.isEmpty(branchKey)) {
                branchKey = branchReferral_.prefHelper_.readBranchKey(isLive);
            }
            boolean isNewBranchKeySet;
            if (branchKey == null || branchKey.equalsIgnoreCase(PrefHelper.NO_STRING_VALUE)) {
                // If Branch key is not available check for Fabric provided Branch key
                String fabricBranchApiKey = null;
                try {
                    Resources resources = context.getResources();
                    fabricBranchApiKey = resources.getString(resources.getIdentifier(FABRIC_BRANCH_API_KEY, "string", context.getPackageName()));
                } catch (Exception ignore) {
                }
                if (!TextUtils.isEmpty(fabricBranchApiKey)) {
                    isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(fabricBranchApiKey);
                } else {
                    Log.i("BranchSDK", "Branch Warning: Please enter your branch_key in your project's Manifest file!");
                    isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(PrefHelper.NO_STRING_VALUE);
                }
            } else {
                isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            }
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }
            
            branchReferral_.context_ = context.getApplicationContext();
            /* If {@link Application} is instantiated register for activity life cycle events. */
            if (context instanceof Application) {
                isAutoSessionMode_ = true;
                branchReferral_.setActivityLifeCycleObserver((Application) context);
            }
        }
        return branchReferral_;
    }
    
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    public static Branch getInstance(@NonNull Context context) {
        return getBranchInstance(context, true, null);
    }
    
    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    public static Branch getTestInstance(@NonNull Context context) {
        return getBranchInstance(context, false, null);
    }
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoInstance(@NonNull Context context) {
        isAutoSessionMode_ = true;
        customReferrableSettings_ = CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT;
        boolean isLive = !BranchUtil.isTestModeEnabled(context);
        getBranchInstance(context, isLive, null);
        return branchReferral_;
    }
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context      A {@link Context} from which this call was made.
     * @param isReferrable A {@link Boolean} value indicating whether initialising a session on this Branch instance
     *                     should be considered as potentially referrable or not. By default, a user is only referrable
     *                     if initSession results in a fresh install. Overriding this gives you control of who is referrable.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoInstance(@NonNull Context context, boolean isReferrable) {
        isAutoSessionMode_ = true;
        customReferrableSettings_ = isReferrable ? CUSTOM_REFERRABLE_SETTINGS.REFERRABLE : CUSTOM_REFERRABLE_SETTINGS.NON_REFERRABLE;
        boolean isDebug = BranchUtil.isTestModeEnabled(context);
        getBranchInstance(context, !isDebug, null);
        return branchReferral_;
    }
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context   A {@link Context} from which this call was made.
     * @param branchKey A {@link String} value used to initialize Branch.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoInstance(@NonNull Context context, @NonNull String branchKey) {
        isAutoSessionMode_ = true;
        customReferrableSettings_ = CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT;
        boolean isLive = !BranchUtil.isTestModeEnabled(context);
        getBranchInstance(context, isLive, branchKey);
        
        if (branchKey.startsWith("key_")) {
            boolean isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }
        } else {
            Log.e("BranchSDK", "Branch Key is invalid.Please check your BranchKey");
        }
        return branchReferral_;
    }
    
    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoTestInstance(@NonNull Context context) {
        isAutoSessionMode_ = true;
        customReferrableSettings_ = CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT;
        getBranchInstance(context, false, null);
        return branchReferral_;
    }
    
    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context      A {@link Context} from which this call was made.
     * @param isReferrable A {@link Boolean} value indicating whether initialising a session on this Branch instance
     *                     should be considered as potentially referrable or not. By default, a user is only referrable
     *                     if initSession results in a fresh install. Overriding this gives you control of who is referrable.
     * @return An initialised {@link Branch} object.
     */
    
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoTestInstance(@NonNull Context context, boolean isReferrable) {
        isAutoSessionMode_ = true;
        customReferrableSettings_ = isReferrable ? CUSTOM_REFERRABLE_SETTINGS.REFERRABLE : CUSTOM_REFERRABLE_SETTINGS.NON_REFERRABLE;
        getBranchInstance(context, false, null);
        return branchReferral_;
    }
    
    /**
     * <p>Initialises an instance of the Branch object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    private static Branch initInstance(@NonNull Context context) {
        return new Branch(context.getApplicationContext());
    }
    
    /**
     * <p>Manually sets the {@link Boolean} value, that indicates that the Branch API connection has
     * been initialised, to false - forcing re-initialisation.</p>
     */
    public void resetUserSession() {
        initState_ = SESSION_STATE.UNINITIALISED;
    }
    
    /**
     * <p>Sets the number of times to re-attempt a timed-out request to the Branch API, before
     * considering the request to have failed entirely. Default 5.</p>
     *
     * @param retryCount An {@link Integer} specifying the number of times to retry before giving
     *                   up and declaring defeat.
     */
    public void setRetryCount(int retryCount) {
        if (prefHelper_ != null && retryCount >= 0) {
            prefHelper_.setRetryCount(retryCount);
        }
    }
    
    /**
     * <p>Sets the amount of time in milliseconds to wait before re-attempting a timed-out request
     * to the Branch API. Default 3000 ms.</p>
     *
     * @param retryInterval An {@link Integer} value specifying the number of milliseconds to
     *                      wait before re-attempting a timed-out request.
     */
    public void setRetryInterval(int retryInterval) {
        if (prefHelper_ != null && retryInterval > 0) {
            prefHelper_.setRetryInterval(retryInterval);
        }
    }
    
    /**
     * <p>Sets the duration in milliseconds that the system should wait for a response before considering
     * any Branch API call to have timed out. Default 3000 ms.</p>
     * <p>Increase this to perform better in low network speed situations, but at the expense of
     * responsiveness to error situation.</p>
     *
     * @param timeout An {@link Integer} value specifying the number of milliseconds to wait before
     *                considering the request to have timed out.
     */
    public void setNetworkTimeout(int timeout) {
        if (prefHelper_ != null && timeout > 0) {
            prefHelper_.setTimeout(timeout);
        }
    }
    
    /**
     * Method to control reading Android ID from device. Set this to true to disable reading the device id.
     * This method should be called from your {@link Application#onCreate()} method before creating Branch auto instance by calling {@link Branch#getAutoInstance(Context)}
     *
     * @param deviceIdFetch {@link Boolean with value true to disable reading the Android id from device}
     */
    public static void disableDeviceIDFetch(Boolean deviceIdFetch) {
        disableDeviceIDFetch_ = deviceIdFetch;
    }
    
    /**
     * Returns true if reading device id is disabled
     *
     * @return {@link Boolean} with value true to disable reading Andoid ID
     */
    public static boolean isDeviceIDFetchDisabled() {
        return disableDeviceIDFetch_;
    }
    
    /**
     * Sets the key-value pairs for debugging the deep link. The key-value set in debug mode is given back with other deep link data on branch init session.
     * This method should be called from onCreate() of activity which listens to Branch Init Session callbacks
     *
     * @param debugParams A {@link JSONObject} containing key-value pairs for debugging branch deep linking
     */
    public void setDeepLinkDebugMode(JSONObject debugParams) {
        deeplinkDebugParams_ = debugParams;
    }
    
    /**
     * @deprecated Branch is not listing external apps any more from v2.11.0
     */
    public void disableAppList() {
        // Do nothing
    }
    
    /**
     * <p>
     * Enable Facebook app link check operation during Branch initialisation.
     * </p>
     */
    public void enableFacebookAppLinkCheck() {
        enableFacebookAppLinkCheck_ = true;
    }
    
    /**
     * Enables or disables app tracking with Branch or any other third parties that Branch use internally
     *
     * @param isLimitFacebookTracking {@code true} to limit app tracking
     */
    public void setLimitFacebookTracking(boolean isLimitFacebookTracking) {
        prefHelper_.setLimitFacebookTracking(isLimitFacebookTracking);
    }
    
    /**
     * <p>Add key value pairs to all requests</p>
     */
    public void setRequestMetadata(@NonNull String key, @NonNull String value) {
        prefHelper_.setRequestMetadata(key, value);
    }

    /**
     * <p>
     * This API allows to tag the install with custom attribute. Add any key-values that qualify or distinguish an install here.
     * Please make sure this method is called before the Branch init, which is on the onStartMethod of first activity.
     * A better place to call this  method is right after Branch#getAutoInstance()
     * </p>
     */
    public Branch addInstallMetadata(@NonNull String key, @NonNull String value) {
        prefHelper_.addInstallMetadata(key, value);
        return this;
    }
    
    /**
     * <p>Initialises a session with the Branch API, assigning a {@link BranchUniversalReferralInitListener}
     * to perform an action upon successful initialisation.</p>
     *
     * @param callback A {@link BranchUniversalReferralInitListener} instance that will be called following
     *                 successful (or unsuccessful) initialisation of the session with the Branch API.
     * @return A {@link Boolean} value, indicating <i>false</i> if initialisation is
     * unsuccessful.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback) {
        return initSession(callback, (Activity) null);
    }
    
    /**
     * <p>Initialises a session with the Branch API, assigning a {@link BranchReferralInitListener}
     * to perform an action upon successful initialisation.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called following
     *                 successful (or unsuccessful) initialisation of the session with the Branch API.
     * @return A {@link Boolean} value, indicating <i>false</i> if initialisation is
     * unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback) {
        return initSession(callback, (Activity) null);
    }
    
    /**
     * <p>Initialises a session with the Branch API, passing the {@link Activity} and assigning a
     * {@link BranchUniversalReferralInitListener} to perform an action upon successful initialisation.</p>
     *
     * @param callback A {@link BranchUniversalReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value, indicating <i>false</i> if initialisation is
     * unsuccessful.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, Activity activity) {
        if (customReferrableSettings_ == CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT) {
            initUserSessionInternal(callback, activity, true);
        } else {
            boolean isReferrable = customReferrableSettings_ == CUSTOM_REFERRABLE_SETTINGS.REFERRABLE;
            initUserSessionInternal(callback, activity, isReferrable);
        }
        return true;
    }
    
    /**
     * <p>Initialises a session with the Branch API, passing the {@link Activity} and assigning a
     * {@link BranchReferralInitListener} to perform an action upon successful initialisation.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value, indicating <i>false</i> if initialisation is
     * unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, Activity activity) {
        if (customReferrableSettings_ == CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT) {
            initUserSessionInternal(callback, activity, true);
        } else {
            boolean isReferrable = customReferrableSettings_ == CUSTOM_REFERRABLE_SETTINGS.REFERRABLE;
            initUserSessionInternal(callback, activity, isReferrable);
        }
        return true;
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback A {@link BranchUniversalReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param data     A {@link  Uri} variable containing the details of the source link that
     *                 led to this initialisation action.
     * @return A {@link Boolean} value that will return <i>false</i> if the supplied
     * <i>data</i> parameter cannot be handled successfully - i.e. is not of a
     * valid URI format.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, Uri data) {
        return initSession(callback, data, null);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param data     A {@link  Uri} variable containing the details of the source link that
     *                 led to this initialisation action.
     * @return A {@link Boolean} value that will return <i>false</i> if the supplied
     * <i>data</i> parameter cannot be handled successfully - i.e. is not of a
     * valid URI format.
     */
    public boolean initSession(BranchReferralInitListener callback, Uri data) {
        return initSession(callback, data, null);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback A {@link BranchUniversalReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param data     A {@link  Uri} variable containing the details of the source link that
     *                 led to this initialisation action.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that will return <i>false</i> if the supplied
     * <i>data</i> parameter cannot be handled successfully - i.e. is not of a
     * valid URI format.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, Uri data, Activity activity) {
        readAndStripParam(data, activity);
        initSession(callback, activity);
        return true;
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param data     A {@link  Uri} variable containing the details of the source link that
     *                 led to this initialisation action.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that will return <i>false</i> if the supplied
     * <i>data</i> parameter cannot be handled successfully - i.e. is not of a
     * valid URI format.
     */
    public boolean initSession(BranchReferralInitListener callback, Uri data, Activity activity) {
        readAndStripParam(data, activity);
        return initSession(callback, activity);
    }
    
    /**
     * <p>Initialises a session with the Branch API, without a callback or {@link Activity}.</p>
     *
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession() {
        return initSession((Activity) null);
    }
    
    /**
     * <p>Initialises a session with the Branch API, without a callback or {@link Activity}.</p>
     *
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(Activity activity) {
        return initSession((BranchReferralInitListener) null, activity);
    }
    
    /**
     * <p>Initialises a session with the Branch API, with associated data from the supplied
     * {@link Uri}.</p>
     *
     * @param data A {@link  Uri} variable containing the details of the source link that
     *             led to this
     *             initialisation action.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSessionWithData(Uri data) {
        return initSessionWithData(data, null);
    }
    
    /**
     * <p>Initialises a session with the Branch API, with associated data from the supplied
     * {@link Uri}.</p>
     *
     * @param data     A {@link  Uri} variable containing the details of the source link that led to this
     *                 initialisation action.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSessionWithData(Uri data, Activity activity) {
        readAndStripParam(data, activity);
        return initSession((BranchReferralInitListener) null, activity);
    }
    
    /**
     * <p>Initialises a session with the Branch API, specifying whether the initialisation can count
     * as a referrable action.</p>
     *
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(boolean isReferrable) {
        return initSession((BranchReferralInitListener) null, isReferrable, (Activity) null);
    }
    
    /**
     * <p>Initialises a session with the Branch API, specifying whether the initialisation can count
     * as a referrable action, and supplying the calling {@link Activity} for context.</p>
     *
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(boolean isReferrable, @NonNull Activity activity) {
        return initSession((BranchReferralInitListener) null, isReferrable, activity);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchUniversalReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param data         A {@link  Uri} variable containing the details of the source link that
     *                     led to this initialisation action.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable, Uri data) {
        return initSession(callback, isReferrable, data, null);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param data         A {@link  Uri} variable containing the details of the source link that
     *                     led to this initialisation action.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data) {
        return initSession(callback, isReferrable, data, null);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchUniversalReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param data         A {@link  Uri} variable containing the details of the source link that
     *                     led to this initialisation action.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable, Uri data, Activity activity) {
        readAndStripParam(data, activity);
        return initSession(callback, isReferrable, activity);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param data         A {@link  Uri} variable containing the details of the source link that
     *                     led to this initialisation action.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data, Activity activity) {
        readAndStripParam(data, activity);
        return initSession(callback, isReferrable, activity);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchUniversalReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable) {
        return initSession(callback, isReferrable, (Activity) null);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable) {
        return initSession(callback, isReferrable, (Activity) null);
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchUniversalReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable, Activity activity) {
        initUserSessionInternal(callback, activity, isReferrable);
        return true;
    }
    
    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Activity activity) {
        initUserSessionInternal(callback, activity, isReferrable);
        return true;
    }
    
    
    private void initUserSessionInternal(BranchUniversalReferralInitListener callback, Activity activity, boolean isReferrable) {
        BranchUniversalReferralInitWrapper branchUniversalReferralInitWrapper = new BranchUniversalReferralInitWrapper(callback);
        initUserSessionInternal(branchUniversalReferralInitWrapper, activity, isReferrable);
    }
    
    private void initUserSessionInternal(BranchReferralInitListener callback, Activity activity, boolean isReferrable) {
        if (activity != null) {
            currentActivityReference_ = new WeakReference<>(activity);
        }
        //If already initialised
        if ((hasUser() && hasSession() && initState_ == SESSION_STATE.INITIALISED)) {
            reportInitSession(callback);
            isInstantDeepLinkPossible = false;
        }
        //If uninitialised or initialising
        else {
            // If an instant deeplink is possible then call init session immediately. This should proceed to a normal open call
            if (isInstantDeepLinkPossible) {
                if (reportInitSession(callback)) {
                    addExtraInstrumentationData(Defines.Jsonkey.InstantDeepLinkSession.getKey(), "true");
                    isInstantDeepLinkPossible = false;
                    checkForAutoDeepLinkConfiguration();
                }
            }
            // In case of Auto session init will be called from Branch before user. So initialising
            // State also need to look for isReferrable value
            if (isReferrable) {
                this.prefHelper_.setIsReferrable();
            } else {
                this.prefHelper_.clearIsReferrable();
            }
            
            //If initialising ,then set new callbacks.
            if (initState_ == SESSION_STATE.INITIALISING) {
                if (callback != null) {
                    requestQueue_.setInstallOrOpenCallback(callback);
                }
            }
            //if Uninitialised move request to the front if there is an existing request or create a new request.
            else {
                initState_ = SESSION_STATE.INITIALISING;
                initializeSession(callback);
            }
        }
    }
    
    private boolean reportInitSession(BranchReferralInitListener callback) {
        if (callback != null) {
            if (isAutoSessionMode_) {
                // Since Auto session mode initialise the session by itself on starting the first activity, we need to provide user
                // the referring params if they call init session after init is completed. Note that user wont do InitSession per activity in auto session mode.
                if (!isInitReportedThroughCallBack) { //Check if session params are reported already in case user call initsession form a different activity(not a normal case)
                    callback.onInitFinished(getLatestReferringParams(), null);
                    isInitReportedThroughCallBack = true;
                } else {
                    callback.onInitFinished(new JSONObject(), null);
                }
            } else {
                // Since user will do init session per activity in non auto session mode , we don't want to repeat the referring params with each initSession()call.
                callback.onInitFinished(new JSONObject(), null);
            }
        }
        return isInitReportedThroughCallBack;
    }
    
    /**
     * <p>Closes the current session, dependent on the state of the
     * PrefHelper#getSmartSession() {@link Boolean} value. If <i>true</i>, take no action.
     * If false, close the session via the {@link #executeClose()} )} method.</p>
     * <p>Note that if smartSession is enabled, closeSession cannot be called within
     * a 2 second time span of another Branch action. This has to do with the method that
     * Branch uses to keep a session alive during Activity transitions</p>
     *
     * @deprecated This method is deprecated from SDK v1.14.6. Session Start and close are  automatically handled by Branch.
     * In case you need to handle sessions manually inorder to support minimum sdk version less than 14 please consider using
     * SDK version 1.14.5
     */
    public void closeSession() {
        Log.w("BranchSDK", "closeSession() method is deprecated from SDK v1.14.6.Session is  automatically handled by Branch." +
                "In case you need to handle sessions manually inorder to support minimum sdk version less than 14 please consider using " +
                " SDK version 1.14.5");
    }
    
    /*
     * <p>Closes the current session. Should be called by on getting the last actvity onStop() event.
     * </p>
     */
    void closeSessionInternal() {
        executeClose();
        sessionReferredLink_ = null;
        trackingController.updateTrackingState(context_); // Update the tracking state for next cold start
    }
    
    /**
     * Clears all pending requests in the queue
     */
    void clearPendingRequests() {
        requestQueue_.clear();
    }
    
    /**
     * <p>
     * Enabled Strong matching check using chrome cookies. This method should be called before
     * Branch#getAutoInstance(Context).</p>
     *
     * @param cookieMatchDomain The domain for the url used to match the cookie (eg. example.app.link)
     */
    public static void enableCookieBasedMatching(String cookieMatchDomain) {
        cookieBasedMatchDomain_ = cookieMatchDomain;
    }
    
    /**
     * <p>
     * Enabled Strong matching check using chrome cookies. This method should be called before
     * Branch#getAutoInstance(Context).</p>
     *
     * @param cookieMatchDomain The domain for the url used to match the cookie (eg. example.app.link)
     * @param delay             Time in millisecond to wait for the strong match to check to finish before Branch init session is called.
     *                          Default time is 750 msec.
     */
    public static void enableCookieBasedMatching(String cookieMatchDomain, int delay) {
        cookieBasedMatchDomain_ = cookieMatchDomain;
        BranchStrongMatchHelper.getInstance().setStrongMatchUrlHitDelay(delay);
    }
    
    /**
     * <p>Perform the state-safe actions required to terminate any open session, and report the
     * closed application event to the Branch API.</p>
     */
    private void executeClose() {
        if (initState_ != SESSION_STATE.UNINITIALISED) {
            if (!hasNetwork_) {
                // if there's no network connectivity, purge the old install/open
                ServerRequest req = requestQueue_.peek();
                if (req != null && (req instanceof ServerRequestRegisterInstall) || (req instanceof ServerRequestRegisterOpen)) {
                    requestQueue_.dequeue();
                }
            } else {
                if (!requestQueue_.containsClose()) {
                    ServerRequest req = new ServerRequestRegisterClose(context_);
                    handleNewRequest(req);
                }
            }
            initState_ = SESSION_STATE.UNINITIALISED;
        }
    }
    
    private boolean readAndStripParam(Uri data, Activity activity) {
        // PRS: isActivityCreatedAndLaunched usage: Single top activities can be launched from stack and there may be a new intent provided with onNewIntent() call. In this case need to wait till onResume to get the latest intent.
        // If activity is created and launched then the intent can be readily consumed.
        // NOTE : IDL will not be working if the activity is launched from stack if `initSession` is called from `onStart()`. TODO Need to check for IDL possibility from any #ServerRequestInitSession
        if (!disableInstantDeepLinking) {
            if (intentState_ == INTENT_STATE.READY || isActivityCreatedAndLaunched) {
                // Check for instant deep linking possibility first
                if (activity != null && activity.getIntent() != null && initState_ != SESSION_STATE.INITIALISED && !checkIntentForSessionRestart(activity.getIntent())) {
                    Intent intent = activity.getIntent();
                    // In case of a cold start by clicking app icon or bringing app to foreground Branch link click is always false.
                    if (intent.getData() == null || (!isActivityCreatedAndLaunched && isIntentParamsAlreadyConsumed(activity))) {
                        // Considering the case of a deferred install. In this case the app behaves like a cold start but still Branch can do probabilistic match.
                        // So skipping instant deep link feature until first Branch open happens
                        if (!prefHelper_.getInstallParams().equals(PrefHelper.NO_STRING_VALUE)) {
                            JSONObject nonLinkClickJson = new JSONObject();
                            try {
                                nonLinkClickJson.put(Defines.Jsonkey.Clicked_Branch_Link.getKey(), false);
                                nonLinkClickJson.put(Defines.Jsonkey.IsFirstSession.getKey(), false);
                                prefHelper_.setSessionParams(nonLinkClickJson.toString());
                                isInstantDeepLinkPossible = true;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } else { // if not check the intent data to see if there is deep link params
                        if (!TextUtils.isEmpty(intent.getStringExtra(Defines.Jsonkey.BranchData.getKey()))) {
                            try {
                                String rawBranchData = intent.getStringExtra(Defines.Jsonkey.BranchData.getKey());
                                // Make sure the data received is complete and in correct format
                                JSONObject branchDataJson = new JSONObject(rawBranchData);
                                branchDataJson.put(Defines.Jsonkey.Clicked_Branch_Link.getKey(), true);
                                prefHelper_.setSessionParams(branchDataJson.toString());
                                isInstantDeepLinkPossible = true;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            // Remove Branch data from the intent once used
                            intent.removeExtra(Defines.Jsonkey.BranchData.getKey());
                            activity.setIntent(intent);
                        }
                    }
                }
            }
        }
        if (intentState_ == INTENT_STATE.READY) {
            // Capture the intent URI and extra for analytics in case started by external intents such as google app search
            try {
                if (data != null && !isIntentParamsAlreadyConsumed(activity)) {
                    String strippedUrl = UniversalResourceAnalyser.getInstance(context_).getStrippedURL(data.toString());
                    sessionReferredLink_ = strippedUrl;
                    prefHelper_.setExternalIntentUri(strippedUrl);
                    
                    if (strippedUrl != null && strippedUrl.equals(data.toString())) {
                        if (activity != null && activity.getIntent() != null && activity.getIntent().getExtras() != null) {
                            Bundle bundle = activity.getIntent().getExtras();
                            Set<String> extraKeys = bundle.keySet();
                            
                            if (extraKeys.size() > 0) {
                                JSONObject extrasJson = new JSONObject();
                                for (String key : EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST) {
                                    if (extraKeys.contains(key)) {
                                        extrasJson.put(key, bundle.get(key));
                                    }
                                }
                                if (extrasJson.length() > 0) {
                                    prefHelper_.setExternalIntentExtra(extrasJson.toString());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            
            //Check for any push identifier in case app is launched by a push notification
            try {
                if (activity != null && activity.getIntent() != null && activity.getIntent().getExtras() != null) {
                    if (!isIntentParamsAlreadyConsumed(activity)) {
                        String pushIdentifier = activity.getIntent().getExtras().getString(Defines.Jsonkey.AndroidPushNotificationKey.getKey()); // This seems producing unmarshalling errors in some corner cases
                        if (pushIdentifier != null && pushIdentifier.length() > 0) {
                            prefHelper_.setPushIdentifier(pushIdentifier);
                            Intent thisIntent = activity.getIntent();
                            thisIntent.putExtra(Defines.Jsonkey.BranchLinkUsed.getKey(), true);
                            activity.setIntent(thisIntent);
                            return false;
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            
            //Check for link click id or app link
            // On Launching app from the recent apps, Android Start the app with the original intent data. So up in opening app from recent list
            // Intent will have App link in data and lead to issue of getting wrong parameters. (In case of link click id since we are  looking for actual link click on back end this case will never happen)
            if (data != null && data.isHierarchical() && activity != null && !isActivityLaunchedFromHistory(activity)) {
                try {
                    if (data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey()) != null) {
                        prefHelper_.setLinkClickIdentifier(data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey()));
                        String paramString = "link_click_id=" + data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey());
                        String uriString = null;
                        if (activity.getIntent() != null) {
                            uriString = activity.getIntent().getDataString();
                        }
                        if (data.getQuery().length() == paramString.length()) {
                            paramString = "\\?" + paramString;
                        } else if (uriString != null && (uriString.length() - paramString.length()) == uriString.indexOf(paramString)) {
                            paramString = "&" + paramString;
                        } else {
                            paramString = paramString + "&";
                        }
                        if (uriString != null) {
                            Uri newData = Uri.parse(uriString.replaceFirst(paramString, ""));
                            activity.getIntent().setData(newData);
                            activity.getIntent().putExtra(Defines.Jsonkey.BranchLinkUsed.getKey(), true);
                        } else {
                            Log.w(TAG, "Branch Warning. URI for the launcher activity is null. Please make sure that intent data is not set to null before calling Branch#InitSession ");
                        }
                        return true;
                    } else {
                        // Check if the clicked url is an app link pointing to this app
                        String scheme = data.getScheme();
                        Intent intent = activity.getIntent();
                        if (scheme != null && intent != null) {
                            if ((scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                                    && data.getHost() != null && data.getHost().length() > 0 && !isIntentParamsAlreadyConsumed(activity)) {
                                String strippedUrl = UniversalResourceAnalyser.getInstance(context_).getStrippedURL(data.toString());
                                if (data.toString().equalsIgnoreCase(strippedUrl)) { // Send app links only if URL is not skipped.
                                    prefHelper_.setAppLink(data.toString());
                                }
                                intent.putExtra(Defines.Jsonkey.BranchLinkUsed.getKey(), true);
                                activity.setIntent(intent);
                                return false;
                            }
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        }
        return false;
    }
    
    private boolean isIntentParamsAlreadyConsumed(Activity activity) {
        return activity != null && activity.getIntent() != null && activity.getIntent().getBooleanExtra(Defines.Jsonkey.BranchLinkUsed.getKey(), false);
    }
    
    private boolean isActivityLaunchedFromHistory(Activity activity) {
        return activity != null && activity.getIntent() != null && (activity.getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
    }
    
    @Override
    public void onGAdsFetchFinished() {
        isGAParamsFetchInProgress_ = false;
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK);
        if (performCookieBasedStrongMatchingOnGAIDAvailable) {
            performCookieBasedStrongMatch();
            performCookieBasedStrongMatchingOnGAIDAvailable = false;
        } else {
            processNextQueueItem();
        }
    }
    
    @Override
    public void onInstallReferrerEventsFinished() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK);
        processNextQueueItem();
    }
    
    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * However the following method provisions application to set SDK to collect only URLs in particular form. This method allow application to specify a set of regular expressions to white list the URL collection.
     * If whitelist is not empty SDK will collect only the URLs that matches the white list.
     * <p>
     * This method should be called immediately after calling {@link Branch#getAutoInstance(Context)}
     *
     * @param urlWhiteListPattern A regular expression with a URI white listing pattern
     * @return {@link Branch} instance for successive method calls
     */
    public Branch addWhiteListedScheme(String urlWhiteListPattern) {
        if (urlWhiteListPattern != null) {
            UniversalResourceAnalyser.getInstance(context_).addToAcceptURLFormats(urlWhiteListPattern);
        }
        return this;
    }
    
    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * However the following method provisions application to set SDK to collect only URLs in particular form. This method allow application to specify a set of regular expressions to white list the URL collection.
     * If whitelist is not empty SDK will collect only the URLs that matches the white list.
     * <p>
     * This method should be called immediately after calling {@link Branch#getAutoInstance(Context)}
     *
     * @param urlWhiteListPatternList {@link List} of regular expressions with URI white listing pattern
     * @return {@link Branch} instance for successive method calls
     */
    public Branch setWhiteListedSchemes(List<String> urlWhiteListPatternList) {
        if (urlWhiteListPatternList != null) {
            UniversalResourceAnalyser.getInstance(context_).addToAcceptURLFormats(urlWhiteListPatternList);
        }
        return this;
    }
    
    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * This method allows applications specify SDK to skip any additional URL patterns to be skipped
     * <p>
     * This method should be called immediately after calling {@link Branch#getAutoInstance(Context)}
     *
     * @param urlSkipPattern {@link String} A URL pattern that Branch SDK should skip from collecting data
     * @return {@link Branch} instance for successive method calls
     */
    public Branch addUriHostsToSkip(String urlSkipPattern) {
        if (!TextUtils.isEmpty(urlSkipPattern))
            UniversalResourceAnalyser.getInstance(context_).addToSkipURLFormats(urlSkipPattern);
        return this;
    }
    
    /**
     * Check and update the URL / URI Skip list in case an update is available.
     */
    void updateSkipURLFormats() {
        UniversalResourceAnalyser.getInstance(context_).checkAndUpdateSkipURLFormats(context_);
    }
    
    /**
     * <p>Identifies the current user to the Branch API by supplying a unique identifier as a
     * {@link String} value. No callback.</p>
     *
     * @param userId A {@link String} value containing the unique identifier of the user.
     */
    public void setIdentity(@NonNull String userId) {
        setIdentity(userId, null);
    }
    
    /**
     * <p>Identifies the current user to the Branch API by supplying a unique identifier as a
     * {@link String} value, with a callback specified to perform a defined action upon successful
     * response to request.</p>
     *
     * @param userId   A {@link String} value containing the unique identifier of the user.
     * @param callback A {@link BranchReferralInitListener} callback instance that will return
     *                 the data associated with the user id being assigned, if available.
     */
    public void setIdentity(@NonNull String userId, @Nullable BranchReferralInitListener
            callback) {
        ServerRequest req = new ServerRequestIdentifyUserRequest(context_, callback, userId);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        } else {
            if (((ServerRequestIdentifyUserRequest) req).isExistingID()) {
                ((ServerRequestIdentifyUserRequest) req).handleUserExist(branchReferral_);
            }
        }
    }
    
    /**
     * Indicates whether or not this user has a custom identity specified for them. Note that this is independent of installs.
     * If you call setIdentity, this device will have that identity associated with this user until logout is called.
     * This includes persisting through uninstalls, as we track device id.
     *
     * @return A {@link Boolean} value that will return <i>true</i> only if user already has an identity.
     */
    public boolean isUserIdentified() {
        return !prefHelper_.getIdentity().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    /**
     * <p>This method should be called if you know that a different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     */
    public void logout() {
        logout(null);
    }
    
    /**
     * <p>This method should be called if you know that a different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     *
     * @param callback An instance of {@link io.branch.referral.Branch.LogoutStatusListener} to callback with the logout operation status.
     */
    public void logout(LogoutStatusListener callback) {
        ServerRequest req = new ServerRequestLogout(context_, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>Fire-and-forget retrieval of rewards for the current session. Without a callback.</p>
     */
    public void loadRewards() {
        loadRewards(null);
    }
    
    /**
     * <p>Retrieves rewards for the current session, with a callback to perform a predefined
     * action following successful report of state change. You'll then need to call getCredits
     * in the callback to update the credit totals in your UX.</p>
     *
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a referral state change.
     */
    public void loadRewards(BranchReferralStateChangedListener callback) {
        ServerRequest req = new ServerRequestGetRewards(context_, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>Retrieve the number of credits available for the "default" bucket.</p>
     *
     * @return An {@link Integer} value of the number credits available in the "default" bucket.
     */
    public int getCredits() {
        return prefHelper_.getCreditCount();
    }
    
    /**
     * Returns an {@link Integer} of the number of credits available for use within the supplied
     * bucket name.
     *
     * @param bucket A {@link String} value indicating the name of the bucket to get credits for.
     * @return An {@link Integer} value of the number credits available in the specified
     * bucket.
     */
    public int getCreditsForBucket(String bucket) {
        return prefHelper_.getCreditCount(bucket);
    }
    
    
    /**
     * <p>Redeems the specified number of credits from the "default" bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param count A {@link Integer} specifying the number of credits to attempt to redeem from
     *              the bucket.
     */
    public void redeemRewards(int count) {
        redeemRewards(Defines.Jsonkey.DefaultBucket.getKey(), count, null);
    }
    
    /**
     * <p>Redeems the specified number of credits from the "default" bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param count    A {@link Integer} specifying the number of credits to attempt to redeem from
     *                 the bucket.
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a executing redeem rewards.
     */
    public void redeemRewards(int count, BranchReferralStateChangedListener callback) {
        redeemRewards(Defines.Jsonkey.DefaultBucket.getKey(), count, callback);
    }
    
    /**
     * <p>Redeems the specified number of credits from the named bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param bucket A {@link String} value containing the name of the referral bucket to attempt
     *               to redeem credits from.
     * @param count  A {@link Integer} specifying the number of credits to attempt to redeem from
     *               the specified bucket.
     */
    public void redeemRewards(@NonNull final String bucket, final int count) {
        redeemRewards(bucket, count, null);
    }
    
    
    /**
     * <p>Redeems the specified number of credits from the named bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param bucket   A {@link String} value containing the name of the referral bucket to attempt
     *                 to redeem credits from.
     * @param count    A {@link Integer} specifying the number of credits to attempt to redeem from
     *                 the specified bucket.
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a executing redeem rewards.
     */
    public void redeemRewards(@NonNull final String bucket,
                              final int count, BranchReferralStateChangedListener callback) {
        ServerRequestRedeemRewards req = new ServerRequestRedeemRewards(context_, bucket, count, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(BranchListResponseListener callback) {
        getCreditHistory(null, null, 100, CreditHistoryOrder.kMostRecentFirst, callback);
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param bucket   A {@link String} value containing the name of the referral bucket that the
     *                 code will belong to.
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(@NonNull final String bucket, BranchListResponseListener
            callback) {
        getCreditHistory(bucket, null, 100, CreditHistoryOrder.kMostRecentFirst, callback);
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param afterId  A {@link String} value containing the ID of the history record to begin after.
     *                 This allows for a partial history to be retrieved, rather than the entire
     *                 credit history of the bucket.
     * @param length   A {@link Integer} value containing the number of credit history records to
     *                 return.
     * @param order    A {@link CreditHistoryOrder} object indicating which order the results should
     *                 be returned in.
     *                 <p>Valid choices:</p>
     *                 <ul>
     *                 <li>{@link CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(@NonNull final String afterId, final int length,
                                 @NonNull final CreditHistoryOrder order, BranchListResponseListener callback) {
        getCreditHistory(null, afterId, length, order, callback);
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param bucket   A {@link String} value containing the name of the referral bucket that the
     *                 code will belong to.
     * @param afterId  A {@link String} value containing the ID of the history record to begin after.
     *                 This allows for a partial history to be retrieved, rather than the entire
     *                 credit history of the bucket.
     * @param length   A {@link Integer} value containing the number of credit history records to
     *                 return.
     * @param order    A {@link CreditHistoryOrder} object indicating which order the results should
     *                 be returned in.
     *                 <p>Valid choices:</p>
     *                 <ul>
     *                 <li>{@link CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(final String bucket, final String afterId, final int length,
                                 @NonNull final CreditHistoryOrder order, BranchListResponseListener callback) {
        ServerRequest req = new ServerRequestGetRewardHistory(context_, bucket, afterId, length, order, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API, with additional app-defined meta data to go along with that action.</p>
     *
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param metadata A {@link JSONObject} containing app-defined meta-data to be attached to a
     *                 user action that has just been completed.
     */
    public void userCompletedAction(@NonNull final String action, JSONObject metadata) {
        userCompletedAction(action, metadata, null);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.</p>
     *
     * @param action A {@link String} value to be passed as an action that the user has carried
     *               out. For example "registered" or "logged in".
     */
    public void userCompletedAction(final String action) {
        userCompletedAction(action, null, null);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.</p>
     *
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param callback instance of {@link BranchViewHandler.IBranchViewEvents} to listen Branch view events
     */
    public void userCompletedAction(final String action, BranchViewHandler.
            IBranchViewEvents callback) {
        userCompletedAction(action, null, callback);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API, with additional app-defined meta data to go along with that action.</p>
     *
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param metadata A {@link JSONObject} containing app-defined meta-data to be attached to a
     *                 user action that has just been completed.
     * @param callback instance of {@link BranchViewHandler.IBranchViewEvents} to listen Branch view events
     */
    public void userCompletedAction(@NonNull final String action, JSONObject
            metadata, BranchViewHandler.IBranchViewEvents callback) {
        ServerRequest req = new ServerRequestActionCompleted(context_, action, metadata, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    public void sendCommerceEvent(@NonNull CommerceEvent commerceEvent, JSONObject
            metadata, BranchViewHandler.IBranchViewEvents callback) {
        ServerRequest req = new ServerRequestRActionCompleted(context_, commerceEvent, metadata, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    public void sendCommerceEvent(@NonNull CommerceEvent commerceEvent) {
        sendCommerceEvent(commerceEvent, null, null);
    }
    
    /**
     * <p>Returns the parameters associated with the link that referred the user. This is only set once,
     * the first time the user is referred by a link. Think of this as the user referral parameters.
     * It is also only set if isReferrable is equal to true, which by default is only true
     * on a fresh install (not upgrade or reinstall). This will change on setIdentity (if the
     * user already exists from a previous device) and logout.</p>
     *
     * @return A {@link JSONObject} containing the install-time parameters as configured
     * locally.
     */
    public JSONObject getFirstReferringParams() {
        String storedParam = prefHelper_.getInstallParams();
        JSONObject firstReferringParams = convertParamsStringToDictionary(storedParam);
        firstReferringParams = appendDebugParams(firstReferringParams);
        return firstReferringParams;
    }
    
    /**
     * <p>This function must be called from a non-UI thread! If Branch has no install link data,
     * and this func is called, it will return data upon initializing, or until LATCH_WAIT_UNTIL.
     * Returns the parameters associated with the link that referred the user. This is only set once,
     * the first time the user is referred by a link. Think of this as the user referral parameters.
     * It is also only set if isReferrable is equal to true, which by default is only true
     * on a fresh install (not upgrade or reinstall). This will change on setIdentity (if the
     * user already exists from a previous device) and logout.</p>
     *
     * @return A {@link JSONObject} containing the install-time parameters as configured
     * locally.
     */
    public JSONObject getFirstReferringParamsSync() {
        getFirstReferringParamsLatch = new CountDownLatch(1);
        if (prefHelper_.getInstallParams().equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getFirstReferringParamsLatch.await(LATCH_WAIT_UNTIL, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
        }
        String storedParam = prefHelper_.getInstallParams();
        JSONObject firstReferringParams = convertParamsStringToDictionary(storedParam);
        firstReferringParams = appendDebugParams(firstReferringParams);
        getFirstReferringParamsLatch = null;
        return firstReferringParams;
    }
    
    /**
     * <p>Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the parameters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.</p>
     *
     * @return A {@link JSONObject} containing the latest referring parameters as
     * configured locally.
     */
    public JSONObject getLatestReferringParams() {
        String storedParam = prefHelper_.getSessionParams();
        JSONObject latestParams = convertParamsStringToDictionary(storedParam);
        latestParams = appendDebugParams(latestParams);
        return latestParams;
    }
    
    /**
     * <p>This function must be called from a non-UI thread! If Branch has not been initialized
     * and this func is called, it will return data upon initialization, or until LATCH_WAIT_UNTIL.
     * Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the parameters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.</p>
     *
     * @return A {@link JSONObject} containing the latest referring parameters as
     * configured locally.
     */
    public JSONObject getLatestReferringParamsSync() {
        getLatestReferringParamsLatch = new CountDownLatch(1);
        try {
            if (initState_ != SESSION_STATE.INITIALISED) {
                getLatestReferringParamsLatch.await(LATCH_WAIT_UNTIL, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
        }
        String storedParam = prefHelper_.getSessionParams();
        JSONObject latestParams = convertParamsStringToDictionary(storedParam);
        latestParams = appendDebugParams(latestParams);
        getLatestReferringParamsLatch = null;
        return latestParams;
    }
    
    /**
     * Append the deep link debug params to the original params
     *
     * @param originalParams A {@link JSONObject} original referrer parameters
     * @return A new {@link JSONObject} with debug params appended.
     */
    private JSONObject appendDebugParams(JSONObject originalParams) {
        try {
            if (originalParams != null && deeplinkDebugParams_ != null) {
                if (deeplinkDebugParams_.length() > 0) {
                    Log.w(TAG, "You're currently in deep link debug mode. Please comment out 'setDeepLinkDebugMode' to receive the deep link parameters from a real Branch link");
                }
                Iterator<String> keys = deeplinkDebugParams_.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    originalParams.put(key, deeplinkDebugParams_.get(key));
                }
            }
        } catch (Exception ignore) {
        }
        return originalParams;
    }
    
    public JSONObject getDeeplinkDebugParams() {
        if (deeplinkDebugParams_ != null && deeplinkDebugParams_.length() > 0) {
            Log.w(TAG, "You're currently in deep link debug mode. Please comment out 'setDeepLinkDebugMode' to receive the deep link parameters from a real Branch link");
        }
        return deeplinkDebugParams_;
    }
    
    
    //-----------------Generate Short URL      -------------------------------------------//
    
    /**
     * <p> Generates a shorl url for the given {@link ServerRequestCreateUrl} object </p>
     *
     * @param req An instance  of {@link ServerRequestCreateUrl} with parameters create the short link.
     * @return A url created with the given request if the request is synchronous else null.
     * Note : This method can be used only internally. Use {@link BranchUrlBuilder} for creating short urls.
     */
    String generateShortLinkInternal(ServerRequestCreateUrl req) {
        if (!req.constructError_ && !req.handleErrors(context_)) {
            if (linkCache_.containsKey(req.getLinkPost())) {
                String url = linkCache_.get(req.getLinkPost());
                req.onUrlAvailable(url);
                return url;
            } else {
                if (req.isAsync()) {
                    generateShortLinkAsync(req);
                } else {
                    return generateShortLinkSync(req);
                }
            }
        }
        return null;
    }



    /**
     * <p>Creates options for sharing a link with other Applications. Creates a link with given attributes and shares with the
     * user selected clients.</p>
     *
     * @param builder A {@link io.branch.referral.Branch.ShareLinkBuilder} instance to build share link.
     */
    private void shareLink(ShareLinkBuilder builder) {
        //Cancel any existing sharing in progress.
        if (shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog(true);
        }
        shareLinkManager_ = new ShareLinkManager();
        shareLinkManager_.shareLink(builder);
    }
    
    /**
     * <p>Cancel current share link operation and Application selector dialog. If your app is not using auto session management, make sure you are
     * calling this method before your activity finishes inorder to prevent any window leak. </p>
     *
     * @param animateClose A {@link Boolean} to specify whether to close the dialog with an animation.
     *                     A value of true will close the dialog with an animation. Setting this value
     *                     to false will close the Dialog immediately.
     */
    public void cancelShareLinkDialog(boolean animateClose) {
        if (shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog(animateClose);
        }
    }
    
    // PRIVATE FUNCTIONS
    
    private String convertDate(Date date) {
        return android.text.format.DateFormat.format("yyyy-MM-dd", date).toString();
    }
    
    private String generateShortLinkSync(ServerRequestCreateUrl req) {
        if (trackingController.isTrackingDisabled()) {
            return req.getLongUrl();
        }
        if (initState_ == SESSION_STATE.INITIALISED) {
            ServerResponse response = null;
            try {
                int timeOut = prefHelper_.getTimeout() + 2000; // Time out is set to slightly more than link creation time to prevent any edge case
                response = new getShortLinkTask().execute(req).get(timeOut, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignore) {
            }
            String url = null;
            if (req.isDefaultToLongUrl()) {
                url = req.getLongUrl();
            }
            if (response != null && response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    url = response.getObject().getString("url");
                    if (req.getLinkPost() != null) {
                        linkCache_.put(req.getLinkPost(), url);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return url;
        } else {
            Log.i("BranchSDK", "Branch Warning: User session has not been initialized");
        }
        return null;
    }
    
    private void generateShortLinkAsync(final ServerRequest req) {
        handleNewRequest(req);
    }
    
    private JSONObject convertParamsStringToDictionary(String paramString) {
        if (paramString.equals(PrefHelper.NO_STRING_VALUE)) {
            return new JSONObject();
        } else {
            try {
                return new JSONObject(paramString);
            } catch (JSONException e) {
                byte[] encodedArray = Base64.decode(paramString.getBytes(), Base64.NO_WRAP);
                try {
                    return new JSONObject(new String(encodedArray));
                } catch (JSONException ex) {
                    ex.printStackTrace();
                    return new JSONObject();
                }
            }
        }
    }
    
    private void processNextQueueItem() {
        try {
            serverSema_.acquire();
            if (networkCount_ == 0 && requestQueue_.getSize() > 0) {
                networkCount_ = 1;
                ServerRequest req = requestQueue_.peek();
                
                serverSema_.release();
                if (req != null) {
                    if (!req.isWaitingOnProcessToFinish()) {
                        // All request except Install request need a valid IdentityID
                        if (!(req instanceof ServerRequestRegisterInstall) && !hasUser()) {
                            Log.i("BranchSDK", "Branch Error: User session has not been initialized!");
                            networkCount_ = 0;
                            handleFailure(requestQueue_.getSize() - 1, BranchError.ERR_NO_SESSION);
                        }
                        //All request except open and install need a session to execute
                        else if (!(req instanceof ServerRequestInitSession) && (!hasSession() || !hasDeviceFingerPrint())) {
                            networkCount_ = 0;
                            handleFailure(requestQueue_.getSize() - 1, BranchError.ERR_NO_SESSION);
                        } else {
                            BranchPostTask postTask = new BranchPostTask(req);
                            postTask.executeTask();
                        }
                    } else {
                        networkCount_ = 0;
                    }
                } else {
                    requestQueue_.remove(null); //In case there is any request nullified remove it.
                }
            } else {
                serverSema_.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleFailure(int index, int statusCode) {
        ServerRequest req;
        if (index >= requestQueue_.getSize()) {
            req = requestQueue_.peekAt(requestQueue_.getSize() - 1);
        } else {
            req = requestQueue_.peekAt(index);
        }
        handleFailure(req, statusCode);
    }
    
    private void handleFailure(final ServerRequest req, int statusCode) {
        if (req == null)
            return;
        req.handleFailure(statusCode, "");
    }
    
    private void updateAllRequestsInQueue() {
        try {
            for (int i = 0; i < requestQueue_.getSize(); i++) {
                ServerRequest req = requestQueue_.peekAt(i);
                if (req != null) {
                    JSONObject reqJson = req.getPost();
                    if (reqJson != null) {
                        if (reqJson.has(Defines.Jsonkey.SessionID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                        }
                        if (reqJson.has(Defines.Jsonkey.IdentityID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
                        }
                        if (reqJson.has(Defines.Jsonkey.DeviceFingerprintID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private boolean hasSession() {
        return !prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private boolean hasDeviceFingerPrint() {
        return !prefHelper_.getDeviceFingerPrintID().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private boolean hasUser() {
        return !prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private void insertRequestAtFront(ServerRequest req) {
        if (networkCount_ == 0) {
            requestQueue_.insert(req, 0);
        } else {
            requestQueue_.insert(req, 1);
        }
    }
    
    private void registerInstallOrOpen(ServerRequest req, BranchReferralInitListener callback) {
        // If there isn't already an Open / Install request, add one to the queue
        if (!requestQueue_.containsInstallOrOpen()) {
            insertRequestAtFront(req);
        }
        // If there is already one in the queue, make sure it's in the front.
        // Make sure a callback is associated with this request. This callback can
        // be cleared if the app is terminated while an Open/Install is pending.
        else {
            // Update the callback to the latest one in init session call
            if (callback != null) {
                requestQueue_.setInstallOrOpenCallback(callback);
            }
            requestQueue_.moveInstallOrOpenToFront(req, networkCount_, callback);
        }
        
        processNextQueueItem();
    }
    
    private void initializeSession(final BranchReferralInitListener callback) {
        if ((prefHelper_.getBranchKey() == null || prefHelper_.getBranchKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE))) {
            initState_ = SESSION_STATE.UNINITIALISED;
            //Report Key error on callback
            if (callback != null) {
                callback.onInitFinished(null, new BranchError("Trouble initializing Branch.", BranchError.ERR_BRANCH_KEY_INVALID));
            }
            Log.i("BranchSDK", "Branch Warning: Please enter your branch_key in your project's res/values/strings.xml!");
            return;
        } else if (prefHelper_.getBranchKey() != null && prefHelper_.getBranchKey().startsWith("key_test_")) {
            Log.i("BranchSDK", "Branch Warning: You are using your test app's Branch Key. Remember to change it to live Branch Key during deployment.");
        }
        
        if (!prefHelper_.getExternalIntentUri().equals(PrefHelper.NO_STRING_VALUE) || !enableFacebookAppLinkCheck_) {
            registerAppInit(callback, null);
        } else {
            // Check if opened by facebook with deferred install data
            boolean appLinkRqSucceeded;
            appLinkRqSucceeded = DeferredAppLinkDataHandler.fetchDeferredAppLinkData(context_, new DeferredAppLinkDataHandler.AppLinkFetchEvents() {
                @Override
                public void onAppLinkFetchFinished(String nativeAppLinkUrl) {
                    prefHelper_.setIsAppLinkTriggeredInit(true); // callback returns when app link fetch finishes with success or failure. Report app link checked in both cases
                    if (nativeAppLinkUrl != null) {
                        Uri appLinkUri = Uri.parse(nativeAppLinkUrl);
                        String bncLinkClickId = appLinkUri.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey());
                        if (!TextUtils.isEmpty(bncLinkClickId)) {
                            prefHelper_.setLinkClickIdentifier(bncLinkClickId);
                        }
                    }
                    requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.FB_APP_LINK_WAIT_LOCK);
                    processNextQueueItem();
                }
            });
            if (appLinkRqSucceeded) {
                registerAppInit(callback, ServerRequest.PROCESS_WAIT_LOCK.FB_APP_LINK_WAIT_LOCK);
            } else {
                registerAppInit(callback, null);
            }
        }
    }
    
    /**
     * Registers app init with params filtered from the intent. This will wait on the wait locks to complete any pending operations
     */
    private void registerAppInit(BranchReferralInitListener
                                         callback, ServerRequest.PROCESS_WAIT_LOCK lock) {
        ServerRequest request = getInstallOrOpenRequest(callback);
        request.addProcessWaitLock(lock);
        if (isGAParamsFetchInProgress_) {
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK);
        }
        if (intentState_ != INTENT_STATE.READY) {
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK);
        }
        if (checkInstallReferrer_ && request instanceof ServerRequestRegisterInstall && !InstallListener.unReportedReferrerAvailable) {
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK);
            InstallListener.captureInstallReferrer(context_, playStoreReferrerFetchTime, this);
        }
        
        registerInstallOrOpen(request, callback);
    }

    /*
     * Register app init without any wait on intents or other referring params. This will not be getting any params from the intent
     */
    void registerAppReInit() {
        // on re-init make sure GAID is available
        if (!trackingController.isTrackingDisabled()) { // Do not get GAID when tracking is disabled
            isGAParamsFetchInProgress_ = systemObserver_.prefetchGAdsParams(this);
        }
        if (networkCount_ != 0) {
            networkCount_ = 0;
            requestQueue_.clear();
        }
        ServerRequest initRequest = getInstallOrOpenRequest(null);
        if(isGAParamsFetchInProgress_) {
            initRequest.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK);
        }
        registerInstallOrOpen(initRequest, null);
    }
    
    private ServerRequest getInstallOrOpenRequest(BranchReferralInitListener callback) {
        ServerRequest request;
        if (hasUser()) {
            // If there is user this is open
            request = new ServerRequestRegisterOpen(context_, callback, systemObserver_);
        } else {
            // If no user this is an Install
            request = new ServerRequestRegisterInstall(context_, callback, systemObserver_, InstallListener.getInstallationID());
        }
        return request;
    }
    
    private void onIntentReady(Activity activity, boolean grabIntentParams) {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK);
        //if (activity.getIntent() != null) {
        if (grabIntentParams) {
            Uri intentData = activity.getIntent().getData();
            readAndStripParam(intentData, activity);
            // Check for cookie based matching only if Tracking is enabled
            if (!isTrackingDisabled() && cookieBasedMatchDomain_ != null && prefHelper_.getBranchKey() != null && !prefHelper_.getBranchKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE)) {
                if (isGAParamsFetchInProgress_) {
                    // Wait for GAID to Available
                    performCookieBasedStrongMatchingOnGAIDAvailable = true;
                } else {
                    performCookieBasedStrongMatch();
                }
            } else {
                processNextQueueItem();
            }
        } else {
            processNextQueueItem();
        }
    }
    
    private void performCookieBasedStrongMatch() {
        if (!trackingController.isTrackingDisabled()) {
            DeviceInfo deviceInfo = DeviceInfo.getInstance(prefHelper_.getExternDebug(), systemObserver_, disableDeviceIDFetch_);
            Activity currentActivity = null;
            if (currentActivityReference_ != null) {
                currentActivity = currentActivityReference_.get();
            }
            Context context = (currentActivity != null) ? currentActivity.getApplicationContext() : null;
            if (context != null) {
                requestQueue_.setStrongMatchWaitLock();
                BranchStrongMatchHelper.getInstance().checkForStrongMatch(context, cookieBasedMatchDomain_, deviceInfo, prefHelper_, systemObserver_, new BranchStrongMatchHelper.StrongMatchCheckEvents() {
                    @Override
                    public void onStrongMatchCheckFinished() {
                        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.STRONG_MATCH_PENDING_WAIT_LOCK);
                        processNextQueueItem();
                    }
                });
            }
        }
    }
    
    /**
     * Handles execution of a new request other than open or install.
     * Checks for the session initialisation and adds a install/Open request in front of this request
     * if the request need session to execute.
     *
     * @param req The {@link ServerRequest} to execute
     */
    public void handleNewRequest(ServerRequest req) {
        // If Tracking is disabled fail all messages with ERR_BRANCH_TRACKING_DISABLED
        if (trackingController.isTrackingDisabled()) {
            req.reportTrackingDisabledError();
            return;
        }
        //If not initialised put an open or install request in front of this request(only if this needs session)
        if (initState_ != SESSION_STATE.INITIALISED && !(req instanceof ServerRequestInitSession)) {
            
            if ((req instanceof ServerRequestLogout)) {
                req.handleFailure(BranchError.ERR_NO_SESSION, "");
                Log.i(TAG, "Branch is not initialized, cannot logout");
                return;
            }
            if ((req instanceof ServerRequestRegisterClose)) {
                Log.i(TAG, "Branch is not initialized, cannot close session");
                return;
            } else {
                Activity currentActivity = null;
                if (currentActivityReference_ != null) {
                    currentActivity = currentActivityReference_.get();
                }
                if (customReferrableSettings_ == CUSTOM_REFERRABLE_SETTINGS.USE_DEFAULT) {
                    initUserSessionInternal((BranchReferralInitListener) null, currentActivity, true);
                } else {
                    boolean isReferrable = customReferrableSettings_ == CUSTOM_REFERRABLE_SETTINGS.REFERRABLE;
                    initUserSessionInternal((BranchReferralInitListener) null, currentActivity, isReferrable);
                }
            }
        }

        if (!(req instanceof ServerRequestPing)) {
            requestQueue_.enqueue(req);
            req.onRequestQueued();
        }
        processNextQueueItem();
    }

    /**
     * Notify Branch when network is available in order to process the next request in the queue.
     */
    public void notifyNetworkAvailable() {
        ServerRequest req = new ServerRequestPing(context_);
        handleNewRequest(req);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setActivityLifeCycleObserver(Application application) {
        try {
            BranchActivityLifeCycleObserver activityLifeCycleObserver = new BranchActivityLifeCycleObserver();
            /* Set an observer for activity life cycle events. */
            application.unregisterActivityLifecycleCallbacks(activityLifeCycleObserver);
            application.registerActivityLifecycleCallbacks(activityLifeCycleObserver);
            isActivityLifeCycleCallbackRegistered_ = true;
            
        } catch (NoSuchMethodError | NoClassDefFoundError Ex) {
            isActivityLifeCycleCallbackRegistered_ = false;
            isAutoSessionMode_ = false;
            /* LifeCycleEvents are  available only from API level 14. */
            Log.w(TAG, new BranchError("", BranchError.ERR_API_LVL_14_NEEDED).getMessage());
        }
    }
    
    /**
     * <p>Class that observes activity life cycle events and determines when to start and stop
     * session.</p>
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private class BranchActivityLifeCycleObserver implements Application.ActivityLifecycleCallbacks {
        private int activityCnt_ = 0; //Keep the count of live  activities.
        
        
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            intentState_ = handleDelayedNewIntents_ ? INTENT_STATE.PENDING : INTENT_STATE.READY;
            isActivityCreatedAndLaunched = true;
            if (BranchViewHandler.getInstance().isInstallOrOpenBranchViewPending(activity.getApplicationContext())) {
                BranchViewHandler.getInstance().showPendingBranchView(activity);
            }
        }
        
        @Override
        public void onActivityStarted(Activity activity) {
            intentState_ = handleDelayedNewIntents_ ? INTENT_STATE.PENDING : INTENT_STATE.READY;
            // If configured on dashboard, trigger content discovery runnable
            if (initState_ == SESSION_STATE.INITIALISED) {
                try {
                    ContentDiscoverer.getInstance().discoverContent(activity, sessionReferredLink_);
                } catch (Exception ignore) {
                }
            }
            if (activityCnt_ < 1) { // Check if this is the first Activity.If so start a session.
                if (initState_ == SESSION_STATE.INITIALISED) {
                    // Handling case :  init session completed previously when app was in background.
                    initState_ = SESSION_STATE.UNINITIALISED;
                }
                // Check if debug mode is set in manifest. If so enable debug.
                if (BranchUtil.isTestModeEnabled(context_)) {
                    prefHelper_.setExternDebug();
                }
                startSession(activity);
            } else if (checkIntentForSessionRestart(activity.getIntent())) { // Case of opening the app by clicking a push notification while app is in foreground
                initState_ = SESSION_STATE.UNINITIALISED;
                // no need call close here since it is session forced restart. Don't want to wait till close finish
                startSession(activity);
            }
            activityCnt_++;
            isActivityCreatedAndLaunched = false;
        }
        
        @Override
        public void onActivityResumed(Activity activity) {
            // Need to check here again for session restart request in case the intent is created while the activity is already running
            if (checkIntentForSessionRestart(activity.getIntent())) {
                initState_ = SESSION_STATE.UNINITIALISED;
                startSession(activity);
            }
            currentActivityReference_ = new WeakReference<>(activity);
            if (handleDelayedNewIntents_) {
                intentState_ = INTENT_STATE.READY;
                // Grab the intent only for first activity unless this activity is intent to  force new session
                boolean grabIntentParams = activity.getIntent() != null && initState_ != SESSION_STATE.INITIALISED;
                onIntentReady(activity, grabIntentParams);
            }
        }
        
        @Override
        public void onActivityPaused(Activity activity) {
            /* Close any opened sharing dialog.*/
            if (shareLinkManager_ != null) {
                shareLinkManager_.cancelShareLinkDialog(true);
            }
        }
        
        @Override
        public void onActivityStopped(Activity activity) {
            ContentDiscoverer.getInstance().onActivityStopped(activity);
            activityCnt_--; // Check if this is the last activity. If so, stop the session.
            if (activityCnt_ < 1) {
                isInstantDeepLinkPossible = false;
                closeSessionInternal();
            }
        }
        
        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }
        
        @Override
        public void onActivityDestroyed(Activity activity) {
            if (currentActivityReference_ != null && currentActivityReference_.get() == activity) {
                currentActivityReference_.clear();
            }
            BranchViewHandler.getInstance().onCurrentActivityDestroyed(activity);
        }
        
    }
    
    private void startSession(Activity activity) {
        Uri intentData = null;
        if (activity.getIntent() != null) {
            intentData = activity.getIntent().getData();
        }
        isInitReportedThroughCallBack = false;
        initSessionWithData(intentData, activity); // indicate  starting of session.
    }
    
    /*
     * Check for forced session restart. The Branch session is restarted if the incoming intent has branch_force_new_session set to true.
     * This is for supporting opening a deep link path while app is already running in the foreground. Such as clicking push notification while app in foreground.
     *
     * We are catching BadParcelableException because of the issue reported here: https://github.com/BranchMetrics/android-branch-deep-linking/issues/464
     * Which is also reported here, affecting Chrome: https://bugs.chromium.org/p/chromium/issues/detail?id=412527
     * Explanation: In some cases the parcel inside the intent we're parsing from Chrome can be malformed, so we need some protection!
     * The commit which resolved the issue in Chrome lives here: https://chromium.googlesource.com/chromium/src/+/4bca3b37801c502a164536b804879c00aba7d304
     * We decided for now to protect this one line with a try/catch.
     */
    private boolean checkIntentForSessionRestart(Intent intent) {
        boolean isRestartSessionRequested = false;
        if (intent != null) {
            try {
                isRestartSessionRequested = intent.getBooleanExtra(Defines.Jsonkey.ForceNewBranchSession.getKey(), false);
            } catch (Throwable ignore) {
            }
            if (isRestartSessionRequested) {
                intent.putExtra(Defines.Jsonkey.ForceNewBranchSession.getKey(), false);
            }
        }
        return isRestartSessionRequested;
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchReferralInitListener}, defining a single method that takes a list of params in
     * {@link JSONObject} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see JSONObject
     * @see BranchError
     */
    public interface BranchReferralInitListener {
        void onInitFinished(JSONObject referringParams, BranchError error);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchUniversalReferralInitListener}, defining a single method that provides
     * {@link BranchUniversalObject}, {@link LinkProperties} and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.
     * In case of an error the value for {@link BranchUniversalObject} and {@link LinkProperties} are set to null.</p>
     *
     * @see BranchUniversalObject
     * @see LinkProperties
     * @see BranchError
     */
    public interface BranchUniversalReferralInitListener {
        void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error);
    }
    
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchReferralStateChangedListener}, defining a single method that takes a value of
     * {@link Boolean} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see Boolean
     * @see BranchError
     */
    public interface BranchReferralStateChangedListener {
        void onStateChanged(boolean changed, BranchError error);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchLinkCreateListener}, defining a single method that takes a URL
     * {@link String} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see String
     * @see BranchError
     */
    public interface BranchLinkCreateListener {
        void onLinkCreate(String url, BranchError error);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchLinkShareListener}, defining methods to listen for link sharing status.</p>
     */
    public interface BranchLinkShareListener {
        /**
         * <p> Callback method to update when share link dialog is launched.</p>
         */
        void onShareLinkDialogLaunched();
        
        /**
         * <p> Callback method to update when sharing dialog is dismissed.</p>
         */
        void onShareLinkDialogDismissed();
        
        /**
         * <p> Callback method to update the sharing status. Called on sharing completed or on error.</p>
         *
         * @param sharedLink    The link shared to the channel.
         * @param sharedChannel Channel selected for sharing.
         * @param error         A {@link BranchError} to update errors, if there is any.
         */
        void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error);
        
        /**
         * <p>Called when user select a channel for sharing a deep link.
         * Branch will create a deep link for the selected channel and share with it after calling this
         * method. On sharing complete, status is updated by onLinkShareResponse() callback. Consider
         * having a sharing in progress UI if you wish to prevent user activity in the window between selecting a channel
         * and sharing complete.</p>
         *
         * @param channelName Name of the selected application to share the link. An empty string is returned if unable to resolve selected client name.
         */
        void onChannelSelected(String channelName);
    }
    
    /**
     * <p>An extended version of {@link BranchLinkShareListener} with callback that supports updating link data or properties after user select a channel to share
     * This will provide the extended callback {@link #onChannelSelected(String, BranchUniversalObject, LinkProperties)} only when sharing a link using Branch Universal Object.</p>
     */
    public interface ExtendedBranchLinkShareListener extends BranchLinkShareListener {
        /**
         * <p>
         * Called when user select a channel for sharing a deep link.
         * This method allows modifying the link data and properties by providing the params  {@link BranchUniversalObject} and {@link LinkProperties}
         * </p>
         *
         * @param channelName    The name of the channel user selected for sharing a link
         * @param buo            {@link BranchUniversalObject} BUO used for sharing link for updating any params
         * @param linkProperties {@link LinkProperties} associated with the sharing link for updating the properties
         * @return Return {@code true} to create link with any updates added to the data ({@link BranchUniversalObject}) or to the properties ({@link LinkProperties}).
         * Return {@code false} otherwise.
         */
        boolean onChannelSelected(String channelName, BranchUniversalObject buo, LinkProperties linkProperties);
    }
    
    /**
     * <p>An interface class for customizing sharing properties with selected channel.</p>
     */
    public interface IChannelProperties {
        /**
         * @param channel The name of the channel selected for sharing.
         * @return {@link String} with value for the message title for sharing the link with the selected channel
         */
        String getSharingTitleForChannel(String channel);
        
        /**
         * @param channel The name of the channel selected for sharing.
         * @return {@link String} with value for the message body for sharing the link with the selected channel
         */
        String getSharingMessageForChannel(String channel);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchListResponseListener}, defining a single method that takes a list of
     * {@link JSONArray} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see JSONArray
     * @see BranchError
     */
    public interface BranchListResponseListener {
        void onReceivingResponse(JSONArray list, BranchError error);
    }
    
    /**
     * <p>
     * Callback interface for listening logout status
     * </p>
     */
    public interface LogoutStatusListener {
        /**
         * Called on finishing the the logout process
         *
         * @param loggedOut A {@link Boolean} which is set to true if logout succeeded
         * @param error     An instance of {@link BranchError} to notify any error occurred during logout.
         *                  A null value is set if logout succeeded.
         */
        void onLogoutFinished(boolean loggedOut, BranchError error);
    }
    
    /**
     * <p>enum containing the sort options for return of credit history.</p>
     */
    public enum CreditHistoryOrder {
        kMostRecentFirst, kLeastRecentFirst
    }
    
    /**
     * Async Task to create  a shorlink for synchronous methods
     */
    private class getShortLinkTask extends AsyncTask<ServerRequest, Void, ServerResponse> {
        @Override
        protected ServerResponse doInBackground(ServerRequest... serverRequests) {
            String urlExtend = "v1/url";
            return branchRemoteInterface_.make_restful_post(serverRequests[0].getPost(), prefHelper_.getAPIBaseUrl() + urlExtend, Defines.RequestPath.GetURL.getPath(), prefHelper_.getBranchKey());
        }
    }

    /**
     * Asynchronous task handling execution of server requests. Execute the network task on background
     * thread and request are  executed in sequential manner. Handles the request execution in
     * Synchronous-Asynchronous pattern. Should be invoked only form main thread and  the results are
     * published in the main thread.
     */
    private class BranchPostTask extends BranchAsyncTask<Void, Void, ServerResponse> {
        ServerRequest thisReq_;
        
        public BranchPostTask(ServerRequest request) {
            thisReq_ = request;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            thisReq_.onPreExecute();
            thisReq_.doFinalUpdateOnMainThread();
        }
        
        @Override
        protected ServerResponse doInBackground(Void... voids) {
            // update queue wait time
            addExtraInstrumentationData(thisReq_.getRequestPath() + "-" + Defines.Jsonkey.Queue_Wait_Time.getKey(), String.valueOf(thisReq_.getQueueWaitTime()));
            thisReq_.doFinalUpdateOnBackgroundThread();
            if (isTrackingDisabled() && thisReq_.prepareExecuteWithoutTracking() == false) {
                return new ServerResponse(thisReq_.getRequestPath(), BranchError.ERR_BRANCH_TRACKING_DISABLED);
            }
            if (thisReq_.isGetRequest()) {
                return branchRemoteInterface_.make_restful_get(thisReq_.getRequestUrl(), thisReq_.getGetParams(), thisReq_.getRequestPath(), prefHelper_.getBranchKey());
            } else {
                return branchRemoteInterface_.make_restful_post(thisReq_.getPostWithInstrumentationValues(instrumentationExtraData_), thisReq_.getRequestUrl(), thisReq_.getRequestPath(), prefHelper_.getBranchKey());
            }
        }
        
        @Override
        protected void onPostExecute(ServerResponse serverResponse) {
            super.onPostExecute(serverResponse);
            if (serverResponse != null) {
                try {
                    int status = serverResponse.getStatusCode();
                    hasNetwork_ = true;
                    
                    if (serverResponse.getStatusCode() == BranchError.ERR_BRANCH_TRACKING_DISABLED) {
                        thisReq_.reportTrackingDisabledError();
                        requestQueue_.remove(thisReq_);
                        
                    } else {
                        //If the request is not succeeded
                        if (status != 200) {
                            //If failed request is an initialisation request then mark session not initialised
                            if (thisReq_ instanceof ServerRequestInitSession) {
                                initState_ = SESSION_STATE.UNINITIALISED;
                            }
                            // On a bad request or in canse of a conflict notify with call back and remove the request.
                            if (status == 400 || status == 409) {
                                requestQueue_.remove(thisReq_);
                                if (thisReq_ instanceof ServerRequestCreateUrl) {
                                    ((ServerRequestCreateUrl) thisReq_).handleDuplicateURLError();
                                } else {
                                    Log.i("BranchSDK", "Branch API Error: Conflicting resource error code from API");
                                    handleFailure(thisReq_, status);
                                }
                            }
                            //On Network error or Branch is down fail all the pending requests in the queue except
                            //for request which need to be replayed on failure.
                            else {
                                hasNetwork_ = false;
                                //Collect all request from the queue which need to be failed.
                                ArrayList<ServerRequest> requestToFail = new ArrayList<>();
                                for (int i = 0; i < requestQueue_.getSize(); i++) {
                                    requestToFail.add(requestQueue_.peekAt(i));
                                }
                                //Remove the requests from the request queue first
                                for (ServerRequest req : requestToFail) {
                                    if (req == null || !req.shouldRetryOnFail()) { // Should remove any nullified request object also from queue
                                        requestQueue_.remove(req);
                                    }
                                }
                                // Then, set the network count to zero, indicating that requests can be started again.
                                networkCount_ = 0;
                                
                                //Finally call the request callback with the error.
                                for (ServerRequest req : requestToFail) {
                                    if (req != null) {
                                        req.handleFailure(status, serverResponse.getFailReason());
                                        //If request need to be replayed, no need for the callbacks
                                        if (req.shouldRetryOnFail())
                                            req.clearCallbacks();
                                    }
                                }
                            }
                        }
                        // If the request succeeded
                        else {
                            hasNetwork_ = true;
                            //On create  new url cache the url.
                            if (thisReq_ instanceof ServerRequestCreateUrl) {
                                if (serverResponse.getObject() != null) {
                                    final String url = serverResponse.getObject().getString("url");
                                    // cache the link
                                    linkCache_.put(((ServerRequestCreateUrl) thisReq_).getLinkPost(), url);
                                }
                            }
                            //On Logout clear the link cache and all pending requests
                            else if (thisReq_ instanceof ServerRequestLogout) {
                                linkCache_.clear();
                                requestQueue_.clear();
                            }
                            requestQueue_.dequeue();
                            
                            // If this request changes a session update the session-id to queued requests.
                            if (thisReq_ instanceof ServerRequestInitSession
                                    || thisReq_ instanceof ServerRequestIdentifyUserRequest) {
                                // Immediately set session and Identity and update the pending request with the params
                                JSONObject respJson = serverResponse.getObject();
                                if (respJson != null) {
                                    boolean updateRequestsInQueue = false;
                                    if (!isTrackingDisabled()) { // Update PII data only if tracking is disabled
                                        if (respJson.has(Defines.Jsonkey.SessionID.getKey())) {
                                            prefHelper_.setSessionID(respJson.getString(Defines.Jsonkey.SessionID.getKey()));
                                            updateRequestsInQueue = true;
                                        }
                                        if (respJson.has(Defines.Jsonkey.IdentityID.getKey())) {
                                            String new_Identity_Id = respJson.getString(Defines.Jsonkey.IdentityID.getKey());
                                            if (!prefHelper_.getIdentityID().equals(new_Identity_Id)) {
                                                //On setting a new identity Id clear the link cache
                                                linkCache_.clear();
                                                prefHelper_.setIdentityID(respJson.getString(Defines.Jsonkey.IdentityID.getKey()));
                                                updateRequestsInQueue = true;
                                            }
                                        }
                                        if (respJson.has(Defines.Jsonkey.DeviceFingerprintID.getKey())) {
                                            prefHelper_.setDeviceFingerPrintID(respJson.getString(Defines.Jsonkey.DeviceFingerprintID.getKey()));
                                            updateRequestsInQueue = true;
                                        }
                                    }
                                    
                                    if (updateRequestsInQueue) {
                                        updateAllRequestsInQueue();
                                    }
                                    
                                    if (thisReq_ instanceof ServerRequestInitSession) {
                                        initState_ = SESSION_STATE.INITIALISED;
                                        thisReq_.onRequestSucceeded(serverResponse, branchReferral_);
                                        if (!isInitReportedThroughCallBack) {
                                            if (!((ServerRequestInitSession) thisReq_).handleBranchViewIfAvailable((serverResponse))) {
                                                checkForAutoDeepLinkConfiguration();
                                            }
                                        }
                                        // Publish success to listeners
                                        if (((ServerRequestInitSession) thisReq_).hasCallBack()) {
                                            isInitReportedThroughCallBack = true;
                                        }
                                        // Count down the latch holding getLatestReferringParamsSync
                                        if (getLatestReferringParamsLatch != null) {
                                            getLatestReferringParamsLatch.countDown();
                                        }
                                        // Count down the latch holding getFirstReferringParamsSync
                                        if (getFirstReferringParamsLatch != null) {
                                            getFirstReferringParamsLatch.countDown();
                                        }
                                    } else {
                                        // For setting identity just call only request succeeded
                                        thisReq_.onRequestSucceeded(serverResponse, branchReferral_);
                                    }
                                }
                            } else {
                                //Publish success to listeners
                                thisReq_.onRequestSucceeded(serverResponse, branchReferral_);
                            }
                        }
                    }
                    networkCount_ = 0;
                    if (hasNetwork_ && initState_ != SESSION_STATE.UNINITIALISED) {
                        processNextQueueItem();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    //-------------------Auto deep link feature-------------------------------------------//
    
    /**
     * <p>Checks if an activity is launched by Branch auto deep link feature. Branch launches activitie configured for auto deep link on seeing matching keys.
     * Keys for auto deep linking should be specified to each activity as a meta data in manifest.</p>
     * Configure your activity in your manifest to enable auto deep linking as follows
     * <!--
     * <activity android:name=".YourActivity">
     * <meta-data android:name="io.branch.sdk.auto_link" android:value="DeepLinkKey1","DeepLinkKey2" />
     * </activity>
     * -->
     *
     * @param activity Instance of activity to check if launched on auto deep link.
     * @return A {Boolean} value whose value is true if this activity is launched by Branch auto deeplink feature.
     */
    public static boolean isAutoDeepLinkLaunch(Activity activity) {
        return (activity.getIntent().getStringExtra(AUTO_DEEP_LINKED) != null);
    }
    
    private void checkForAutoDeepLinkConfiguration() {
        JSONObject latestParams = getLatestReferringParams();
        String deepLinkActivity = null;
        
        try {
            //Check if the application is launched by clicking a Branch link.
            if (!latestParams.has(Defines.Jsonkey.Clicked_Branch_Link.getKey())
                    || !latestParams.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey())) {
                return;
            }
            if (latestParams.length() > 0) {
                // Check if auto deep link is disabled.
                ApplicationInfo appInfo = context_.getPackageManager().getApplicationInfo(context_.getPackageName(), PackageManager.GET_META_DATA);
                if (appInfo.metaData != null && appInfo.metaData.getBoolean(AUTO_DEEP_LINK_DISABLE, false)) {
                    return;
                }
                PackageInfo info = context_.getPackageManager().getPackageInfo(context_.getPackageName(), PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
                ActivityInfo[] activityInfos = info.activities;
                int deepLinkActivityReqCode = DEF_AUTO_DEEP_LINK_REQ_CODE;
                
                if (activityInfos != null) {
                    for (ActivityInfo activityInfo : activityInfos) {
                        if (activityInfo != null && activityInfo.metaData != null && (activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY) != null || activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH) != null)) {
                            if (checkForAutoDeepLinkKeys(latestParams, activityInfo) || checkForAutoDeepLinkPath(latestParams, activityInfo)) {
                                deepLinkActivity = activityInfo.name;
                                deepLinkActivityReqCode = activityInfo.metaData.getInt(AUTO_DEEP_LINK_REQ_CODE, DEF_AUTO_DEEP_LINK_REQ_CODE);
                                break;
                            }
                        }
                    }
                }
                if (deepLinkActivity != null && currentActivityReference_ != null) {
                    Activity currentActivity = currentActivityReference_.get();
                    if (currentActivity != null) {
                        Intent intent = new Intent(currentActivity, Class.forName(deepLinkActivity));
                        intent.putExtra(AUTO_DEEP_LINKED, "true");
                        
                        // Put the raw JSON params as extra in case need to get the deep link params as JSON String
                        intent.putExtra(Defines.Jsonkey.ReferringData.getKey(), latestParams.toString());
                        
                        // Add individual parameters in the data
                        Iterator<?> keys = latestParams.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            intent.putExtra(key, latestParams.getString(key));
                        }
                        currentActivity.startActivityForResult(intent, deepLinkActivityReqCode);
                    } else {
                        // This case should not happen. Adding a safe handling for any corner case
                        Log.w(TAG, "No activity reference to launch deep linked activity");
                    }
                }
            }
        } catch (final PackageManager.NameNotFoundException e) {
            Log.i("BranchSDK", "Branch Warning: Please make sure Activity names set for auto deep link are correct!");
        } catch (ClassNotFoundException e) {
            Log.i("BranchSDK", "Branch Warning: Please make sure Activity names set for auto deep link are correct! Error while looking for activity " + deepLinkActivity);
        } catch (Exception ignore) {
            // Can get TransactionTooLarge Exception here if the Application info exceeds 1mb binder data limit. Usually results with manifest merge from SDKs
        }
    }
    
    private boolean checkForAutoDeepLinkKeys(JSONObject params, ActivityInfo activityInfo) {
        if (activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY) != null) {
            String[] activityLinkKeys = activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY).split(",");
            for (String activityLinkKey : activityLinkKeys) {
                if (params.has(activityLinkKey)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean checkForAutoDeepLinkPath(JSONObject params, ActivityInfo activityInfo) {
        String deepLinkPath = null;
        try {
            if (params.has(Defines.Jsonkey.AndroidDeepLinkPath.getKey())) {
                deepLinkPath = params.getString(Defines.Jsonkey.AndroidDeepLinkPath.getKey());
            } else if (params.has(Defines.Jsonkey.DeepLinkPath.getKey())) {
                deepLinkPath = params.getString(Defines.Jsonkey.DeepLinkPath.getKey());
            }
        } catch (JSONException ignored) {
        }
        if (activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH) != null && deepLinkPath != null) {
            String[] activityLinkPaths = activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH).split(",");
            for (String activityLinkPath : activityLinkPaths) {
                if (pathMatch(activityLinkPath.trim(), deepLinkPath)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean pathMatch(String templatePath, String path) {
        boolean matched = true;
        String[] pathSegmentsTemplate = templatePath.split("\\?")[0].split("/");
        String[] pathSegmentsTarget = path.split("\\?")[0].split("/");
        if (pathSegmentsTemplate.length != pathSegmentsTarget.length) {
            return false;
        }
        for (int i = 0; i < pathSegmentsTemplate.length && i < pathSegmentsTarget.length; i++) {
            String pathSegmentTemplate = pathSegmentsTemplate[i];
            String pathSegmentTarget = pathSegmentsTarget[i];
            if (!pathSegmentTemplate.equals(pathSegmentTarget) && !pathSegmentTemplate.contains("*")) {
                matched = false;
                break;
            }
        }
        return matched;
    }
    
    public static void enableSimulateInstalls() {
        isSimulatingInstalls_ = true;
    }
    
    public static void disableSimulateInstalls() {
        isSimulatingInstalls_ = false;
    }
    
    public static void enableLogging() {
        isLogging_ = true;
    }
    
    public static void disableLogging() {
        isLogging_ = false;
    }
    
    //-------------------------- Branch Builders--------------------------------------//
    
    /**
     * <p> Class for building a share link dialog.This creates a chooser for selecting application for
     * sharing a link created with given parameters. </p>
     */
    public static class ShareLinkBuilder {
        
        private final Activity activity_;
        private final Branch branch_;
        
        private String shareMsg_;
        private String shareSub_;
        private Branch.BranchLinkShareListener callback_ = null;
        private Branch.IChannelProperties channelPropertiesCallback_ = null;
        
        private ArrayList<SharingHelper.SHARE_WITH> preferredOptions_;
        private String defaultURL_;
        
        //Customise more and copy url option
        private Drawable moreOptionIcon_;
        private String moreOptionText_;
        private Drawable copyUrlIcon_;
        private String copyURlText_;
        private String urlCopiedMessage_;
        private int styleResourceID_;
        private boolean setFullWidthStyle_;
        private int dialogThemeResourceID_;
        private int dividerHeight = -1;
        private String sharingTitle = null;
        private View sharingTitleView = null;
        private int iconSize_ = 50;
        
        BranchShortLinkBuilder shortLinkBuilder_;
        private List<String> includeInShareSheet = new ArrayList<>();
        private List<String> excludeFromShareSheet = new ArrayList<>();
        
        /**
         * <p>Creates options for sharing a link with other Applications. Creates a builder for sharing the link with
         * user selected clients</p>
         *
         * @param activity   The {@link Activity} to show the dialog for choosing sharing application.
         * @param parameters A {@link JSONObject} value containing the deep link params.
         */
        public ShareLinkBuilder(Activity activity, JSONObject parameters) {
            this.activity_ = activity;
            this.branch_ = branchReferral_;
            shortLinkBuilder_ = new BranchShortLinkBuilder(activity);
            try {
                Iterator<String> keys = parameters.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    shortLinkBuilder_.addParameters(key, (String) parameters.get(key));
                }
            } catch (Exception ignore) {
            }
            shareMsg_ = "";
            callback_ = null;
            channelPropertiesCallback_ = null;
            preferredOptions_ = new ArrayList<>();
            defaultURL_ = null;
            
            moreOptionIcon_ = BranchUtil.getDrawable(activity.getApplicationContext(), android.R.drawable.ic_menu_more);
            moreOptionText_ = "More...";
            
            copyUrlIcon_ = BranchUtil.getDrawable(activity.getApplicationContext(), android.R.drawable.ic_menu_save);
            copyURlText_ = "Copy link";
            urlCopiedMessage_ = "Copied link to clipboard!";
        }
        
        /**
         * *<p>Creates options for sharing a link with other Applications. Creates a builder for sharing the link with
         * user selected clients</p>
         *
         * @param activity         The {@link Activity} to show the dialog for choosing sharing application.
         * @param shortLinkBuilder An instance of {@link BranchShortLinkBuilder} to create link to be shared
         */
        public ShareLinkBuilder(Activity activity, BranchShortLinkBuilder shortLinkBuilder) {
            this(activity, new JSONObject());
            shortLinkBuilder_ = shortLinkBuilder;
        }
        
        /**
         * <p>Sets the message to be shared with the link.</p>
         *
         * @param message A {@link String} to be shared with the link
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setMessage(String message) {
            this.shareMsg_ = message;
            return this;
        }
        
        /**
         * <p>Sets the subject of this message. This will be added to Email and SMS Application capable of handling subject in the message.</p>
         *
         * @param subject A {@link String} subject of this message.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setSubject(String subject) {
            this.shareSub_ = subject;
            return this;
        }
        
        /**
         * <p>Adds the given tag an iterable {@link Collection} of {@link String} tags associated with a deep
         * link.</p>
         *
         * @param tag A {@link String} to be added to the iterable {@link Collection} of {@link String} tags associated with a deep
         *            link.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addTag(String tag) {
            this.shortLinkBuilder_.addTag(tag);
            return this;
        }
        
        /**
         * <p>Adds the given tag an iterable {@link Collection} of {@link String} tags associated with a deep
         * link.</p>
         *
         * @param tags A {@link java.util.List} of tags to be added to the iterable {@link Collection} of {@link String} tags associated with a deep
         *             link.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addTags(ArrayList<String> tags) {
            this.shortLinkBuilder_.addTags(tags);
            return this;
        }
        
        /**
         * <p>Adds a feature that make use of the link.</p>
         *
         * @param feature A {@link String} value identifying the feature that the link makes use of.
         *                Should not exceed 128 characters.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setFeature(String feature) {
            this.shortLinkBuilder_.setFeature(feature);
            return this;
        }
        
        /**
         * <p>Adds a stage application or user flow associated with this link.</p>
         *
         * @param stage A {@link String} value identifying the stage in an application or user flow
         *              process. Should not exceed 128 characters.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setStage(String stage) {
            this.shortLinkBuilder_.setStage(stage);
            return this;
        }
        
        /**
         * <p>Adds a callback to get the sharing status.</p>
         *
         * @param callback A {@link BranchLinkShareListener} instance for getting sharing status.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setCallback(BranchLinkShareListener callback) {
            this.callback_ = callback;
            return this;
        }
        
        /**
         * @param channelPropertiesCallback A {@link io.branch.referral.Branch.IChannelProperties} instance for customizing sharing properties for channels.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setChannelProperties(IChannelProperties channelPropertiesCallback) {
            this.channelPropertiesCallback_ = channelPropertiesCallback;
            return this;
        }
        
        /**
         * <p>Adds application to the preferred list of applications which are shown on share dialog.
         * Only these options will be visible when the application selector dialog launches. Other options can be
         * accessed by clicking "More"</p>
         *
         * @param preferredOption A list of applications to be added as preferred options on the app chooser.
         *                        Preferred applications are defined in {@link io.branch.referral.SharingHelper.SHARE_WITH}.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addPreferredSharingOption(SharingHelper.SHARE_WITH preferredOption) {
            this.preferredOptions_.add(preferredOption);
            return this;
        }
        
        /**
         * <p>Adds application to the preferred list of applications which are shown on share dialog.
         * Only these options will be visible when the application selector dialog launches. Other options can be
         * accessed by clicking "More"</p>
         *
         * @param preferredOptions A list of applications to be added as preferred options on the app chooser.
         *                         Preferred applications are defined in {@link io.branch.referral.SharingHelper.SHARE_WITH}.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addPreferredSharingOptions(ArrayList<SharingHelper.SHARE_WITH> preferredOptions) {
            this.preferredOptions_.addAll(preferredOptions);
            return this;
        }
        
        /**
         * Add the given key value to the deep link parameters
         *
         * @param key   A {@link String} with value for the key for the deep link params
         * @param value A {@link String} with deep link parameters value
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addParam(String key, String value) {
            try {
                this.shortLinkBuilder_.addParameters(key, value);
            } catch (Exception ignore) {
            }
            return this;
        }
        
        /**
         * <p> Set a default url to share in case there is any error creating the deep link </p>
         *
         * @param url A {@link String} with value of default url to be shared with the selected application in case deep link creation fails.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setDefaultURL(String url) {
            defaultURL_ = url;
            return this;
        }
        
        /**
         * <p> Set the icon and label for the option to expand the application list to see more options.
         * Default label is set to "More" </p>
         *
         * @param icon  Drawable to set as the icon for more option. Default icon is system menu_more icon.
         * @param label A {@link String} with value for the more option label. Default label is "More"
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setMoreOptionStyle(Drawable icon, String label) {
            moreOptionIcon_ = icon;
            moreOptionText_ = label;
            return this;
        }
        
        /**
         * <p> Set the icon and label for the option to expand the application list to see more options.
         * Default label is set to "More" </p>
         *
         * @param drawableIconID Resource ID for the drawable to set as the icon for more option. Default icon is system menu_more icon.
         * @param stringLabelID  Resource ID for String label for the more option. Default label is "More"
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setMoreOptionStyle(int drawableIconID, int stringLabelID) {
            moreOptionIcon_ = BranchUtil.getDrawable(activity_.getApplicationContext(), drawableIconID);
            moreOptionText_ = activity_.getResources().getString(stringLabelID);
            return this;
        }
        
        /**
         * <p> Set the icon, label and success message for copy url option. Default label is "Copy link".</p>
         *
         * @param icon    Drawable to set as the icon for copy url  option. Default icon is system menu_save icon
         * @param label   A {@link String} with value for the copy url option label. Default label is "Copy link"
         * @param message A {@link String} with value for a toast message displayed on copying a url.
         *                Default message is "Copied link to clipboard!"
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setCopyUrlStyle(Drawable icon, String label, String message) {
            copyUrlIcon_ = icon;
            copyURlText_ = label;
            urlCopiedMessage_ = message;
            return this;
        }
        
        /**
         * <p> Set the icon, label and success message for copy url option. Default label is "Copy link".</p>
         *
         * @param drawableIconID  Resource ID for the drawable to set as the icon for copy url  option. Default icon is system menu_save icon
         * @param stringLabelID   Resource ID for the string label the copy url option. Default label is "Copy link"
         * @param stringMessageID Resource ID for the string message to show toast message displayed on copying a url
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setCopyUrlStyle(int drawableIconID, int stringLabelID, int stringMessageID) {
            copyUrlIcon_ = BranchUtil.getDrawable(activity_.getApplicationContext(), drawableIconID);
            copyURlText_ = activity_.getResources().getString(stringLabelID);
            urlCopiedMessage_ = activity_.getResources().getString(stringMessageID);
            return this;
            
        }
        
        /**
         * <p> Sets the alias for this link. </p>
         *
         * @param alias Link 'alias' can be used to label the endpoint on the link.
         *              <p>
         *              For example:
         *              http://bnc.lt/AUSTIN28.
         *              Should not exceed 128 characters
         *              </p>
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setAlias(String alias) {
            this.shortLinkBuilder_.setAlias(alias);
            return this;
        }
        
        /**
         * <p> Sets the amount of time that Branch allows a click to remain outstanding.</p>
         *
         * @param matchDuration A {@link Integer} value specifying the time that Branch allows a click to
         *                      remain outstanding and be eligible to be matched with a new app session.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setMatchDuration(int matchDuration) {
            this.shortLinkBuilder_.setDuration(matchDuration);
            return this;
        }
        
        /**
         * <p>
         * Sets the share dialog to full width mode. Full width mode will show a non modal sheet with entire screen width.
         * </p>
         *
         * @param setFullWidthStyle {@link Boolean} With value true if a full width style share sheet is desired.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setAsFullWidthStyle(boolean setFullWidthStyle) {
            this.setFullWidthStyle_ = setFullWidthStyle;
            return this;
        }
        
        /**
         * <p>
         * Sets the given resource id as the theme id for share sheet dialog view.
         * </p>
         *
         * @param styleResourceID the id of the theme to be applied to the share sheet dialog.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setDialogThemeResourceID(@StyleRes int styleResourceID) {
            this.dialogThemeResourceID_ = styleResourceID;
            return this;
        }
        
        /**
         * Set the height for the divider for the sharing channels in the list. Set this to zero to remove the dividers
         *
         * @param height The new height of the divider in pixels.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setDividerHeight(int height) {
            this.dividerHeight = height;
            return this;
        }
        
        /**
         * Set the title for the sharing dialog
         *
         * @param title {@link String} containing the value for the title text.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setSharingTitle(String title) {
            this.sharingTitle = title;
            return this;
        }
        
        /**
         * Set the title for the sharing dialog
         *
         * @param titleView {@link View} for setting the title.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setSharingTitle(View titleView) {
            this.sharingTitleView = titleView;
            return this;
        }
        
        /**
         * Set icon size for the sharing dialog
         *
         * @param iconSize {@link int} for setting the share sheet icon size.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder setIconSize(int iconSize) {
            this.iconSize_ = iconSize;
            return this;
        }
        
        /**
         * Exclude items from the ShareSheet by package name String.
         *
         * @param packageName {@link String} package name to be excluded.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder excludeFromShareSheet(@NonNull String packageName) {
            this.excludeFromShareSheet.add(packageName);
            return this;
        }
        
        /**
         * Exclude items from the ShareSheet by package name array.
         *
         * @param packageName {@link String[]} package name to be excluded.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder excludeFromShareSheet(@NonNull String[] packageName) {
            this.excludeFromShareSheet.addAll(Arrays.asList(packageName));
            return this;
        }
        
        /**
         * Exclude items from the ShareSheet by package name List.
         *
         * @param packageNames {@link List} package name to be excluded.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder excludeFromShareSheet(@NonNull List<String> packageNames) {
            this.excludeFromShareSheet.addAll(packageNames);
            return this;
        }
        
        /**
         * Include items from the ShareSheet by package name String. If only "com.Slack"
         * is included, then only preferred sharing options + Slack
         * will be displayed, for example.
         *
         * @param packageName {@link String} package name to be included.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder includeInShareSheet(@NonNull String packageName) {
            this.includeInShareSheet.add(packageName);
            return this;
        }
        
        /**
         * Include items from the ShareSheet by package name Array. If only "com.Slack"
         * is included, then only preferred sharing options + Slack
         * will be displayed, for example.
         *
         * @param packageName {@link String[]} package name to be included.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder includeInShareSheet(@NonNull String[] packageName) {
            this.includeInShareSheet.addAll(Arrays.asList(packageName));
            return this;
        }
        
        /**
         * Include items from the ShareSheet by package name List. If only "com.Slack"
         * is included, then only preferred sharing options + Slack
         * will be displayed, for example.
         *
         * @param packageNames {@link List} package name to be included.
         * @return this Builder object to allow for chaining of calls to set methods.
         */
        public ShareLinkBuilder includeInShareSheet(@NonNull List<String> packageNames) {
            this.includeInShareSheet.addAll(packageNames);
            return this;
        }
        
        /**
         * <p> Set the given style to the List View showing the share sheet</p>
         *
         * @param resourceID A Styleable resource to be applied to the share sheet list view
         */
        public void setStyleResourceID(@StyleRes int resourceID) {
            styleResourceID_ = resourceID;
        }
        
        public void setShortLinkBuilderInternal(BranchShortLinkBuilder shortLinkBuilder) {
            this.shortLinkBuilder_ = shortLinkBuilder;
        }
        
        /**
         * <p>Creates an application selector dialog and share a link with user selected sharing option.
         * The link is created with the parameters provided to the builder. </p>
         */
        public void shareLink() {
            branchReferral_.shareLink(this);
        }
        
        public Activity getActivity() {
            return activity_;
        }
        
        public ArrayList<SharingHelper.SHARE_WITH> getPreferredOptions() {
            return preferredOptions_;
        }
        
        List<String> getExcludedFromShareSheet() {
            return excludeFromShareSheet;
        }
        
        List<String> getIncludedInShareSheet() {
            return includeInShareSheet;
        }
        
        public Branch getBranch() {
            return branch_;
        }
        
        public String getShareMsg() {
            return shareMsg_;
        }
        
        public String getShareSub() {
            return shareSub_;
        }
        
        public BranchLinkShareListener getCallback() {
            return callback_;
        }
        
        public IChannelProperties getChannelPropertiesCallback() {
            return channelPropertiesCallback_;
        }
        
        public String getDefaultURL() {
            return defaultURL_;
        }
        
        public Drawable getMoreOptionIcon() {
            return moreOptionIcon_;
        }
        
        public String getMoreOptionText() {
            return moreOptionText_;
        }
        
        public Drawable getCopyUrlIcon() {
            return copyUrlIcon_;
        }
        
        public String getCopyURlText() {
            return copyURlText_;
        }
        
        public String getUrlCopiedMessage() {
            return urlCopiedMessage_;
        }
        
        public BranchShortLinkBuilder getShortLinkBuilder() {
            return shortLinkBuilder_;
        }
        
        public boolean getIsFullWidthStyle() {
            return setFullWidthStyle_;
        }
        
        public int getDialogThemeResourceID() {
            return dialogThemeResourceID_;
        }
        
        public int getDividerHeight() {
            return dividerHeight;
        }
        
        public String getSharingTitle() {
            return sharingTitle;
        }
        
        public View getSharingTitleView() {
            return sharingTitleView;
        }
        
        public int getStyleResourceID() {
            return styleResourceID_;
        }
        
        public int getIconSize() {
            return iconSize_;
        }
    }
    
    //------------------------ Content Indexing methods----------------------//
    
    public void registerView(BranchUniversalObject
                                     branchUniversalObject, BranchUniversalObject.RegisterViewStatusListener callback) {
        if (context_ != null) {
            new BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM)
                    .addContentItems(branchUniversalObject)
                    .logEvent(context_);
        }
    }
    
    ///-------Instrumentation additional data---------------///
    
    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param instrumentationData A {@link HashMap} with key value pairs for instrumentation data.
     */
    public void addExtraInstrumentationData(HashMap<String, String> instrumentationData) {
        instrumentationExtraData_.putAll(instrumentationData);
    }
    
    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param key   A {@link String} Value for instrumentation data key
     * @param value A {@link String} Value for instrumentation data value
     */
    public void addExtraInstrumentationData(String key, String value) {
        instrumentationExtraData_.put(key, value);
    }
    
    
    //-------------------- Branch view handling--------------------//
    
    
    @Override
    public void onBranchViewVisible(String action, String branchViewID) {
        //No Implementation on purpose
    }
    
    @Override
    public void onBranchViewAccepted(String action, String branchViewID) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration();
        }
    }
    
    @Override
    public void onBranchViewCancelled(String action, String branchViewID) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration();
        }
    }
    
    @Override
    public void onBranchViewError(int errorCode, String errorMsg, String action) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration();
        }
    }
    
    /**
     * Interface for defining optional Branch view behaviour for Activities
     */
    public interface IBranchViewControl {
        /**
         * Defines if an activity is interested to show Branch views or not.
         * By default activities are considered as Branch view enabled. In case of activities which are not interested to show a Branch view (Splash screen for example)
         * should implement this and return false. The pending Branch view will be shown with the very next Branch view enabled activity
         *
         * @return A {@link Boolean} whose value is true if the activity don't want to show any Branch view.
         */
        boolean skipBranchViewsOnThisActivity();
    }
    
    
    ///----------------- Instant App  support--------------------------//
    
    /**
     * Checks if this is an Instant app instance
     *
     * @param context Current {@link Context}
     * @return {@code true}  if current application is an instance of instant app
     */
    public static boolean isInstantApp(@NonNull Context context) {
        return InstantAppUtil.isInstantApp(context);
    }
    
    /**
     * Method shows play store install prompt for the full app. Thi passes the referrer to the installed application. The same deep link params as the instant app are provided to the
     * full app up on Branch#initSession()
     *
     * @param activity    Current activity
     * @param requestCode Request code for the activity to receive the result
     * @return {@code true} if install prompt is shown to user
     */
    public static boolean showInstallPrompt(@NonNull Activity activity, int requestCode) {
        String installReferrerString = "";
        if (Branch.getInstance() != null) {
            JSONObject latestReferringParams = Branch.getInstance().getLatestReferringParams();
            String referringLinkKey = "~" + Defines.Jsonkey.ReferringLink.getKey();
            if (latestReferringParams != null && latestReferringParams.has(referringLinkKey)) {
                String referringLink = "";
                try {
                    referringLink = latestReferringParams.getString(referringLinkKey);
                    // Considering the case that url may contain query params with `=` and `&` with it and may cause issue when parsing play store referrer
                    referringLink = URLEncoder.encode(referringLink, "UTF-8");
                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (!TextUtils.isEmpty(referringLink)) {
                    installReferrerString = Defines.Jsonkey.IsFullAppConv.getKey() + "=true&" + Defines.Jsonkey.ReferringLink.getKey() + "=" + referringLink;
                }
            }
        }
        return InstantAppUtil.doShowInstallPrompt(activity, requestCode, installReferrerString);
    }
    
    /**
     * Method shows play store install prompt for the full app. Use this method only if you have custom parameters to pass to the full app using referrer else use
     * {@link #showInstallPrompt(Activity, int)}
     *
     * @param activity    Current activity
     * @param requestCode Request code for the activity to receive the result
     * @param referrer    Any custom referrer string to pass to full app (must be of format "referrer_key1=referrer_value1%26referrer_key2=referrer_value2")
     * @return {@code true} if install prompt is shown to user
     */
    public static boolean showInstallPrompt(@NonNull Activity activity, int requestCode, @Nullable String referrer) {
        String installReferrerString = Defines.Jsonkey.IsFullAppConv.getKey() + "=true&" + referrer;
        return InstantAppUtil.doShowInstallPrompt(activity, requestCode, installReferrerString);
    }
    
    /**
     * Method shows play store install prompt for the full app. Use this method only if you want the full app to receive a custom {@link BranchUniversalObject} to do deferred deep link.
     * Please see {@link #showInstallPrompt(Activity, int)}
     * NOTE :
     * This method will do a synchronous generation of Branch short link for the BUO. So please consider calling this method on non UI thread
     * Please make sure your instant app and full ap are using same Branch key in order for the deferred deep link working
     *
     * @param activity    Current activity
     * @param requestCode Request code for the activity to receive the result
     * @param buo         {@link BranchUniversalObject} to pass to the full app up on install
     * @return {@code true} if install prompt is shown to user
     */
    public static boolean showInstallPrompt(@NonNull Activity activity, int requestCode, @NonNull BranchUniversalObject buo) {
        if (buo != null) {
            String shortUrl = buo.getShortUrl(activity, new LinkProperties());
            String installReferrerString = Defines.Jsonkey.ReferringLink.getKey() + "=" + shortUrl;
            if (!TextUtils.isEmpty(installReferrerString)) {
                return showInstallPrompt(activity, requestCode, installReferrerString);
            } else {
                return showInstallPrompt(activity, requestCode, "");
            }
        }
        return false;
    }
}
