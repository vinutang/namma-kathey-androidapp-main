package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapplication.auth.AppIconAliasManager
import com.example.myapplication.prefs.AppPrefs
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(AppPrefs.FILE_UI, MODE_PRIVATE)
        val isEnglish = prefs.getString(AppPrefs.KEY_LANGUAGE_CODE, AppPrefs.LANG_EN) != AppPrefs.LANG_KN
        AppIconAliasManager.syncOnColdStart(this, savedLanguageUsesEnglish = isEnglish)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SplashIntro(onFinished = ::openNextScreen)
                }
            }
        }
    }

    private fun openNextScreen() {
        val session = NammaKatheyApplication.userSession(this)
        val target = if (session.canAccessMainContent()) MainActivity::class.java else AuthActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }
}

@Composable
private fun SplashIntro(onFinished: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        var targetLogoScale by remember { mutableFloatStateOf(0.38f) }
        val logoScale by animateFloatAsState(
            targetValue = targetLogoScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "logoScaleSpring",
        )
        var targetTitleYOffset by remember { mutableFloatStateOf(42f) }
        val titleSlide by animateFloatAsState(
            targetValue = targetTitleYOffset,
            animationSpec = tween(700),
            label = "titleSlide",
        )
        var targetAlpha by remember { mutableFloatStateOf(0f) }
        val titleAlpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(700),
            label = "titleFade",
        )

        LaunchedEffect(Unit) {
            targetLogoScale = 1f
            delay(180)
            targetAlpha = 1f
            targetTitleYOffset = 0f
            delay(1820)
            onFinished()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.42f))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground_art),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(220.dp)
                    .scale(logoScale),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .offset(y = titleSlide.dp)
                    .alpha(titleAlpha.coerceIn(0f, 1f)),
            )
            Spacer(modifier = Modifier.weight(0.58f))
        }
    }
}
