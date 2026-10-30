package com.example.data.repository

import com.example.data.model.Playlist

object SampleData {

    const val URL_ADULT_1 = "https://raw.githubusercontent.com/atsushi444/iptv/refs/heads/main/adult.m3u"
    const val URL_ADULT_2 = "https://raw.githubusercontent.com/atsushi444/iptv/refs/heads/main/adult2.m3u"
    const val URL_0108 = "https://raw.githubusercontent.com/atsushi444/iptv/refs/heads/main/0108.m3u"

    const val DEFAULT_M3U_URL = URL_ADULT_1

    val DEFAULT_SOURCES = listOf(
        Playlist(
            id = 1L,
            title = "PL1",
            sourceUrl = URL_ADULT_1,
            isLocalFile = false,
            channelCount = 6548,
            isDefault = true
        ),
        Playlist(
            id = 2L,
            title = "PL2",
            sourceUrl = URL_ADULT_2,
            isLocalFile = false,
            channelCount = 1073,
            isDefault = false
        ),
        Playlist(
            id = 3L,
            title = "PL3",
            sourceUrl = URL_0108,
            isLocalFile = false,
            channelCount = 245,
            isDefault = false
        )
    )

    val DEFAULT_PLAYLIST = DEFAULT_SOURCES[0]

    val STABLE_FALLBACK_STREAMS = listOf(
        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
    )

    const val SAMPLE_SRT_SUBTITLE = """1
00:00:01,000 --> 00:00:04,500
[StreamSync 4K] Sinkronisasi Subtitle Aktif

2
00:00:05,000 --> 00:00:09,000
Putar streaming langsung dengan resolusi tinggi dan lancar.

3
00:00:09,500 --> 00:00:14,000
Gunakan panel pengaturan untuk menyesuaikan offset waktu subtitle.
"""
}
