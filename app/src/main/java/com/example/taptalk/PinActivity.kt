package com.example.taptalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptalk.ui.theme.BackgroundLight
import com.example.taptalk.ui.theme.PurplePrimary
import com.example.taptalk.ui.theme.TapTalkTheme
import com.example.taptalk.ui.theme.White

class PinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapTalkTheme {
                Surface(Modifier.fillMaxSize(), color = BackgroundLight) {
                    PinScreen(
                        pinLength = 4,
                        onComplete = { pin ->
                            // TODO: validate the PIN
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PinScreen(
    pinLength: Int = 4,
    onComplete: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color(0x22000000),
                        1f to Color(0x33000000)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Top)
        ) {
            Spacer(Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_taptalk),
                contentDescription = "TapTalk logo",
                modifier = Modifier.size(140.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "TapTalk",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF163A2C),
                textAlign = TextAlign.Center
            )

            DotsRow(
                total = pinLength,
                filled = input.length,
                emptyColor = Color(0xFFBDBDBD),
                filledColor = Color(0xFF5C5C5C),
                size = 14.dp,
                spacing = 10.dp
            )

            Spacer(Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                ).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { label ->
                            CircleKey(label) {
                                if (input.length < pinLength) {
                                    input += label
                                    if (input.length == pinLength) onComplete(input)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DotsRow(
    total: Int,
    filled: Int,
    emptyColor: Color,
    filledColor: Color,
    size: Dp = 12.dp,
    spacing: Dp = 8.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(if (index < filled) filledColor else emptyColor)
            )
        }
    }
}

@Composable
private fun CircleKey(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(96.dp)
    ) {
        Text(
            text = label,
            color = White,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
    }
}