package com.yourpax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

data class TableColumn(val name: String, val minWidth: Dp = 80.dp)
data class TableRow(val cells: List<String>, val background: Color? = null)

@Composable
fun DataTable(
    columns: List<TableColumn>, rows: List<TableRow>, modifier: Modifier = Modifier, fontSize: Float = 12f
) {
    val colors = rememberAppColors()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(0.5.dp, colors.subtleText.copy(alpha = 0.3f), MaterialTheme.shapes.small)
    ) {
        Row {
            columns.forEach { col ->
                Box(
                    modifier = Modifier
                        .background(colors.info.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = col.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp()),
                        minLines = 1
                    )
                }
            }
        }
        rows.forEach { row ->
            Row {
                row.cells.forEachIndexed { _, cell ->
                    val bg = row.background
                    Box(
                        modifier = Modifier
                            .then(if (bg != null) Modifier.background(bg) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp()),
                            textAlign = TextAlign.Start, minLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun Float.sp(): TextUnit = TextUnit(this, TextUnitType.Sp)
