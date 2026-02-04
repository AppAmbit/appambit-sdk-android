package com.appambit.sdk;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import androidx.annotation.Nullable;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.remoteConfigs.RemoteConfigEntity;
import com.appambit.sdk.models.responses.RemoteConfigResponse;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.ConsumerService;
import com.appambit.sdk.services.endpoints.RemoteConfigEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class RemoteConfig {

    private static ExecutorService mExecutorService;
    private static ApiService mApiService;
    private static Context mContext;
    private static Storable mStorable;
    private static final String TAG = "RemoteConfig";

    public static void initialize(Context context, ExecutorService executorService, ApiService apiService,
            Storable storable) {
        mContext = context;
        mExecutorService = executorService;
        mApiService = apiService;
        mStorable = storable;
    }

    private static RemoteConfigResponse mRemoteConfig;
    private static Map<String, Object> mDefaults;

    public static void setDefaultsAsync(Map<String, Object> defaults) {
        mDefaults = new HashMap<>(defaults);
    }

    public static void setDefaultsAsync(int resourceId) {
        final AppAmbitTaskFuture<Boolean> future = new AppAmbitTaskFuture<>();

        if (mExecutorService == null || mContext == null) {
            Log.d(TAG, "No initialized services or context");
            future.complete(false);
            return;
        }

        mExecutorService.execute(() -> {
            try (XmlResourceParser parser = mContext.getResources().getXml(resourceId)) {
                Map<String, Object> defaults = new HashMap<>();
                int eventType = parser.getEventType();
                String key = null;
                String value = null;

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String tagName = parser.getName();
                        if ("entry".equals(tagName)) {
                            // Simplified for generic XML: matches
                            // <entry><key>...</key><value>...</value></entry>
                        } else if ("key".equals(tagName)) {
                            key = parser.nextText();
                        } else if ("value".equals(tagName)) {
                            value = parser.nextText();
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("entry".equals(parser.getName())) {
                            if (key != null && value != null) {
                                defaults.put(key, value);
                            }
                            key = null;
                            value = null;
                        }
                    }
                    eventType = parser.next();
                }
                mDefaults = defaults;
                future.complete(true);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing XML defaults", e);
                future.fail(e);
            }
        });

    }

    public static AppAmbitTaskFuture<Boolean> fetch() {
        final AppAmbitTaskFuture<Boolean> future = new AppAmbitTaskFuture<>();

        if (mExecutorService == null || mApiService == null) {
            Log.d(TAG, "No initialized services");
            future.complete(false);
            return future;
        }

        mExecutorService.execute(() -> {
            try {
                ApiResult<RemoteConfigResponse> result = mApiService.executeRequest(new RemoteConfigEndpoint(), RemoteConfigResponse.class);

                if (result.errorType == ApiErrorType.None) {
                    mRemoteConfig = result.data;
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            } catch (Exception e) {
                future.fail(e);
            }
        });

        return future;
    }

    public static AppAmbitTaskFuture<Boolean> activate() {
        final AppAmbitTaskFuture<Boolean> future = new AppAmbitTaskFuture<>();

        if (mExecutorService == null || mStorable == null) {
            future.complete(false);
            return future;
        }

        mExecutorService.execute(() -> {
            boolean activated = false;
            if (mRemoteConfig != null && mRemoteConfig.getConfigs() != null) {
                try {
                    List<RemoteConfigEntity> configList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : mRemoteConfig.getConfigs().entrySet()) {
                        RemoteConfigEntity entity = new RemoteConfigEntity();
                        entity.setId(UUID.randomUUID());
                        entity.setKey(entry.getKey());
                        entity.setValue(String.valueOf(entry.getValue()));
                        configList.add(entity);
                    }
                    mStorable.putConfigs(configList);
                    activated = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error activating remote configs", e);
                }
            }
            future.complete(activated);
        });

        return future;
    }

    public static AppAmbitTaskFuture<Boolean> fetchAndActivate() {
        final AppAmbitTaskFuture<Boolean> future = new AppAmbitTaskFuture<>();

        AppAmbitTaskFuture<Boolean> fetchFuture = fetch();
        fetchFuture.then(fetched -> {
            if (fetched) {
                activate().then(future::complete);
            } else {
                future.complete(false);
            }
        });
        fetchFuture.onError(future::fail);

        return future;
    }

    @Nullable
    public static String getString(String key) {
        Object value = getValue(key);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? String.valueOf(value) : null;
    }

    public static boolean getBoolean(String key) {
        Object value = getValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    public static int getInt(String key) {
        Object value = getValue(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "Error: Integer number couldn't be parsed");
            }
        }
        return 0;
    }

    public static double getDouble(String key) {
        Object value = getValue(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "Error: Double number couldn't be parsed");
            }
        }
        return 0.0;
    }

    @Nullable
    private static Object getValue(String key) {
        if (mStorable != null) {
            String dbValue = mStorable.getConfig(key);
            if (dbValue != null) {
                return dbValue;
            }
        }
        if (mDefaults != null) {
            return mDefaults.get(key);
        }
        return null;
    }

}
