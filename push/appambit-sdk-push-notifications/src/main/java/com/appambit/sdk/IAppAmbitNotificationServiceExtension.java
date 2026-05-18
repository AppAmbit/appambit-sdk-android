package com.appambit.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import com.appambit.sdk.models.AppAmbitNotification;

public interface IAppAmbitNotificationServiceExtension {

    void onNotificationForeground(@NonNull AppAmbitNotification notification);

    void onNotificationBackground(@NonNull AppAmbitNotification notification);

    default void onNotificationForeground(@NonNull Context context, @NonNull AppAmbitNotification notification) {
        onNotificationForeground(notification);
    }
    
    default void onNotificationBackground(@NonNull Context context, @NonNull AppAmbitNotification notification) {
        onNotificationBackground(notification);
    }
}
