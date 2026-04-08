package com.petmatch.mobile.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.ConversationItem
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.PetMatchLoading
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// CreateGroupChatScreen – Tạo nhóm chat mới
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupChatScreen(
    navController: NavController,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val loading by chatVm.groupLoading.collectAsState()
    val error by chatVm.groupError.collectAsState()
    val success by chatVm.groupCreateSuccess.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var groupAvatarUri by remember { mutableStateOf<Uri?>(null) }
    val selectedMembers = remember { mutableStateListOf<ConversationItem>() }
    var step by remember { mutableIntStateOf(1) }  // 1=Select members, 2=Name group

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { groupAvatarUri = it }
    }

    // Load danh sách người đã match (conversations thật từ API)
    val conversations by chatVm.conversations.collectAsState()
    val conversationsLoading by chatVm.conversationsLoading.collectAsState()

    LaunchedEffect(Unit) {
        // Nếu chưa load conversations thì load ngay
        if (conversations.isEmpty()) {
            chatVm.loadConversations(ctx)
        }
    }

    // Dùng danh sách matched users thật thay vì mock
    val allContacts = conversations

    LaunchedEffect(success) {
        if (success) {
            chatVm.resetGroupCreateSuccess()
            navController.popBackStack()
        }
    }

    val filteredContacts = allContacts.filter {
        searchQuery.isEmpty() || it.userName.contains(searchQuery, ignoreCase = true)
    }

    val isNameValid = groupName.isNotBlank()
    val hasMembersSelected = selectedMembers.size >= 1  // At least 1 other member (creator + 1)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == 2) step = 1
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                title = {
                    Column {
                        Text(
                            if (step == 1) "Chọn thành viên" else "Đặt tên nhóm",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            "Bước $step / 2",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f))
                        )
                    }
                },
                actions = {
                    if (step == 1 && hasMembersSelected) {
                        TextButton(onClick = { step = 2 }) {
                            Text(
                                "Tiếp tục",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Progress indicator ───────────────────────────────────────────
            LinearProgressIndicator(
                progress = { if (step == 1) 0.5f else 1f },
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryPink,
                trackColor = PrimaryPink.copy(0.2f)
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
                label = "step_transition"
            ) { currentStep ->
                when (currentStep) {
                    1 -> SelectMembersStep(
                        contacts = filteredContacts,
                        isLoadingContacts = conversationsLoading,
                        selectedMembers = selectedMembers,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onToggleMember = { contact ->
                            if (contact in selectedMembers) selectedMembers.remove(contact)
                            else selectedMembers.add(contact)
                        },
                        onNext = { step = 2 }
                    )
                    2 -> NameGroupStep(
                        groupName = groupName,
                        onGroupNameChange = { groupName = it },
                        selectedMembers = selectedMembers,
                        isLoading = loading,
                        error = error,
                        groupAvatarUri = groupAvatarUri,
                        onAvatarPick = { imagePicker.launch("image/*") },
                        onCreate = {
                            if (isNameValid && hasMembersSelected) {
                                chatVm.createGroup(
                                    ctx = ctx,
                                    name = groupName,
                                    avatarUri = groupAvatarUri,
                                    memberIds = selectedMembers.map { it.userId }
                                ) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── Step 1: Select Members ────────────────────────────────────────────────────
@Composable
private fun SelectMembersStep(
    contacts: List<ConversationItem>,
    isLoadingContacts: Boolean = false,
    selectedMembers: List<ConversationItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleMember: (ConversationItem) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoadingContacts && contacts.isEmpty()) {
            PetMatchLoading()
            return@Column
        }
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder = { Text("Tìm kiếm bạn bè...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, null, tint = TextSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = Divider,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = SurfaceVariant
            ),
            singleLine = true
        )

        // Selected preview
        AnimatedVisibility(visible = selectedMembers.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Đã chọn (${selectedMembers.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPink
                        )
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedMembers, key = { it.userId }) { member ->
                        SelectedMemberChip(member = member, onRemove = { onToggleMember(member) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Divider)
            }
        }

        // All contacts
        Text(
            "Bạn bè của bạn",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(contacts, key = { it.userId }) { contact ->
                val isSelected = contact in selectedMembers
                ContactSelectRow(
                    contact = contact,
                    isSelected = isSelected,
                    onClick = { onToggleMember(contact) }
                )
            }
        }

        // Bottom action
        Surface(shadowElevation = 8.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                GradientButton(
                    text = if (selectedMembers.isEmpty()) "Chọn ít nhất 1 thành viên"
                           else "Tiếp tục với ${selectedMembers.size} thành viên →",
                    onClick = { if (selectedMembers.isNotEmpty()) onNext() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedMembers.isNotEmpty()
                )
            }
        }
    }
}

// ── Step 2: Name the Group ────────────────────────────────────────────────────
@Composable
private fun NameGroupStep(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    selectedMembers: List<ConversationItem>,
    isLoading: Boolean,
    error: String?,
    groupAvatarUri: Uri?,
    onAvatarPick: () -> Unit,
    onCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Group icon picker area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(petMatchGradient)
                        .clickable { onAvatarPick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (groupAvatarUri != null) {
                        AsyncImage(
                            model = groupAvatarUri,
                            contentDescription = "Ảnh nhóm",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (groupName.isNotEmpty()) {
                        Text(
                            groupName.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt, null,
                                tint = Color.White.copy(0.8f),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Ảnh nhóm",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f))
                            )
                        }
                    }
                }
                Text(
                    "Chọn ảnh đại diện nhóm",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryPink
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group name
            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tên nhóm *") },
                placeholder = { Text("Vd: Hội yêu chó cún, Nhóm mèo xinh...") },
                leadingIcon = { Icon(Icons.Default.Groups, null, tint = PrimaryPink) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPink,
                    unfocusedBorderColor = Divider
                ),
                singleLine = true,
                supportingText = {
                    Text(
                        "${groupName.length}/100",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            )

            // Member preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Divider),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Thành viên nhóm (${selectedMembers.size + 1})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    // Creator chip
                    MemberPreviewChip(name = "Bạn (người tạo)", isCreator = true)
                    // Selected members
                    selectedMembers.forEach { member ->
                        MemberPreviewChip(name = member.userName, isCreator = false)
                    }
                }
            }

            // Error
            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = DislikeRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Create button
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPink)
                }
            } else {
                GradientButton(
                    text = "Tạo nhóm 👥",
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = groupName.isNotBlank()
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Contact Row (selectable) ──────────────────────────────────────────────────
@Composable
private fun ContactSelectRow(
    contact: ConversationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) PrimaryPink.copy(0.06f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box {
            AsyncImage(
                model = contact.userAvatar ?: "https://loremflickr.com/48/48/dog?lock=${contact.userId}",
                contentDescription = contact.userName,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (contact.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .background(LikeGreen, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }

        Text(
            contact.userName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        )

        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) PrimaryPink else Divider,
                    shape = CircleShape
                )
                .background(
                    if (isSelected) PrimaryPink else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ── Selected Member Chip (in horizontal list) ─────────────────────────────────
@Composable
private fun SelectedMemberChip(member: ConversationItem, onRemove: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            AsyncImage(
                model = member.userAvatar ?: "https://loremflickr.com/44/44/dog?lock=${member.userId}",
                contentDescription = member.userName,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryPink, CircleShape),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(DislikeRed, CircleShape)
                    .offset(x = 2.dp, y = (-2).dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        Text(
            member.userName.take(8) + if (member.userName.length > 8) "..." else "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

// ── Member Preview Chip (in name step) ───────────────────────────────────────
@Composable
private fun MemberPreviewChip(name: String, isCreator: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isCreator) petMatchGradient else Brush.linearGradient(listOf(AccentPurple.copy(0.2f), AccentPurple.copy(0.2f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isCreator) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                null,
                tint = if (isCreator) Color.White else AccentPurple,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(name, style = MaterialTheme.typography.bodySmall)
        if (isCreator) {
            Surface(color = PrimaryPink.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    "Admin",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(color = PrimaryPink)
                )
            }
        }
    }
}
