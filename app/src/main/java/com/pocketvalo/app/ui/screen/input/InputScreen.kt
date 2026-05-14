package com.pocketvalo.app.ui.screen.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pocketvalo.app.ui.navigation.Screen
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

@Composable
fun InputScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel = viewModel()
) {
    var riotId by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun onContinue() {
        if (riotId.isBlank() || tagline.isBlank()) {
            isError = true
            return
        }
        isError = false
        playerViewModel.loadPlayerData(riotId.trim(), tagline.trim())
        navController.navigate(Screen.Home.route)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "POCKET VALO",
                color = Color(0xFFFF4655),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Text(
                text = "Enter your Riot ID to continue",
                color = Color(0xFF9BA3AF),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = riotId,
                onValueChange = {
                    riotId = it
                    isError = false
                },
                label = { Text("Riot ID", color = Color(0xFF9BA3AF)) },
                placeholder = { Text("PlayerName", color = Color(0xFF4B5563)) },
                singleLine = true,
                isError = isError && riotId.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF4655),
                    unfocusedBorderColor = Color(0xFF374151),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = Color(0xFFFF4655)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tagline,
                onValueChange = {
                    tagline = it
                    isError = false
                },
                label = { Text("Tagline", color = Color(0xFF9BA3AF)) },
                placeholder = { Text("EUW / NA1 / etc", color = Color(0xFF4B5563)) },
                singleLine = true,
                isError = isError && tagline.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onContinue() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF4655),
                    unfocusedBorderColor = Color(0xFF374151),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = Color(0xFFFF4655)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (isError) {
                Text(
                    text = "Please fill in both fields",
                    color = Color(0xFFFF4655),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onContinue() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4655)
                )
            ) {
                Text(
                    text = "CONTINUE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}