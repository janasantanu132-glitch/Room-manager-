package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.RoomManagementRoot
import com.example.ui.theme.RoomManagementTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomManagementTheme {
                val mainViewModel: MainViewModel = viewModel()
                RoomManagementRoot(viewModel = mainViewModel)
            }
        }
    }
}
