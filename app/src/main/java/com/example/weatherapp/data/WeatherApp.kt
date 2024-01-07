package com.example.weatherapp.data

import com.example.weatherapp.data.Clouds
import com.example.weatherapp.data.Coord
import com.example.weatherapp.data.Main
import com.example.weatherapp.data.Sys
import com.example.weatherapp.data.Weather
import com.example.weatherapp.data.Wind

data class WeatherApp(
    val base: String,
    val clouds: Clouds,
    val cod: Int,
    val coord: Coord,
    val dt: Int,
    val id: Int,
    val main: Main,
    val name: String,
    val sys: Sys,
    val timezone: Int,
    val visibility: Int,
    val weather: List<Weather>,
    val wind: Wind
)