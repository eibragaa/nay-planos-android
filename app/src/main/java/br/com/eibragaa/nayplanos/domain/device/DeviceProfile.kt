package br.com.eibragaa.nayplanos.domain.device

/** Capacidades do aparelho lidas pela camada Android. Valores são medidos, não presumidos. */
data class DeviceProfile(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuAbi: String,
    val cpuCores: Int,
    val freeStorageMb: Long,
)
