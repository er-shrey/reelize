package com.shrey.reelize.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shrey.reelize.Prefs
import com.shrey.reelize.overlay.AMBER_AT
import com.shrey.reelize.overlay.BUBBLE_SWATCHES
import com.shrey.reelize.overlay.BubbleImage
import com.shrey.reelize.overlay.BubblePhoto
import com.shrey.reelize.overlay.BubblePosition
import com.shrey.reelize.overlay.RED_AT
import com.shrey.reelize.overlay.bubbleColors

/** How the floating counter looks and where it appears. Every change is saved at once. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BubbleSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var enabled by remember { mutableStateOf(prefs.bubbleEnabled) }
    var byCount by remember { mutableStateOf(prefs.colorByCount) }
    var color by remember { mutableIntStateOf(prefs.bubbleColor) }
    var opacity by remember { mutableStateOf(prefs.bubbleOpacity) }
    var image by remember { mutableStateOf(prefs.bubbleImage) }
    var position by remember { mutableStateOf(prefs.bubblePosition) }
    var rememberDrag by remember { mutableStateOf(prefs.rememberDrag) }
    var photo by remember { mutableStateOf(BubblePhoto.load(context)?.asImageBitmap()) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (BubblePhoto.save(context, uri)) {
            photo = BitmapFactory.decodeFile(BubblePhoto.file(context).path)?.asImageBitmap()
            image = BubbleImage.PHOTO
            prefs.bubbleImage = image
        } else {
            Toast.makeText(context, "Couldn't read that picture", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bubble") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Section("Preview") {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Color(0xFF2B2930), RoundedCornerShape(12.dp))
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val samples = if (byCount) listOf(12, AMBER_AT + 5, RED_AT + 10) else listOf(12)
                        samples.forEach { BubblePreview(it, byCount, color, opacity, image, photo) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show the bubble", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = enabled, onCheckedChange = { enabled = it; prefs.bubbleEnabled = it })
                    }
                }
            }

            item {
                Section("Colour") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = byCount,
                            onClick = { byCount = true; prefs.colorByCount = true },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("Changes with count") }
                        SegmentedButton(
                            selected = !byCount,
                            onClick = { byCount = false; prefs.colorByCount = false },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("One colour") }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (byCount) {
                        Text(
                            "Green until $AMBER_AT, amber until $RED_AT, then red.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BUBBLE_SWATCHES.forEach { swatch ->
                                val selected = swatch == color
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(Color(swatch))
                                        .border(
                                            BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                            CircleShape,
                                        )
                                        .clickable { color = swatch; prefs.bubbleColor = swatch },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Filled.Check, "Selected", tint = Color(bubbleColors(0, false, swatch).second))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Section("Opacity · ${(opacity * 100).toInt()}%") {
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        onValueChangeFinished = { prefs.bubbleOpacity = opacity },
                        valueRange = 0.3f..1f,
                    )
                }
            }

            item {
                Section("Picture") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BubbleImage.entries.filter { it != BubbleImage.PHOTO }.forEach { option ->
                            Choice(selected = image == option, onClick = { image = option; prefs.bubbleImage = option }) {
                                Text(option.emoji ?: "–", fontSize = 22.sp)
                            }
                        }
                        Choice(
                            selected = image == BubbleImage.PHOTO,
                            onClick = {
                                if (photo == null) {
                                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    image = BubbleImage.PHOTO; prefs.bubbleImage = BubbleImage.PHOTO
                                }
                            },
                        ) {
                            val p = photo
                            if (p != null) {
                                Image(p, "Your photo", Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Filled.AddPhotoAlternate, "Add your photo")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${image.label}. The last option uses a picture from your gallery, which fills the bubble.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (photo != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("Choose a different photo") }
                    }
                }
            }

            item {
                Section("Starting position") {
                    PositionPicker(position) { position = it; prefs.bubblePosition = it; prefs.clearDraggedPosition() }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            rememberDrag = !rememberDrag
                            prefs.rememberDrag = rememberDrag
                            if (!rememberDrag) prefs.clearDraggedPosition()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = rememberDrag, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Remember where I drag it", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (rememberDrag) "The bubble comes back where you last left it."
                                else "You can still drag it, but it starts at ${position.label.lowercase()} each time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A miniature of the overlay bubble, drawn the same way the service draws it. */
@Composable
private fun BubblePreview(count: Int, byCount: Boolean, color: Int, opacity: Float, image: BubbleImage, photo: ImageBitmap?) {
    val (bg, fg) = bubbleColors(count, byCount, color)
    if (image == BubbleImage.PHOTO && photo != null) {
        Box(Modifier.size(56.dp).alpha(opacity).shadow(6.dp, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
            Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Text(
                count.toString(),
                style = TextStyle(
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    shadow = Shadow(Color.Black, blurRadius = 6f),
                ),
            )
        }
    } else {
        Box(
            Modifier.alpha(opacity).shadow(6.dp, RoundedCornerShape(50)).background(Color(bg), RoundedCornerShape(50))
                .heightIn(min = 44.dp).widthIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val prefix = image.emoji?.let { "$it " }.orEmpty()
            Text("$prefix$count", color = Color(fg), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** A phone outline with six spots to tap. */
@Composable
private fun PositionPicker(selected: BubblePosition, onSelect: (BubblePosition) -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(140.dp).aspectRatio(0.5f)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(
                BubblePosition.TOP_LEFT to BubblePosition.TOP_RIGHT,
                BubblePosition.MIDDLE_LEFT to BubblePosition.MIDDLE_RIGHT,
                BubblePosition.BOTTOM_LEFT to BubblePosition.BOTTOM_RIGHT,
            ).forEach { (left, right) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(left, right).forEach { pos ->
                        val on = pos == selected
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onSelect(pos) },
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        selected.label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun Choice(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                if (selected) 2.dp else 0.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
