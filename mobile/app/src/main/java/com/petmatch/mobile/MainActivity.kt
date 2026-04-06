package com.petmatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.petmatch.mobile.ui.screen.*
import com.petmatch.mobile.ui.theme.PetMatchTheme
import com.petmatch.mobile.ui.theme.PrimaryPink
import com.petmatch.mobile.ui.theme.TextSecondary

sealed class Screen {
    object Messages : Screen()
    object Chat : Screen()
    object AudioCall : Screen()
    object VideoCall : Screen()
    object Appointments : Screen()
    object CreateAppointment : Screen()
    object Review : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetMatchTheme {
                PetMatchApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetMatchApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Messages) }
    var showCreateAppointmentSheet by remember { mutableStateOf(false) }

    when (currentScreen) {
        Screen.Messages -> {
            MessagesScreen(
                onConversationClick = { currentScreen = Screen.Chat }
            )
        }

        Screen.Chat -> {
            ChatScreen(
                contactName = "Trần Thị Lan",
                onBackClick = { currentScreen = Screen.Messages },
                onCallClick = { currentScreen = Screen.AudioCall },
                onVideoCallClick = { currentScreen = Screen.VideoCall },
                onAppointmentClick = { currentScreen = Screen.Appointments }
            )
        }

        Screen.AudioCall -> {
            AudioCallScreen(
                contactName = "Trần Thị Lan",
                petName = "Bella",
                onEndCall = { currentScreen = Screen.Chat }
            )
        }

        Screen.VideoCall -> {
            VideoCallScreen(
                contactName = "Trần Thị Lan",
                onEndCall = { currentScreen = Screen.Chat }
            )
        }

        Screen.Appointments -> {
            AppointmentScreen(
                onBackClick = { currentScreen = Screen.Chat },
                onCreateAppointment = { showCreateAppointmentSheet = true },
                onAppointmentClick = { currentScreen = Screen.Review }
            )

            if (showCreateAppointmentSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCreateAppointmentSheet = false }
                ) {
                    CreateAppointmentSheet(
                        contactName = "Trần Thị Lan",
                        onDismiss = { showCreateAppointmentSheet = false },
                        onConfirm = { showCreateAppointmentSheet = false }
                    )
                }
            }
        }

        Screen.Review -> {
            ReviewScreen(
                contactName = "Trần Thị Lan",
                petName = "Bella",
                meetingDate = "Thứ 7, 12/04/2026",
                meetingPlace = "Công viên Thống Nhất",
                onBackClick = { currentScreen = Screen.Appointments },
                onSubmit = { currentScreen = Screen.Messages }
            )
        }
    }
}