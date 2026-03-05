package com.multibankgroup.pricetracker.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multibankgroup.pricetracker.R
import com.multibankgroup.pricetracker.feature.shared_ui.components.ConnectionIndicator
import com.multibankgroup.pricetracker.feature.shared_ui.components.StockRow
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalSpacing
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalStockColors

/** Feed screen — obtains ViewModel, collects state, delegates to stateless [FeedContent]. */
@Composable
fun FeedScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeedContent(
        uiState = uiState,
        onToggleFeed = viewModel::onToggleFeed,
        onDismissError = viewModel::onDismissError,
        onStockClick = onNavigateToDetail
    )
}

/** Stateless — testable without ViewModel or Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    uiState: FeedUiState,
    onToggleFeed: () -> Unit,
    onDismissError: () -> Unit,
    onStockClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show Snackbar when error arrives, clear ViewModel state when it dismisses
    uiState.error?.let { error ->
        val errorMessage = stringResource(error.messageResId)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            // Called on both auto-dismiss and user swipe
            onDismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    ConnectionIndicator(isConnected = uiState.isConnected)
                },
                actions = {
                    FeedToggleSwitch(
                        isFeedActive = uiState.isFeedActive,
                        onToggle = onToggleFeed
                    )
                    Spacer(modifier = Modifier.width(spacing.mediumSmall))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.medium)
            ) {
                items(
                    items = uiState.stocks,
                    key = { stock -> stock.symbol },
                    contentType = { "stock_row" }
                ) { stock ->
                    StockRow(
                        item = stock,
                        onClick = { onStockClick(stock.symbol) }
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/** Switch with "Feed" label. Green track = live. */
@Composable
private fun FeedToggleSwitch(
    isFeedActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Text(
            text = stringResource(R.string.feed_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = isFeedActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = stockColors.up,
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}