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
import com.example.taptalk.viewmodel.LoginViewModel
import com.example.taptalk.ui.theme.BackgroundLight
import com.example.taptalk.ui.theme.ErrorRed
import com.example.taptalk.ui.theme.GreenPrimary
import com.example.taptalk.ui.theme.PurplePrimary
import com.example.taptalk.ui.theme.TapTalkTheme
import com.google.firebase.auth.FirebaseAuth


/**
 * The entry point activity for the application, handling user login.
 *
 * This activity serves two main purposes:
 * 1. If a user is already authenticated with Firebase, it immediately navigates
 *    them to the main part of the app ([AccActivity]) and finishes itself.
 * 2. If no user is logged in, it displays the [LoginScreen] Composable,
 *    allowing the user to enter their credentials and attempt to sign in.
 *
 * The UI is built using Jetpack Compose and features a background gradient
 * and the main login form.
 */
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, AccActivity::class.java))
            finish()
            return
        }

        setContent {
            TapTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundLight
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

/**
 * A composable that renders a decorative vertical gradient at the bottom of the screen.
 * This gradient transitions from transparent to light purple shades, providing a subtle
 * visual effect for the background of the login screen.
 *
 * @param modifier The modifier to be applied to the gradient container. Defaults to [Modifier].
 */
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

/**
 * A Composable function that displays the login UI.
 *
 * This screen includes fields for email and password, a login button, and displays
 * the app logo. It observes the state from the [LoginViewModel] to handle UI updates
 * for loading, success, and error states. On successful login, it navigates to
 * the `AccActivity`.
 *
 * @param viewModel An instance of [LoginViewModel] used to manage the login logic
 *                  and state. Defaults to a ViewModel provided by `androidx.lifecycle.viewmodel.compose.viewModel()`.
 */
@Composable
private fun LoginScreen(
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.success) {
        if (state.success) {
            context.startActivity(Intent(context, AccActivity::class.java))
            if (context is android.app.Activity) {
                context.finish()
            }
        }
    }

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
            color = GreenPrimary
        )

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    focusedContainerColor = PurplePrimary,
                    unfocusedContainerColor = PurplePrimary,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )

            Spacer(Modifier.height(14.dp))

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
                    focusedContainerColor = PurplePrimary,
                    unfocusedContainerColor = PurplePrimary,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )

            Spacer(Modifier.height(22.dp))

            // Login button
            Button(
                onClick = { viewModel.login(email.trim(), password) },
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
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

            when {
                state.error != null -> Text(state.error ?: "", color = ErrorRed)
                state.success -> Text(
                    "Login successful!",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
