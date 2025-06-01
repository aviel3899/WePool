package com.wepool.app.ui.screens.adminScreens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons

@Composable
fun AdminMenuScreen(
    uid: String,
    navController: NavController
) {
    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Admin Menu",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DashboardButton(
                            label = "Companies",
                            iconRes = R.drawable.company_svgrepo_com,
                            onClick = { navController.navigate("companyList/$uid") }
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        DashboardButton(
                            label = "Users",
                            iconRes = R.drawable.users_young_svgrepo_com,
                            onClick = { navController.navigate("userList/$uid") }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DashboardButton(
                            label = "Rides",
                            iconRes = R.drawable.car_svgrepo_com,
                            onClick = { navController.navigate("ridesList/$uid") }
                        )
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

@Composable
fun DashboardButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.medium,
            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(72.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2
        )
    }
}
