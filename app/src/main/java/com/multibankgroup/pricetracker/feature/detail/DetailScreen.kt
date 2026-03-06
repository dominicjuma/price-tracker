package com.multibankgroup.pricetracker.feature.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multibankgroup.pricetracker.R
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.components.PriceChangeIndicator
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalSpacing
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalStockColors
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerIcons
import java.util.Locale
import kotlinx.coroutines.delay

private const val FLASH_DURATION_MS = 800

/** Detail screen — obtains ViewModel, collects state, delegates to stateless [DetailContent]. */
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDismissError = viewModel::onDismissError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    uiState: DetailUiState,
    onNavigateBack: () -> Unit,
    onDismissError: () -> Unit,         // ← new
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.error?.let { error ->
        val errorMessage = stringResource(error.messageResId)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            onDismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            DetailTopBar(
                symbol = uiState.symbol,
                companyName = uiState.companyName,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                PriceCard(
                    currentPrice = uiState.currentPrice,
                    previousPrice = uiState.previousPrice,
                    direction = uiState.priceDirection,
                    lastUpdatedTimestamp = uiState.lastUpdatedTimestamp
                )

                AboutCard(description = uiState.description)

                DeepLinkHint(symbol = uiState.symbol)
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/** Price card with flash animation. Flash via drawBehind (draw phase only). */
@Composable
private fun PriceCard(
    currentPrice: Double,
    previousPrice: Double,
    direction: PriceDirection,
    lastUpdatedTimestamp: Long,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current
    val spacing = LocalSpacing.current
    val currentStockColors by rememberUpdatedState(stockColors)
    val flashColor = remember {
        Animatable(Color.Transparent, Color.VectorConverter(Color.Transparent.colorSpace))
    }

    LaunchedEffect(lastUpdatedTimestamp) {
        val targetColor = when (direction) {
            PriceDirection.UP -> currentStockColors.upFlash
            PriceDirection.DOWN -> currentStockColors.downFlash
            PriceDirection.NONE -> return@LaunchedEffect
        }
        flashColor.snapTo(targetColor)
        flashColor.animateTo(
            targetValue = Color.Transparent,
            animationSpec = tween(durationMillis = FLASH_DURATION_MS)
        )
    }

    val priceFormat = stringResource(R.string.price_format)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(spacing.medium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(flashColor.value) }
                .padding(spacing.extraLarge)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.detail_section_current_price),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.small))

                val formattedPrice = remember(currentPrice, priceFormat) {
                    String.format(Locale.US, priceFormat, currentPrice)
                }
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(spacing.mediumSmall))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    PriceChangeIndicator(
                        currentPrice = currentPrice,
                        previousPrice = previousPrice,
                        direction = direction
                    )
                    Text(
                        text = stringResource(R.string.detail_price_context),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Stock description card. Hidden when description is empty. */
@Composable
private fun AboutCard(
    description: String,
    modifier: Modifier = Modifier
) {
    if (description.isEmpty()) return

    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(spacing.medium)
    ) {
        Column(
            modifier = Modifier.padding(spacing.large)
        ) {
            Text(
                text = stringResource(R.string.detail_section_about),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.mediumSmall))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Tappable deep link URI. Copies to clipboard with 1.5s "Copied" feedback. */
@Composable
private fun DeepLinkHint(
    symbol: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val deepLinkUri = "stocks://symbol/$symbol"
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(1_500)
            showCopied = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.mediumSmall))
            .clickable(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("deep link", deepLinkUri))
                    showCopied = true
                },
                onClickLabel = stringResource(R.string.action_copy_deep_link)
            )
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = spacing.medium, vertical = spacing.mediumSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Text(
            text = if (showCopied) "✓" else "🔗",
            fontSize = 14.sp
        )
        Text(
            text = if (showCopied) {
                stringResource(R.string.detail_deep_link_copied)
            } else {
                deepLinkUri
            },
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Top bar with symbol title, company subtitle, and back navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    symbol: String,
    companyName: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (companyName.isNotEmpty()) {
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = PriceTrackerIcons.ArrowBack,
                    contentDescription = stringResource(R.string.detail_navigate_back),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}