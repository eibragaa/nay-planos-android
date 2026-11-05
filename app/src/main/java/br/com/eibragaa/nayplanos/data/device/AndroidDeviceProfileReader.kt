package br.com.eibragaa.nayplanos.data.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import br.com.eibragaa.nayplanos.domain.device.DeviceProfile

/** Lê RAM, ABI, núcleos e armazenamento livre do aparelho. Sem permissões especiais. */
class AndroidDeviceProfileReader(private val context: Context) {
    fun read(): DeviceProfile {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(context.filesDir.path)
        return DeviceProfile(
            totalRamMb = memory.totalMem / BYTES_PER_MB,
            availableRamMb = memory.availMem / BYTES_PER_MB,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            freeStorageMb = storage.availableBytes / BYTES_PER_MB,
        )
    }

    private companion object {
        const val BYTES_PER_MB = 1_048_576L
    }
}
