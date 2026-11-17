package com.penny.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.penny.app.PennyServices
import com.penny.app.R
import com.penny.app.core.HomeLogic
import kotlinx.coroutines.delay

private val TILE_IMAGES = listOf(R.drawable.course_page, R.drawable.expense_tracker, R.drawable.predict_stocks, R.drawable.practice)

/** Flutter draws an asset's pixels as logical pixels: PNG size (px) → dp. */
private val TILE_IMAGE_DP = listOf(47 to 47, 63 to 86, 69 to 83, 115 to 111)

private fun tileImageModifier(index: Int) = Modifier.size(TILE_IMAGE_DP[index].first.dp, TILE_IMAGE_DP[index].second.dp)

/** Home (Pages/home/home.dart): greeting + avatar, 2×2 grid, Finance News, bottom bar. */
@Composable
fun HomeScreen(services: PennyServices, navigate: (String) -> Unit) {
    var username by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val articles = remember {
        HomeLogic.articles(context.assets.open("finance.json").bufferedReader().use { it.readText() })
    }

    LaunchedEffect(Unit) {
        val (u, f) = HomeLogic.identity(services.auth.customClaims(), services.prefs.username(), services.prefs.fullName())
        username = u
        fullName = f
        if (u != null) {
            // _loadProfileImage now and every 50 minutes; errors keep the old URL.
            while (true) {
                runCatching { services.profileImages.imageUrl(u) }.onSuccess { imageUrl = it }
                delay(HomeLogic.PROFILE_REFRESH_MINUTES * 60_000)
            }
        }
    }

    Scaffold(
        containerColor = PennyPalette.background,
        bottomBar = { HomeBottomBar(navigate) },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("home-page"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    HomeLogic.greeting(fullName),
                    Modifier.weight(1f).testTag("home-greeting"),
                    fontSize = 34.sp, fontWeight = FontWeight.Medium, color = PennyPalette.text,
                    lineHeight = 40.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Avatar(imageUrl)
            }
            Spacer(Modifier.height(15.dp))
            HomeLogic.TILES.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { col, tile ->
                        val index = row * 2 + col
                        GridTile(index, tile, Modifier.weight(1f)) { navigate(tile.route) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Finance News", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                modifier = Modifier.testTag("home-news-title"))
            Spacer(Modifier.height(8.dp))
            val pager = rememberPagerState { HomeLogic.NEWS_PAGES }
            val uri = LocalUriHandler.current
            HorizontalPager(pager, Modifier.fillMaxWidth().height(260.dp).testTag("home-news-pager")) { page ->
                val card = HomeLogic.newsCard(articles, page)
                if (card != null) {
                    NewsCardView(page, card) { card.url?.let { runCatching { uri.openUri(it) } } }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().testTag("home-news-dots"), horizontalArrangement = Arrangement.Center) {
                repeat(HomeLogic.NEWS_PAGES) { i ->
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(5.dp).background(
                            if (pager.currentPage == i) PennyPalette.button else PennyPalette.card, CircleShape,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar(url: String?) {
    if (url != null) {
        SubcomposeAsyncImage(
            model = url, contentDescription = "Profile picture",
            modifier = Modifier.size(60.dp).clip(CircleShape).testTag("home-avatar-image"),
            contentScale = ContentScale.Crop,
            loading = { CircularProgressIndicator() },
            error = { Icon(Icons.Filled.Error, null) },
        )
    } else {
        Box(
            Modifier.size(60.dp).background(PennyPalette.button, CircleShape).testTag("home-avatar-default"),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Person, null, tint = PennyPalette.background, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun GridTile(index: Int, tile: HomeLogic.Tile, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.aspectRatio(1f).clickable(onClick = onClick).testTag("home-tile-$index"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PennyPalette.card),
        elevation = CardDefaults.cardElevation(10.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Text(tile.title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
                lineHeight = 30.sp)
            if (index == 0) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(HomeLogic.COURSE_PROGRESS_LABEL, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                        color = PennyPalette.text, modifier = Modifier.testTag("home-course-progress"))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { HomeLogic.COURSE_PROGRESS },
                            modifier = Modifier.size(80.dp),
                            color = PennyPalette.button, trackColor = Color.Transparent,
                        )
                        Image(painterResource(TILE_IMAGES[index]), null, tileImageModifier(index))
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    Image(painterResource(TILE_IMAGES[index]), null, tileImageModifier(index))
                }
            }
        }
    }
}

@Composable
private fun NewsCardView(page: Int, card: com.penny.app.core.NewsCard, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxSize().padding(horizontal = 4.dp).clickable(onClick = onClick).testTag("news-card-$page"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PennyPalette.button),
        elevation = CardDefaults.cardElevation(1.5.dp),
    ) {
        SubcomposeAsyncImage(
            model = card.imageUrl, contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            contentScale = ContentScale.Crop,
            error = {
                Box(Modifier.fillMaxWidth().height(150.dp).background(PennyPalette.button), Alignment.Center) {
                    Icon(Icons.Filled.Error, null)
                }
            },
        )
        Column(Modifier.padding(8.dp)) {
            Text(card.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PennyPalette.background,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("news-title-$page"))
            Spacer(Modifier.height(4.dp))
            Text(card.description, fontSize = 12.sp, color = PennyPalette.background, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
private fun HomeBottomBar(navigate: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).testTag("home-bottom-bar"),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        IconButton(onClick = {}, enabled = false, modifier = Modifier.testTag("home-nav-home")) {
            Icon(Icons.Filled.Home, "Home", Modifier.size(40.dp), tint = PennyPalette.text)
        }
        IconButton(onClick = { navigate(com.penny.app.core.AppRoutes.CHATBOT) }, modifier = Modifier.testTag("home-nav-chat")) {
            Icon(Icons.Outlined.Chat, "Chatbot", Modifier.size(38.dp), tint = PennyPalette.text)
        }
        IconButton(
            onClick = { navigate(com.penny.app.core.AppRoutes.PROFILE_SETTINGS) },
            modifier = Modifier.testTag("home-nav-profile"),
        ) {
            Icon(Icons.Rounded.PersonOutline, "Profile", Modifier.size(40.dp), tint = PennyPalette.text)
        }
    }
}
