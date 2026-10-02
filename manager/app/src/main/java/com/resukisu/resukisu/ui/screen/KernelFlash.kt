package com.resukisu.resukisu.ui.screen.kernelFlash

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resukisu.resukisu.R
import com.resukisu.resukisu.domain.model.FlashProgress
import com.resukisu.resukisu.ui.component.settings.AppBackButton
import com.resukisu.resukisu.ui.navigation.LocalNavigator
import com.resukisu.resukisu.ui.screen.FlashOutputScreen
import com.resukisu.resukisu.ui.theme.CardConfig
import com.resukisu.resukisu.ui.theme.ThemeConfig
import com.resukisu.resukisu.ui.theme.blurEffect
import com.resukisu.resukisu.ui.util.LocalSnackbarHost
import com.resukisu.resukisu.ui.util.showReplacingSnackbar
import com.resukisu.resukisu.ui.viewmodel.FlashState
import com.resukisu.resukisu.ui.viewmodel.FlashingStatus
import com.resukisu.resukisu.ui.viewmodel.KernelFlashUiAction
import com.resukisu.resukisu.ui.viewmodel.KernelFlashUiEvent
import com.resukisu.resukisu.ui.viewmodel.KernelFlashViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author ShirkNeko
 * @date 2025/5/31.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KernelFlashScreen(
    kernelUri: String,
    selectedSlot: String? = null,
    skipKsud: Boolean = false
) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val viewModel = koinViewModel<KernelFlashViewModel>()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val flashState = uiState.flash
    val logSavedString = stringResource(R.string.log_saved)
    val horizonFlashComplete = stringResource(R.string.horizon_flash_complete)
    val logText = buildString {
        append(flashState.logs.joinToString("\n"))
        if (flashState.error.isNotEmpty()) append("\n${flashState.error}\n")
        if (flashState.isCompleted) append("\n$horizonFlashComplete\n\n\n")
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is KernelFlashUiEvent.Error -> if (event.message.isNotBlank()) {
                    snackBarHost.showReplacingSnackbar(event.message)
                }
            }
        }
    }

    // 开始刷写
    LaunchedEffect(kernelUri, selectedSlot, skipKsud) {
        viewModel.dispatch(KernelFlashUiAction.Start(kernelUri, selectedSlot, skipKsud))
    }

    LaunchedEffect(flashState.isCompleted, uiState.autoExit) {
        if (flashState.isCompleted && uiState.autoExit) {
            delay(1500.milliseconds)
            viewModel.dispatch(KernelFlashUiAction.ConsumeAutoExit)
            (context as? ComponentActivity)?.finish()
        }
    }

    val navigator = LocalNavigator.current

    BackHandler(flashState.isFlashing) {
        // deny
    }

    val commonState = FlashState(
        status = when {
            flashState.error.isNotEmpty() -> FlashingStatus.FAILED
            flashState.isCompleted -> FlashingStatus.SUCCESS
            else -> FlashingStatus.FLASHING
        },
        output = logText,
        showReboot = flashState.isCompleted,
    )
    FlashOutputScreen(
        state = commonState,
        onBack = {
            if (!flashState.isFlashing) navigator.pop()
        },
        logFilePrefix = "KernelSU_kernel_flash_log",
        onReboot = { viewModel.dispatch(KernelFlashUiAction.Reboot) },
    )
}
