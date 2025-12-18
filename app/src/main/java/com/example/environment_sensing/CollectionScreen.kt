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
    val rareTierMap = remember { RareEnvironmentChecker.environments.associate { it.name to it.tier } }

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
            val isRare = name in rareDefs
            CollectionUiItem(
                name = name,
                isRare = isRare,
                obtained = list.isNotEmpty(),
                isNew = list.any { it.isNew },
                count = list.size,
                lastTimestamp = list.maxOfOrNull { it.timestamp },
                tier = if (isRare) rareTierMap[name] else null
            )
        }.sortedWith(
            compareByDescending<CollectionUiItem> { it.isNew }
                .thenByDescending { it.obtained }
                .thenByDescending { it.tier ?: 0 }
                .thenBy { it.name }
        )
    }
    var filter by remember { mutableStateOf(CollectionFilter.All) }
    val filtered = remember(items, filter) {
        when (filter) {
            CollectionFilter.All         -> items
            CollectionFilter.Rare        -> items.filter { it.isRare }
            CollectionFilter.Normal      -> items.filter { !it.isRare }
            CollectionFilter.Unobtained  -> items.filter { !it.obtained }
            CollectionFilter.Tier1       -> items.filter { it.isRare && it.tier == 1 }
            CollectionFilter.Tier2       -> items.filter { it.isRare && it.tier == 2 }
            CollectionFilter.Tier3       -> items.filter { it.isRare && it.tier == 3 }
            CollectionFilter.TierUltra  -> items.filter { it.isRare && it.tier == 99 }
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

enum class CollectionFilter {
    All, Rare, Normal, Unobtained,
    Tier1, Tier2, Tier3, TierUltra
}

@Composable
private fun FilterChips(current: CollectionFilter, onChange: (CollectionFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // 1段目：基本フィルタ
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("すべて",   current == CollectionFilter.All)        { onChange(CollectionFilter.All) }
            FilterChip("レア",     current == CollectionFilter.Rare)       { onChange(CollectionFilter.Rare) }
            FilterChip("ノーマル", current == CollectionFilter.Normal)     { onChange(CollectionFilter.Normal) }
            FilterChip("未取得",   current == CollectionFilter.Unobtained) { onChange(CollectionFilter.Unobtained) }
        }

        // 2段目：Tierフィルタ（レア度）
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierFilterChip("R★1", current == CollectionFilter.Tier1, Color(0xFF3F51B5)) { onChange(CollectionFilter.Tier1) }
            TierFilterChip("R★2", current == CollectionFilter.Tier2, Color(0xFFE91E63)) { onChange(CollectionFilter.Tier2) }
            TierFilterChip("R★3", current == CollectionFilter.Tier3, Color(0xFFFFC107)) { onChange(CollectionFilter.Tier3) }
            TierFilterChip("Ultra", current == CollectionFilter.TierUltra, Color(0xFF7E57C2)) { onChange(CollectionFilter.TierUltra) }
        }
    }
}

@Composable
private fun TierFilterChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, tint = color) }
        } else null,
        border = if (selected) null else AssistChipDefaults.assistChipBorder(true),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) color.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surface,
            labelColor     = if (selected) color else MaterialTheme.colorScheme.onSurface
        )
    )
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
    val lastTimestamp: Long?,
    val tier: Int?
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
                if (item.isRare) {
                    val label = when (item.tier) {
                        1 -> "RARE"
                        2 -> "R★2"
                        3 -> "R★3"
                        99 -> "Ultra"
                        else -> "RARE"
                    }
                    val color = when (item.tier) {
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> Color(0xFFE91E63)
                        3 -> Color(0xFFFFC107)
                        99 -> Color(0xFF7E57C2)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    TierPill(label, color)
                    Spacer(Modifier.width(8.dp))
                }
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
    "真夏の密室レア環境" -> Color(0xFFD32F2F)
    "熱帯低気圧レア環境"   -> Color(0xFFB71C1C)
    "クラブわいわいレア環境" -> Color(0xFF6A1B9A)
    "工事現場みたいなレア環境" -> Color(0xFF8D6E63)
    "南国リゾートレア環境"   -> Color(0xFFFFA000)
    "星空キャンプレア環境"   -> Color(0xFF1565C0)
    "勉強はかどる集中レア環境" -> Color(0xFF2E7D32)
    "カラオケ大会レア環境"   -> Color(0xFFF06292)
    "焚き火レア環境"         -> Color(0xFFFF7043)
    "電車ラッシュレア環境"   -> Color(0xFFFFA726)
    "映画館レア環境"         -> Color(0xFF455A64)
    "ととのいサウナっぽいレア環境" -> Color(0xFFE57373)
    "めっちゃ静かレア環境" -> Color(0xFFB0BEC5)
    "まるで北極レア環境" -> Color(0xFF81D4FA)
    "無響室レア環境" -> Color(0xFF90A4AE)
    "オーロラレア環境" -> Color(0xFF64FFDA)
    "真空スーパーレア環境" -> Color(0xFF000000)
    "ブラックホール直前環境" -> Color(0xFF212121)
    "火星コロニーレア環境" -> Color(0xFFD84315)
    // ノーマル
    "調理中っぽい環境"       -> Color(0xFFFF8A65)
    "リビングまったり環境"   -> Color(0xFF26A69A)
    "交通量多め環境"         -> Color(0xFFFFEB3B)
    "静かめ快適環境" -> Color(0xFF4CAF50)
    "作業はかどり環境" -> Color(0xFF66BB6A)
    "夜ふかしの薄暗い部屋" -> Color(0xFF7986CB)
    "カフェっぽい環境" -> Color(0xFF8D6E63)
    "フードコートっぽい環境" -> Color(0xFFFFB74D)
    "空気こもり気味環境" -> Color(0xFF90A4AE)
    "冷房つよめ環境" -> Color(0xFF4FC3F7)
    "じめじめ環境" -> Color(0xFF26A69A)
    "カラカラ環境" -> Color(0xFFA1887F)
    "明るい屋外っぽい環境" -> Color(0xFF43A047)
    "集中できないザワザワ環境" -> Color(0xFFFF7043)
    "早朝の静けさ環境" -> Color(0xFF81C784)
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
private fun RareBadge(tier: Int?) {
    val (label, color) = when (tier) {
        1   -> "RARE" to MaterialTheme.colorScheme.primary
        2   -> "RARE★2" to Color(0xFFE91E63)
        3   -> "RARE★3" to Color(0xFFFFC107)
        99  -> "Ultra" to Color(0xFF7E57C2)
        else-> "RARE" to MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) { Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun DetailSheet(item: CollectionUiItem) {
    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.isRare) { RareBadge(item.tier); Spacer(Modifier.width(8.dp)) }
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
    "熱帯低気圧レア環境" -> "空気が重たくてムワッと暑い日。雨の気配や台風前みたいな“だる〜い”感じが近いよ！🌧️🌡️🥵"
    "クラブわいわいレア環境" -> "暗いのに音だけドン！ライブハウスやクラブのフロア、ゲームセンターの奥のような雰囲気。🕶️🔊🎶"
    "工事現場みたいなレア環境" -> "工事や車の大きな音が響いて、空気が粉っぽい／排気っぽいとき。🏗️🚚😷"
    "真夏の密室レア環境" -> "暑い＋換気弱めで息がこもる感じ。人が多い会議室や真夏の窓を閉め切った部屋が近いかもっ🥵🫁🚪"
    "南国リゾートレア環境" -> "明るくてあったかい場所で、人の声や音楽がゆる〜く流れてる。フードコートや海沿いのテラスっぽい。🌺🏖️🎶"
    "星空キャンプレア環境" -> "外がしんと静かで寒い夜。街灯が少ない公園やキャンプ場で空を見上げてる感じ。✨🏕️🌌"
    "勉強はかどる集中レア環境" -> "明るさはほどよく、雑音は小さめ。図書館や自習室みたいに落ち着くけどピリッと集中できる空気。📚💡🔇"
    "カラオケ大会レア環境" -> "人がたくさんの小さめの部屋でワイワイ歌って盛り上がってるとき。楽しそう🎤🎵🙌"
    "焚き火レア環境" -> "外でパチパチ音とほのかな煙の匂い。火の温かさを近くで感じるムード。🔥🌲😌"
    "電車ラッシュレア環境" -> "ぎゅうぎゅうで暑く、空気がこもりがち。通勤ラッシュの車内そのもの。🚆👥💨"
    "映画館レア環境" -> "かなり暗くて静か、人がじっと座ってる空間。上映中の映画館の雰囲気。🎬🍿🤫"

    "ととのいサウナっぽいレア環境" -> "高温多湿で換気弱め。整う前後の休憩スペースも近い空気。♨️🧖"
    "めっちゃ静かレア環境" -> "耳鳴りがしそうなくらい静か。喋らないで！🤫🔇️"
    "焼肉屋っぽいレア環境" -> "香り・煙・賑わいが混ざる。少し暑い🥩🔥😋"
    "山頂絶景レア環境" -> "気圧が低くひんやり。屋外で明るく、風景が開けた場所。綺麗だね⛰️🌤️"
    "高原さわやかレア環境" -> "少し低い気圧＋涼しくて明るい。外の空気が気持ちいい。🍃☀️"
    "まるで北極レア環境" -> "とにかく寒くて静か。屋外に近い空気感。🧊❄️"
    "お昼の公園っぽいレア環境" -> "明るくてほどよい賑わい。外気に近い清々しさ。🏞️👨‍👩‍👧"
    "放課後教室っぽいレア環境" -> "教室がぽつぽつ埋まり、軽い会話とCO2が少し高め。🏫📖"
    "地下鉄ホームレア環境" -> "うなる走行音＋人工照明。ややこもった空気。🚇🔊"
    "無響室レア環境" -> "超低騒音の特別な環境。音が吸い込まれる感じ。🔇🧪"
    "厳冬オーロラレア環境" -> "極寒＋静けさ＋淡い明るさ。澄んだ外気。🌌❄️"
    "真空スーパーレア環境" -> "現実では到達不可。数値的に「ほぼ0気圧」のロマン。🛰️🛑"
    "ブラックホール直前環境" -> "暗黒＋超低圧のネタ枠。観測したら天才。🕳️😵‍💫"
    "火星コロニーレア環境" -> "低圧・極寒・CO2多めのSF枠。宇宙飛行士なの？🚀🪐"


    // ===== ノーマル環境 =====
    "静かめ快適環境" -> "空気はさらっと、音は控えめ。自宅や静かな部屋で落ち着く感じ。🌿🛋️🤫"
    "リビングまったり環境" -> "明るさはほどほど、生活音が小さめ。ソファでだらっと過ごす雰囲気。🛋️📺☕"
    "夜ふかしの薄暗い部屋" -> "部屋の明かりを落として静かに過ごす時間帯。寝る前っぽい空気。🌙🛏️"
    "早朝の静けさ環境" -> "外はまだ薄暗くて音が少ない。起きたての時間帯に近い。🌅🌫️"
    "空気こもり気味環境" -> "換気が弱くて息がこもる感じ。会議室や閉め切った部屋で出やすい。🫁🚪"
    "作業はかどり環境" -> "明るさと静かさのバランスが良い。机に向かうと集中しやすい。💻📚"
    "集中できないザワザワ環境" -> "話し声や物音が多くて落ち着かない。人が多い場所っぽい。🗣️💦"
    "明るい屋外っぽい環境" -> "日中の外みたいに明るく開放感がある。屋外や窓際に近い感覚。☀️🌿"
    "交通量多め環境" -> "車の音が継続的に聞こえる。大通り沿いで出やすい。🚗🛣️"
    "カフェっぽい環境" -> "明るさはほどほどで話し声が一定。作業してる人が多い空間。☕📖"
    "フードコートっぽい環境" -> "人が多くてガヤガヤ。空気が少しこもりやすい。🍔👥"
    "調理中っぽい環境" -> "におい成分が増えやすい。キッチンで加熱してる雰囲気。🍳🔥"
    "カラカラ環境" -> "乾燥していて喉が渇きやすい。暖房が効いた部屋で出やすい。😷"
    "じめじめ環境" -> "湿気が強くてベタつく。梅雨やお風呂上がりに近い。🌧️"
    "冷房つよめ環境" -> "ひんやりしすぎて少し寒い。冷房直撃席で出やすい。❄️"
    "暗い静か環境" -> "明かりが低くて静か。寝室やカーテン閉めた部屋の雰囲気。🌙🤫"
    "ざわざわ環境" -> "人の往来が多くてガヤガヤ。駅や通路っぽい。👥🔉"
    "涼しめ明るい環境" -> "少し涼しくて明るい。朝の教室やオフィスに近い。💡❄️"

    // ===== フォールバック =====
    else -> "“空気の雰囲気”に注目しよう。明るさ・静かさ・人の多さ・換気の効き具合を感じ取ると見つけやすいよ。🌬️👀"
}

/* =============== Utils =============== */

private fun formatTs(ts: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(ts))