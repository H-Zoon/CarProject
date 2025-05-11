package com.devidea.aicar.ui.navi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.devidea.aicar.ui.navi.compose.Navigation
import com.devidea.aicar.ui.theme.CarProjectTheme
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