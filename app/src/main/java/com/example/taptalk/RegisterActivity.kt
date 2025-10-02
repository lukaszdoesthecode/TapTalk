package com.example.taptalk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taptalk.ui.RegisterViewModel
import com.example.taptalk.ui.theme.TapTalkTheme
import java.util.Calendar

private val GreenBg = Color(0xFFE6F2E6)
private val BrandGreen = Color(0xFF1A3B1A)
private val BrandPurple = Color(0xFF7B4B9A)

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenBg
                ) {
                    Box(Modifier.fillMaxSize()) {
                        BottomGradient(Modifier.align(Alignment.BottomCenter))
                        RegisterScreen(
                            onSuccess = {
                                //TODO replace HomeActivity with acc
                            }
                        )
                    }
                }
            }
        }
    }
}

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

@Composable
private fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    var optPassword by remember { mutableStateOf(true) }
    var optPin by remember { mutableStateOf(false) }
    var optRemember by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val scroll = rememberScrollState()

    LaunchedEffect(state.success) {
        if (state.success) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_taptalk),
            contentDescription = "TapTalk Logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "TapTalk",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PurpleField(name, { name = it }, "Name")
                    Spacer(Modifier.height(16.dp))
                    PurpleField(email, { email = it }, "E-mail")
                    Spacer(Modifier.height(16.dp))
                    PurpleField(password, { password = it }, "Password", isPassword = true)
                }
                Column(modifier = Modifier.weight(1f)) {
                    PurpleField(repeatPassword, { repeatPassword = it }, "Repeat Password", isPassword = true)
                    Spacer(Modifier.height(16.dp))
                    DatePickerField(
                        selectedDate = dob,
                        onDateSelected = { dob = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    PurpleField(pin, { pin = it }, "PIN (optional)")
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckboxWithLabel(optPassword, { optPassword = it }, "I want to log in\nwith Password")
                    CheckboxWithLabel(optPin, { optPin = it }, "I want to log in\nwith PIN")
                    CheckboxWithLabel(optRemember, { optRemember = it }, "I want to be\nremembered")
                }

                Spacer(Modifier.height(18.dp))

                CheckboxWithLabel(
                    acceptTerms, { acceptTerms = it },
                    "By registration I am accepting the rules and terms of the application TapTalk"
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        viewModel.register(
                            name = name.trim(),
                            email = email.trim(),
                            password = password,
                            repeatPassword = repeatPassword,
                            dob = dob.trim(),
                            pin = pin.trim().ifBlank { null },
                            optPassword = optPassword,
                            optPin = optPin
                        )
                    },
                    enabled = acceptTerms && !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        disabledContainerColor = Color(0xFFCCD7CC)
                    ),
                    shape = RoundedCornerShape(100),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(64.dp)
                        .shadow(6.dp, RoundedCornerShape(100), clip = false)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
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
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            when {
                state.error != null -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.error ?: "",
                        color = Color(0xFFB00020),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                state.success -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Registered successfully!",
                        color = BrandGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PurpleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                placeholder,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        },
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        ),
        visualTransformation = if (isPassword)
            PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(100),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BrandPurple,
            unfocusedContainerColor = BrandPurple,
            disabledContainerColor = BrandPurple,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    )
}

@Composable
private fun CheckboxWithLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = BrandGreen)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFF1F1F1F), lineHeight = 16.sp)
    }
}

@Composable
fun DatePickerField(
    label: String = "Date of Birth",
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val parts = selectedDate.split("-")
    if (parts.size == 3) {
        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
    }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BrandPurple, RoundedCornerShape(100))
            .clickable { datePickerDialog.show() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (selectedDate.isEmpty()) label else selectedDate,
            color = Color.White,
            fontSize = if (selectedDate.isEmpty()) 20.sp else 18.sp,
            fontWeight = if (selectedDate.isEmpty()) FontWeight.ExtraBold else FontWeight.Medium,
            letterSpacing = if (selectedDate.isEmpty()) 1.sp else 0.sp
        )
    }
}



