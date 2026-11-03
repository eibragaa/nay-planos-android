package br.com.eibragaa.nayplanos.domain.updater

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseUpdatePolicyTest {
    @Test fun acceptsTrustedHigherVersion() {
        assertEquals(UpdateDecision.UpdateAvailable, ReleaseUpdatePolicy.evaluate("1.2.3", candidate("1.3.0")))
    }

    @Test fun equalVersionIsUpToDate() {
        assertEquals(UpdateDecision.UpToDate, ReleaseUpdatePolicy.evaluate("1.2.3", candidate("1.2.3")))
    }

    @Test fun blocksDowngrade() {
        assertBlocked(BlockReason.DOWNGRADE, "1.2.3", candidate("1.2.2"))
    }

    @Test fun blocksInvalidVersion() {
        assertBlocked(BlockReason.INVALID_RELEASE_VERSION, "1.0.0", candidate("1.0"))
    }

    @Test fun blocksMissingApk() {
        assertBlocked(BlockReason.INVALID_APK_ASSET, "1.0.0", candidate("1.1.0").copy(apkAssetName = null))
    }

    @Test fun blocksMissingDigest() {
        assertBlocked(BlockReason.MISSING_SHA256, "1.0.0", candidate("1.1.0").copy(sha256 = null))
    }

    @Test fun blocksUntrustedMetadataAndUnverifiedSignature() {
        assertBlocked(BlockReason.UNTRUSTED_METADATA, "1.0.0", candidate("1.1.0").copy(metadataTrusted = false))
        assertBlocked(BlockReason.UNVERIFIED_SIGNATURE, "1.0.0", candidate("1.1.0").copy(signatureVerified = false))
    }

    @Test fun blocksExternalAssetUrl() {
        assertBlocked(BlockReason.INVALID_APK_URL, "1.0.0", candidate("1.1.0").copy(apkUrl = "https://example.com/nay-planos-1.1.0.apk"))
    }

    private fun assertBlocked(reason: BlockReason, current: String, target: ReleaseCandidate) {
        assertEquals(UpdateDecision.Blocked(reason), ReleaseUpdatePolicy.evaluate(current, target))
    }

    private fun candidate(version: String) = ReleaseCandidate(
        version = version,
        apkAssetName = "nay-planos-$version.apk",
        apkUrl = "https://github.com/eibragaa/nay-planos-android/releases/download/v$version/nay-planos-$version.apk",
        sha256 = "a".repeat(64),
        metadataTrusted = true,
        signatureVerified = true,
    )
}
