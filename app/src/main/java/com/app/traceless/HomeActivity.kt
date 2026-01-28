package com.app.traceless

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.traceless.analytic.Analytics
import com.app.traceless.analytic.UIAction
import com.app.traceless.analytic.UIScreen
import com.app.traceless.ui.theme.SDKTraceLessTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.enterScreen(UIScreen.Main)
        
        enableEdgeToEdge()
        setContent {
            SDKTraceLessTheme {
                SDKDemoApp()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Analytics.enterScreen(UIScreen.Main)
    }
}

@Composable
fun SDKDemoApp() {
    var currentScreen by remember { mutableStateOf("home") }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Traceless SDK Demo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Current Screen: $currentScreen",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ButtonGroupCard(
                        title = "Navigation Screens",
                        buttons = listOf(
                            "Detail" to AppUIScreen.Detail,
                        ),
                        onButtonClick = { screenName, screen ->
                            currentScreen = screenName
                            Analytics.enterScreen(screen)
                            Analytics.trackUI("btn_${screenName.lowercase()}", UIAction.Click)
                        }
                    )
                }
                
//                item {
//                    ButtonGroupCard(
//                        title = "User Flow Screens",
//                        buttons = listOf(
//                            "Login" to Screen.Login,
//                            "Register" to Screen.Register,
//                            "Profile" to Screen.Profile
//                        ),
//                        onButtonClick = { screenName, screen ->
//                            currentScreen = screenName
//                            Analytics.enterScreen(screen)
//                            Analytics.trackUI("btn_${screenName.lowercase()}", UIAction.Click)
//                        }
//                    )
//                }
//
//                item {
//                    ButtonGroupCard(
//                        title = "E-commerce Screens",
//                        buttons = listOf(
//                            "ProductList" to Screen.ProductList,
//                            "ProductDetail" to Screen.ProductDetail,
//                            "Cart" to Screen.Cart,
//                            "Checkout" to Screen.Checkout,
//                            "OrderSuccess" to Screen.OrderSuccess
//                        ),
//                        onButtonClick = { screenName, screen ->
//                            currentScreen = screenName
//                            Analytics.enterScreen(screen)
//                            Analytics.trackUI("btn_${screenName.lowercase()}", UIAction.Click)
//                        }
//                    )
//                }
                
                item {
                    GoToFeatureCard()
                }
                
                item {
                    UIActionsCard()
                }
            }
        }
    }
}

@Composable
fun ButtonGroupCard(
    title: String,
    buttons: List<Pair<String, UIScreen>>,
    onButtonClick: (String, UIScreen) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                buttons.forEach { (name, screen) ->
                    Button(
                        onClick = { onButtonClick(name, screen) },
                        modifier = Modifier
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoToFeatureCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Feature Navigation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Button(
                onClick = {
                    Analytics.trackUI("btn_go_to_feature", UIAction.Click)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Go to Feature",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun UIActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "UI Actions Demo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Analytics.trackUI("btn_submit", UIAction.Submit)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Submit",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
//                Button(
//                    onClick = {
//                        Analytics.trackUI("content_scroll", UIAction)
//                    },
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text(
//                        text = "Scroll",
//                        style = MaterialTheme.typography.bodySmall
//                    )
//                }
                
                Button(
                    onClick = {
                        Analytics.trackUI("btn_custom", UIAction("special_action"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SDKDemoAppPreview() {
    SDKTraceLessTheme {
        SDKDemoApp()
    }
}