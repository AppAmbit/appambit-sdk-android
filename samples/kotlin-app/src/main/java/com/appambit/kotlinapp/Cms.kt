package com.appambit.kotlinapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.appambit.kotlinapp.models.CmsExampleModel
import com.appambit.sdk.Cms
import com.appambit.sdk.services.interfaces.ICmsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val imageHttpClient = OkHttpClient()
private const val CMS_CONTENT_TYPE = "android_blog_posts"

@Composable
fun Cms() {
    val scope = rememberCoroutineScope()
    var posts by remember { mutableStateOf(emptyList<CmsExampleModel>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadPosts(query: ICmsQuery<CmsExampleModel>) {
        errorMessage = null
        query.getList()
            .then { result ->
                if (result != null) {
                    posts = result
                }
            }
            .onError { error ->
                errorMessage = error.message ?: "Unknown error"
            }
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadPosts(Cms.content(CMS_CONTENT_TYPE, CmsExampleModel::class.java))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CMS Blog Posts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        errorMessage?.let {
            Text(
                text = "Error: $it",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        FilterButtons { query -> loadPosts(query) }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts) { post ->
                PostItem(post)
            }
        }
    }
}

@Composable
fun FilterButtons(onQuery: (ICmsQuery<CmsExampleModel>) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    
    val options = listOf(
        "All Posts",
        "Title = first",
        "Title ≠ first",
        "Is Published = true",
        "Is Published = false",
        "Title contains 'st'",
        "Title starts with 'f'",
        "Category IN [science]",
        "Category NOT IN [tech, news]",
        "Likes > 500",
        "Rating ≥ 2.1",
        "Reading Time < 15m",
        "Reading Time ≤ 15m",
        "Sort Published At ↑",
        "Sort Published At ↓",
        "Sort Likes ↑",
        "Sort Likes ↓",
        "Page 1 (2 per page)",
        "Page 2 (2 per page)"
    )

    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(options[selectedIndex], maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedIndex = index
                            searchText = ""
                            expanded = false
                        }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = searchText,
            onValueChange = { 
                searchText = it
                if (it.isNotEmpty() && selectedIndex != 0) {
                    selectedIndex = 0
                }
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search...") },
            singleLine = true
        )
        
        Button(onClick = {
            var query = Cms.content(CMS_CONTENT_TYPE, CmsExampleModel::class.java)

            if (searchText.isNotBlank()) {
                query = query.search(searchText.trim())
            }

            query = when (selectedIndex) {
                1 -> query.equals("title", "first")
                2 -> query.notEquals("title", "first")
                3 -> query.equals("is_published", "true")
                4 -> query.equals("is_published", "false")
                5 -> query.contains("title", "st")
                6 -> query.startsWith("title", "f")
                7 -> query.inList("category", listOf("science"))
                8 -> query.notInList("category", listOf("technology", "news"))
                9 -> query.greaterThan("likes", 500)
                10 -> query.greaterThanOrEqual("rating", 2.1)
                11 -> query.lessThan("reading_time", 15)
                12 -> query.lessThanOrEqual("reading_time", 15)
                13 -> query.orderByAscending("published_at")
                14 -> query.orderByDescending("published_at")
                15 -> query.orderByAscending("likes")
                16 -> query.orderByDescending("likes")
                17 -> query.getPage(1).getPerPage(2)
                18 -> query.getPage(2).getPerPage(2)
                else -> query
            }
            onQuery(query)
        }) {
            Text("Run")
        }
    }
}

@Composable
fun PostItem(post: CmsExampleModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            post.featuredImage?.let { url ->
                NetworkImage(url)
            }

            Text(text = post.title ?: "No Title", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = post.body ?: "", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${post.author} in ${post.category?.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "❤️ ${post.likes}")
                Text(text = "⭐ ${post.rating}/5")
                Text(text = "📖 ${post.readingTime} min")
            }

            post.isPublished?.let { published ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (published)
                        androidx.compose.ui.graphics.Color(0xFF2E7D32).copy(alpha = 0.12f)
                    else
                        androidx.compose.ui.graphics.Color(0xFFE65100).copy(alpha = 0.12f),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = if (published) "✓ Published" else "✕ Draft",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (published)
                            androidx.compose.ui.graphics.Color(0xFF2E7D32)
                        else
                            androidx.compose.ui.graphics.Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkImage(url: String) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(url) {
        val loadedBitmap = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get().build()
                imageHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        null
                    } else {
                        response.body?.byteStream()?.use(BitmapFactory::decodeStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        bitmap = loadedBitmap
    }
    
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )
    }
}
