package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TakoFlowViewModelFactory<T : ViewModel>(
    private val creator: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
        val viewModel = creator()
        require(modelClass.isInstance(viewModel)) {
            "Factory created ${viewModel::class.java.name} for ${modelClass.name}."
        }
        return viewModel as VM
    }
}
