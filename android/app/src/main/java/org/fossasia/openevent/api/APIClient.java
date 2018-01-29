package org.fossasia.openevent.api;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.fossasia.openevent.BuildConfig;
import org.fossasia.openevent.api.network.OpenEventAPI;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * User: mohit
 * Date: 25/5/15
 */
public final class APIClient {
    /**
     * This is the base url can be changed via a config Param
     * Or Build Config
     */

    private static final int CONNECT_TIMEOUT_MILLIS = 20 * 1000; // 15s

    private static final int READ_TIMEOUT_MILLIS = 50 * 1000; // 20s

    private final OpenEventAPI openEventAPI;

    public APIClient() {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient().newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        if(BuildConfig.DEBUG)
            okHttpClientBuilder.addNetworkInterceptor(new StethoInterceptor());

        OkHttpClient okHttpClient = okHttpClientBuilder.addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Urls.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        openEventAPI = retrofit.create(OpenEventAPI.class);
    }

    public OpenEventAPI getOpenEventAPI() {

        return openEventAPI;
    }


}
