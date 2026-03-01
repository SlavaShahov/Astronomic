package com.example.astronomic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val newsItems by viewModel.newsItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            NewsQuarter(
                newsItem = newsItems[0],
                onLike = { viewModel.likeNews(0) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            NewsQuarter(
                newsItem = newsItems[1],
                onLike = { viewModel.likeNews(1) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            NewsQuarter(
                newsItem = newsItems[2],
                onLike = { viewModel.likeNews(2) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            NewsQuarter(
                newsItem = newsItems[3],
                onLike = { viewModel.likeNews(3) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun NewsQuarter(
    newsItem: NewsItem,
    onLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Контент новости (90%)
            Box(
                modifier = Modifier
                    .weight(9f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = newsItem.news.title,
                        fontSize = if (isLandscape()) 16.sp else 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = newsItem.news.content,
                        fontSize = if (isLandscape()) 14.sp else 12.sp,
                        maxLines = if (isLandscape()) 5 else 3
                    )
                }
            }

            // Лайки (10%)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onLike() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "❤️",
                    fontSize = if (isLandscape()) 16.sp else 14.sp
                )
                Text(
                    text = newsItem.likes.toString(),
                    fontSize = if (isLandscape()) 16.sp else 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun isLandscape(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
}