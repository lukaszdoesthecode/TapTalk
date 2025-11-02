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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptalk.ui.theme.BackgroundLight
import com.example.taptalk.ui.theme.GreenPrimary
import com.example.taptalk.ui.theme.PurplePrimary
import com.example.taptalk.ui.theme.TapTalkTheme
import com.google.firebase.auth.FirebaseAuth

/**
 * The main entry point of the application.
 *
 * This activity serves as the initial screen for users. It checks if a user is already
 * signed in with Firebase Authentication.
 * - If a user is logged in, it immediately redirects them to the [AccActivity].
 * - If no user is logged in, it displays the [TapTalkScreen], which provides options
 *   to either log in or register.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
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

/**
 * A composable that displays a decorative vertical gradient at the bottom of the screen.
 * This gradient transitions from transparent to a light purple, providing a subtle visual
 * flourish to the background.
 *
 * @param modifier The modifier to be applied to this composable. Defaults to [Modifier].
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

/**
 * The main welcome screen for the TapTalk application.
 *
 * This composable function displays the application's logo and name, along with
 * prominent buttons for logging in and registering. It serves as the initial
 * landing page for users who are not yet authenticated.
 *
 * @param onLoginClick A lambda function to be executed when the "Log in" button is clicked.
 *                     This should typically navigate the user to the login screen.
 * @param onRegisterClick A lambda function to be executed when the "Register" button is clicked.
 *                        This should typically navigate the user to the registration screen.
 */
@Composable
fun TapTalkScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
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
            color = GreenPrimary,
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
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
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
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
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

        }
    }
}
