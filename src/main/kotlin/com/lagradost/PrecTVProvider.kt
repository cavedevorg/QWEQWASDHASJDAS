package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class PrecTVProvider : MainAPI() {
    override var mainUrl = "https://m.prectv60.lol"
    override var name = "PrecTV"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.TvSeries,
        TvType.Movie
    )

    // API credentials - bunları güncellemeniz gerekebilir
    private val apiToken = "4F5A9C3D9A86FA54EACEDDD635185"
    private val apiUuid = "c3c5bd17-e37b-4b94-a944-8a3688a30452"

    private val apiUrl = "$mainUrl/api/first/$apiToken/$apiUuid/"
    private val searchUrl = "$mainUrl/api/search"
    private val seasonUrl = "$mainUrl/api/season/by/serie"

    private fun getHeaders() = mapOf(
        "Accept-Encoding" to "gzip",
        "Connection" to "Keep-Alive",
        "Host" to "m.prectv60.lol",
        "User-Agent" to "okhttp/4.12.0"
    )

    data class ApiResponse(
        val channels: List<Any>?,
        val slides: List<Slide>?
    )

    data class SearchResponse(
        val channels: List<Any>?,
        val posters: List<Poster>?
    )

    data class Season(
        val id: Int,
        val title: String,
        val episodes: List<Episode>?
    )

    data class Episode(
        val id: Int,
        val title: String,
        val description: String?,
        val duration: String?,
        val downloadas: String?,
        val playas: String?,
        val sources: List<Source>?
    )

    data class Slide(
        val id: Int,
        val title: String,
        val type: String,
        val image: String,
        val poster: Poster
    )

    data class Poster(
        val id: Int,
        val title: String,
        val label: String?,
        val sublabel: String?,
        val type: String,
        val description: String?,
        val year: Int?,
        val imdb: Double?,
        val rating: Double?,
        val duration: String?,
        val classification: String?,
        val image: String,
        val cover: String?,
        val genres: List<Genre>?,
        val trailer: Trailer?,
        val sources: List<Source>?
    )

    data class Genre(
        val id: Int,
        val title: String
    )

    data class Trailer(
        val id: Int,
        val type: String,
        val url: String
    )

    data class Source(
        val id: Int?,
        val title: String?,
        val url: String?,
        val quality: String?,
        val size: String?,
        val kind: String?,
        val premium: String?,
        val external: Boolean?,
        val type: String?
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get(
            apiUrl,
            headers = mapOf(
                "Accept-Encoding" to "gzip",
                "Connection" to "Keep-Alive",
                "Host" to "m.prectv60.lol",
                "User-Agent" to "okhttp/4.12.0"
            )
        ).parsed<ApiResponse>()

        val items = response.slides?.mapNotNull { slide ->
            val poster = slide.poster
            newMovieSearchResponse(
                name = poster.title,
                url = "${poster.id}",
                type = if (poster.type == "serie") TvType.TvSeries else TvType.Movie
            ) {
                this.posterUrl = poster.image
                this.year = poster.year
                this.quality = SearchQuality.HD
            }
        } ?: emptyList()

        return newHomePageResponse(
            list = listOf(HomePageList("Öne Çıkanlar", items)),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<com.lagradost.cloudstream3.SearchResponse> {
        val encodedQuery = query.replace(" ", "%20")
        val response = app.get(
            "$searchUrl/$encodedQuery/$apiToken/$apiUuid/",
            headers = getHeaders()
        ).parsed<SearchResponse>()

        return response.posters?.map { poster ->
            newMovieSearchResponse(
                name = poster.title,
                url = "${poster.id}",
                type = if (poster.type == "serie") TvType.TvSeries else TvType.Movie
            ) {
                this.posterUrl = poster.image
                this.year = poster.year
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        // url = poster id
        // İlk önce ana listeden poster bilgilerini çekelim
        val response = app.get(apiUrl, headers = getHeaders()).parsed<ApiResponse>()
        val poster = response.slides?.find { it.poster.id.toString() == url }?.poster

        // Bulamazsa search yaparak deneyelim
        val actualPoster = poster ?: run {
            val searchResponse = app.get(
                "$searchUrl/${url}/$apiToken/$apiUuid/",
                headers = getHeaders()
            ).parsed<SearchResponse>()
            searchResponse.posters?.firstOrNull { it.id.toString() == url }
        } ?: throw ErrorLoadingException("İçerik bulunamadı")

        val tags = actualPoster.genres?.map { it.title }

        return if (actualPoster.type == "serie") {
            // Sezon ve bölümleri çek
            val seasons = app.get(
                "$seasonUrl/$url/$apiToken/$apiUuid/",
                headers = getHeaders()
            ).parsed<List<Season>>()

            val episodes = mutableListOf<com.lagradost.cloudstream3.Episode>()

            seasons.forEachIndexed { seasonIndex, season ->
                season.episodes?.forEachIndexed { episodeIndex, episode ->
                    episodes.add(
                        com.lagradost.cloudstream3.Episode(
                            data = "$url:${episode.id}", // posterID:episodeID formatı
                            name = episode.title,
                            season = seasonIndex + 1,
                            episode = episodeIndex + 1,
                            description = episode.description
                        )
                    )
                }
            }

            newTvSeriesLoadResponse(
                name = actualPoster.title,
                url = url,
                type = TvType.TvSeries,
                episodes = episodes
            ) {
                this.posterUrl = actualPoster.image
                this.year = actualPoster.year
                this.plot = actualPoster.description
                this.tags = tags
                this.rating = actualPoster.imdb?.times(1000)?.toInt()
                this.duration = null
                this.recommendations = null
            }
        } else {
            newMovieLoadResponse(
                name = actualPoster.title,
                url = url,
                type = TvType.Movie,
                dataUrl = url
            ) {
                this.posterUrl = actualPoster.image
                this.year = actualPoster.year
                this.plot = actualPoster.description
                this.tags = tags
                this.rating = actualPoster.imdb?.times(1000)?.toInt()
                this.recommendations = null
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // data formatı: "posterID:episodeID" (diziler için) veya "posterID" (filmler için)

        if (data.contains(":")) {
            // Dizi bölümü
            val (posterId, episodeId) = data.split(":")

            // Sezonları çek
            val seasons = app.get(
                "$seasonUrl/$posterId/$apiToken/$apiUuid/",
                headers = getHeaders()
            ).parsed<List<Season>>()

            // Episode'u bul
            seasons.forEach { season ->
                val episode = season.episodes?.find { it.id.toString() == episodeId }
                if (episode != null) {
                    episode.sources?.forEach { source ->
                        source.url?.let { url ->
                            callback.invoke(
                                ExtractorLink(
                                    source = this.name,
                                    name = "${source.title ?: "PrecTV"} ${source.size ?: ""}",
                                    url = url,
                                    referer = mainUrl,
                                    quality = when (source.size) {
                                        "HD" -> Qualities.P720.value
                                        "FHD" -> Qualities.P1080.value
                                        "SD" -> Qualities.P480.value
                                        else -> Qualities.Unknown.value
                                    },
                                    isM3u8 = source.type == "m3u8"
                                )
                            )
                        }
                    }
                    return true
                }
            }
        } else {
            // Film
            val response = app.get(apiUrl, headers = getHeaders()).parsed<ApiResponse>()
            val moviePoster = response.slides?.find {
                it.poster.id.toString() == data && it.poster.type != "serie"
            }?.poster

            moviePoster?.sources?.forEach { source ->
                source.url?.let { url ->
                    callback.invoke(
                        ExtractorLink(
                            source = this.name,
                            name = "${source.title ?: "PrecTV"} ${source.size ?: ""}",
                            url = url,
                            referer = mainUrl,
                            quality = when (source.size) {
                                "HD" -> Qualities.P720.value
                                "FHD" -> Qualities.P1080.value
                                "SD" -> Qualities.P480.value
                                else -> Qualities.Unknown.value
                            },
                            isM3u8 = source.type == "m3u8"
                        )
                    )
                }
            }
        }

        return true
    }
}
