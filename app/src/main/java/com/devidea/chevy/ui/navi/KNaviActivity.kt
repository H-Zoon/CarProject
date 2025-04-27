package com.devidea.chevy.ui.navi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devidea.chevy.R
import com.devidea.chevy.ui.activity.ui.theme.CarProjectTheme
import com.devidea.chevy.ui.map.MapViewModel
import com.devidea.chevy.ui.navi.compose.Navigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KNaviActivity : AppCompatActivity() {

    private val viewModel: NaviViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CarProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation(viewModel)
                }
            }
        }
    }
}