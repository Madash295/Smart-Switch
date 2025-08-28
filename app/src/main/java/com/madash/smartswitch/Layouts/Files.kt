package com.madash.smartswitch.Layouts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.madash.smartswitch.SmartBottomBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFiles(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) } // Start at 'Photos' tab
    val tabTitles = listOf(
        "Photos", "Videos","Music",  "Contacts", "Apps", "Documents", "Archives","Files"
    )
    var isGrid by remember { mutableStateOf(true) }

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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { SmartBottomBar(navController) }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            // Scrollable Tabs, with start/end padding for better spacing
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
                            Text(
                                title,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    )
                }
            }
            // Main Content (Photos example for now)
            if (tabTitles[selectedTab] == "Photos") {
                SectionedFilesDisplay(
                    isGrid = isGrid,
                    onGridToggle = { isGrid = !isGrid }
                )
            } else {
                // Placeholder for other categories
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Content for ${tabTitles[selectedTab]}", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SectionedFilesDisplay(
    isGrid: Boolean,
    onGridToggle: () -> Unit
) {
    // Example photo items for demonstration
    val todayItems = listOf(
        PhotoItem(1, "IMG_001", ".jpg", "2.1 MB"),
        PhotoItem(2, "IMG_002", ".png", "1.4 MB"),
        PhotoItem(3, "Vacation2023", ".jpg", "3.2 MB"),
        PhotoItem(4, "Snap_04", ".jpeg", "2.7 MB"),
        PhotoItem(5, "Family", ".jpg", "1.9 MB"),
        PhotoItem(6, "Pet", ".jpg", "1.1 MB")
    )
    val yesterdayItems = listOf(
        PhotoItem(7, "GradPic", ".jpg", "3.3 MB"),
        PhotoItem(8, "Cat", ".jpeg", "2.3 MB"),
        PhotoItem(9, "Rose", ".png", "1.6 MB")
    )

    val Lastdateitems = listOf(
        PhotoItem(10, "GradPic", ".jpg", "3.3 MB"),
        PhotoItem(11, "Cat", ".jpeg", "2.3 MB"),
        PhotoItem(12, "Rose", ".png", "1.6 MB")
    )

    val allItems = todayItems + yesterdayItems + Lastdateitems
    val allItemIds = allItems.map { it.id }
    var checkedItems by remember { mutableStateOf(setOf<Int>()) }
    val isAllSelected by remember { derivedStateOf { checkedItems.size == allItemIds.size } }
    Column {
        // Unified top row: Toggle + Select All control
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grid/List toggle icon at start (left)
            if (!isGrid) {
                IconButton(onClick = onGridToggle) {
                    Icon(Icons.Filled.GridView, contentDescription = "Grid")
                }
            } else {
                IconButton(onClick = onGridToggle) {
                    Icon(Icons.Filled.List, contentDescription = "List")
                }
            }
            // Select All box at end (right)
            Box(
                Modifier
                    .background(
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
                            checkedItems =
                                if (checked) checkedItems.union(allItemIds) else checkedItems.minus(
                                    allItemIds
                                )
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        FileSection(
            title = "Today",
            items = todayItems,
            checkedItems = checkedItems,
            onCheckedChange = { idx, checked ->
                checkedItems = if (checked) checkedItems + idx else checkedItems - idx
            },
            isGrid = isGrid
        )
        FileSection(
            title = "Yesterday",
            items = yesterdayItems,
            checkedItems = checkedItems,
            onCheckedChange = { idx, checked ->
                checkedItems = if (checked) checkedItems + idx else checkedItems - idx
            },
            isGrid = isGrid
        )
        FileSection(
            title = "8 August 2023",
            items = Lastdateitems,
            checkedItems = checkedItems,
            onCheckedChange = { idx, checked ->
                checkedItems = if (checked) checkedItems + idx else checkedItems - idx
            },
            isGrid = isGrid
        )
        // Bottom Action Bar
        Surface(shadowElevation = 2.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${checkedItems.size} Photos Selected",
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = { /* handle send */ }) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun FileSection(
    title: String,
    items: List<PhotoItem>,
    checkedItems: Set<Int>,
    onCheckedChange: (Int, Boolean) -> Unit,
    isGrid: Boolean
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
                checked = items.all { checkedItems.contains(it.id) },
                onCheckedChange = { checked ->
                    items.forEach { onCheckedChange(it.id, checked) }
                },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
    if (isGrid) {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val itemMinSize = 110.dp
        val spacing = 8.dp
        val totalSpace = screenWidthDp - 16.dp // 8dp padding each side
        val itemsPerRow = maxOf(
            1,
            ((totalSpace.value + spacing.value) / (itemMinSize.value + spacing.value)).toInt()
        )
        val rows = (items.indices).chunked(itemsPerRow)
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(rows) { rowIndices ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowIndices.forEach { idx ->
                        val item = items[idx]
                        FileCardItem(
                            item = item,
                            checked = checkedItems.contains(item.id),
                            onCheckedChange = { checked -> onCheckedChange(item.id, checked) },
                            gridMode = true,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                    repeat(itemsPerRow - rowIndices.size) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)) { }
                    }
                }
            }
        }
    } else {
        LazyColumn(Modifier.padding(horizontal = 8.dp)) {
            items(items) { item ->
                FileCardItem(
                    item = item,
                    checked = checkedItems.contains(item.id),
                    onCheckedChange = { checked -> onCheckedChange(item.id, checked) },
                    gridMode = false
                )
            }
        }
    }
}

@Composable
fun FileCardItem(
    item: PhotoItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    gridMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (gridMode) {
        Box(
            modifier
                .aspectRatio(1f)
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
        ) {
            // Placeholder for image
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (checked) Color(0xFFCFDEFF) else Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.name + item.extension,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            // Checkbox at top right
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        Row(
            modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
                .fillMaxWidth().height(84.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

           Row(modifier= Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
               .fillMaxWidth(),
               verticalAlignment = Alignment.CenterVertically


           ) {   // Icon in colored circle
            Box(
                Modifier
                    .size(54.dp)
                    .background(Color(0xFFE5FAFF), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = Color(0xFF04C2FB),
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name + item.extension,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.size,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 8.dp),
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = Color(0xFF20A6FF)
                )
            )
        }
        }
    }
}

// Helper data class for demonstration
public data class PhotoItem(
    val id: Int,
    val name: String,
    val extension: String,
    val size: String // e.g. "2.1 MB"
)

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7_PRO)
@Composable
fun SelectFilesPreview() {
    SelectFiles(navController = NavHostController(LocalContext.current))
}