package com.example.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FileRepository
import com.example.data.SettingsRepository
import com.example.models.FileModel
import com.example.network.OpenAiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repository: FileRepository,
    val settings: SettingsRepository
) : ViewModel() {

    private val _files = MutableStateFlow<List<FileModel>>(emptyList())
    val files: StateFlow<List<FileModel>> = _files.asStateFlow()

    private val _currentFile = MutableStateFlow<FileModel?>(null)
    val currentFile: StateFlow<FileModel?> = _currentFile.asStateFlow()

    private val _fileContent = MutableStateFlow(TextFieldValue())
    val fileContent: StateFlow<TextFieldValue> = _fileContent.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        val list = repository.getFiles()
        _files.value = list
        if (list.isNotEmpty() && _currentFile.value == null) {
            openFile(list.first())
        }
    }

    fun openFile(file: FileModel) {
        saveCurrentFile()
        _currentFile.value = file
        val content = repository.readFile(file.file)
        _fileContent.value = TextFieldValue(text = content)
        _isPreviewMode.value = false
    }

    fun updateContent(newContent: TextFieldValue) {
        _fileContent.value = newContent
    }

    fun saveCurrentFile() {
        val file = _currentFile.value?.file ?: return
        repository.saveFile(file, _fileContent.value.text)
    }

    fun createFile(name: String, parentDir: FileModel? = null) {
        repository.createFile(name, parentDir?.file)
        loadFiles()
    }

    fun createFolder(name: String, parentDir: FileModel? = null) {
        repository.createFolder(name, parentDir?.file)
        loadFiles()
    }

    fun deleteFile(fileModel: FileModel) {
        if (_currentFile.value?.path == fileModel.path) {
            _currentFile.value = null
            _fileContent.value = TextFieldValue()
        }
        repository.deleteFile(fileModel.file)
        loadFiles()
    }

    fun moveFile(source: FileModel, targetDir: FileModel) {
        repository.moveFile(source.file, targetDir.file)
        loadFiles()
    }

    fun importFile(uri: android.net.Uri) {
        repository.importFile(uri)
        loadFiles()
    }

    fun exportCurrentFile(): String? {
        val file = _currentFile.value?.file ?: return null
        return repository.exportFile(file)
    }

    fun togglePreview() {
        saveCurrentFile()
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun askAi(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = "Thinking..."
            
            val systemPrompt = """
                Kamu adalah Lucientness AI, asisten koding cerdas. 
                Gunakan bahasa Indonesia.
                Jawab pertanyaan pengguna mengenai file yang sedang dibuka dengan tepat dan singkat.
                
                File saat ini: ${_currentFile.value?.name ?: "Tidak ada file"}
                Isi file:
                ${_fileContent.value.text}
            """.trimIndent()

            val response = OpenAiClient.generateContent(settings, systemPrompt, prompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }
}

class EditorViewModelFactory(
    private val repository: FileRepository,
    private val settings: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(repository, settings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
