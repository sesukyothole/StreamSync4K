package com.example

import com.example.data.model.Channel
import com.example.data.model.ChannelSortOrder
import com.example.data.parser.M3uParser
import com.example.data.parser.SrtVttParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testM3uParser_basicAndExtendedTags() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="vtv1hd" tvg-logo="https://example.com/logo.png" group-title="VTV",VTV1 HD
            #EXTVLCOPT:http-user-agent=Dalvik/2.1.0
            #KODIPROP:inputstream.adaptive.license_type=clearkey
            #KODIPROP:inputstream.adaptive.license_key=cca908f736f54dfc90ec844fe123942c:9fea9e1b52e1e87a7d4c1dec0c46307f
            https://example.com/stream.mpd
            #EXTINF:-1 group-title="Movies",Big Buck Bunny 4K UHD
            https://example.com/bbb_4k.mp4
        """.trimIndent()

        val channels = M3uParser.parse(sampleM3u, playlistId = 1L)
        assertEquals(2, channels.size)

        val channel1 = channels[0]
        assertEquals("VTV1 HD", channel1.name)
        assertEquals("https://example.com/stream.mpd", channel1.streamUrl)
        assertEquals("https://example.com/logo.png", channel1.logoUrl)
        assertEquals("VTV", channel1.groupTitle)
        assertEquals("Dalvik/2.1.0", channel1.userAgent)
        assertEquals("cca908f736f54dfc90ec844fe123942c:9fea9e1b52e1e87a7d4c1dec0c46307f", channel1.licenseKey)
        assertEquals("clearkey", channel1.licenseType)
        assertFalse(channel1.is4k)

        val channel2 = channels[1]
        assertEquals("Big Buck Bunny 4K UHD", channel2.name)
        assertTrue(channel2.is4k)
        assertEquals("4K UHD", channel2.resolutionLabel)
    }

    @Test
    fun testChannel_fileNameExtraction() {
        val channel = Channel(
            id = "test_1",
            playlistId = 1L,
            name = "VTV2 HD",
            streamUrl = "https://live.example.com/media/vtv2/live247.m3u8?token=12345"
        )
        assertEquals("live247.m3u8", channel.fileName)

        val channelMp4 = Channel(
            id = "test_2",
            playlistId = 1L,
            name = "Tears of Steel",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        )
        assertEquals("TearsOfSteel.mp4", channelMp4.fileName)
    }

    @Test
    fun testChannel_sorting() {
        val c1 = Channel(id = "1", playlistId = 1L, name = "Beta Stream", streamUrl = "http://test/b.mp4", orderIndex = 0, addedAt = 1000L)
        val c2 = Channel(id = "2", playlistId = 1L, name = "Alpha Stream", streamUrl = "http://test/a.mp4", orderIndex = 1, addedAt = 2000L)
        val c3 = Channel(id = "3", playlistId = 1L, name = "Gamma Stream", streamUrl = "http://test/g.mp4", orderIndex = 2, addedAt = 3000L)

        val list = listOf(c1, c2, c3)

        val sortedOriginal = list.sortedBy { it.orderIndex }
        assertEquals("Beta Stream", sortedOriginal[0].name)
        assertEquals("Alpha Stream", sortedOriginal[1].name)
        assertEquals("Gamma Stream", sortedOriginal[2].name)

        val sortedNameAsc = list.sortedBy { it.name.lowercase() }
        assertEquals("Alpha Stream", sortedNameAsc[0].name)
        assertEquals("Beta Stream", sortedNameAsc[1].name)
        assertEquals("Gamma Stream", sortedNameAsc[2].name)

        val sortedDateDesc = list.sortedByDescending { it.addedAt }
        assertEquals("Gamma Stream", sortedDateDesc[0].name)
        assertEquals("Alpha Stream", sortedDateDesc[1].name)
        assertEquals("Beta Stream", sortedDateDesc[2].name)
    }

    @Test
    fun testSrtVttParser_timestampsAndOffset() {
        val srtSample = """
            1
            00:00:01,000 --> 00:00:03,000
            <i>Halo Dunia</i>

            2
            00:00:04,500 --> 00:00:08,000
            Subtitle 4K Streaming
        """.trimIndent()

        val cues = SrtVttParser.parse(srtSample)
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startTimeMs)
        assertEquals(3000L, cues[0].endTimeMs)
        assertEquals("Halo Dunia", cues[0].text)

        assertEquals(4500L, cues[1].startTimeMs)
        assertEquals(8000L, cues[1].endTimeMs)
        assertEquals("Subtitle 4K Streaming", cues[1].text)

        assertNull(SrtVttParser.getActiveCue(cues, currentPositionMs = 900L, offsetMs = 0L))

        val activeCueWithOffset = SrtVttParser.getActiveCue(cues, currentPositionMs = 900L, offsetMs = 200L)
        assertNotNull(activeCueWithOffset)
        assertEquals("Halo Dunia", activeCueWithOffset?.text)
    }

    @Test
    fun testNormalizeUrl_githubBlobConversion() {
        val blobUrl = "https://github.com/atsushi444/iptv/blob/97681bc6e5588820a1dfcecf565aa7bf24bb819d/adult.m3u"
        val expected = "https://raw.githubusercontent.com/atsushi444/iptv/97681bc6e5588820a1dfcecf565aa7bf24bb819d/adult.m3u"
        assertEquals(expected, M3uParser.normalizeUrl(blobUrl))

        val regularUrl = "https://example.com/playlist.m3u8"
        assertEquals(regularUrl, M3uParser.normalizeUrl(regularUrl))
    }

    @Test
    fun testNewM3uSources_configuredCorrectly() {
        assertEquals("https://raw.githubusercontent.com/atsushi444/iptv/refs/heads/main/adult.m3u", com.example.data.repository.SampleData.URL_ADULT_1)
        assertEquals("https://raw.githubusercontent.com/atsushi444/iptv/refs/heads/main/adult2.m3u", com.example.data.repository.SampleData.URL_ADULT_2)
        assertEquals("https://raw.githubusercontent.com/atsushi444/iptv/refs/heads/main/0108.m3u", com.example.data.repository.SampleData.URL_0108)
        assertEquals(3, com.example.data.repository.SampleData.DEFAULT_SOURCES.size)
    }
}
