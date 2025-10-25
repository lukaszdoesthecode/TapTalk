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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptalk.ui.LoginViewModel
import com.example.taptalk.ui.theme.TapTalkTheme
import com.google.firebase.auth.FirebaseAuth

// 💚 Your theme colors
private val GreenBg = Color(0xFFE6F2E6)
private val BrandGreen = Color(0xFF1A3B1A)
private val BrandPurple = Color(0xFF7B4B9A)

@Composable
private fun BottomGradient(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFFE5E0EF),
                        Color(0xFFD1C2E3)
                    )
                )
            )
    )
}

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🧠 Step 1: Skip login if already authenticated
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, AccActivity::class.java))
            finish()
            return
        }

        // 🖤 Step 2: Otherwise show login screen
        setContent {
            TapTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenBg
                ) {
                    Box(Modifier.fillMaxSize()) {
                        BottomGradient(Modifier.align(Alignment.BottomCenter))
                        LoginScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // 💫 Step 3: Navigate to ACCActivity after login success
    LaunchedEffect(state.success) {
        if (state.success) {
            context.startActivity(Intent(context, AccActivity::class.java))
            if (context is android.app.Activity) {
                context.finish()
            }
        }
    }

    // 🧁 Step 4: UI
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
            modifier = Modifier.size(140.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "TapTalk",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen
        )

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✉️ Email
            TextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                placeholder = {
                    Text(
                        "E-mail",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                shape = RoundedCornerShape(100),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BrandPurple,
                    unfocusedContainerColor = BrandPurple,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )

            Spacer(Modifier.height(14.dp))

            // 🔒 Password
            TextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                placeholder = {
                    Text(
                        "Password",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                visualTransformation = if (showPassword)
                    VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }
                },
                shape = RoundedCornerShape(100),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BrandPurple,
                    unfocusedContainerColor = BrandPurple,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )

            Spacer(Modifier.height(22.dp))

            // 🔘 Login button
            Button(
                onClick = { viewModel.login(email.trim(), password) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                shape = RoundedCornerShape(100),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(64.dp)
                    .shadow(6.dp, RoundedCornerShape(100), clip = false)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_login),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Log in",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 💬 Status
            when {
                state.error != null -> Text(state.error ?: "", color = Color(0xFFB00020))
                state.success -> Text(
                    "Login successful!",
                    color = BrandGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
