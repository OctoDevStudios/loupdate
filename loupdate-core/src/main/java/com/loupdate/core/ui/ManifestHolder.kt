package com.loupdate.core.ui

import com.loupdate.core.UpdateManifest

internal object ManifestHolder {
    @Volatile
    var current: UpdateManifest? = null
}
