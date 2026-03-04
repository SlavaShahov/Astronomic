package com.example.astronomic

import android.content.Intent
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class PlanetInfoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PLANET_NAME = "planet_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val planetName = intent.getStringExtra(EXTRA_PLANET_NAME) ?: "Солнце"
        val planetInfo = getPlanetInfo(planetName)

        setContent {
            MaterialTheme {
                if (planetName == "Луна") {
                    MoonInfoScreen(planetInfo)
                } else {
                    PlanetInfoScreen(planetInfo)
                }
            }
        }
    }

    private fun getPlanetInfo(name: String): PlanetInfo {
        return when (name) {
            "Солнце" -> PlanetInfo(
                "☀️ СОЛНЦЕ",
                "Солнце — звезда, вокруг которой вращаются все планеты. Это гигантский раскаленный газовый шар. " +
                        "Температура на поверхности 5500°C, в ядре — 15 миллионов градусов. " +
                        "Солнце содержит 99.8% массы всей Солнечной системы. " +
                        "Свет от Солнца доходит до Земли за 8 минут. " +
                        "Возраст Солнца около 4.6 миллиарда лет, оно просуществует еще примерно столько же.",
                "sun_detail.jpg",
                "1.39 млн км",
                "0 км",
                "25 дней",
                0,
                "5500°C"
            )
            "Меркурий" -> PlanetInfo(
                "☿ МЕРКУРИЙ",
                "Самая близкая к Солнцу планета. Температура прыгает от -173°C ночью до +427°C днем — самый большой перепад. " +
                        "День на Меркурии длится 59 земных суток, а год — 88 дней. То есть день длиннее года. " +
                        "Поверхность вся в кратерах, как Луна. Железное ядро занимает 60% массы планеты.",
                "mercury_detail.jpg",
                "4,879 км",
                "57.9 млн км",
                "88 дней",
                0,
                "-173°C до +427°C"
            )
            "Венера" -> PlanetInfo(
                "♀ ВЕНЕРА",
                "Самая горячая планета — +462°C круглый год из-за парникового эффекта. Атмосфера из углекислого газа, " +
                        "давление в 92 раза выше земного (как под водой на глубине 900 м). Облака из серной кислоты. " +
                        "Вращается в другую сторону, день длиннее года (243 дня против 225).",
                "venus_detail.jpg",
                "12,104 км",
                "108.2 млн км",
                "225 дней",
                0,
                "+462°C"
            )
            "Земля" -> PlanetInfo(
                "🌍 ЗЕМЛЯ",
                "Наш дом. Единственная планета с жизнью. 71% поверхности покрыто водой. " +
                        "Атмосфера: азот (78%) и кислород (21%). Имеет один спутник — Луну. " +
                        "Ось наклонена на 23.5°, поэтому есть времена года. Возраст 4.5 миллиарда лет.",
                "earth_detail.jpg",
                "12,742 км",
                "149.6 млн км",
                "365 дней",
                1,
                "-88°C до +58°C"
            )
            "Марс" -> PlanetInfo(
                "♂ МАРС",
                "Красная планета из-за оксида железа. Тут самый высокий вулкан Олимп (21 км) и самый большой каньон. " +
                        "Атмосфера разреженная, но есть полярные шапки из льда. Имеет два спутника — Фобос и Деймос. " +
                        "Там ищут следы жизни, скоро полетят люди.",
                "mars_detail.jpg",
                "6,779 км",
                "227.9 млн км",
                "687 дней",
                2,
                "-87°C до -5°C"
            )
            "Юпитер" -> PlanetInfo(
                "♃ ЮПИТЕР",
                "Самая большая планета. Если собрать все остальные планеты вместе, Юпитер всё равно будет в 2.5 раза тяжелее. " +
                        "Имеет 79 спутников, включая Ганимед — самый большой спутник в системе. " +
                        "Большое красное пятно — гигантский шторм, бушующий сотни лет. " +
                        "Состоит из водорода и гелия, твердой поверхности нет.",
                "jupiter_detail.jpg",
                "139,820 км",
                "778.5 млн км",
                "11.9 лет",
                79,
                "-108°C"
            )
            "Сатурн" -> PlanetInfo(
                "♄ САТУРН",
                "Знаменит своими кольцами из льда и пыли. Ширина колец огромна, но толщина всего километр. " +
                        "Имеет 82 спутника, самый большой — Титан, у которого есть атмосфера и моря из метана. " +
                        "Плотность Сатурна меньше воды — если найти океан, он бы плавал.",
                "saturn_detail.jpg",
                "116,460 км",
                "1.43 млрд км",
                "29.5 лет",
                82,
                "-139°C"
            )
            "Уран" -> PlanetInfo(
                "♅ УРАН",
                "Вращается 'лежа на боку' — ось наклонена на 98°. Из-за этого времена года длятся по 20 лет. " +
                        "Имеет 27 спутников и тонкие кольца. Состоит изо льдов и камня. " +
                        "Цвет голубой из-за метана в атмосфере.",
                "uranus_detail.jpg",
                "50,724 км",
                "2.87 млрд км",
                "84 года",
                27,
                "-197°C"
            )
            "Нептун" -> PlanetInfo(
                "♆ НЕПТУН",
                "Самая дальняя планета. Там дуют самые сильные ветры — до 2100 км/ч. " +
                        "Имеет 14 спутников, самый крупный — Тритон. " +
                        "Голубой цвет тоже из-за метана. До Нептуна долетел только Вояджер-2 в 1989 году.",
                "neptune_detail.jpg",
                "49,244 км",
                "4.5 млрд км",
                "165 лет",
                14,
                "-201°C"
            )
            else -> PlanetInfo(
                "🌕 ЛУНА",
                "Единственный спутник Земли. Всегда повернута к нам одной стороной. " +
                        "Поверхность покрыта реголитом и кратерами от метеоритов. Нет атмосферы и воды. " +
                        "Температура скачет от -173°C до +127°C. " +
                        "Первые люди высадились в 1969 году (Аполлон-11).",
                "moon_detail.jpg",
                "3,474 км",
                "384,400 км от Земли",
                "27.3 дней",
                0,
                "-173°C до +127°C"
            )
        }
    }
}

// ЭКРАН ДЛЯ ЛУНЫ
@Composable
fun MoonInfoScreen(planetInfo: PlanetInfo) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(planetInfo.imageRes) {
        try {
            val inputStream = context.assets.open(planetInfo.imageRes)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = planetInfo.name,
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = planetInfo.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = planetInfo.description,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("ХАРАКТЕРИСТИКИ", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Диаметр:", planetInfo.diameter)
                InfoRow("Расстояние от Земли:", planetInfo.distanceFromSun)
                InfoRow("Орбитальный период:", planetInfo.orbitalPeriod)
                InfoRow("Температура:", planetInfo.temperature)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // КНОПКА ДЛЯ 3D МОДЕЛИ ЛУНЫ
        Button(
            onClick = {
                val intent = Intent(context, Moon3DActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5A5A5A)
            )
        ) {
            Text(
                text = "🌕 ПОСМОТРЕТЬ 3D МОДЕЛЬ ЛУНЫ",
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}
