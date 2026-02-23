package com.example.astronomic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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

    // Используем простое сеточное расположение без LazyVerticalGrid
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        // Первая строка (2 новости)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Новость 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NewsQuarter(
                    newsItem = newsItems[0],
                    onLike = { viewModel.likeNews(0) }
                )
            }

            // Новость 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NewsQuarter(
                    newsItem = newsItems[1],
                    onLike = { viewModel.likeNews(1) }
                )
            }
        }

        // Вторая строка (2 новости)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Новость 3
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NewsQuarter(
                    newsItem = newsItems[2],
                    onLike = { viewModel.likeNews(2) }
                )
            }

            // Новость 4
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NewsQuarter(
                    newsItem = newsItems[3],
                    onLike = { viewModel.likeNews(3) }
                )
            }
        }
    }
}

@Composable
fun NewsQuarter(
    newsItem: NewsItem,
    onLike: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),  // Небольшой отступ между карточками
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 90% - контент новости
            Box(
                modifier = Modifier
                    .weight(9f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = newsItem.news.title,
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = newsItem.news.content,
                        fontSize = 16.sp,
                        maxLines = 6
                    )
                }
            }

            // 10% - лайки (кликабельно)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onLike() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "❤️ ЛАЙК",
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = newsItem.likes.toString(),
                    fontSize = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}