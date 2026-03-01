package com.example.astronomic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {

    private val allNews = listOf(
        News(1, "🚀 ОТКРЫТА ГАЛАКТИКА", "Телескоп Джеймс Уэбб обнаружил галактику на расстоянии 13 млрд световых лет"),
        News(2, "🛸 ЗАПУСК SPACEX", "Успешный запуск миссии к Международной космической станции"),
        News(3, "☀️ СОЛНЕЧНОЕ ЗАТМЕНИЕ", "Жители Северной Америки наблюдали кольцеобразное затмение"),
        News(4, "🤖 МАРСОХОД", "Найдены следы древнего озера на Марсе"),
        News(5, "⚒ ТРУДКРУТ В КОСМОСЕ", "Студенты Новосибирска покаряют целину на межгалактической студенческой стройке"),
        News(6, "☄️ АСТЕРОИД", "К Земле приближается потенциально опасный астероид"),
        News(7, "✨ СЕВЕРНОЕ СИЯНИЕ", "Необычно яркое северное сияние наблюдали в Европе"),
        News(8, "🌕 ПОЛЕТ НА ЛУНУ", "Объявлена дата первой туристической миссии"),
        News(9, "📡 РАДИОСИГНАЛ", "Зафиксирован загадочный повторяющийся сигнал"),
        News(10, "🪐 СПУТНИК ЮПИТЕРА", "Астрономы открыли 80-й спутник планеты-гиганта")
    )

    // Хранилище лайков для всех новостей
    private val savedLikesMap = mutableMapOf<Int, Int>()
    private val _newsItems = MutableStateFlow(
        allNews.shuffled().take(4).map { news ->
            val savedLikes = savedLikesMap[news.id] ?: 0
            NewsItem(news, savedLikes)
        }
    )
    val newsItems: StateFlow<List<NewsItem>> = _newsItems

    init {
        startNewsRotation()
    }

    private fun startNewsRotation() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                rotateRandomNews()
            }
        }
    }

    fun likeNews(index: Int) {
        val currentList = _newsItems.value.toMutableList()
        if (index < currentList.size) {
            val newsItem = currentList[index]
            val newLikes = newsItem.likes + 1

            // Сохраняем лайк в общее хранилище
            savedLikesMap[newsItem.news.id] = newLikes

            val updatedItem = newsItem.copy(likes = newLikes)
            currentList[index] = updatedItem
            _newsItems.value = currentList
        }
    }

    private fun rotateRandomNews() {
        val currentList = _newsItems.value.toMutableList()
        val randomIndex = (0..3).random()

        val currentNewsIds = currentList.map { it.news.id }.toSet()

        val availableNews = allNews.filter { it.id !in currentNewsIds }

        if (availableNews.isNotEmpty()) {
            val randomNews = availableNews.random()
            val savedLikes = savedLikesMap[randomNews.id] ?: 0

            currentList[randomIndex] = NewsItem(randomNews, savedLikes)
            _newsItems.value = currentList
        } else {
            val randomNews = allNews.random()
            val savedLikes = savedLikesMap[randomNews.id] ?: 0
            currentList[randomIndex] = NewsItem(randomNews, savedLikes)
            _newsItems.value = currentList
        }
    }
}