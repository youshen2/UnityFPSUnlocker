package io.github.hexstr.UnityFPSUnlocker.logging;

import android.app.Application;
import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.hexstr.UnityFPSUnlocker.BuildConfig;
import io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract;

public final class HookEventDispatcher {
    private static final int MAX_PENDING_EVENTS = 64;
    private static final Object LOCK = new Object();
    private static final ArrayDeque<PendingEvent> PENDING_EVENTS = new ArrayDeque<>();

    private static WeakReference<Context> eventContext = new WeakReference<>(null);
    private static String eventToken;
    private static boolean contextHookInstalled;

    private HookEventDispatcher() {}

    public static void setToken(XSharedPreferences settings) {
        synchronized (LOCK) {
            eventToken = settings == null
                    ? null
                    : settings.getString(PreferenceContract.KEY_HOOK_LOG_TOKEN, null);
        }
    }

    public static void installContextHook() {
        synchronized (LOCK) {
            if (contextHookInstalled) return;
            contextHookInstalled = true;
        }
        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            setContext((Context) param.args[0]);
                        }
                    });
        } catch (Throwable error) {
            XposedBridge.log(error);
        }
    }

    public static void setContext(Context context) {
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) applicationContext = context;

        List<PendingEvent> pendingEvents;
        synchronized (LOCK) {
            eventContext = new WeakReference<>(applicationContext);
            pendingEvents = new ArrayList<>(PENDING_EVENTS);
            PENDING_EVENTS.clear();
        }
        for (PendingEvent event : pendingEvents) {
            send(applicationContext, event);
        }
    }

    public static void record(String packageName, String status, String message) {
        PendingEvent event = new PendingEvent(packageName, status, message);
        Context context;
        synchronized (LOCK) {
            context = eventContext.get();
            if (context == null) {
                if (PENDING_EVENTS.size() == MAX_PENDING_EVENTS) {
                    PENDING_EVENTS.removeFirst();
                }
                PENDING_EVENTS.addLast(event);
                return;
            }
        }
        send(context, event);
    }

    private static void send(Context context, PendingEvent event) {
        try {
            String token;
            synchronized (LOCK) {
                token = eventToken;
            }
            if (token == null || token.isEmpty()) return;

            Intent intent = new Intent(HookLogContract.ACTION)
                    .setComponent(new ComponentName(
                            BuildConfig.APPLICATION_ID,
                            HookLogContract.RECEIVER_CLASS))
                    .putExtra(HookLogContract.EXTRA_PROTOCOL_TOKEN, token)
                    .putExtra(HookLogContract.EXTRA_PACKAGE_NAME, event.packageName)
                    .putExtra(HookLogContract.EXTRA_STATUS, event.status)
                    .putExtra(HookLogContract.EXTRA_MESSAGE, event.message);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.sendBroadcast(
                        intent,
                        null,
                        BroadcastOptions.makeBasic()
                                .setShareIdentityEnabled(true)
                                .toBundle());
            } else {
                context.sendBroadcast(intent);
            }
        } catch (Throwable error) {
            XposedBridge.log(error);
        }
    }

    private static final class PendingEvent {
        final String packageName;
        final String status;
        final String message;

        PendingEvent(String packageName, String status, String message) {
            this.packageName = packageName;
            this.status = status;
            this.message = message;
        }
    }
}
