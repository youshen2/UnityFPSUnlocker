# Native core provenance

The FPS limiter, Unity bindings, and xDL sources in this directory are derived
from the `zygisk_module` branch of:

https://github.com/youshen2/UnityFPSUnlocker

Imported from commit `0f93f81f63b86337cb32c849676ad47c37972a4b`.

The Android app uses a dedicated JNI entry point and a reduced configuration
implementation, so the Xposed library does not depend on Zygisk, Abseil, file
watching, or the JSON configuration used by the Magisk module.
