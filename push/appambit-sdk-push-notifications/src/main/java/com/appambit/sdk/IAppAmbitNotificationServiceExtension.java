package com.appambit.sdk;

import androidx.annotation.NonNull;
import com.appambit.sdk.models.AppAmbitNotification;

public interface IAppAmbitNotificationServiceExtension {

    void onNotificationForeground(@NonNull AppAmbitNotification notification);

    void onNotificationBackground(@NonNull AppAmbitNotification notification);
}
