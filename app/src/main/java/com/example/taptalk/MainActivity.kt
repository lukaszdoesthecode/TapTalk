package com.example.taptalk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptalk.ui.theme.TapTalkTheme

private val GreenBg = Color(0xFFE6F2E6)
private val BrandGreen = Color(0xFF1A3B1A)
private val BrandPurple = Color(0xFF7B4B9A)

/**
 * Bottom gradient like in your mock:
 * transparent -> light lavender -> deeper lavender
 */
@Composable
private fun BottomGradient(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE5E0EF),
                        Color(0xFFD1C2E3)
                    )
                )
            )
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenBg
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BottomGradient(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        TapTalkScreen(
                            onLoginClick = {
                                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            },
                            onRegisterClick = {
                                startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TapTalkScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_taptalk),
            contentDescription = "TapTalk Logo",
            modifier = Modifier
                .size(220.dp)
                .padding(bottom = 12.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "TapTalk",
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen,
            letterSpacing = 0.5.sp
        )

        Text(
            text = "AI-powered ACC Support",
            fontSize = 14.sp,
            color = Color(0xFF6F6F6F),
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                shape = RoundedCornerShape(100),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(6.dp, RoundedCornerShape(100), clip = false)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_login),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Log in",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRegisterClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(100),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(6.dp, RoundedCornerShape(100), clip = false)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_register),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Register",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(context, AccActivity::class.java))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998)),
                shape = RoundedCornerShape(100),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(6.dp, RoundedCornerShape(100), clip = false)
            ) {
                Text(
                    text = "Open AAC Board",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
