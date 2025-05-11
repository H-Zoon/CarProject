package com.devidea.chevy.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.devidea.chevy.R
import com.devidea.chevy.k10s.components.NeumorphicBox
import com.devidea.chevy.ui.main.compose.CarManagementMainScreen
import com.devidea.chevy.ui.theme.CarProjectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val splashScreen = installSplashScreen()

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // 아이콘 애니메이션의 전체 지속시간과 시작 시간을 가져옵니다.
            val iconAnimationDuration = splashScreenView.iconAnimationDurationMillis
            val iconAnimationStart = splashScreenView.iconAnimationStartMillis
            val iconView = splashScreenView.iconView
            val remainingDuration =
                (iconAnimationDuration - (iconAnimationStart - System.currentTimeMillis())).coerceAtLeast(
                    0L
                )
            iconView.animate().translationY(iconView.height.toFloat()).alpha(0f)
                .setStartDelay(remainingDuration).setDuration(500L).withEndAction {
                    splashScreenView.remove()
                }.start()
        }

        splashScreen.setKeepOnScreenCondition {
            false
        }

        setContent {
            CarProjectTheme {
                CarManagementMainScreen(viewModel)
            }
        }
    }

    @Composable
    fun CarInformationSummary(viewModel: MainViewModel) {
        NeumorphicBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoSection(
                    title = stringResource(R.string.summary_last_connect),
                    content = viewModel.lastConnectDate.collectAsState().value
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                InfoSection(
                    title = stringResource(R.string.summary_recent_mileage),
                    content = viewModel.recentMileage.collectAsState().value
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                InfoSection(
                    title = stringResource(R.string.summary_recent_fuel_efficiency),
                    content = viewModel.fullEfficiency.collectAsState().value
                )
            }
        }
    }

    @Composable
    fun InfoSection(title: String, content: String, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
