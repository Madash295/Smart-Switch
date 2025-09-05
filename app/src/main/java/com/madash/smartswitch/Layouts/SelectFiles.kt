package com.madash.smartswitch.Layouts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.madash.smartswitch.DataClass.AppItem
import com.madash.smartswitch.DataClass.ContactItem
import com.madash.smartswitch.DataClass.FileItem
import com.madash.smartswitch.DataClass.MediaItem
import com.madash.smartswitch.SmartSwitchTheme
import com.madash.smartswitch.util.loadApps
import com.madash.smartswitch.util.loadArchives
import com.madash.smartswitch.util.loadContacts
import com.madash.smartswitch.util.loadDocuments
import com.madash.smartswitch.util.loadFiles
import com.madash.smartswitch.util.loadMusic
import com.madash.smartswitch.util.loadPhotos
import com.madash.smartswitch.util.loadVideos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class MediaType {
    PHOTO, VIDEO, MUSIC, DOCUMENT, ARCHIVE, APK, CONTACT, FOLDER, FILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFiles(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        "Photos", "Videos", "Music", "Contacts", "Apps", "Documents", "Archives", "Files"
    )
    var isGrid by remember { mutableStateOf(true) }
    var hasPermissions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var requestPermissionsKey by remember { mutableStateOf(0) }

    // Data state
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var appItems by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var contactItems by remember { mutableStateOf<List<ContactItem>>(emptyList()) }
    var fileItems by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    // Selection state for all tabs
    var checkedMediaItems by remember { mutableStateOf(setOf<Int>()) }
    var checkedAppItems by remember { mutableStateOf(setOf<String>()) }
    var checkedContactItems by remember { mutableStateOf(setOf<Long>()) }
    var checkedFileItems by remember { mutableStateOf(setOf<String>()) }

    // Calculate total selected items across all tabs
    val totalSelectedItems by remember {
        derivedStateOf {
            checkedMediaItems.size + checkedAppItems.size + checkedContactItems.size + checkedFileItems.size
        }
    }

    val dynamic = false

    val selectedIconColor = if (dynamic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val selectedTextColor = if (dynamic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val unselectedIconColor = if (dynamic) MaterialTheme.colorScheme.onBackground.copy(0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
    val unselectedTextColor = if (dynamic) MaterialTheme.colorScheme.onBackground.copy(0.4f) else MaterialTheme.colorScheme.onSurfaceVariant

    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            requestPermissionsKey++
        }
    }

    // Check permissions and load data when tab changes or permission granted
    LaunchedEffect(selectedTab, requestPermissionsKey) {
        isLoading = true
        val requiredPermissions = getRequiredPermissions(selectedTab)
        val hasAllPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            hasPermissions = true
            loadDataForCurrentTab(selectedTab, context) { items, apps, contacts, files ->
                mediaItems = items
                appItems = apps
                contactItems = contacts
                fileItems = files
                isLoading = false
            }
        } else {
            isLoading = false
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select " + tabTitles[selectedTab],
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Fixed bottom action bar that shows total count from all tabs
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalSelectedItems Items Selected",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = { /* handle send - navigate to next screen */ },
                        enabled = totalSelectedItems > 0
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
                .pointerInput(selectedTab) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 40) {
                            if (selectedTab > 0) selectedTab--
                        }
                        if (dragAmount < -40) {
                            if (selectedTab < tabTitles.size - 1) selectedTab++
                        }
                    }
                }
        ) {
            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            val textColor = if (selectedTab == i) selectedTextColor else unselectedTextColor
                            Text(
                                title,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp),
                                color = textColor
                            )
                        },
                        selectedContentColor = selectedIconColor,
                        unselectedContentColor = unselectedIconColor
                    )
                }
            }

            // Content based on selected tab
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (!hasPermissions) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permission required to access ${tabTitles[selectedTab]}")
                        Button(
                            onClick = {
                                val requiredPermissions = getRequiredPermissions(selectedTab)
                                permissionLauncher.launch(requiredPermissions.toTypedArray())
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0, 1 -> { // Photos, Videos - Grid/List layout
                        SectionedMediaDisplay(
                            items = mediaItems,
                            isGrid = isGrid,
                            onGridToggle = { isGrid = !isGrid },
                            mediaType = if (selectedTab == 0) MediaType.PHOTO else MediaType.VIDEO,
                            checkedItems = checkedMediaItems,
                            onCheckedChange = { id, checked ->
                                checkedMediaItems = if (checked) checkedMediaItems + id else checkedMediaItems - id
                            }
                        )
                    }
                    2 -> { // Music - List only
                        SectionedMediaDisplay(
                            items = mediaItems,
                            isGrid = false,
                            onGridToggle = { },
                            mediaType = MediaType.MUSIC,
                            showGridToggle = false,
                            checkedItems = checkedMediaItems,
                            onCheckedChange = { id, checked ->
                                checkedMediaItems = if (checked) checkedMediaItems + id else checkedMediaItems - id
                            }
                        )
                    }
                    3 -> { // Contacts - List only
                        ContactsDisplay(
                            contacts = contactItems,
                            checkedContacts = checkedContactItems,
                            onCheckedChange = { id, checked ->
                                checkedContactItems = if (checked) checkedContactItems + id else checkedContactItems - id
                            }
                        )
                    }
                    4 -> { // Apps - List only
                        AppsDisplay(
                            apps = appItems,
                            checkedApps = checkedAppItems,
                            onCheckedChange = { packageName, checked ->
                                checkedAppItems = if (checked) checkedAppItems + packageName else checkedAppItems - packageName
                            }
                        )
                    }
                    5, 6 -> { // Documents, Archives - List only
                        SectionedMediaDisplay(
                            items = mediaItems,
                            isGrid = false,
                            onGridToggle = { },
                            mediaType = if (selectedTab == 5) MediaType.DOCUMENT else MediaType.ARCHIVE,
                            showGridToggle = false,
                            checkedItems = checkedMediaItems,
                            onCheckedChange = { id, checked ->
                                checkedMediaItems = if (checked) checkedMediaItems + id else checkedMediaItems - id
                            }
                        )
                    }
                    7 -> { // Files - List only
                        FilesDisplay(
                            files = fileItems,
                            checkedFiles = checkedFileItems,
                            onCheckedChange = { path, checked ->
                                checkedFileItems = if (checked) checkedFileItems + path else checkedFileItems - path
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionedMediaDisplay(
    items: List<MediaItem>,
    isGrid: Boolean,
    onGridToggle: () -> Unit,
    mediaType: MediaType,
    showGridToggle: Boolean = true,
    checkedItems: Set<Int>,
    onCheckedChange: (Int, Boolean) -> Unit
) {
    val groupedItems = items.groupBy { item ->
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = item.dateAdded * 1000

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
        }
    }

    val allItemIds = items.map { it.id.toInt() }
    val isAllSelected by remember { derivedStateOf { checkedItems.containsAll(allItemIds) && allItemIds.isNotEmpty() } }

    Column {
        // Top controls
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showGridToggle) {
                IconButton(onClick = onGridToggle) {
                    Icon(
                        if (isGrid) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                        contentDescription = if (isGrid) "List" else "Grid"
                    )
                }
            } else {
                Box(Modifier.size(48.dp)) // Spacer
            }

            Box(
                Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select All",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            allItemIds.forEach { id ->
                                onCheckedChange(id, checked)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Grouped content
        LazyColumn {
            groupedItems.forEach { (date, dateItems) ->
                item {
                    MediaSection(
                        title = date,
                        items = dateItems,
                        checkedItems = checkedItems,
                        onCheckedChange = onCheckedChange,
                        isGrid = isGrid && showGridToggle,
                        mediaType = mediaType
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsDisplay(
    contacts: List<ContactItem>,
    checkedContacts: Set<Long>,
    onCheckedChange: (Long, Boolean) -> Unit
) {
    val isAllSelected by remember {
        derivedStateOf {
            contacts.isNotEmpty() && contacts.all { checkedContacts.contains(it.id) }
        }
    }

    Column {
        // Select all header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select All",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            contacts.forEach { contact ->
                                onCheckedChange(contact.id, checked)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        LazyColumn {
            items(contacts) { contact ->
                ContactListItem(
                    contact = contact,
                    checked = checkedContacts.contains(contact.id),
                    onCheckedChange = { checked -> onCheckedChange(contact.id, checked) }
                )
            }
        }
    }
}

@Composable
fun AppsDisplay(
    apps: List<AppItem>,
    checkedApps: Set<String>,
    onCheckedChange: (String, Boolean) -> Unit
) {
    val isAllSelected by remember {
        derivedStateOf {
            apps.isNotEmpty() && apps.all { checkedApps.contains(it.packageName) }
        }
    }

    Column {
        // Select all header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select All",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            apps.forEach { app ->
                                onCheckedChange(app.packageName, checked)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        LazyColumn {
            items(apps) { app ->
                AppListItem(
                    app = app,
                    checked = checkedApps.contains(app.packageName),
                    onCheckedChange = { checked -> onCheckedChange(app.packageName, checked) }
                )
            }
        }
    }
}

@Composable
fun FilesDisplay(
    files: List<FileItem>,
    checkedFiles: Set<String>,
    onCheckedChange: (String, Boolean) -> Unit
) {
    val isAllSelected by remember {
        derivedStateOf {
            files.isNotEmpty() && files.all { checkedFiles.contains(it.path) }
        }
    }

    Column {
        // Select all header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select All",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            files.forEach { file ->
                                onCheckedChange(file.path, checked)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        LazyColumn {
            items(files) { file ->
                FileListItem(
                    file = file,
                    checked = checkedFiles.contains(file.path),
                    onCheckedChange = { checked -> onCheckedChange(file.path, checked) }
                )
            }
        }
    }
}

@Composable
fun MediaSection(
    title: String,
    items: List<MediaItem>,
    checkedItems: Set<Int>,
    onCheckedChange: (Int, Boolean) -> Unit,
    isGrid: Boolean,
    mediaType: MediaType
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Select ", fontSize = 12.sp)
            Checkbox(
                checked = items.isNotEmpty() && items.all { checkedItems.contains(it.id.toInt()) },
                onCheckedChange = { checked ->
                    items.forEach { onCheckedChange(it.id.toInt(), checked) }
                },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    if (isGrid && (mediaType == MediaType.PHOTO || mediaType == MediaType.VIDEO)) {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val itemMinSize = 110.dp
        val spacing = 8.dp
        val totalSpace = screenWidthDp - 16.dp
        val itemsPerRow = maxOf(1, ((totalSpace.value + spacing.value) / (itemMinSize.value + spacing.value)).toInt())

        val rows = items.chunked(itemsPerRow)
        rows.forEach { rowItems ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { item ->
                    MediaGridItem(
                        item = item,
                        checked = checkedItems.contains(item.id.toInt()),
                        onCheckedChange = { checked -> onCheckedChange(item.id.toInt(), checked) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(itemsPerRow - rowItems.size) {
                    Box(modifier = Modifier.weight(1f)) { }
                }
            }
        }
    } else {
        items.forEach { item ->
            MediaListItem(
                item = item,
                checked = checkedItems.contains(item.id.toInt()),
                onCheckedChange = { checked -> onCheckedChange(item.id.toInt(), checked) },
                mediaType = mediaType
            )
        }
    }
}

@Composable
fun MediaGridItem(
    item: MediaItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .aspectRatio(1f)
            .background(
                if (checked) Color(0xFFCFDEFF) else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (item.type == MediaType.PHOTO || item.type == MediaType.VIDEO) {
            if (item.type == MediaType.VIDEO) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uri)
                        .setParameter("video_frame_millis", 0)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.name,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
        ) {
            Checkbox(
                colors = CheckboxDefaults.colors(),
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MediaListItem(
    item: MediaItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    mediaType: MediaType
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(getMediaTypeColor(mediaType), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (mediaType == MediaType.PHOTO || mediaType == MediaType.VIDEO) {
                if (mediaType == MediaType.VIDEO) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            .setParameter("video_frame_millis", 0)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray, shape = RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray, shape = RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Icon(
                    imageVector = getMediaTypeIcon(mediaType),
                    contentDescription = null,
                    tint = getMediaTypeIconTint(mediaType),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.size,
                fontSize= 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ContactListItem(
    contact: ContactItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(54.dp)
                .background(Color(0xFFE8F5E8), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = contact.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            contact.phoneNumber?.let { phone ->
                Text(
                    text = phone,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun AppListItem(
    app: AppItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(54.dp)
                .background(Color.Transparent, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = app.icon),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = app.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.size,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(54.dp)
                .background(getMediaTypeColor(file.type), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Filled.Folder else getMediaTypeIcon(file.type),
                contentDescription = null,
                tint = getMediaTypeIconTint(file.type),
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = file.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (file.isDirectory) "Folder" else file.size,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(28.dp)
        )
    }
}

// Helper functions and data loading functions
private suspend fun loadDataForCurrentTab(
    tabIndex: Int,
    context: Context,
    onResult: (List<MediaItem>, List<AppItem>, List<ContactItem>, List<FileItem>) -> Unit
) {
    withContext(Dispatchers.IO) {
        when (tabIndex) {
            0 -> { // Photos
                val photos = loadPhotos(context)
                withContext(Dispatchers.Main) {
                    onResult(photos, emptyList(), emptyList(), emptyList())
                }
            }
            1 -> { // Videos
                val videos = loadVideos(context)
                withContext(Dispatchers.Main) {
                    onResult(videos, emptyList(), emptyList(), emptyList())
                }
            }
            2 -> { // Music
                val music = loadMusic(context)
                withContext(Dispatchers.Main) {
                    onResult(music, emptyList(), emptyList(), emptyList())
                }
            }
            3 -> { // Contacts
                val contacts = loadContacts(context)
                withContext(Dispatchers.Main) {
                    onResult(emptyList(), emptyList(), contacts, emptyList())
                }
            }
            4 -> { // Apps
                val apps = loadApps(context)
                withContext(Dispatchers.Main) {
                    onResult(emptyList(), apps, emptyList(), emptyList())
                }
            }
            5 -> { // Documents
                val documents = loadDocuments(context)
                withContext(Dispatchers.Main) {
                    onResult(documents, emptyList(), emptyList(), emptyList())
                }
            }
            6 -> { // Archives
                val archives = loadArchives(context)
                withContext(Dispatchers.Main) {
                    onResult(archives, emptyList(), emptyList(), emptyList())
                }
            }
            7 -> { // Files
                val files = loadFiles(context)
                withContext(Dispatchers.Main) {
                    onResult(emptyList(), emptyList(), emptyList(), files)
                }
            }
        }
    }
}

private fun getRequiredPermissions(tabIndex: Int): List<String> {
    return when (tabIndex) {
        0, 1, 2, 5, 6 -> { // Photos, Videos, Music, Documents, Archives
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when (tabIndex) {
                    0 -> listOf(Manifest.permission.READ_MEDIA_IMAGES)
                    1 -> listOf(Manifest.permission.READ_MEDIA_VIDEO)
                    2 -> listOf(Manifest.permission.READ_MEDIA_AUDIO)
                    else -> listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                }
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        3 -> listOf(Manifest.permission.READ_CONTACTS) // Contacts
        7 -> { // Files
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        else -> emptyList() // Apps don't need special permissions
    }
}

private fun getMediaTypeIcon(mediaType: MediaType): ImageVector {
    return when (mediaType) {
        MediaType.MUSIC -> Icons.Filled.MusicNote
        MediaType.DOCUMENT -> Icons.Filled.Description
        MediaType.ARCHIVE -> Icons.Filled.Archive
        MediaType.APK -> Icons.Filled.Android
        MediaType.CONTACT -> Icons.Filled.Person
        MediaType.FOLDER -> Icons.Filled.Folder
        else -> Icons.AutoMirrored.Filled.List
    }
}

private fun getMediaTypeColor(mediaType: MediaType): Color {
    return when (mediaType) {
        MediaType.PHOTO -> Color(0xFFE3F2FD)
        MediaType.VIDEO -> Color(0xFFE8F5E8)
        MediaType.MUSIC -> Color(0xFFFFF3E0)
        MediaType.DOCUMENT -> Color(0xFFF3E5F5)
        MediaType.ARCHIVE -> Color(0xFFE0F2F1)
        MediaType.APK -> Color(0xFFE8F5E8)
        MediaType.CONTACT -> Color(0xFFE8F5E8)
        MediaType.FOLDER -> Color(0xFFFFF8E1)
        else -> Color(0xFFE5FAFF)
    }
}

private fun getMediaTypeIconTint(mediaType: MediaType): Color {
    return when (mediaType) {
        MediaType.PHOTO -> Color(0xFF2196F3)
        MediaType.VIDEO -> Color(0xFF4CAF50)
        MediaType.MUSIC -> Color(0xFFFF9800)
        MediaType.DOCUMENT -> Color(0xFF9C27B0)
        MediaType.ARCHIVE -> Color(0xFF009688)
        MediaType.APK -> Color(0xFF4CAF50)
        MediaType.CONTACT -> Color(0xFF4CAF50)
        MediaType.FOLDER -> Color(0xFFFFC107)
        else -> Color(0xFF04C2FB)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7_PRO)
@Composable
fun SelectFilesPreview() {
    SelectFiles(navController = NavHostController(LocalContext.current))
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7_PRO)
@Composable
fun SelectFilesdarkPreview() {
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = false) {
        SelectFiles(navController = NavHostController(LocalContext.current))
    }
}