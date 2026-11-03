package br.com.eibragaa.nayplanos.data.updater

data class GithubReleaseMetadata(
    val version: String,
    val apk: ReleaseAsset,
    val sha256: String,
    val signature: ReleaseAsset,
    /** GitHub TLS/origin validation is not a cryptographic signature verification. */
    val metadataTrusted: Boolean = true,
    val signatureVerified: Boolean = false,
)

data class ReleaseAsset(val name: String, val downloadUrl: String)
