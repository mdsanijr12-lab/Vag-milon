package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.Character2AssetManager
import com.example.game.GameEngine
import com.example.game.PlayerPose
import com.example.game.SelectedCharacter

@Composable
fun CharacterSelectionScreen(
    engine: GameEngine,
    onBack: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val selected = engine.selectedCharacter
    val assetVersion = engine.char2AssetVersion

    val char2Run = remember(assetVersion) { Character2AssetManager.loadRunSprite(context) }
    val char2Jump = remember(assetVersion) { Character2AssetManager.loadJumpSprite(context) }
    val char2Slide = remember(assetVersion) { Character2AssetManager.loadSlideSprite(context) }

    val runPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && Character2AssetManager.saveCustomPoseUri(context, PlayerPose.RUN, uri)) {
            engine.notifyChar2AssetsUpdated()
        }
    }

    val jumpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && Character2AssetManager.saveCustomPoseUri(context, PlayerPose.JUMP, uri)) {
            engine.notifyChar2AssetsUpdated()
        }
    }

    val slidePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && Character2AssetManager.saveCustomPoseUri(context, PlayerPose.CROUCH, uri)) {
            engine.notifyChar2AssetsUpdated()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("character_selection_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("char_select_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BACK",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CHARACTER SELECTION",
                        color = Color(0xFFFFEB3B),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "All Characters Are 100% FREE to Equip",
                        color = Color(0xFF69F0AE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onStartGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("char_select_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PLAY NOW",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Character Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CHARACTER 1 CARD
                val isChar1Equipped = selected == SelectedCharacter.CHARACTER_1
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isChar1Equipped) Color(0xFF1A237E).copy(alpha = 0.85f) else Color(0xFF263238).copy(alpha = 0.85f),
                    border = BorderStroke(
                        width = if (isChar1Equipped) 3.dp else 1.5.dp,
                        color = if (isChar1Equipped) Color(0xFF00E676) else Color(0xFF90CAF9)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { engine.equipCharacter(SelectedCharacter.CHARACTER_1) }
                        .testTag("card_character_1")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Character 1",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF00E676))
                            ) {
                                Text(
                                    text = "FREE",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Poses preview row (Run, Jump, Slide)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PosePreviewItem(
                                label = "RUN",
                                content = {
                                    Image(
                                        painter = painterResource(id = R.drawable.player_run),
                                        contentDescription = "Character 1 Run",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                            PosePreviewItem(
                                label = "JUMP",
                                content = {
                                    Image(
                                        painter = painterResource(id = R.drawable.player_jump),
                                        contentDescription = "Character 1 Jump",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                            PosePreviewItem(
                                label = "SLIDE",
                                content = {
                                    Image(
                                        painter = painterResource(id = R.drawable.player_crouch),
                                        contentDescription = "Character 1 Slide",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                        }

                        Text(
                            text = "Original Runner • Classic Jump Sound",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                engine.equipCharacter(SelectedCharacter.CHARACTER_1)
                                engine.audio.playJumpSound(SelectedCharacter.CHARACTER_1)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChar1Equipped) Color(0xFF00E676) else Color(0xFFFFB300)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("equip_character_1_button")
                        ) {
                            if (isChar1Equipped) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Equipped",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EQUIPPED",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Text(
                                    text = "EQUIP",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // CHARACTER 2 CARD
                val isChar2Equipped = selected == SelectedCharacter.CHARACTER_2
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isChar2Equipped) Color(0xFF1A237E).copy(alpha = 0.85f) else Color(0xFF263238).copy(alpha = 0.85f),
                    border = BorderStroke(
                        width = if (isChar2Equipped) 3.dp else 1.5.dp,
                        color = if (isChar2Equipped) Color(0xFF00E676) else Color(0xFFFFD54F)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { engine.equipCharacter(SelectedCharacter.CHARACTER_2) }
                        .testTag("card_character_2")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Character 2",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF00E676))
                            ) {
                                Text(
                                    text = "FREE",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Poses preview row (Run, Jump, Slide) - tap any pose to optionally load custom image
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PosePreviewItem(
                                label = "RUN",
                                onClick = {
                                    runPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                content = {
                                    Image(
                                        bitmap = char2Run.bitmap.asImageBitmap(),
                                        contentDescription = "Character 2 Run",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                            PosePreviewItem(
                                label = "JUMP",
                                onClick = {
                                    jumpPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                content = {
                                    Image(
                                        bitmap = char2Jump.bitmap.asImageBitmap(),
                                        contentDescription = "Character 2 Jump",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                            PosePreviewItem(
                                label = "SLIDE",
                                onClick = {
                                    slidePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                content = {
                                    Image(
                                        bitmap = char2Slide.bitmap.asImageBitmap(),
                                        contentDescription = "Character 2 Slide",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                        }

                        Text(
                            text = "New Runner • \"FAAAH\" Jump Sound (Tap pose to set custom image)",
                            color = Color(0xFFFFD54F),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                engine.equipCharacter(SelectedCharacter.CHARACTER_2)
                                engine.audio.playJumpSound(SelectedCharacter.CHARACTER_2)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChar2Equipped) Color(0xFF00E676) else Color(0xFFFFB300)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("equip_character_2_button")
                        ) {
                            if (isChar2Equipped) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Equipped",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EQUIPPED",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Text(
                                    text = "EQUIP",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosePreviewItem(
    label: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
