package com.hereliesaz.et2bruteforce

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.et2bruteforce.data.SettingsRepository
import com.hereliesaz.et2bruteforce.ui.theme.Et2BruteForceTheme
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor1
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor2
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor3
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor4
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor5
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor6
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor7
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor8
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalkthroughActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val walkthroughColors = listOf(
            WalkthroughColor1,
            WalkthroughColor2,
            WalkthroughColor3,
            WalkthroughColor4,
            WalkthroughColor5,
            WalkthroughColor6,
            WalkthroughColor7,
            WalkthroughColor8
        )

        val pages = listOf(
            WalkthroughPage("Welcome!", "This app helps you automate repetitive tasks by simulating screen taps.", Icons.Default.Celebration, WalkthroughColor1),
            WalkthroughPage("Permissions", "First, please grant both 'Draw Over Other Apps' and 'Accessibility Service' permissions on the main screen.", Icons.Default.Shield, WalkthroughColor2),
            WalkthroughPage("The Shortcut", "Press (Ctrl + G) on a physical keyboard to show or hide the floating menu at any time.", Icons.Default.Keyboard, WalkthroughColor3),
            WalkthroughPage("The Menu", "Tap the floating dagger icon to open the menu. You can drag the icon anywhere on the screen.", Icons.Default.Menu, WalkthroughColor4),
            WalkthroughPage("Identify Input", "Drag the 'Input' button over the text field where the app should type (e.g., a password field).", Icons.Default.TextFields, WalkthroughColor5),
            WalkthroughPage("Identify Submit", "Drag the 'Submit' button over the button that submits the form (e.g., a 'Login' button).", Icons.Default.ArrowForward, WalkthroughColor6),
            WalkthroughPage("Identify Popups", "If a popup appears after a failed attempt, drag the 'Popup' button over the button that closes it (e.g., 'OK').", Icons.Default.Warning, WalkthroughColor7),
            WalkthroughPage("You're All Set!", "Once the 'Input' and 'Submit' buttons have identified their targets, you can start the process from the menu.", Icons.Default.CheckCircle, WalkthroughColor8)
        )

        setContent {
            Et2BruteForceTheme {
                val pagerState = rememberPagerState(pageCount = { pages.size })
                val scope = rememberCoroutineScope()

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { pageIndex ->
                            WalkthroughPageContent(page = pages[pageIndex])
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Text("Prev")
                            }

                            Text(text = "${pagerState.currentPage + 1} / ${pages.size}")

                            if (pagerState.currentPage < pages.size - 1) {
                                Button(onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }) {
                                    Text("Next")
                                }
                            } else {
                                Button(onClick = {
                                    scope.launch {
                                        settingsRepository.updateWalkthroughCompleted(true)
                                        finish()
                                    }
                                }) {
                                    Text("Finish")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class WalkthroughPage(
    val title: String,
    val text: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun WalkthroughPageContent(page: WalkthroughPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = page.title,
            modifier = Modifier.size(128.dp),
            tint = page.color
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
