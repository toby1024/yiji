package com.bluearcyiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import com.bluearcyiji.main.MainViewModel
import com.bluearcyiji.ui.MainScreen
import com.bluearcyiji.ui.theme.YIJITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.bluearcyiji.auth.AuthManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            YIJITheme {
                val vm = remember {
                    ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
                }
                MainScreen(vm)
            }
        }
    }
}
