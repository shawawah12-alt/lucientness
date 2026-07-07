package com.example.data

import android.content.Context
import com.example.models.FileModel
import java.io.File

class FileRepository(private val context: Context) {
    private val rootDir = File(context.filesDir, "lucientness_projects")

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
            // Create a default file
            val defaultIndex = File(rootDir, "index.html")
            defaultIndex.writeText("<!DOCTYPE html>\n<html>\n<head>\n  <title>Lucientness</title>\n  <style>\n    body { background: #0F0E13; color: #DFDCE3; font-family: monospace; }\n  </style>\n</head>\n<body>\n  <h1>Hello Lucientness</h1>\n</body>\n</html>")
            
            val defaultJs = File(rootDir, "script.js")
            defaultJs.writeText("console.log('Lucientness initialized');\nfunction test() {\n  return true;\n}")
        }
    }

    fun getFiles(): List<FileModel> {
        val result = mutableListOf<FileModel>()
        fun traverse(dir: File, level: Int) {
            val sortedFiles = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            sortedFiles?.forEach {
                result.add(FileModel(it.name, it.absolutePath, it.isDirectory, it, level))
                if (it.isDirectory) {
                    traverse(it, level + 1)
                }
            }
        }
        traverse(rootDir, 0)
        return result
    }

    fun readFile(file: File): String {
        return if (file.exists() && !file.isDirectory) file.readText() else ""
    }

    fun saveFile(file: File, content: String) {
        if (!file.isDirectory) {
            file.writeText(content)
        }
    }

    fun createFile(name: String, parentDir: File? = null) {
        val parent = parentDir ?: rootDir
        val newFile = File(parent, name)
        if (!newFile.exists()) {
            newFile.createNewFile()
        }
    }

    fun createFolder(name: String, parentDir: File? = null) {
        val parent = parentDir ?: rootDir
        val newFolder = File(parent, name)
        if (!newFolder.exists()) {
            newFolder.mkdirs()
        }
    }

    fun deleteFile(file: File) {
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun moveFile(source: File, destDir: File) {
        if (source.exists() && destDir.isDirectory) {
            source.renameTo(File(destDir, source.name))
        }
    }
}
