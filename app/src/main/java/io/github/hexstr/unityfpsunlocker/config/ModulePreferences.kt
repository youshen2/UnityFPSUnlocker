package io.github.hexstr.UnityFPSUnlocker.config

/** 单个应用的策略视图，供设置界面直接使用。 */
data class AppPolicy(
    val packageName: String,
    val managed: Boolean,
    val enabled: Boolean,
    val useGlobal: Boolean,
    val config: ModuleConfig,
)

/**
 * 旧版设置界面的仓储接口适配层，内部完全复用 [ConfigStore]，
 * 因此界面与 Hook 进程始终读写同一组远程首选项。
 */
class ModulePreferences(private val store: ConfigStore) {

    fun readGlobal(): ModuleConfig = store.readGlobal()

    fun writeGlobal(config: ModuleConfig) = store.writeGlobal(config)

    fun onlyManagedApps(): Boolean = store.onlyManagedApps()

    fun setOnlyManagedApps(enabled: Boolean) = store.setOnlyManagedApps(enabled)

    fun managedPackages(): Set<String> = store.managedApps()

    fun enabledManagedCount(): Int = managedPackages().count(store::appEnabled)

    fun readAppPolicy(packageName: String): AppPolicy {
        val managed = store.isManaged(packageName)
        return AppPolicy(
            packageName = packageName,
            managed = managed,
            enabled = if (managed) store.appEnabled(packageName) else true,
            useGlobal = if (managed) store.appUsesGlobal(packageName) else true,
            config = if (managed && !store.appUsesGlobal(packageName)) {
                store.readApp(packageName)
            } else {
                store.readGlobal()
            },
        )
    }

    fun readAppPolicies(packageNames: Iterable<String>): Map<String, AppPolicy> =
        packageNames.associateWith(::readAppPolicy)

    fun writeAppPolicy(policy: AppPolicy) {
        if (!policy.managed) {
            store.removeApp(policy.packageName)
            return
        }
        store.manage(policy.packageName, enabled = policy.enabled)
        store.setAppUsesGlobal(policy.packageName, policy.useGlobal)
        store.writeApp(policy.packageName, policy.config)
    }
}
