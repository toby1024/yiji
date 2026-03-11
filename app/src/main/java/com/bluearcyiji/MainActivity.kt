package com.bluearcyiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bluearcyiji.ui.theme.YIJITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YIJITheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,

        // 底部菜单
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {

            Text("Home Page")

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    YIJITheme {
        MainScreen()
    }
}