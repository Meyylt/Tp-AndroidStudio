package com.example.runcorse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RunViewModel(private val repository: RunRepository) : ViewModel() {

    // Transforme le Flow de Room en StateFlow pour Compose
    val runs: StateFlow<List<RunEntity>> = repository.allRuns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addRun(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.insertRun(text)
            }
        }
    }
}

// "L'usine" pour pouvoir passer le Repository au ViewModel
class RunViewModelFactory(private val repository: RunRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RunViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel inconnu")
    }
}