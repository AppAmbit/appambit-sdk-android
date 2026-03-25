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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.appambit.kotlinapp.models.Post
import com.appambit.sdk.Cms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun Cms() {
    val scope = rememberCoroutineScope()
    var posts by remember { mutableStateOf(emptyList<Post>()) }

    fun loadPosts(query: Cms.CmsQuery<Post>) {
        scope.launch(Dispatchers.IO) {
            try {
                query.getList().then { result ->
                    if (result != null) {
                        posts = result
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadPosts(Cms.content("blog_posts", Post::class.java))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CMS Blog Posts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
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
fun FilterButtons(onQuery: (Cms.CmsQuery<Post>) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { FilterButton("All Posts") { onQuery(Cms.content("blog_posts", Post::class.java)) } }
        item { FilterButton("Category = technology") { onQuery(Cms.content("blog_posts", Post::class.java).equals("category", "technology")) } }
        item { FilterButton("Category ≠ test") { onQuery(Cms.content("blog_posts", Post::class.java).notEquals("category", "test")) } }
        item { FilterButton("Search 'test'") { onQuery(Cms.content("blog_posts", Post::class.java).search("test")) } }
        item { FilterButton("Title contains 'st'") { onQuery(Cms.content("blog_posts", Post::class.java).contains("title", "st")) } }
        item { FilterButton("Body starts with 'orem'") { onQuery(Cms.content("blog_posts", Post::class.java).startsWith("body", "orem")) } }
        item { FilterButton("Category IN [science, technology]") { onQuery(Cms.content("blog_posts", Post::class.java).inList("category", listOf("science", "technology"))) } }
        item { FilterButton("Category NOT IN [technology, test]") { onQuery(Cms.content("blog_posts", Post::class.java).notInList("category", listOf("technology", "test"))) } }
        item { FilterButton("Likes > 1000") { onQuery(Cms.content("blog_posts", Post::class.java).greaterThan("likes", 1000)) } }
        item { FilterButton("Rating ≥ 4.3") { onQuery(Cms.content("blog_posts", Post::class.java).greaterThanOrEqual("rating", 4.3)) } }
        item { FilterButton("Reading Time < 15m") { onQuery(Cms.content("blog_posts", Post::class.java).lessThan("reading_time", 15)) } }
        item { FilterButton("Reading Time ≤ 15m") { onQuery(Cms.content("blog_posts", Post::class.java).lessThanOrEqual("reading_time", 15)) } }
        item { FilterButton("Sort Title ↑") { onQuery(Cms.content("blog_posts", Post::class.java).orderByAscending("title")) } }
        item { FilterButton("Sort Title ↓") { onQuery(Cms.content("blog_posts", Post::class.java).orderByDescending("title")) } }
        item { FilterButton("Page 1 (2 per page)") { onQuery(Cms.content("blog_posts", Post::class.java).setPage(1).setPerPage(2)) } }
    }
}

@Composable
fun FilterButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}

@Composable
fun PostItem(post: Post) {
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
                text = "${post.author} in ${post.category}",
                style = MaterialTheme.typography.labelSmall,
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
        }
    }
}

@Composable
fun NetworkImage(url: String) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(url).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
