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
import com.example.taptalk.viewmodel.RegisterViewModel
import com.example.taptalk.ui.theme.TapTalkTheme
import java.util.Calendar

private val GreenBg = Color(0xFFE6F2E6)
private val BrandGreen = Color(0xFF1A3B1A)
private val BrandPurple = Color(0xFF7B4B9A)

/**
 * Activity for user registration.
 *
 * This activity provides a user interface for new users to register for the TapTalk application.
 * It hosts the [RegisterScreen] Composable, which contains the registration form fields
 * and logic for handling user input and submission.
 * Upon successful registration, it navigates the user to the [AccActivity].
 */
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
                                startActivity(Intent(this@RegisterActivity, AccActivity::class.java))
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
 *
 * This component is used to add a subtle visual effect to the background, transitioning
 * from transparent to light purple shades. It fills the width of its parent and has a fixed height.
 *
 * @param modifier The modifier to be applied to the gradient container. It is aligned to the bottom center
 * in the parent `Box`.
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
 * A styled [TextField] composable with a distinct purple background, rounded corners,
 * and white text, used for input fields in the registration form.
 *
 * @param value The current text value of the field.
 * @param onValueChange The callback that is triggered when the input service updates the text.
 * @param placeholder The text to be displayed when the input field is empty.
 * @param isPassword If true, the text will be visually transformed to obscure it,
 * suitable for password entry. Defaults to false.
 */
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

/**
 * A Composable that displays a checkbox followed by a text label.
 *
 * This is a small helper component that arranges a [Checkbox] and a [Text]
 * horizontally in a [Row]. It's used for creating labeled checkbox options
 * in the registration form.
 *
 * @param checked The current checked state of the checkbox.
 * @param onCheckedChange A callback that is invoked when the user clicks the checkbox,
 *                        providing the new checked state.
 * @param label The text to display next to the checkbox.
 */
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

/**
 * A Composable that displays a field for date selection.
 *
 * This component looks like a text field but, when clicked, opens a native Android
 * [android.app.DatePickerDialog] to allow the user to pick a date. The selected date
 * is then displayed within the field. If no date is selected, it shows a placeholder label.
 *
 * @param label The placeholder text to display when no date has been selected. Defaults to "Date of Birth".
 * @param selectedDate The currently selected date string in "YYYY-MM-DD" format. An empty string signifies no selection.
 * @param onDateSelected A callback function that is invoked when the user selects a date from the dialog.
 *                       The selected date is passed as a string in "YYYY-MM-DD" format.
 */
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

    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

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


/**
 * A composable that displays the user registration form.
 *
 * This screen provides input fields for the user's name, email, password, date of birth,
 * and an optional PIN. It also includes checkboxes for login preferences and accepting
 * the terms of service. The form's state is managed by a [RegisterViewModel].
 * User interactions, such as button clicks, trigger validation and registration logic
 * within the ViewModel. The UI updates to show loading indicators, error messages,
 * or a success state based on the data flowing from the ViewModel.
 *
 * @param viewModel The [RegisterViewModel] instance used to manage the state and logic of the registration process.
 *                  It defaults to a ViewModel provided by `viewModel()`.
 * @param onSuccess A lambda function to be invoked when the registration is successful.
 *                  This is used for navigation or other side effects upon completion.
 */
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

    var acceptTerms by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

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
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PurpleField(name, { name = it }, "Name")
                    Spacer(Modifier.height(16.dp))
                    PurpleField(email, { email = it }, "E-mail")
                }
                Column(modifier = Modifier.weight(1f)) {
                    PurpleField(password, { password = it }, "Password", isPassword = true)
                    Spacer(Modifier.height(16.dp))
                    PurpleField(repeatPassword, { repeatPassword = it }, "Repeat Password", isPassword = true)
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier.fillMaxWidth(0.6f),
                contentAlignment = Alignment.Center
            ) {
                DatePickerField(
                    selectedDate = dob,
                    onDateSelected = { dob = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Checkbox(
                    checked = acceptTerms,
                    onCheckedChange = { acceptTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandGreen)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "By registering I accept the Terms of Service and Privacy Policy",
                    fontSize = 12.sp,
                    color = Color(0xFF1F1F1F),
                    lineHeight = 16.sp,
                    modifier = Modifier.clickable { showTerms = true }
                )
            }

            if (showTerms) {
                TermsDialog(onDismiss = { showTerms = false })
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.register(
                        name = name.trim(),
                        email = email.trim(),
                        password = password,
                        repeatPassword = repeatPassword,
                        dob = dob.trim(),
                        pin = null,
                        optPassword = true,
                        optPin = false
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
fun TermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BrandGreen)
            }
        },
        title = { Text("TapTalk Terms of Service & Privacy Policy") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = """
                    TapTalk complies with EU GDPR regulations.
                    
                    • We collect only necessary data to create and manage your account.
                    • Your data will not be shared with third parties without consent.
                    • You can request deletion of your account at any time.
                    • By registering, you agree to responsible app usage according to EU law.

                    For full details, please read our Privacy Policy at:
                    https://taptalk.example.com/privacy
                    """.trimIndent(),
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
