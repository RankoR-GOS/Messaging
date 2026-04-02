package com.android.messaging.ui.conversation.v2.mediapicker.component

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.util.Size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.scale
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.messaging.util.ContentType
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FIRST_PASS_DIVISOR = 12
private const val FIRST_PASS_MAXIMUM_SIZE = 24
private const val FIRST_PASS_MINIMUM_SIZE = 12
private const val SECOND_PASS_DIVISOR = 3
private const val SECOND_PASS_MAXIMUM_SIZE = 48
private const val SECOND_PASS_MINIMUM_SIZE = 24
private const val THUMBNAIL_FADE_IN_DURATION_MILLIS = 90

@Composable
internal fun ConversationMediaThumbnail(
    modifier: Modifier = Modifier,
    contentUri: String,
    contentType: String,
    size: IntSize,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeEnabled: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    useBitmapLoader: Boolean = false,
    softenBitmap: Boolean = false,
) {
    val context = LocalContext.current

    val contentUriAsUri = rememberContentUri(contentUri = contentUri)
    val normalizedSize = remember(size) {
        size.sanitized()
    }

    val resolvedBackgroundColor = resolveBackgroundColor(backgroundColor = backgroundColor)

    val shouldUseCoilImageLoader = shouldUseCoilImageLoader(
        contentType = contentType,
        useBitmapLoader = useBitmapLoader,
    )

    when {
        shouldUseCoilImageLoader -> {
            CoilThumbnail(
                modifier = modifier,
                context = context,
                contentUri = contentUriAsUri,
                size = normalizedSize,
                contentScale = contentScale,
                crossfadeEnabled = crossfadeEnabled,
            )
        }

        else -> {
            BitmapThumbnail(
                modifier = modifier,
                contentUri = contentUriAsUri,
                contentType = contentType,
                size = normalizedSize,
                contentScale = contentScale,
                crossfadeEnabled = crossfadeEnabled,
                backgroundColor = resolvedBackgroundColor,
                softenBitmap = softenBitmap,
                useBitmapLoader = useBitmapLoader,
            )
        }
    }
}

@Composable
internal fun rememberConversationMediaThumbnailBitmap(
    contentUri: Uri,
    contentType: String,
    size: IntSize,
    softenBitmap: Boolean = false,
): Bitmap? {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        contentUri,
        contentType,
        size,
        softenBitmap,
    ) {
        value = loadConversationMediaThumbnailBitmap(
            contentResolver = context.contentResolver,
            contentUri = contentUri,
            contentType = contentType,
            size = size,
            softenBitmap = softenBitmap,
        )
    }

    return bitmap
}

@Composable
private fun BitmapThumbnail(
    modifier: Modifier,
    contentUri: Uri,
    contentType: String,
    size: IntSize,
    contentScale: ContentScale,
    crossfadeEnabled: Boolean,
    backgroundColor: Color,
    softenBitmap: Boolean,
    useBitmapLoader: Boolean,
) {
    val bitmap = rememberConversationMediaThumbnailBitmap(
        contentUri = contentUri,
        contentType = contentType,
        size = size,
        softenBitmap = softenBitmap,
    )
    val bitmapAlpha = rememberThumbnailAlpha(
        crossfadeEnabled = crossfadeEnabled,
        isLoaded = bitmap != null,
        animationLabel = "conversationMediaThumbnailBitmapAlpha",
    )
    val filterQuality = resolveBitmapFilterQuality(useBitmapLoader = useBitmapLoader)

    Box(modifier = modifier) {
        ThumbnailPlaceholder(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = backgroundColor,
        )

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                filterQuality = filterQuality,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha = bitmapAlpha),
            )
        }
    }
}

@Composable
private fun CoilThumbnail(
    modifier: Modifier,
    context: Context,
    contentUri: Uri,
    size: IntSize,
    contentScale: ContentScale,
    crossfadeEnabled: Boolean,
) {
    val imageRequest = remember(
        context,
        contentUri,
        size,
    ) {
        ImageRequest.Builder(context)
            .data(contentUri)
            .size(width = size.width, height = size.height)
            .build()
    }

    val isImageLoaded = remember(contentUri, size, crossfadeEnabled) {
        mutableStateOf(value = !crossfadeEnabled)
    }

    val visibilityAlpha = rememberThumbnailAlpha(
        crossfadeEnabled = crossfadeEnabled,
        isLoaded = isImageLoaded.value,
        animationLabel = "conversationMediaThumbnailImageAlpha",
    )

    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = contentScale,
        filterQuality = FilterQuality.Low,
        modifier = modifier.alpha(alpha = visibilityAlpha),
        onError = {
            isImageLoaded.value = true
        },
        onSuccess = {
            isImageLoaded.value = true
        },
    )
}

@Composable
private fun ThumbnailPlaceholder(
    modifier: Modifier,
    backgroundColor: Color,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
        }
    }
}

internal suspend fun loadConversationMediaThumbnailBitmap(
    contentResolver: ContentResolver,
    contentUri: Uri,
    contentType: String,
    size: IntSize,
    softenBitmap: Boolean,
): Bitmap? {
    return withContext(context = Dispatchers.IO) {
        val rawBitmap = loadPlatformThumbnail(
            contentResolver = contentResolver,
            contentUri = contentUri,
            size = size,
        ) ?: loadFallbackBitmap(
            contentResolver = contentResolver,
            contentUri = contentUri,
            contentType = contentType,
            size = size,
        )

        maybeSoftenBitmap(
            bitmap = rawBitmap,
            outputSize = size,
            softenBitmap = softenBitmap,
        )
    }
}

private fun createSoftenedBitmap(
    sourceBitmap: Bitmap,
    outputSize: IntSize,
): Bitmap {
    val sanitizedOutputSize = outputSize.sanitized()
    val targetWidth = sanitizedOutputSize.width
    val targetHeight = sanitizedOutputSize.height
    val centerCroppedBitmap = createCenterCroppedBitmap(
        sourceBitmap = sourceBitmap,
        targetSize = sanitizedOutputSize,
    )

    // Multi-pass downscaling keeps softened placeholders smooth without introducing blur kernels
    val firstPassWidth = (targetWidth / FIRST_PASS_DIVISOR).coerceIn(
        minimumValue = FIRST_PASS_MINIMUM_SIZE,
        maximumValue = FIRST_PASS_MAXIMUM_SIZE,
    )
    val firstPassHeight = (targetHeight / FIRST_PASS_DIVISOR).coerceIn(
        minimumValue = FIRST_PASS_MINIMUM_SIZE,
        maximumValue = FIRST_PASS_MAXIMUM_SIZE,
    )
    val secondPassWidth = (targetWidth / SECOND_PASS_DIVISOR).coerceIn(
        minimumValue = SECOND_PASS_MINIMUM_SIZE,
        maximumValue = SECOND_PASS_MAXIMUM_SIZE,
    )
    val secondPassHeight = (targetHeight / SECOND_PASS_DIVISOR).coerceIn(
        minimumValue = SECOND_PASS_MINIMUM_SIZE,
        maximumValue = SECOND_PASS_MAXIMUM_SIZE,
    )

    val firstPassBitmap = centerCroppedBitmap.scale(firstPassWidth, firstPassHeight)
    val secondPassBitmap = firstPassBitmap.scale(secondPassWidth, secondPassHeight)

    return secondPassBitmap.scale(targetWidth, targetHeight)
}

private fun createCenterCroppedBitmap(
    sourceBitmap: Bitmap,
    targetSize: IntSize,
): Bitmap {
    val sanitizedTargetSize = targetSize.sanitized()
    val targetAspectRatio =
        sanitizedTargetSize.width.toFloat() / sanitizedTargetSize.height.toFloat()
    val sourceAspectRatio = sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat()

    val cropWidth: Int
    val cropHeight: Int

    when {
        sourceAspectRatio > targetAspectRatio -> {
            cropHeight = sourceBitmap.height
            cropWidth = (cropHeight * targetAspectRatio).toInt()
        }

        else -> {
            cropWidth = sourceBitmap.width
            cropHeight = (cropWidth / targetAspectRatio).toInt()
        }
    }

    val left = (sourceBitmap.width - cropWidth) / 2
    val top = (sourceBitmap.height - cropHeight) / 2

    return Bitmap.createBitmap(
        sourceBitmap,
        left,
        top,
        cropWidth.coerceAtLeast(minimumValue = 1),
        cropHeight.coerceAtLeast(minimumValue = 1),
    )
}

@Composable
private fun rememberContentUri(
    contentUri: String,
): Uri {
    return remember(contentUri) {
        contentUri.toUri()
    }
}

@Composable
private fun rememberThumbnailAlpha(
    crossfadeEnabled: Boolean,
    isLoaded: Boolean,
    animationLabel: String,
): Float {
    val targetAlpha = when {
        !crossfadeEnabled || isLoaded -> 1f
        else -> 0f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = THUMBNAIL_FADE_IN_DURATION_MILLIS),
        label = animationLabel,
    )

    return animatedAlpha
}

@Composable
private fun resolveBackgroundColor(
    backgroundColor: Color,
): Color {
    return when {
        backgroundColor != Color.Unspecified -> backgroundColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

private fun shouldUseCoilImageLoader(
    contentType: String,
    useBitmapLoader: Boolean,
): Boolean {
    return ContentType.isImageType(contentType) && !useBitmapLoader
}

private fun resolveBitmapFilterQuality(useBitmapLoader: Boolean): FilterQuality {
    return when {
        useBitmapLoader -> FilterQuality.Medium
        else -> FilterQuality.Low
    }
}

private fun loadPlatformThumbnail(
    contentResolver: ContentResolver,
    contentUri: Uri,
    size: IntSize,
): Bitmap? {
    return runCatching {
        contentResolver.loadThumbnail(
            contentUri,
            Size(size.width, size.height),
            null,
        )
    }.getOrNull()
}

private fun loadFallbackBitmap(
    contentResolver: ContentResolver,
    contentUri: Uri,
    contentType: String,
    size: IntSize,
): Bitmap? {
    return when {
        ContentType.isImageType(contentType) -> {
            loadImageBitmapFallback(
                contentResolver = contentResolver,
                contentUri = contentUri,
                size = size,
            )
        }

        ContentType.isVideoType(contentType) -> {
            loadVideoFrameFallback(
                contentUri = contentUri,
                size = size,
            )
        }

        else -> null
    }
}

private fun loadImageBitmapFallback(
    contentResolver: ContentResolver,
    contentUri: Uri,
    size: IntSize,
): Bitmap? {
    return runCatching {
        val decodeBoundsOptions = Options().apply {
            inJustDecodeBounds = true
        }

        contentResolver
            .openInputStream(contentUri)
            ?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeBoundsOptions)
            }

        val decodeBitmapOptions = Options().apply {
            inSampleSize = calculateBitmapSampleSize(
                sourceWidth = decodeBoundsOptions.outWidth,
                sourceHeight = decodeBoundsOptions.outHeight,
                targetSize = size,
            )
        }

        contentResolver
            .openInputStream(contentUri)
            ?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeBitmapOptions)
            }
    }.getOrNull()
}

private fun loadVideoFrameFallback(
    contentUri: Uri,
    size: IntSize,
): Bitmap? {
    val retriever = MediaMetadataRetrieverWrapper()

    return try {
        runCatching {
            retriever.setDataSource(contentUri)
            retriever.frameAtTime?.let { bitmap ->
                scaleBitmapDownIfNeeded(
                    bitmap = bitmap,
                    targetSize = size,
                )
            }
        }.getOrNull()
    } finally {
        retriever.release()
    }
}

private fun maybeSoftenBitmap(
    bitmap: Bitmap?,
    outputSize: IntSize,
    softenBitmap: Boolean,
): Bitmap? {
    return when {
        bitmap == null -> null
        !softenBitmap -> bitmap

        else -> {
            createSoftenedBitmap(
                sourceBitmap = bitmap,
                outputSize = outputSize,
            )
        }
    }
}

private fun calculateBitmapSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetSize: IntSize,
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0) {
        return 1
    }

    var sampleSize = 1
    val targetWidth = targetSize.width.coerceAtLeast(minimumValue = 1)
    val targetHeight = targetSize.height.coerceAtLeast(minimumValue = 1)

    while (
        canDoubleBitmapSampleSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            sampleSize = sampleSize,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
        )
    ) {
        sampleSize *= 2
    }

    return sampleSize
}

private fun canDoubleBitmapSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    sampleSize: Int,
    targetWidth: Int,
    targetHeight: Int,
): Boolean {
    val doubledSampleSize = sampleSize * 2
    val doubledDecodedWidth = sourceWidth / doubledSampleSize
    val doubledDecodedHeight = sourceHeight / doubledSampleSize

    return doubledDecodedWidth >= targetWidth &&
        doubledDecodedHeight >= targetHeight
}

private fun scaleBitmapDownIfNeeded(
    bitmap: Bitmap,
    targetSize: IntSize,
): Bitmap {
    val targetWidth = targetSize.width.coerceAtLeast(minimumValue = 1)
    val targetHeight = targetSize.height.coerceAtLeast(minimumValue = 1)

    if (bitmap.width <= targetWidth && bitmap.height <= targetHeight) {
        return bitmap
    }

    val widthScale = targetWidth.toFloat() / bitmap.width.toFloat()
    val heightScale = targetHeight.toFloat() / bitmap.height.toFloat()
    val scale = minOf(widthScale, heightScale)

    return bitmap.scale(
        width = (bitmap.width * scale).toInt().coerceAtLeast(minimumValue = 1),
        height = (bitmap.height * scale).toInt().coerceAtLeast(minimumValue = 1),
    )
}

private fun IntSize.sanitized(): IntSize {
    if (width >= 1 && height >= 1) {
        return this
    }

    return IntSize(
        width = width.coerceAtLeast(minimumValue = 1),
        height = height.coerceAtLeast(minimumValue = 1),
    )
}
