package io.sentry.android.gradle.instrumentation.binder

data class BinderMethodSpec(
  val owner: String,
  val name: String,
  val component: String,
  val isStatic: Boolean = false,
)

object BinderMethodRegistry {

  private val registry: Map<String, List<BinderMethodSpec>> = buildRegistry()

  fun lookup(owner: String, name: String): BinderMethodSpec? {
    val specs = registry[owner] ?: return null
    return specs.find { it.name == name }
  }

  @Suppress("LongMethod")
  private fun buildRegistry(): Map<String, List<BinderMethodSpec>> {
    val specs = mutableListOf<BinderMethodSpec>()

    specs.addAll(
      "android/content/ContentResolver",
      "ContentResolver",
      "query",
      "insert",
      "update",
      "delete",
      "call",
      "bulkInsert",
      "openInputStream",
      "openOutputStream",
      "openAssetFileDescriptor",
      "openFileDescriptor",
      "acquireContentProviderClient",
      "registerContentObserver",
      "getType",
    )

    specs.addAll(
      "android/content/pm/PackageManager",
      "PackageManager",
      "getInstalledPackages",
      "getPackageInfo",
      "resolveActivity",
      "queryIntentActivities",
      "getInstalledApplications",
      "resolveService",
      "queryIntentServices",
      "getApplicationInfo",
      "getActivityInfo",
      "getServiceInfo",
      "getReceiverInfo",
      "getProviderInfo",
      "checkPermission",
      "hasSystemFeature",
      "getLaunchIntentForPackage",
      "getComponentEnabledSetting",
      "setComponentEnabledSetting",
      "getPackagesForUid",
      "getInstallerPackageName",
      "getInstallSourceInfo",
    )

    for (settingsClass in
      listOf(
        "android/provider/Settings\$Secure",
        "android/provider/Settings\$Global",
        "android/provider/Settings\$System",
      )) {
      val component = "Settings." + settingsClass.substringAfterLast("\$")
      for (method in listOf("getString", "getInt", "getLong", "getFloat", "putString", "putInt")) {
        specs.add(BinderMethodSpec(settingsClass, method, component, isStatic = true))
      }
    }

    for (ctx in listOf("android/content/Context", "android/content/ContextWrapper")) {
      specs.addAll(
        ctx,
        "Context",
        "startService",
        "stopService",
        "bindService",
        "unbindService",
        "sendBroadcast",
        "sendOrderedBroadcast",
        "startActivity",
        "startActivities",
        "startForegroundService",
        "registerReceiver",
        "unregisterReceiver",
        "checkSelfPermission",
        "checkPermission",
      )
    }

    specs.addAll(
      "android/net/ConnectivityManager",
      "ConnectivityManager",
      "getActiveNetworkInfo",
      "getActiveNetwork",
      "getNetworkCapabilities",
      "getAllNetworks",
      "isActiveNetworkMetered",
      "registerDefaultNetworkCallback",
      "registerNetworkCallback",
    )

    specs.addAll(
      "android/app/ActivityManager",
      "ActivityManager",
      "getRunningAppProcesses",
      "getMemoryInfo",
      "getRunningServices",
      "getProcessMemoryInfo",
    )

    specs.addAll(
      "android/view/inputmethod/InputMethodManager",
      "InputMethodManager",
      "showSoftInput",
      "hideSoftInputFromWindow",
      "restartInput",
      "isActive",
    )

    specs.addAll(
      "android/hardware/camera2/CameraManager",
      "CameraManager",
      "getCameraIdList",
      "getCameraCharacteristics",
      "openCamera",
    )

    specs.addAll(
      "android/os/PowerManager",
      "PowerManager",
      "isInteractive",
      "isDeviceIdleMode",
      "isPowerSaveMode",
    )
    specs.addAll("android/os/PowerManager\$WakeLock", "PowerManager.WakeLock", "acquire", "release")

    specs.addAll(
      "android/location/LocationManager",
      "LocationManager",
      "getLastKnownLocation",
      "requestLocationUpdates",
      "getProviders",
    )

    specs.addAll(
      "android/telephony/TelephonyManager",
      "TelephonyManager",
      "getDeviceId",
      "getNetworkOperator",
      "getSimOperator",
      "getNetworkOperatorName",
      "getSimOperatorName",
      "getLine1Number",
      "getSubscriberId",
      "getNetworkType",
      "getDataNetworkType",
      "getAllCellInfo",
    )

    specs.addAll(
      "android/net/wifi/WifiManager",
      "WifiManager",
      "getConnectionInfo",
      "isWifiEnabled",
      "setWifiEnabled",
      "getScanResults",
      "startScan",
      "getConfiguredNetworks",
      "addNetwork",
      "removeNetwork",
      "disconnect",
      "reconnect",
      "reassociate",
      "getDhcpInfo",
      "getWifiState",
    )

    specs.addAll(
      "android/bluetooth/BluetoothAdapter",
      "BluetoothAdapter",
      "isEnabled",
      "getState",
      "getName",
      "getAddress",
      "getBondedDevices",
      "startDiscovery",
      "cancelDiscovery",
      "isDiscovering",
      "enable",
      "disable",
      "getScanMode",
      "setScanMode",
    )

    specs.addAll(
      "android/bluetooth/BluetoothDevice",
      "BluetoothDevice",
      "getName",
      "getBondState",
      "getType",
      "createBond",
      "removeBond",
      "connectGatt",
      "getBatteryLevel",
      "getUuids",
    )

    specs.addAll(
      "android/bluetooth/BluetoothGatt",
      "BluetoothGatt",
      "connect",
      "disconnect",
      "discoverServices",
      "readCharacteristic",
      "writeCharacteristic",
      "readDescriptor",
      "writeDescriptor",
      "readRemoteRssi",
      "requestMtu",
    )

    specs.addAll(
      "android/bluetooth/BluetoothManager",
      "BluetoothManager",
      "getConnectedDevices",
      "getConnectionState",
      "getDevicesMatchingConnectionStates",
      "openGattServer",
    )

    specs.addAll(
      "android/media/AudioManager",
      "AudioManager",
      "getStreamVolume",
      "getStreamMaxVolume",
      "setStreamVolume",
      "getRingerMode",
      "setRingerMode",
      "requestAudioFocus",
      "abandonAudioFocus",
      "getMode",
      "setMode",
      "isMusicActive",
      "isBluetoothA2dpOn",
      "isBluetoothScoOn",
    )

    specs.addAll(
      "android/content/ClipboardManager",
      "ClipboardManager",
      "getPrimaryClip",
      "setPrimaryClip",
      "hasPrimaryClip",
    )

    specs.addAll(
      "android/app/NotificationManager",
      "NotificationManager",
      "notify",
      "cancel",
      "cancelAll",
      "getActiveNotifications",
    )

    specs.addAll(
      "android/app/AlarmManager",
      "AlarmManager",
      "set",
      "setExact",
      "setRepeating",
      "setWindow",
      "cancel",
    )

    specs.addAll(
      "android/app/KeyguardManager",
      "KeyguardManager",
      "isKeyguardLocked",
      "isDeviceLocked",
      "isKeyguardSecure",
    )

    specs.addAll(
      "android/accounts/AccountManager",
      "AccountManager",
      "getAccounts",
      "getAccountsByType",
      "getAuthToken",
    )

    specs.addAll("android/os/UserManager", "UserManager", "getUserProfiles", "isUserUnlocked")

    specs.addAll(
      "android/hardware/display/DisplayManager",
      "DisplayManager",
      "getDisplays",
      "getDisplay",
    )

    specs.addAll(
      "android/os/Vibrator",
      "Vibrator",
      "vibrate",
      "cancel",
      "hasVibrator",
      "hasAmplitudeControl",
    )

    specs.addAll(
      "android/os/VibratorManager",
      "VibratorManager",
      "getVibratorIds",
      "getDefaultVibrator",
      "vibrate",
      "cancel",
    )

    specs.addAll(
      "android/app/job/JobScheduler",
      "JobScheduler",
      "schedule",
      "enqueue",
      "cancel",
      "cancelAll",
      "getAllPendingJobs",
      "getPendingJob",
    )

    specs.addAll(
      "android/content/pm/ShortcutManager",
      "ShortcutManager",
      "setDynamicShortcuts",
      "addDynamicShortcuts",
      "removeDynamicShortcuts",
      "removeAllDynamicShortcuts",
      "getDynamicShortcuts",
      "getPinnedShortcuts",
      "updateShortcuts",
      "requestPinShortcut",
      "isRequestPinShortcutSupported",
      "pushDynamicShortcut",
    )

    specs.addAll(
      "android/app/AppOpsManager",
      "AppOpsManager",
      "checkOp",
      "checkOpNoThrow",
      "noteOp",
      "noteOpNoThrow",
      "noteProxyOp",
      "noteProxyOpNoThrow",
      "startOp",
      "startOpNoThrow",
      "finishOp",
      "checkPackage",
    )

    specs.addAll(
      "android/os/storage/StorageManager",
      "StorageManager",
      "getStorageVolumes",
      "getPrimaryStorageVolume",
      "getAllocatableBytes",
      "getCacheSizeBytes",
    )

    specs.addAll(
      "android/telephony/SubscriptionManager",
      "SubscriptionManager",
      "getActiveSubscriptionInfoList",
      "getActiveSubscriptionInfo",
      "getActiveSubscriptionInfoCount",
      "getActiveSubscriptionInfoForSimSlotIndex",
    )

    specs.addAll(
      "android/app/WallpaperManager",
      "WallpaperManager",
      "getDrawable",
      "peekDrawable",
      "getWallpaperColors",
      "setResource",
      "setBitmap",
      "getDesiredMinimumWidth",
      "getDesiredMinimumHeight",
      "isWallpaperSupported",
      "isSetWallpaperAllowed",
    )

    val strictMode = "android/os/StrictMode"
    for (method in
      listOf(
        "setThreadPolicy",
        "setThreadPolicyMask",
        "allowThreadDiskWrites",
        "allowThreadDiskReads",
        "allowThreadViolations",
      )) {
      specs.add(BinderMethodSpec(strictMode, method, "StrictMode", isStatic = true))
    }

    return specs.groupBy { it.owner }
  }

  private fun MutableList<BinderMethodSpec>.addAll(
    owner: String,
    component: String,
    vararg methods: String,
  ) {
    for (method in methods) {
      add(BinderMethodSpec(owner, method, component))
    }
  }
}
