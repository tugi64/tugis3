package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CalculationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CalculationScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationScreen(onBack: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf(0.0) }
    var operation by remember { mutableStateOf("") }
    var isNewInput by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hesap Makinesi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val buttons = listOf(
                listOf("C", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=", "")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(2f)
            ) {
                buttons.flatten().filter { it.isNotEmpty() }.forEach { btn ->
                    item {
                        Button(
                            onClick = {
                                when (btn) {
                                    "C" -> {
                                        display = "0"
                                        operand1 = 0.0
                                        operation = ""
                                        isNewInput = true
                                    }
                                    "⌫" -> {
                                        display = if (display.length > 1) display.dropLast(1) else "0"
                                    }
                                    in listOf("+", "-", "×", "÷", "%") -> {
                                        operand1 = display.toDoubleOrNull() ?: 0.0
                                        operation = btn
                                        isNewInput = true
                                    }
                                    "=" -> {
                                        val operand2 = display.toDoubleOrNull() ?: 0.0
                                        val result = when (operation) {
                                            "+" -> operand1 + operand2
                                            "-" -> operand1 - operand2
                                            "×" -> operand1 * operand2
                                            "÷" -> if (operand2 != 0.0) operand1 / operand2 else 0.0
                                            "%" -> operand1 % operand2
                                            else -> operand2
                                        }
                                        display = result.toString()
                                        operation = ""
                                        isNewInput = true
                                    }
                                    else -> {
                                        if (isNewInput) {
                                            display = btn
                                            isNewInput = false
                                        } else {
                                            display += btn
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (btn) {
                                    in listOf("+", "-", "×", "÷", "%", "=") -> MaterialTheme.colorScheme.primary
                                    "C", "⌫" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        ) {
                            Text(
                                text = btn,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

