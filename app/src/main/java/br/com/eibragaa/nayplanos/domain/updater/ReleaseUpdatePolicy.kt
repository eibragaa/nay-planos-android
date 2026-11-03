package br.com.eibragaa.nayplanos.domain.updater

/** Dados de uma release depois de obtidos pelo cliente e validados quanto à origem. */
data class ReleaseCandidate(
    val version: String,
    val apkAssetName: String?,
    val apkUrl: String?,
    val sha256: String?,
    val metadataTrusted: Boolean,
    val signatureVerified: Boolean,
)

sealed interface UpdateDecision {
    data object UpdateAvailable : UpdateDecision
    data object UpToDate : UpdateDecision
    data class Blocked(val reason: BlockReason) : UpdateDecision
}

enum class BlockReason {
    INVALID_CURRENT_VERSION,
    INVALID_RELEASE_VERSION,
    DOWNGRADE,
    INVALID_APK_ASSET,
    INVALID_APK_URL,
    MISSING_SHA256,
    INVALID_SHA256,
    UNTRUSTED_METADATA,
    UNVERIFIED_SIGNATURE,
}

/** Regras puras de aceitação. Esta política nunca inicia download nem instalação. */
object ReleaseUpdatePolicy {
    private val versionPattern = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")
    private val apkNamePattern = Regex("^nay-planos-([0-9]+\\.[0-9]+\\.[0-9]+)\\.apk$")

    fun evaluate(currentVersion: String, release: ReleaseCandidate): UpdateDecision {
        val current = parseVersion(currentVersion)
            ?: return UpdateDecision.Blocked(BlockReason.INVALID_CURRENT_VERSION)
        val target = parseVersion(release.version)
            ?: return UpdateDecision.Blocked(BlockReason.INVALID_RELEASE_VERSION)
        if (compare(target, current) < 0) return UpdateDecision.Blocked(BlockReason.DOWNGRADE)

        val apkName = release.apkAssetName
            ?: return UpdateDecision.Blocked(BlockReason.INVALID_APK_ASSET)
        val apkVersion = apkNamePattern.matchEntire(apkName)?.groupValues?.get(1)
        if (apkVersion != release.version) return UpdateDecision.Blocked(BlockReason.INVALID_APK_ASSET)

        val url = release.apkUrl
            ?: return UpdateDecision.Blocked(BlockReason.INVALID_APK_URL)
        if (!isTrustedGithubAssetUrl(url, apkName)) {
            return UpdateDecision.Blocked(BlockReason.INVALID_APK_URL)
        }
        if (release.sha256.isNullOrBlank()) return UpdateDecision.Blocked(BlockReason.MISSING_SHA256)
        if (!sha256Pattern.matches(release.sha256)) return UpdateDecision.Blocked(BlockReason.INVALID_SHA256)
        if (!release.metadataTrusted) return UpdateDecision.Blocked(BlockReason.UNTRUSTED_METADATA)
        if (!release.signatureVerified) return UpdateDecision.Blocked(BlockReason.UNVERIFIED_SIGNATURE)

        return if (compare(target, current) == 0) UpdateDecision.UpToDate else UpdateDecision.UpdateAvailable
    }

    private fun parseVersion(value: String): List<Long>? {
        val match = versionPattern.matchEntire(value) ?: return null
        return match.groupValues.drop(1).mapNotNull { it.toLongOrNull() }.takeIf { it.size == 3 }
    }

    private fun compare(left: List<Long>, right: List<Long>): Int {
        for (index in 0..2) {
            val comparison = left[index].compareTo(right[index])
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun isTrustedGithubAssetUrl(value: String, assetName: String): Boolean = try {
        val uri = java.net.URI(value)
        uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null &&
            uri.port == -1 && uri.query == null && uri.fragment == null &&
            uri.path.startsWith("/eibragaa/nay-planos-android/releases/download/") &&
            uri.path.endsWith("/$assetName")
    } catch (_: java.net.URISyntaxException) {
        false
    }
}
