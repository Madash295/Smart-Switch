package com.madash.smartswitch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.madash.smartswitch.com.madash.smartswitch.DataClass.OnboardPage
import com.madash.smartswitch.com.madash.smartswitch.DataClass.pages


@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState()
    val scope      = rememberCoroutineScope()

    Scaffold {innerPadding ->


    Box(Modifier.fillMaxSize().padding(innerPadding)) {

        /* ---- Horizontal pager ---- */
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            PageContent(page = pages[index])
        }

        /* ---- Skip button on top‑right (pages 0 & 1) ---- */
        if (pagerState.currentPage < pages.lastIndex) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) { Text("Skip") }
        }

        /* ---- Dots indicator ---- */
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier   = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            activeColor   = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        /* ---- Get Started (only on last page) ---- */
        if (pagerState.currentPage == pages.lastIndex) {
            Button(
                onClick = onFinished,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) { Text("Get Started") }
        }
    }
}
}

/* ---------- single slide ---------- */
@Composable
private fun PageContent(page: OnboardPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        Icon(
            ImageVector.vectorResource(id = page.icon),
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp)
        )

        Spacer(Modifier.height(48.dp))

        Text(
            text  = page.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text  = page.body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen {}
}
