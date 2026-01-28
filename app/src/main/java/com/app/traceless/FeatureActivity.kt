package com.app.traceless

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.app.traceless.ui.theme.SDKTraceLessTheme
import timber.log.Timber

class FeatureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Analytics.enterScreen(AppUIScreen.Feature)
        
        enableEdgeToEdge()
        setContent {
            SDKTraceLessTheme {
                FeatureDemoApp()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
    }
}

@Composable
fun FeatureDemoApp() {
    val listState = rememberLazyListState()
    var currentScreen by remember { mutableStateOf("feature") }
    var interactionCount by remember { mutableIntStateOf(0) }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Feature Analytics Demo",
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
            
            Text(
                text = "Interactions: $interactionCount",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ClickActionsCard(
                        onInteraction = { elementId, action ->
                            interactionCount++
                            Analytics.trackUI(elementId, action)
                            Timber.d("Feature Demo: $elementId ${action.value} (interactions: $interactionCount)")
                        }
                    )
                }
                
                item {
                    SubmitActionsCard(
                        onInteraction = { elementId, action ->
                            interactionCount++
                            Analytics.trackUI(elementId, action)
                            Timber.d("Feature Demo: $elementId ${action.value} (interactions: $interactionCount)")
                        }
                    )
                }
                
                item {
                    ScrollDemoCard(
                        onInteraction = { elementId, action ->
                            interactionCount++
                            Analytics.trackUI(elementId, action)
                            Timber.d("Feature Demo: $elementId ${action.value} (interactions: $interactionCount)")
                        }
                    )
                }
                
                item {
                    CustomActionsCard(
                        onInteraction = { elementId, action ->
                            interactionCount++
                            Analytics.trackUI(elementId, action)
                            Timber.d("Feature Demo: $elementId ${action.value} (interactions: $interactionCount)")
                        }
                    )
                }
                
                item {
                    NavigationCard(
                        onInteraction = { elementId, action ->
                            interactionCount++
                            Analytics.trackUI(elementId, action)
                            Timber.d("Feature Demo: $elementId ${action.value} (interactions: $interactionCount)")
                        }
                    )
                }
                
                itemsIndexed((1..20).toList()) { index, item ->
                    ScrollContentItem(index = index)
                }
            }
        }
    }
    
    // Track scroll events
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex > 0) {
            Analytics.trackUI("feature_content", UIAction.Click)
            Timber.d("Feature Demo: Scrolled to item ${listState.firstVisibleItemIndex}")
        }
    }
}



@Composable
fun ClickActionsCard(onInteraction: (String, UIAction) -> Unit) {
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
                text = "Click Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onInteraction("btn_primary", UIAction.Click) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Primary", style = MaterialTheme.typography.bodySmall)
                }
                
                Button(
                    onClick = { onInteraction("btn_secondary", UIAction.Click) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Secondary", style = MaterialTheme.typography.bodySmall)
                }
                
                Button(
                    onClick = { onInteraction("btn_tertiary", UIAction.Click) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tertiary", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SubmitActionsCard(onInteraction: (String, UIAction) -> Unit) {
    var formData by remember { mutableStateOf("") }
    
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
                text = "Submit Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Form Data: $formData",
                style = MaterialTheme.typography.bodySmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        formData = "User input"
                        onInteraction("form_contact", UIAction.Submit)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Contact", style = MaterialTheme.typography.bodySmall)
                }
                
                Button(
                    onClick = { 
                        formData = "Purchase data"
                        onInteraction("form_purchase", UIAction.Submit)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Purchase", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ScrollDemoCard(onInteraction: (String, UIAction) -> Unit) {
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
                text = "Scroll Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Button(
                onClick = { onInteraction("feature_list", UIAction.Click) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simulate Scroll Event", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CustomActionsCard(onInteraction: (String, UIAction) -> Unit) {
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
                text = "Custom Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onInteraction("gesture_swipe", UIAction.Swipe) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Swipe", style = MaterialTheme.typography.bodySmall)
                }

            }
        }
    }
}

@Composable
fun NavigationCard(onInteraction: (String, UIAction) -> Unit) {
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
                text = "Navigation Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Button(
                onClick = { onInteraction("btn_back", UIAction.Click) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ScrollContentItem(index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Scroll Content Item $index",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeatureDemoAppPreview() {
    SDKTraceLessTheme {
        FeatureDemoApp()
    }
}