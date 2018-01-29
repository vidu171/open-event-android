package org.fossasia.openevent.api;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.retrofit.JSONAPIConverterFactory;

import org.fossasia.openevent.BuildConfig;
import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.api.network.FacebookGraphAPI;
import org.fossasia.openevent.api.network.LoklakAPI;
import org.fossasia.openevent.api.network.OpenEventAPI;
import org.fossasia.openevent.data.Event;
import org.fossasia.openevent.data.Microlocation;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.data.Speaker;
import org.fossasia.openevent.data.Sponsor;
import org.fossasia.openevent.data.Track;
import org.fossasia.openevent.data.auth.User;
import org.fossasia.openevent.utils.AuthUtil;
import org.fossasia.openevent.utils.NetworkUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import timber.log.Timber;

/**
 * User: mohit
 * Date: 25/5/15
 */
public final class APIClient {
    /**
     * This is the base url can be changed via a config Param
     * Or Build Config
     */

    private static final int CONNECT_TIMEOUT_SECONDS = 15; // 15s

    private static final int READ_TIMEOUT_SECONDS = 15; // 15s
    private static final String CACHE_CONTROL = "Cache-Control";

    private static OpenEventAPI openEventAPI;
    private static FacebookGraphAPI facebookGraphAPI;
    private static LoklakAPI loklakAPI;

    private static OkHttpClient.Builder okHttpClientBuilder;
    private static Retrofit.Builder retrofitBuilder;

    static {
        okHttpClientBuilder = new OkHttpClient().newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG)
            okHttpClientBuilder.addNetworkInterceptor(new StethoInterceptor());

        retrofitBuilder = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create(OpenEventApp.getObjectMapper()));
    }

    public static OpenEventAPI getOpenEventAPI() {
        if (openEventAPI == null) {
            OkHttpClient okHttpClient = okHttpClientBuilder.addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .authenticator(AuthUtil.getAuthenticator())
                    .build();

            ObjectMapper objectMapper = OpenEventApp.getObjectMapper();
            Class[] classes = {Event.class, Track.class, Speaker.class, Sponsor.class, Session.class, Microlocation.class, User.class};

            openEventAPI = new Retrofit.Builder()
                    .client(okHttpClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(new JSONAPIConverterFactory(objectMapper, classes))
                    .addConverterFactory(JacksonConverterFactory.create(OpenEventApp.getObjectMapper()))
                    .baseUrl(Urls.BASE_URL)
                    .build()
                    .create(OpenEventAPI.class);
        }

        return openEventAPI;
    }

    public static FacebookGraphAPI getFacebookGraphAPI() {
        if (facebookGraphAPI == null) {
            OkHttpClient okHttpClient = okHttpClientBuilder.addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .addInterceptor(provideOfflineCacheInterceptor())
                    .addNetworkInterceptor(provideCacheInterceptor())
                    .cache(provideCache())
                    .build();

            retrofitBuilder.client(okHttpClient);

            facebookGraphAPI = retrofitBuilder
                    .baseUrl(Urls.FACEBOOK_BASE_URL)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(FacebookGraphAPI.class);
        }

        return facebookGraphAPI;
    }

    private static Cache provideCache() {
        Cache cache = null;
        try {
            cache = new Cache(new File(OpenEventApp.getAppContext().getCacheDir(), "feed-cache"),
                    10 * 1024 * 1024); // 10 MB
        } catch (Exception e) {
            Timber.e(e, "Could not create Cache!");
        }
        return cache;
    }

    private static Interceptor provideCacheInterceptor() {
        return chain -> {
            Response response = chain.proceed(chain.request());

            // re-write response header to force use of cache
            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(2, TimeUnit.MINUTES)
                    .build();

            return response.newBuilder()
                    .removeHeader("Pragma")
                    .header(CACHE_CONTROL, cacheControl.toString())
                    .build();
        };
    }

    private static Interceptor provideOfflineCacheInterceptor() {
        return chain -> {
            Request request = chain.request();

            if (!NetworkUtils.haveNetworkConnection(OpenEventApp.getAppContext())) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build();

                request = request.newBuilder()
                        .removeHeader("Pragma")
                        .cacheControl(cacheControl)
                        .build();
            }

            return chain.proceed(request);
        };
    }

    public static LoklakAPI getLoklakAPI() {
        if (loklakAPI == null) {
            OkHttpClient okHttpClient = okHttpClientBuilder.addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .addInterceptor(provideOfflineCacheInterceptor())
                    .addNetworkInterceptor(provideCacheInterceptor())
                    .cache(provideCache())
                    .build();

            retrofitBuilder.client(okHttpClient);

            loklakAPI = retrofitBuilder.baseUrl(Urls.LOKLAK_BASE_URL)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(LoklakAPI.class);
        }
        return loklakAPI;
    }
}
