package com.example.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

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

@Composable
inline fun <reified VM : ViewModel> takoFlowViewModel(noinline creator: () -> VM): VM {
    val factory = remember { TakoFlowViewModelFactory(creator) }
    return viewModel(factory = factory)
}
