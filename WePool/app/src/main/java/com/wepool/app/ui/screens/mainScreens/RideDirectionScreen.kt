package com.wepool.app.ui.screens.mainScreens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons

@Composable
fun RideDirectionScreen(
    navController: NavController,
    uid: String,
    onToWorkClick: () -> Unit,
    onToHomeClick: () -> Unit
) {
    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Choose Direction", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = onToWorkClick,
                                modifier = Modifier.size(120.dp),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.work_is_money_svgrepo_com),
                                    contentDescription = "WorkBound",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("To Work", style = MaterialTheme.typography.labelLarge)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = onToHomeClick,
                                modifier = Modifier.size(120.dp),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.home_house_svgrepo_com),
                                    contentDescription = "WorkBound",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("To Home", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    BottomNavigationButtons(
                        uid = uid,
                        navController = navController,
                        showBackButton = true,
                        showHomeButton = true
                    )
                }
            }
        }
    }
}
