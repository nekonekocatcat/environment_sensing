package com.example.environment_sensing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.environment_sensing.data.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val rareDao = remember { AppDatabase.getInstance(context).rareEnvironmentLogDao() }
    val normalDao = remember { AppDatabase.getInstance(context).normalEnvironmentLogDao() }

    val rareLogsFlow = remember { rareDao.getAllLogs() }
    val normalLogsFlow = remember { normalDao.getAllLogs() }

    val rareLogs by rareLogsFlow.collectAsState(initial = emptyList())
    val normalLogs by normalLogsFlow.collectAsState(initial = emptyList())

    val rareTierMap = remember { RareEnvironmentChecker.environments.associate { it.name to it.tier } }

    // 1本のリストにまとめて時刻降順
    val all by remember(rareLogs, normalLogs) {
        mutableStateOf(
            (rareLogs.map { UiLog(it.environmentName, true,  it.timestamp, rareTierMap[it.environmentName]) } +
                    normalLogs.map { UiLog(it.environmentName, false, it.timestamp, null) })
                .sortedByDescending { it.timestamp }
        )
    }

    var filter by remember { mutableStateOf(HistoryFilter.All) }
    val filtered = remember(all, filter) {
        when (filter) {
            HistoryFilter.All        -> all
            HistoryFilter.Rare       -> all.filter { it.isRare }
            HistoryFilter.Normal     -> all.filter { !it.isRare }
            HistoryFilter.Tier1      -> all.filter { it.isRare && it.tier == 1 }
            HistoryFilter.Tier2      -> all.filter { it.isRare && it.tier == 2 }
            HistoryFilter.Tier3      -> all.filter { it.isRare && it.tier == 3 }
            HistoryFilter.TierUltra -> all.filter { it.isRare && it.tier == 99 }
        }
    }

    val dateFmt = remember { SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val groups = remember(filtered) {
        filtered.groupBy { dateFmt.format(Date(it.timestamp)) }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text("環境ゲット履歴🔍") })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryHeader(
                rareCount = rareLogs.size,
                normalCount = normalLogs.size,
                total = all.size
            )

            FilterRow(filter = filter, onChange = { filter = it })

            if (filtered.isEmpty()) {
                EmptyHistory()
                return@Column
            }


            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                groups.forEach { (date, items) ->
                    item(key = "header_$date") {
                        DateHeader(date)
                    }
                    itemsIndexed(
                        items,
                        key = { idx, item -> "${item.timestamp}_${item.name}_$idx" }
                    ) { index, item ->
                        val isLastInGroup = index == items.lastIndex
                        TimelineRow(
                            item = item,
                            timeLabel = timeFmt.format(Date(item.timestamp)),
                            drawConnector = !isLastInGroup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("まだ環境をゲットできてないみたい…", style = MaterialTheme.typography.titleMedium)
            Text(
                "環境をゲットするとここに出てくるよ！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------- UI pieces ---------- */

@Composable
private fun SummaryHeader(rareCount: Int, normalCount: Int, total: Int) {
    val ratio = if (total == 0) 0f else (rareCount + normalCount).toFloat() / max(total, 1)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("トータルゲット数", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("レア $rareCount") },
                    leadingIcon = { Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary) }
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("ノーマル $normalCount") },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary) }
                )
                Spacer(Modifier.weight(1f))
                Text("合計 $total", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private enum class HistoryFilter {
    All, Rare, Normal,
    Tier1, Tier2, Tier3, TierUltra
}
@Composable
private fun FilterRow(filter: HistoryFilter, onChange: (HistoryFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegChip("すべて",  filter == HistoryFilter.All)    { onChange(HistoryFilter.All) }
            SegChip("レア",    filter == HistoryFilter.Rare)   { onChange(HistoryFilter.Rare) }
            SegChip("ノーマル",filter == HistoryFilter.Normal) { onChange(HistoryFilter.Normal) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegChip("R★1",    filter == HistoryFilter.Tier1)      { onChange(HistoryFilter.Tier1) }
            SegChip("R★2",    filter == HistoryFilter.Tier2)      { onChange(HistoryFilter.Tier2) }
            SegChip("R★3",    filter == HistoryFilter.Tier3)      { onChange(HistoryFilter.Tier3) }
            SegChip("Ultra", filter == HistoryFilter.TierUltra) { onChange(HistoryFilter.TierUltra) }
        }
    }
}

@Composable
private fun SegChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
        } else null
    )
}

@Composable
private fun DateHeader(date: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun TimelineRow(item: UiLog, timeLabel: String, drawConnector: Boolean) {
    val tone = historyEnvColor(item.name).copy(alpha = if (item.isRare) 1f else 0.8f)

    val badgeLabel = when (item.tier) {
        1    -> "RARE"
        2    -> "R★2"
        3    -> "R★3"
        99   -> "Ultra"
        else -> if (item.isRare) "RARE" else "NORMAL"
    }
    val badgeTint = when (item.tier) {
        1    -> MaterialTheme.colorScheme.primary
        2    -> Color(0xFFE91E63)
        3    -> Color(0xFFFFC107)
        99   -> Color(0xFF7E57C2)
        else -> if (item.isRare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .width(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (item.isRare) 12.dp else 10.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(tone)
            )
            if (drawConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(tone.copy(0.5f), tone.copy(0.05f))
                            ),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )
            }
        }

        // カード本体
        val badgeIcon = if (item.isRare) Icons.Filled.Star else Icons.Filled.CheckCircle
        val bg = if (item.isRare)
            Brush.verticalGradient(listOf(tone.copy(0.18f), tone.copy(0.07f)))
        else
            Brush.verticalGradient(listOf(tone.copy(0.12f), tone.copy(0.04f)))

        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp),
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .background(bg, MaterialTheme.shapes.large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(badgeIcon, null, tint = badgeTint)
                        Spacer(Modifier.width(8.dp))
                        if (item.isRare) {
                            TierPill(badgeLabel, badgeTint)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = vibeHintEmoji(item.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TierPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// 履歴用の色マップ（Collection の envColor と同等）
@Composable
private fun historyEnvColor(name: String): Color = when (name) {
    // ===== レア =====
    "真夏の密室レア環境" -> Color(0xFFD32F2F)
    "熱帯低気圧レア環境" -> Color(0xFFB71C1C)
    "クラブわいわいレア環境" -> Color(0xFF6A1B9A)
    "工事現場みたいなレア環境" -> Color(0xFF8D6E63)
    "南国リゾートレア環境" -> Color(0xFFFFA000)
    "星空キャンプレア環境" -> Color(0xFF1565C0)
    "勉強はかどる集中レア環境" -> Color(0xFF2E7D32)
    "カラオケ大会レア環境" -> Color(0xFFF06292)
    "焚き火レア環境" -> Color(0xFFFF7043)
    "電車ラッシュレア環境" -> Color(0xFFFFA726)
    "映画館レア環境" -> Color(0xFF455A64)
    "ととのいサウナっぽいレア環境" -> Color(0xFFD84315)
    "めっちゃ静かレア環境" -> Color(0xFF455A64)
    "まるで北極レア環境" -> Color(0xFF90CAF9)
    "無響室レア環境" -> Color(0xFFB0BEC5)
    "オーロラレア環境" -> Color(0xFF7E57C2)
    "真空スーパーレア環境" -> Color(0xFF212121)
    "ブラックホール直前環境" -> Color(0xFF000000)
    "火星コロニーレア環境" -> Color(0xFFBF360C)

    // ===== ノーマル =====
    "静かめ快適環境" -> Color(0xFF4CAF50)
    "リビングまったり環境" -> Color(0xFF26A69A)
    "夜ふかしの薄暗い部屋" -> Color(0xFF7986CB)
    "早朝の静けさ環境" -> Color(0xFF81C784)
    "空気こもり気味環境" -> Color(0xFF90A4AE)
    "作業はかどり環境" -> Color(0xFF66BB6A)
    "集中できないザワザワ環境" -> Color(0xFFFF7043)
    "明るい屋外っぽい環境" -> Color(0xFF43A047)
    "交通量多め環境" -> Color(0xFFFFEB3B)
    "カフェっぽい環境" -> Color(0xFF8D6E63)
    "フードコートっぽい環境" -> Color(0xFFFFB74D)
    "調理中っぽい環境" -> Color(0xFFFF8A65)
    "カラカラ環境" -> Color(0xFFA1887F)
    "じめじめ環境" -> Color(0xFF26A69A)
    "冷房つよめ環境" -> Color(0xFF4FC3F7)
    "暗い静か環境" -> Color(0xFF90A4AE)
    "ざわざわ環境" -> Color(0xFFFFA000)
    "涼しめ明るい環境" -> Color(0xFF81D4FA)
    else -> when {
        listOf("高温","真夏","熱").any { name.contains(it) } -> Color(0xFFD32F2F)
        listOf("暗","夜","星","映画").any { name.contains(it) } -> Color(0xFF3949AB)
        listOf("騒","うる","ラッシュ").any { name.contains(it) } -> Color(0xFFFFA000)
        listOf("集中","快適","静").any { name.contains(it) } -> Color(0xFF2E7D32)
        listOf("交通","道路","車").any { name.contains(it) } -> Color(0xFFFFEB3B)
        else -> MaterialTheme.colorScheme.outline
    }
}



private data class UiLog(
    val name: String,
    val isRare: Boolean,
    val timestamp: Long,
    val tier: Int?
)

/* ---------- 絵文字/色ユーティリティ（既存の envColor を活用） ---------- */

// ざっくり雰囲気の絵文字を名前から推定
private fun vibeHintEmoji(name: String): String = when {
    // ===== レア =====
    name.contains("南国") -> "🏝️🌺 あったかくて開放的"
    name.contains("星空") -> "🌌✨ 静かな夜の空気"
    name.contains("映画") -> "🎬🤫 しん…とした世界"
    name.contains("電車") -> "🚃💨 ぎゅうぎゅう"
    name.contains("カラオケ") -> "🎤🎶 大盛り上がり"
    name.contains("焚き火") -> "🔥🌲 ほのかに煙"
    name.contains("集中") -> "📚🧠 ちょうどいい静けさ"
    name.contains("クラブ") -> "🔊🕺 たのしいね！"
    name.contains("真夏") -> "🥵💦 あっついね〜"
    name.contains("北極") -> "❄️🧊 ひんやり"
    name.contains("無響室") -> "🔇🧪 音が消える"
    name.contains("オーロラ") -> "🌈❄️ 幻想的"
    name.contains("火星") -> "🚀🪐 異世界感"

    // ===== ノーマル =====
    name.contains("リビング") -> "🛋️☕ くつろぎ"
    name.contains("作業") -> "💻📖 頑張ってるね！"
    name.contains("カフェ") -> "☕📚 ゆったり"
    name.contains("フードコート") -> "🍔👥 にぎやか"
    name.contains("調理") -> "🍳🔥 いい匂い"
    name.contains("早朝") -> "🌅😴 しんとした朝"
    name.contains("夜ふかし") -> "🌙📱 静かな夜"
    name.contains("冷房") -> "❄️🥶 ちょっと寒い"
    name.contains("じめじめ") -> "🌧️😓 ベタっと"
    name.contains("カラカラ") -> "😷💨 乾燥気味"
    name.contains("交通量") -> "🚗🛣️ ザワザワ"
    name.contains("屋外") -> "☀️🌿 明るい"
    name.contains("ざわざわ") -> "👥🔉 落ち着かない"
    name.contains("暗い") -> "🌙🤫 しずか"
    name.contains("静か") -> "🌿🤫 落ち着く"

    else -> "🌬️👀 空気が変わった"
}