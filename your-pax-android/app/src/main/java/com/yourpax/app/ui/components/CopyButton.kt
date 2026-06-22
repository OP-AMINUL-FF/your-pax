package com.yourpax.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay

@Composable
fun CopyButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    Crossfade(targetState = isCopied, label = "copyTransition", modifier = modifier) { copied ->
        if (copied) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Copied",
                tint = rememberAppColors().success,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(size)
                    .clickable {
                        if (textToCopy.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            isCopied = true
                        }
                    }
            )
        }
    }
}
