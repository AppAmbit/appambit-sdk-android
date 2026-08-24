package com.appambitsdk.test.unit

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.appambit.sdk.Analytics
import com.appambit.sdk.AppAmbit
import com.appambit.sdk.BreadcrumbManager
import com.appambit.sdk.ServiceLocator
import com.appambit.sdk.services.ApplicationInfoService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for the crash reported in production:
 *
 *   java.lang.NullPointerException: Attempt to invoke interface method
 *   'void java.util.concurrent.ExecutorService.execute(java.lang.Runnable)' on a null object reference
 *     at com.appambit.sdk.BreadcrumbManager.saveToFile (BreadcrumbManager.java:69)
 *     at com.appambit.sdk.AppAmbit.onEnd (AppAmbit.java:270)
 *     at com.appambit.sdk.AppAmbit$1.onActivityStopped (AppAmbit.java:153)
 *
 * AppAmbit.start() used to register the ActivityLifecycleCallbacks *before* wiring the
 * services, so a failure during initialization left the callbacks live while
 * BreadcrumbManager was still uninitialized; backgrounding the app then killed the process.
 */
class BreadcrumbLifecycleCrashTest {

    private lateinit var context: Application
    private lateinit var appContext: Application
    private val lifecycleSlot = slot<Application.ActivityLifecycleCallbacks>()
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setup() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run(); true
        }
        every { anyConstructed<Handler>().postDelayed(any(), any()) } returns true
        every { anyConstructed<Handler>().removeCallbacks(any()) } just runs

        mockkConstructor(NetworkRequest.Builder::class)
        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns mockk(relaxed = true)
        every { anyConstructed<NetworkRequest.Builder>().build() } returns mockk(relaxed = true)

        appContext = mockk(relaxed = true)
        every { appContext.registerActivityLifecycleCallbacks(capture(lifecycleSlot)) } just runs
        every { appContext.filesDir } returns java.io.File(System.getProperty("java.io.tmpdir")!!)

        context = mockk(relaxed = true)
        every { context.applicationContext } returns appContext
        every { context.filesDir } returns java.io.File(System.getProperty("java.io.tmpdir")!!)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockk<ConnectivityManager>(relaxed = true)

        // Clean SDK static state: nothing initialized yet, automatic sessions on.
        setStaticField(BreadcrumbManager::class.java, "mExecutorService", null)
        setStaticField(BreadcrumbManager::class.java, "mApiService", null)
        setStaticField(BreadcrumbManager::class.java, "mStorageService", null)
        setStaticField(Analytics::class.java, "isManualSessionEnabled", false)
        setStaticField(AppAmbit::class.java, "isInitialized", false)
        setStaticField(AppAmbit::class.java, "startedActivities", 0)
        setStaticField(AppAmbit::class.java, "resumedActivities", 0)
        setStaticField(AppAmbit::class.java, "foreground", false)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    /**
     * Root cause: when initialization fails, no lifecycle callback may survive it. The SDK
     * stays inert instead of crashing the host app on the next backgrounding.
     */
    @Test
    fun `a failed start registers no lifecycle callbacks`() {
        mockkStatic(ServiceLocator::class)
        every { ServiceLocator.initialize(any()) } throws RuntimeException("SQLite disk I/O error")

        try {
            AppAmbit.start(context, "TEST_KEY")
        } catch (ignored: Throwable) {
            // Host app swallows SDK init failures.
        }

        verify(exactly = 0) { appContext.registerActivityLifecycleCallbacks(any()) }
        assertFalse("the SDK must not report itself as initialized", AppAmbit.isInitialized())
        assertFalse("no callbacks were captured", lifecycleSlot.isCaptured)
    }

    /**
     * Defense in depth: saveToFile was the only public BreadcrumbManager entry point without
     * the "not initialized yet" guard that every sibling already had.
     */
    @Test
    fun `saveToFile no-ops when uninitialized just like its siblings`() {
        BreadcrumbManager.addAsync("On Pause")
        BreadcrumbManager.sendBatchBreadcrumbs()
        BreadcrumbManager.loadBreadcrumbsFromFileAsync(null)
        BreadcrumbManager.saveToFile("On Pause")   // used to throw NullPointerException
    }

    /**
     * The trigger seen in the field: PackageInfoHelper returns null when the PackageManager
     * is unavailable, and `assert` is a no-op on Android, so ApplicationInfoService used to
     * abort ServiceLocator initialization with an NPE.
     */
    @Test
    fun `ApplicationInfoService survives a missing PackageManager`() {
        val brokenContext = mockk<Context>(relaxed = true)
        every { brokenContext.packageManager } returns null as PackageManager?

        val info = ApplicationInfoService(brokenContext)

        assertEquals("", info.appVersion)
    }

    /**
     * The ordering invariant the fix relies on: by the time the SDK hands its
     * ActivityLifecycleCallbacks to the Application, every facade a callback can reach is
     * already wired. Moving registerLifecycleObserver() after onStartApp() must not make the
     * SDK miss the current activity either -- start() is synchronous, so registration still
     * happens inside the same call, before the Looper can dispatch any lifecycle event.
     */
    @Test
    fun `services are wired before the lifecycle callbacks are registered`() {
        val inlineExecutor = mockk<java.util.concurrent.ExecutorService>(relaxed = true)
        every { inlineExecutor.execute(any()) } answers { firstArg<Runnable>().run() }

        mockkStatic(ServiceLocator::class)
        every { ServiceLocator.initialize(any()) } just runs
        every { ServiceLocator.getExecutorService() } returns inlineExecutor
        every { ServiceLocator.getStorageService() } returns mockk(relaxed = true)
        every { ServiceLocator.getApiService() } returns mockk(relaxed = true)
        every { ServiceLocator.getAppInfoService() } returns mockk(relaxed = true)
        every { ServiceLocator.getDbService() } returns mockk(relaxed = true)
        every { ServiceLocator.getCloudCodeService() } returns mockk(relaxed = true)

        var breadcrumbsReadyAtRegistration: Boolean? = null
        every { appContext.registerActivityLifecycleCallbacks(capture(lifecycleSlot)) } answers {
            breadcrumbsReadyAtRegistration = readStaticField(
                BreadcrumbManager::class.java, "mExecutorService"
            ) != null
        }

        AppAmbit.start(context, "TEST_KEY")

        assertTrue("registerActivityLifecycleCallbacks was never called", lifecycleSlot.isCaptured)
        assertEquals(
            "BreadcrumbManager must already be initialized when the callbacks go live",
            true,
            breadcrumbsReadyAtRegistration
        )
        assertTrue("the SDK reports itself as initialized", AppAmbit.isInitialized())
    }

    private fun readStaticField(clazz: Class<*>, fieldName: String): Any? {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(null)
    }

    private fun setStaticField(clazz: Class<*>, fieldName: String, value: Any?) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, value)
    }
}
