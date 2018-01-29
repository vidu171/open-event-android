package org.fossasia.openevent.activities;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.otto.Subscribe;

import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.R;
import org.fossasia.openevent.api.DataDownloadManager;
import org.fossasia.openevent.api.Urls;
import org.fossasia.openevent.data.Event;
import org.fossasia.openevent.data.Microlocation;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.data.Speaker;
import org.fossasia.openevent.data.Sponsor;
import org.fossasia.openevent.data.Track;
import org.fossasia.openevent.dbutils.RealmDataRepository;
import org.fossasia.openevent.events.CounterEvent;
import org.fossasia.openevent.events.DataDownloadEvent;
import org.fossasia.openevent.events.DownloadEvent;
import org.fossasia.openevent.events.EventDownloadEvent;
import org.fossasia.openevent.events.EventLoadedEvent;
import org.fossasia.openevent.events.JsonReadEvent;
import org.fossasia.openevent.events.MicrolocationDownloadEvent;
import org.fossasia.openevent.events.NoInternetEvent;
import org.fossasia.openevent.events.RetrofitError;
import org.fossasia.openevent.events.RetrofitResponseEvent;
import org.fossasia.openevent.events.SessionDownloadEvent;
import org.fossasia.openevent.events.ShowNetworkDialogEvent;
import org.fossasia.openevent.events.SpeakerDownloadEvent;
import org.fossasia.openevent.events.SponsorDownloadEvent;
import org.fossasia.openevent.events.TracksDownloadEvent;
import org.fossasia.openevent.fragments.AboutFragment;
import org.fossasia.openevent.fragments.LocationsFragment;
import org.fossasia.openevent.fragments.ScheduleFragment;
import org.fossasia.openevent.fragments.SpeakersListFragment;
import org.fossasia.openevent.fragments.SponsorsFragment;
import org.fossasia.openevent.fragments.TracksFragment;
import org.fossasia.openevent.utils.CommonTaskLoop;
import org.fossasia.openevent.utils.ConstantStrings;
import org.fossasia.openevent.utils.DateUtils;
import org.fossasia.openevent.utils.DownloadCompleteHandler;
import org.fossasia.openevent.utils.NetworkUtils;
import org.fossasia.openevent.utils.ShowNotificationSnackBar;
import org.fossasia.openevent.utils.SmoothActionBarDrawerToggle;
import org.fossasia.openevent.utils.Utils;
import org.fossasia.openevent.views.CustomTabsSpan;
import org.fossasia.openevent.widget.DialogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import timber.log.Timber;

import static org.fossasia.openevent.R.id.headerDrawer;

public class MainActivity extends BaseActivity {

    private final static String STATE_FRAGMENT = "stateFragment";

    private static final String NAV_ITEM = "navItem";

    private static final String BOOKMARK = "bookmarks";

    private static final String FRAGMENT_TAG_HOME = "FTAGH";

    private final String FRAGMENT_TAG_TRACKS = "FTAGT";

    private final String FRAGMENT_TAG_REST = "FTAGR";

    private boolean fromServer = true;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.layout_main) CoordinatorLayout mainFrame;
    @BindView(R.id.appbar) AppBarLayout appBarLayout;

    private ImageView headerView;

    private DrawerLayout drawerLayout;
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean atHome = true;
    private boolean backPressedOnce = false;
    private int currentMenuItemId;
    private boolean mTwoPane = false;
    private boolean customTabsSupported;
    private CustomTabsServiceConnection customTabsServiceConnection;
    private CustomTabsClient customTabsClient;
    private Runnable runnable;
    private Handler handler;
    public static Dialog dialogNetworkNotiff;

    private DownloadCompleteHandler completeHandler;

    private CompositeDisposable disposable;

    private RealmDataRepository realmRepo = RealmDataRepository.getDefaultInstance();
    private Event event; // Future Event, stored to remove listeners

    public static Intent createLaunchFragmentIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .putExtra(NAV_ITEM, BOOKMARK);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        ButterKnife.setDebug(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putInt(ConstantStrings.SESSION_MAP_ID, -1).apply();
        setTheme(R.style.AppTheme_NoActionBar_MainTheme);
        super.onCreate(savedInstanceState);

        if(findViewById(R.id.drawer)!=null) {
            drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
            mTwoPane = false;
        } else {
            mTwoPane = true;
        }

        disposable = new CompositeDisposable();

        setUpToolbar();
        setUpNavDrawer();
        setUpCustomTab();
        setupEvent();

        completeHandler = DownloadCompleteHandler.with(context);

        if (Utils.isBaseUrlEmpty()) {
            if (!sharedPreferences.getBoolean(ConstantStrings.IS_DOWNLOAD_DONE, false)) {
                downloadFromAssets();
                sharedPreferences.edit().putBoolean(ConstantStrings.IS_DOWNLOAD_DONE, true).apply();
            }
        } else {
            downloadFromServer();
        }

        if (savedInstanceState == null) {
            currentMenuItemId = R.id.nav_home;
        } else {
            currentMenuItemId = savedInstanceState.getInt(STATE_FRAGMENT);
        }

        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_HOME) == null &&
                getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_REST) == null) {
            doMenuAction(currentMenuItemId);
        }
    }

    private void downloadFromServer(){
        NetworkUtils.checkConnection(new WeakReference<>(this), new NetworkUtils.NetworkStateReceiverListener() {
            @Override
            public void activeConnection() {
                //Internet is working
                if (!sharedPreferences.getBoolean(ConstantStrings.IS_DOWNLOAD_DONE, false)) {
                    DialogFactory.createDownloadDialog(context, R.string.download_assets, R.string.charges_warning, (dialogInterface, button) -> {
                        if (button==DialogInterface.BUTTON_POSITIVE) {
                            fromServer = true;
                            Boolean preference = sharedPreferences.getBoolean(getResources().getString(R.string.download_mode_key), true);
                            if (preference) {
                                disposable.add(NetworkUtils.haveNetworkConnectionObservable(MainActivity.this)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(connected -> {
                                            if (connected) {
                                                OpenEventApp.postEventOnUIThread(new DataDownloadEvent());
                                            } else {
                                                final Snackbar snackbar = Snackbar.make(mainFrame, R.string.internet_preference_warning, Snackbar.LENGTH_INDEFINITE);
                                                snackbar.setAction(R.string.yes, view -> downloadFromAssets());
                                                snackbar.show();
                                            }
                                        }));
                            } else {
                                OpenEventApp.postEventOnUIThread(new DataDownloadEvent());
                            }
                        } else if (button==DialogInterface.BUTTON_NEGATIVE) {
                            fromServer = false;
                            downloadFromAssets();
                        }
                    }).show();
                } else {
                    completeHandler.hide();
                }
            }

            @Override
            public void inactiveConnection() {
                //Device is connected to WI-FI or Mobile Data but Internet is not working
                ShowNotificationSnackBar showNotificationSnackBar = new ShowNotificationSnackBar(MainActivity.this,mainFrame,null) {
                    @Override
                    public void refreshClicked() {
                        OpenEventApp.getEventBus().unregister(this);
                        OpenEventApp.getEventBus().register(this);
                    }
                };
                //show snackbar
                showNotificationSnackBar.showSnackBar();
                //snow notification (Only when connected to WiFi)
                showNotificationSnackBar.buildNotification();
            }

            @Override
            public void networkAvailable() {
                // Waiting for connectivity
            }

            @Override
            public void networkUnavailable() {
                Snackbar.make(mainFrame, R.string.display_offline_schedule, Snackbar.LENGTH_LONG).show();
                downloadFromAssets();
            }
        });
    }

    private void setUpCustomTab() {
        Intent customTabIntent = new Intent("android.support.customtabs.action.CustomTabsService");
        customTabIntent.setPackage("com.android.chrome");
        customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                customTabsClient = client;
                customTabsClient.warmup(0L);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                //initially left empty
            }
        };
        customTabsSupported = bindService(customTabIntent, customTabsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void onPause() {
        super.onPause();
        OpenEventApp.getEventBus().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenEventApp.getEventBus().register(this);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_FRAGMENT, currentMenuItemId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        currentMenuItemId = savedInstanceState.getInt(STATE_FRAGMENT);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        setupDrawerContent(navigationView);
        return super.onPrepareOptionsMenu(menu);
    }

    private void setUpToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private void setupEvent() {
        event = realmRepo.getEventSync();

        if(event == null)
            return;

        setNavHeader(event);
    }

    private void setUpNavDrawer() {
        headerView = (ImageView) navigationView.getHeaderView(0).findViewById(headerDrawer);
        if (toolbar != null && !mTwoPane) {
            final ActionBar ab = getSupportActionBar();
            if(ab == null) return;
            SmoothActionBarDrawerToggle smoothActionBarToggle = new SmoothActionBarDrawerToggle(this,
                    drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

                @Override
                public void onDrawerStateChanged(int newState) {
                    super.onDrawerStateChanged(newState);

                    if(toolbar.getTitle().equals(getString(R.string.menu_about))) {
                        navigationView.setCheckedItem(R.id.nav_home);
                    }
                }
            };

            drawerLayout.addDrawerListener(smoothActionBarToggle);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            smoothActionBarToggle.syncState();
        } else if (toolbar!=null && toolbar.getTitle().equals(getString(R.string.menu_about))) {
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private void setNavHeader(Event event) {
        String logo = event.getLogo();
        if (!logo.isEmpty()) {
            OpenEventApp.picassoWithCache.load(logo).into(headerView);
        }
    }

    private void saveEventDates(Event event) {
        String startTime = event.getStartTime();
        String endTime = event.getEndTime();

        Observable.fromCallable(() ->
                DateUtils.getDaysInBetween(startTime, endTime)
        ).subscribe(eventDates -> realmRepo.saveEventDates(eventDates).subscribe(), throwable -> {
            Timber.e(throwable);
            Timber.e("Error start parsing start date: %s and end date: %s in ISO format",
                    startTime, endTime);
            OpenEventApp.postEventOnUIThread(new RetrofitError(new Throwable("Error parsing dates")));
        });
    }

    private void syncComplete() {
        String successMessage = "Data loaded from JSON";

        if (fromServer) {
            // Event successfully loaded, set data downloaded to true
            sharedPreferences.edit().putBoolean(ConstantStrings.IS_DOWNLOAD_DONE, true).apply();

            successMessage = "Download done";
        }

        Snackbar.make(mainFrame, getString(R.string.download_complete), Snackbar.LENGTH_SHORT).show();
        Timber.d(successMessage);

        setupEvent();
        OpenEventApp.postEventOnUIThread(new EventLoadedEvent(event));
        saveEventDates(event);
    }

    private void downloadFailed(final DownloadEvent event) {
        Snackbar.make(mainFrame, getString(R.string.download_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, view -> {
            if (event == null)
                OpenEventApp.postEventOnUIThread(new DataDownloadEvent());
            else
                OpenEventApp.postEventOnUIThread(event);
        }).show();

    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            final int id = menuItem.getItemId();
            if(!mTwoPane) {
                drawerLayout.closeDrawers();
                drawerLayout.postDelayed(() -> doMenuAction(id), 300);
            } else {
                doMenuAction(id);
            }
            return true;
        });
    }

    private void doMenuAction(int menuItemId) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        switch (menuItemId) {
            case R.id.nav_home:
                atHome = true;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new AboutFragment(), FRAGMENT_TAG_HOME).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_home);
                }
                if (currentMenuItemId == R.id.nav_schedule){
                    addShadowToAppBar(false);
                }
                break;
            case R.id.nav_tracks:
                atHome = false;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new TracksFragment(), FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(true);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_tracks);
                }
                break;
            case R.id.nav_schedule:
                atHome = false;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new ScheduleFragment(), FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(false);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_schedule);
                }
                break;
            case R.id.nav_speakers:
                atHome = false;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new SpeakersListFragment(), FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(true);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_speakers);
                }
                break;
            case R.id.nav_sponsors:
                atHome = false;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new SponsorsFragment(), FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(true);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_sponsor);
                }
                break;
            case R.id.nav_locations:
                atHome = false;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new LocationsFragment(), FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(true);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_locations);
                }
                break;
            case R.id.nav_map:
                atHome = false;
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                fragmentTransaction.replace(R.id.content_frame,
                        ((OpenEventApp) getApplication())
                                .getMapModuleFactory()
                                .provideMapModule()
                                .provideMapFragment(), FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(true);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("");
                }
                break;
            case R.id.nav_settings:
                final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_share:
                try {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.whatsapp_promo_msg_template),
                            String.format(getString(R.string.app_share_url),getPackageName())));
                    startActivity(shareIntent);
                }
                catch (Exception e) {
                    Snackbar.make(mainFrame, getString(R.string.error_msg_retry), Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.nav_about:

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String app_name = sharedPreferences.getString(ConstantStrings.APP_NAME, "");
                String org_description = sharedPreferences.getString(ConstantStrings.ORG_DESCRIPTION, "");

                final AlertDialog aboutUs = new AlertDialog.Builder(this)
                        .setTitle(app_name)
                        .setMessage(Html.fromHtml(org_description))
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                aboutUs.show();

                TextView aboutUsTV = (TextView) aboutUs.findViewById(android.R.id.message);
                if(aboutUsTV == null) break;
                aboutUsTV.setMovementMethod(LinkMovementMethod.getInstance());
                if (customTabsSupported) {
                    SpannableString welcomeAlertSpannable = new SpannableString(aboutUsTV.getText());
                    URLSpan[] spans = welcomeAlertSpannable.getSpans(0, welcomeAlertSpannable.length(), URLSpan.class);
                    for (URLSpan span : spans) {
                        CustomTabsSpan newSpan = new CustomTabsSpan(span.getURL(), getApplicationContext(), this,
                                customTabsClient.newSession(new CustomTabsCallback()));
                        welcomeAlertSpannable.setSpan(newSpan, welcomeAlertSpannable.getSpanStart(span),
                                welcomeAlertSpannable.getSpanEnd(span),
                                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                        welcomeAlertSpannable.removeSpan(span);
                    }
                    aboutUsTV.setText(welcomeAlertSpannable);
                }
                break;
            default:
                //Do nothing
        }
        currentMenuItemId = menuItemId;
    }

    @Override
    public void onBackPressed() {
        if(!mTwoPane) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (atHome) {
                if (backPressedOnce) {
                    super.onBackPressed();
                } else {
                    backPressedOnce = true;
                    Snackbar snackbar = Snackbar.make(mainFrame, R.string.press_back_again, 2000);
                    snackbar.show();
                    runnable = () -> backPressedOnce = false;
                    handler = new Handler();
                    long timer = 2000;
                    handler.postDelayed(runnable, timer);
                }
            } else {
                atHome = true;
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, new AboutFragment(), FRAGMENT_TAG_REST).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_home);
                }
                if (currentMenuItemId == R.id.nav_schedule) {
                    addShadowToAppBar(false);
                }
            }
        }
    }

    public void addShadowToAppBar(boolean addShadow) {
        if (addShadow) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                appBarLayout.setElevation(12);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                appBarLayout.setElevation(0);
            }
        }
    }

    private void startDownloadListener() {
        completeHandler.startListening()
                .show()
                .withCompletionListener()
                .subscribe(this::syncComplete, throwable -> {
                    throwable.printStackTrace();
                    Timber.e(throwable);
                    if (throwable instanceof DownloadCompleteHandler.DataEventError) {
                        downloadFailed(((DownloadCompleteHandler.DataEventError) throwable)
                                .getDataDownloadEvent());
                    } else {
                        OpenEventApp.postEventOnUIThread(new RetrofitError(throwable));
                    }
                });
    }

    private void startDownload() {
        realmRepo.clearVersions().subscribe(() -> {
            Timber.d("Cleared JSON db versions");
        }, throwable -> {
            throwable.printStackTrace();
            Timber.e(throwable);
        });

        DataDownloadManager.getInstance().downloadEvents();
        startDownloadListener();
        Timber.d("Download has started");
    }

    public void showErrorDialog(String errorType, String errorDesc) {
        completeHandler.hide();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error)
                .setMessage(errorType + ": " + errorDesc)
                .setNeutralButton(android.R.string.ok, null)
                .create();
        builder.show();
    }

    @Subscribe
    public void noInternet(NoInternetEvent event) {
        downloadFailed(null);
    }

    @Subscribe
    public void showNetworkDialog(ShowNetworkDialogEvent event) {
        completeHandler.hide();
        dialogNetworkNotiff = DialogFactory.createSimpleActionDialog(this,
                R.string.net_unavailable,
                R.string.turn_on,
                (dialog, which) -> {
                    Intent setNetworkIntent = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(setNetworkIntent);
                });
        dialogNetworkNotiff.show();
    }

    @Subscribe
    public void downloadData(DataDownloadEvent event) {
        switch (Urls.getBaseUrl()) {
            case Urls.INVALID_LINK:
                showErrorDialog("Invalid Api", "Api link doesn't seem to be valid");
                downloadFromAssets();
                break;
            case Urls.EMPTY_LINK:
                downloadFromAssets();
                break;
            default:
                startDownload();
                break;
        }
    }

    @Subscribe
    public void handleResponseEvent(RetrofitResponseEvent responseEvent) {
        Integer statusCode = responseEvent.getStatusCode();
        if (statusCode.equals(404)) {
            showErrorDialog("HTTP Error", statusCode + "Api Not Found");
        }
    }

    @Subscribe
    public void handleJsonEvent(final JsonReadEvent jsonReadEvent) {
        final String name = jsonReadEvent.getName();
        final String json = jsonReadEvent.getJson();

        Completable.fromAction(() -> {
            final Gson gson = new Gson();

            // Need separate instance for background thread
            Realm realm = Realm.getDefaultInstance();

            RealmDataRepository realmDataRepository = RealmDataRepository
                    .getInstance(realm);

            switch (name) {
                case ConstantStrings.EVENT: {
                    Event event = gson.fromJson(json, Event.class);

                    saveEventDates(event);
                    realmDataRepository.saveEvent(event).subscribe();

                    realmDataRepository.saveEvent(event).subscribe();

                    OpenEventApp.postEventOnUIThread(new EventDownloadEvent(true));
                    break;
                } case ConstantStrings.TRACKS: {
                    Type listType = new TypeToken<List<Track>>() {}.getType();
                    List<Track> tracks = gson.fromJson(json, listType);

                    realmDataRepository.saveTracks(tracks).subscribe();

                    OpenEventApp.postEventOnUIThread(new TracksDownloadEvent(true));
                    break;
                } case ConstantStrings.SESSIONS: {
                    Type listType = new TypeToken<List<Session>>() {}.getType();
                    List<Session> sessions = gson.fromJson(json, listType);

                    for (Session current : sessions) {
                        current.setStartDate(current.getStartTime().split("T")[0]);
                    }

                    realmDataRepository.saveSessions(sessions).subscribe();

                    OpenEventApp.postEventOnUIThread(new SessionDownloadEvent(true));
                    break;
                } case ConstantStrings.SPEAKERS: {
                    Type listType = new TypeToken<List<Speaker>>() {}.getType();
                    List<Speaker> speakers = gson.fromJson(json, listType);

                    realmRepo.saveSpeakers(speakers).subscribe();

                    OpenEventApp.postEventOnUIThread(new SpeakerDownloadEvent(true));
                    break;
                } case ConstantStrings.SPONSORS: {
                    Type listType = new TypeToken<List<Sponsor>>() {}.getType();
                    List<Sponsor> sponsors = gson.fromJson(json, listType);

                    realmRepo.saveSponsors(sponsors).subscribe();

                    OpenEventApp.postEventOnUIThread(new SponsorDownloadEvent(true));
                    break;
                } case ConstantStrings.MICROLOCATIONS: {
                    Type listType = new TypeToken<List<Microlocation>>() {}.getType();
                    List<Microlocation> microlocations = gson.fromJson(json, listType);

                    realmRepo.saveLocations(microlocations).subscribe();

                    OpenEventApp.postEventOnUIThread(new MicrolocationDownloadEvent(true));
                    break;
                } default:
                    //do nothing
            }

            realm.close();
        }).observeOn(Schedulers.computation()).subscribe(() -> Timber.d("Saved event from JSON"), throwable -> {
            throwable.printStackTrace();
            Timber.e(throwable);
            OpenEventApp.postEventOnUIThread(new RetrofitError(throwable));
        });
    }

    @Subscribe
    public void errorHandlerEvent(RetrofitError error) {
        String errorType;
        String errorDesc;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = connMgr.getActiveNetworkInfo();
        if (!(netinfo != null && netinfo.isConnected())) {
            OpenEventApp.postEventOnUIThread(new ShowNetworkDialogEvent());
        } else {
            if (error.getThrowable() instanceof IOException) {
                errorType = "Timeout";
                errorDesc = String.valueOf(error.getThrowable().getCause());
            } else if (error.getThrowable() instanceof IllegalStateException) {
                errorType = "ConversionError";
                errorDesc = String.valueOf(error.getThrowable().getCause());
            } else {
                errorType = "Other Error";
                errorDesc = String.valueOf(error.getThrowable().getLocalizedMessage());
            }
            Timber.tag(errorType).e(errorDesc);
            showErrorDialog(errorType, errorDesc);
        }
    }

    public void downloadFromAssets() {

        if (!sharedPreferences.getBoolean(ConstantStrings.DATABASE_RECORDS_EXIST, false)) {
            //TODO: Add and Take counter value from to config.json
            sharedPreferences.edit().putBoolean(ConstantStrings.DATABASE_RECORDS_EXIST, true).apply();

            startDownloadListener();
            Timber.d("JSON parsing started");

            OpenEventApp.postEventOnUIThread(new CounterEvent(6)); // Bump if increased

            readJsonAsset(Urls.EVENT);
            readJsonAsset(Urls.SESSIONS);
            readJsonAsset(Urls.SPEAKERS);
            readJsonAsset(Urls.TRACKS);
            readJsonAsset(Urls.SPONSORS);
            readJsonAsset(Urls.MICROLOCATIONS);
        } else {
            completeHandler.hide();
        }
    }

    public void readJsonAsset(final String name) {
        CommonTaskLoop.getInstance().post(new Runnable() {
            String json = null;

            @Override
            public void run() {
                try {
                    InputStream inputStream = getAssets().open(name);
                    int size = inputStream.available();
                    byte[] buffer = new byte[size];
                    inputStream.read(buffer);
                    inputStream.close();
                    json = new String(buffer, "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                OpenEventApp.postEventOnUIThread(new JsonReadEvent(name, json));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(customTabsServiceConnection);
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
        if(disposable != null && !disposable.isDisposed())
            disposable.dispose();
        if(event != null)
            event.removeAllChangeListeners();
        if(completeHandler != null)
            completeHandler.stopListening();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}