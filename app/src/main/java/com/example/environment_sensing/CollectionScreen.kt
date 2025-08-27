package com.example.environment_sensing

import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.EnvironmentCollection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen() {
    val context = LocalContext.current

    // 定義から全環境名を作成
    val rareDefs = remember { RareEnvironmentChecker.environments.map { it.name }.toSet() }
    val normalDefs = remember { NormalEnvironmentChecker.environments.map { it.name }.toSet() }
    val allEnvironments = remember { (rareDefs + normalDefs).toList() }

    var collected by remember { mutableStateOf<List<EnvironmentCollection>>(emptyList()) }

    // DB購読
    LaunchedEffect(Unit) {
        val dao = AppDatabase.getInstance(context).environmentCollectionDao()
        dao.getAll().collectLatest { result -> collected = result }
    }

    // NEWフラグは5秒経ってから自動で消す
    LaunchedEffect(collected) {
        if (collected.any { it.isNew }) {
            delay(5_000)
            AppDatabase.getInstance(context).environmentCollectionDao().clearNewFlags()
        }
    }


    val grouped = remember(collected) { collected.groupBy { it.environmentName } }
    val items = remember(collected) {
        allEnvironments.map { name ->
            val list = grouped[name].orEmpty()
            CollectionUiItem(
                name = name,
                isRare = name in rareDefs,
                obtained = list.isNotEmpty(),
                isNew = list.any { it.isNew },
                count = list.size,
                lastTimestamp = list.maxOfOrNull { it.timestamp }
            )
        }.sortedWith(
            compareByDescending<CollectionUiItem> { it.isNew }
                .thenByDescending { it.obtained }
                .thenByDescending { it.isRare }
                .thenBy { it.name }
        )
    }

    var filter by remember { mutableStateOf(CollectionFilter.All) }
    val filtered = remember(items, filter) {
        when (filter) {
            CollectionFilter.All -> items
            CollectionFilter.Rare -> items.filter { it.isRare }
            CollectionFilter.Normal -> items.filter { !it.isRare }
            CollectionFilter.Unobtained -> items.filter { !it.obtained }
        }
    }

    var detail by remember { mutableStateOf<CollectionUiItem?>(null) }

    Scaffold(topBar = { SmallTopAppBar(title = { Text("環境コレクション📕") }) }) { inner ->
        val layoutDir = LocalLayoutDirection.current

        Column(
            modifier = Modifier
                .padding(
                    start = inner.calculateStartPadding(layoutDir),
                    top   = inner.calculateTopPadding(),
                    end   = inner.calculateEndPadding(layoutDir)
                )
                .consumeWindowInsets(inner)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressHeader(obtained = items.count { it.obtained }, total = items.size)
            FilterChips(current = filter, onChange = { filter = it })

            // スクロール制御
            val gridState = rememberLazyGridState()
            val lifecycleOwner = LocalLifecycleOwner.current
            val scope = rememberCoroutineScope()

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.name }) { it ->
                    CollectionCard(item = it, onClick = { detail = it })
                }
            }

            // フィルタ変更時は先頭へ
            LaunchedEffect(filter, items.size) {
                gridState.scrollToItem(0)
            }

            // 画面復帰時も先頭へ
            DisposableEffect(lifecycleOwner, gridState) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        scope.launch { gridState.scrollToItem(0) }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
        }
    }

    if (detail != null) {
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            DetailSheet(item = detail!!)
        }
    }
}

/* ================= パーツ ================= */

@Composable
private fun ProgressHeader(obtained: Int, total: Int) {
    val ratio = if (total == 0) 0f else obtained.toFloat() / total
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("図鑑進捗", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
            Text("$obtained / $total 取得", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class CollectionFilter { All, Rare, Normal, Unobtained }

@Composable
private fun FilterChips(current: CollectionFilter, onChange: (CollectionFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("すべて", current == CollectionFilter.All) { onChange(CollectionFilter.All) }
        FilterChip("レア", current == CollectionFilter.Rare) { onChange(CollectionFilter.Rare) }
        FilterChip("ノーマル", current == CollectionFilter.Normal) { onChange(CollectionFilter.Normal) }
        FilterChip("未取得", current == CollectionFilter.Unobtained) { onChange(CollectionFilter.Unobtained) }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        } else null,
        border = if (selected) null else AssistChipDefaults.assistChipBorder(true),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(0.12f)
            else MaterialTheme.colorScheme.surface
        )
    )
}

data class CollectionUiItem(
    val name: String,
    val isRare: Boolean,
    val obtained: Boolean,
    val isNew: Boolean,
    val count: Int,
    val lastTimestamp: Long?
)

@Composable
private fun CollectionCard(item: CollectionUiItem, onClick: () -> Unit) {
    val tone = envColor(item.name)

    val obtainedBg = Brush.verticalGradient(
        listOf(tone.copy(alpha = 0.15f), tone.copy(alpha = 0.05f))
    )
    val lockedBg = SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
    val borderStroke: BorderStroke? = if (item.obtained) BorderStroke(3.dp, tone) else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(18.dp)) else Modifier)
            .background(if (item.obtained) obtainedBg else lockedBg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(tone)
                )
                Spacer(Modifier.width(8.dp))
                if (item.isRare) { RareBadge(); Spacer(Modifier.width(8.dp)) }
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.obtained) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (item.obtained) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.95f),
                    modifier = Modifier.weight(1f)
                )
            }

            if (item.obtained) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("取得回数: ${item.count}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    item.lastTimestamp?.let {
                        Text("最終取得: ${formatTs(it)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.72f),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("未取得", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // NEWバッジ
        NewBadge(visible = item.isNew, modifier = Modifier.align(Alignment.TopEnd))

        // 透かし
        val watermark = if (item.obtained) Icons.Filled.Check else Icons.Filled.Lock
        val tint = if (item.obtained) MaterialTheme.colorScheme.primary.copy(0.20f)
        else MaterialTheme.colorScheme.onSurface.copy(0.08f)
        Icon(
            imageVector = watermark, contentDescription = null, tint = tint,
            modifier = Modifier.align(Alignment.BottomEnd).size(if (item.obtained) 20.dp else 48.dp)
        )
    }
}

@Composable
private fun envColor(name: String): Color = when (name) {
    // レア
    "高温×息苦しさレア環境" -> Color(0xFFD32F2F)
    "低気圧×高温レア環境"   -> Color(0xFFB71C1C)
    "暗い×うるさいレア環境" -> Color(0xFF6A1B9A)
    "うるさい×汚いレア環境" -> Color(0xFF8D6E63)
    "南国リゾートレア環境"   -> Color(0xFFFFA000)
    "星空キャンプレア環境"   -> Color(0xFF1565C0)
    "勉強はかどる集中レア環境" -> Color(0xFF2E7D32)
    "カラオケ大会レア環境"   -> Color(0xFFF06292)
    "焚き火レア環境"         -> Color(0xFFFF7043)
    "電車ラッシュレア環境"   -> Color(0xFFFFA726)
    "映画館レア環境"         -> Color(0xFF455A64)
    // ノーマル
    "調理中っぽい環境"       -> Color(0xFFFF8A65)
    "リビングまったり環境"   -> Color(0xFF26A69A)
    "交通量多め環境"         -> Color(0xFFFFEB3B)
    // fallback
    else -> when {
        listOf("高温","真夏","熱").any { name.contains(it) } -> Color(0xFFD32F2F)
        listOf("暗","夜","星","映画").any { name.contains(it) } -> Color(0xFF3949AB)
        listOf("騒","うる", "ラッシュ").any { name.contains(it) } -> Color(0xFFFFA000)
        listOf("集中","快適","静").any { name.contains(it) } -> Color(0xFF2E7D32)
        listOf("交通","道路","車").any { name.contains(it) } -> Color(0xFFFFEB3B)
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun NewBadge(visible: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = pulseAlpha))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("NEW", fontWeight = FontWeight.Bold, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun RareBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.primary.copy(0.18f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text("RARE", color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailSheet(item: CollectionUiItem) {
    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.isRare) RareBadge()
            Spacer(Modifier.width(8.dp))
            Text(item.name, style = MaterialTheme.typography.titleLarge)
        }
        if (item.obtained) {
            Text("取得回数：${item.count}")
            item.lastTimestamp?.let {
                Text("最終取得：${formatTs(it)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("まだ出会っていない環境。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Divider()

        HintAccordion(title = "ヒント👀") {
            Text(hintFor(item.name), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HintAccordion(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "expandRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }

        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

/* =============== ヒント =============== */

private fun hintFor(name: String): String = when (name) {
    // ===== レア環境 =====
    "低気圧×高温レア環境" -> "空気が重たくてムワッと暑い日。雨の気配や台風前みたいな“だる〜い”感じが近いよ！🌧️🌡️🥵"
    "暗い×うるさいレア環境" -> "暗いのに音だけドン！ライブハウスやクラブのフロア、ゲームセンターの奥のような雰囲気。🕶️🔊🎶"
    "うるさい×汚いレア環境" -> "工事のガガガや車のゴーッが響いて、空気が粉っぽい／排気っぽいとき。🏗️🚚😷"
    "高温×息苦しさレア環境" -> "暑い＋換気弱めで息がこもる感じ。人が多い会議室や真夏の窓を閉め切った部屋が近いかもっ🥵🫁🚪"
    "南国リゾートレア環境" -> "明るくてあったかい場所で、人の声や音楽がゆる〜く流れてる。フードコートや海沿いのテラスっぽい。🌺🏖️🎶"
    "星空キャンプレア環境" -> "外がしんと静かで暗い夜。街灯が少ない公園やキャンプ場で空を見上げてる感じ。✨🏕️🌌"
    "勉強はかどる集中レア環境" -> "明るさはほどよく、雑音は小さめ。図書館や自習室みたいに落ち着くけどピリッと集中できる空気。📚💡🔇"
    "カラオケ大会レア環境" -> "小さめの部屋でワイワイ歌って盛り上がってるとき。楽しそう🎤🎵🙌"
    "焚き火レア環境" -> "外でパチパチ音とほのかな煙の匂い。火の温かさを近くで感じるムード。🔥🌲😌"
    "電車ラッシュレア環境" -> "ぎゅうぎゅうで暑く、空気がこもりがち。通勤ラッシュの車内そのもの。🚆👥💨"
    "映画館レア環境" -> "かなり暗くて静か、人がじっと座ってる空間。上映中の映画館の雰囲気。🎬🍿🤫"

    // ===== ノーマル環境 =====
    "静かめ快適環境" -> "空気はさらっと、音は控えめ。おうちでひと休みしてるときの落ち着き。🌿🛋️🤫"
    "涼しめ明るい環境" -> "ひんやり＆明るい場所。朝のオフィスや教室みたいなクリアな空気感。❄️💡📎"
    "ざわざわ環境" -> "人の往来が多くてガヤガヤ。ショッピングモールの通路や駅の構内っぽい。👥🏬🔉"
    "暗い静か環境" -> "明かりを落として静かに過ごす時間。就寝前の寝室やカーテンを閉めた部屋。🌙🛏️🤫"
    "リビングまったり環境" -> "ソファでだらっと、テレビや音楽は小さめ。お茶でも飲みたくなる居心地。🛋️📺☕"
    "交通量多め環境" -> "車の流れをずっと感じるような場所。大通り沿いで時々クラクションも🚗🛣️💨"
    "早朝の静けさ環境" -> "夜明け前後のひっそり感。外はまだ薄暗く、音も少なめ。🌅🌫️🕊️"
    "調理中っぽい環境" -> "キッチンで湯気や香りがふわっと。炒め物の音や換気扇の気配があると近いよ。🍳🔥🧅"

    // ===== フォールバック =====
    else -> "“空気の雰囲気”に注目しよう。明るさ・静かさ・人の多さ・換気の効き具合を感じ取ると見つけやすいよ。🌬️👀"
}

/* =============== Utils =============== */

private fun formatTs(ts: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(ts))