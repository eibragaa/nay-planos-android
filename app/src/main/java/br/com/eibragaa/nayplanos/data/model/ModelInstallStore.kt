package br.com.eibragaa.nayplanos.data.model

import android.content.Context
import br.com.eibragaa.nayplanos.domain.device.LocalModel
import java.io.File

/** Diretório privado dos modelos instalados. Não é acessível por outros apps. */
class ModelInstallStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "models")

    fun modelPath(model: LocalModel): File = File(installDir(model), model.model.fileName)

    fun mmprojPath(model: LocalModel): File = File(installDir(model), model.mmproj.fileName)

    /** Instalado quando os dois arquivos existem com o tamanho exato publicado. */
    fun isInstalled(model: LocalModel): Boolean =
        modelPath(model).length() == model.model.sizeBytes &&
            mmprojPath(model).length() == model.mmproj.sizeBytes

    fun installDir(model: LocalModel): File = File(root, model.id)

    fun delete(model: LocalModel) {
        installDir(model).deleteRecursively()
    }

    fun ensureInstallDir(model: LocalModel): File = installDir(model).apply { mkdirs() }
}
