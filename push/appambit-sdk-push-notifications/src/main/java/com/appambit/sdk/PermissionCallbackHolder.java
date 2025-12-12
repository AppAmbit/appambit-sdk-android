package com.appambit.sdk;

final class PermissionCallbackHolder {
    private static final PermissionCallbackHolder INSTANCE = new PermissionCallbackHolder();
    private PushKernel.PermissionListener listener;

    private PermissionCallbackHolder() {}

    public static PermissionCallbackHolder getInstance() {
        return INSTANCE;
    }

    public void setListener(PushKernel.PermissionListener listener) {
        this.listener = listener;
    }

    public PushKernel.PermissionListener getListener() {
        return listener;
    }

    public void clearListener() {
        this.listener = null;
    }
}
