/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.perflyst.twire.lowlatency

import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.C.RoleFlags
import androidx.media3.common.C.SelectionFlags
import androidx.media3.common.DrmInitData
import androidx.media3.common.DrmInitData.SchemeData
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.ParserException
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.UriUtil
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.hls.HlsTrackMetadataEntry
import androidx.media3.exoplayer.hls.HlsTrackMetadataEntry.VariantInfo
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist.PlaylistType
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist.RenditionReport
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist.ServerControl
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist.Rendition
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.upstream.ParsingLoadable
import androidx.media3.extractor.mp4.PsshAtomUtil
import com.google.common.collect.Iterables
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf
import org.checkerframework.checker.nullness.qual.PolyNull
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.util.ArrayDeque
import java.util.Queue
import java.util.TreeMap
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * HLS playlists parsing logic.
 */
@UnstableApi
class LLHlsPlaylistParser
/**
 * Creates an instance where media playlists are parsed without inheriting attributes from a
 * multivariant playlist.
 */ @JvmOverloads constructor(
    private val multivariantPlaylist: HlsMultivariantPlaylist = HlsMultivariantPlaylist.EMPTY,
    private val previousMediaPlaylist: HlsMediaPlaylist? = null
) : ParsingLoadable.Parser<HlsPlaylist> {
    /**
     * Exception thrown when merging a delta update fails.
     */
    class DeltaUpdateException : IOException()

    /**
     * Creates an instance where parsed media playlists inherit attributes from the given master
     * playlist.
     *
     * @param multivariantPlaylist  The multivariant playlist from which media playlists will inherit
     * attributes.
     * @param previousMediaPlaylist The previous media playlist from which the new media playlist may
     * inherit skipped segments.
     */

    @Throws(IOException::class)
    override fun parse(uri: Uri, inputStream: InputStream): HlsPlaylist {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val extraLines: Queue<String?> = ArrayDeque<String?>()
        var line: String?
        try {
            if (!checkPlaylistHeader(reader)) {
                throw ParserException.createForMalformedManifest( /* message= */
                    "Input does not start with the #EXTM3U header.",  /* cause= */null
                )
            }
            while ((reader.readLine().also { line = it }) != null) {
                line = line!!.trim { it <= ' ' }
                if (line.isEmpty()) {
                    // Do nothing.
                } else if (line.startsWith(TAG_STREAM_INF)) {
                    extraLines.add(line)
                    return parseMultivariantPlaylist(
                        LineIterator(extraLines, reader),
                        uri.toString()
                    )
                } else if (line.startsWith(TAG_TARGET_DURATION)
                    || line.startsWith(TAG_MEDIA_SEQUENCE)
                    || line.startsWith(TAG_MEDIA_DURATION)
                    || line.startsWith(TAG_KEY)
                    || line.startsWith(TAG_BYTERANGE)
                    || line == TAG_DISCONTINUITY
                    || line == TAG_DISCONTINUITY_SEQUENCE
                    || line == TAG_ENDLIST
                ) {
                    extraLines.add(line)
                    return parseMediaPlaylist(
                        multivariantPlaylist,
                        previousMediaPlaylist,
                        LineIterator(extraLines, reader),
                        uri.toString()
                    )
                } else {
                    extraLines.add(line)
                }
            }
        } finally {
            Util.closeQuietly(reader)
        }
        throw ParserException.createForMalformedManifest(
            "Failed to parse the playlist, could not identify any tags.",  /* cause= */null
        )
    }

    private class LineIterator(
        private val extraLines: Queue<String?>,
        private val reader: BufferedReader
    ) {
        private var next: String? = null

        @EnsuresNonNullIf(expression = ["next"], result = true)
        @Throws(IOException::class)
        fun hasNext(): Boolean {
            if (next != null) {
                return true
            }
            if (!extraLines.isEmpty()) {
                next = Assertions.checkNotNull<String?>(extraLines.poll())
                return true
            }
            while ((reader.readLine().also { next = it }) != null) {
                next = next!!.trim { it <= ' ' }
                if (!next!!.isEmpty()) {
                    return true
                }
            }
            return false
        }

        /**
         * Return the next line, or throw [NoSuchElementException] if none.
         */
        @Throws(IOException::class)
        fun next(): String {
            if (hasNext()) {
                val result = next
                next = null
                return result!!
            } else {
                throw NoSuchElementException()
            }
        }
    }

    companion object {
        private const val LOG_TAG = "HlsPlaylistParser"

        private const val PLAYLIST_HEADER = "#EXTM3U"

        private const val TAG_PREFIX = "#EXT"

        private const val TAG_VERSION = "#EXT-X-VERSION"
        private const val TAG_PLAYLIST_TYPE = "#EXT-X-PLAYLIST-TYPE"
        private const val TAG_DEFINE = "#EXT-X-DEFINE"
        private const val TAG_SERVER_CONTROL = "#EXT-X-SERVER-CONTROL"
        private const val TAG_STREAM_INF = "#EXT-X-STREAM-INF"
        private const val TAG_PART_INF = "#EXT-X-PART-INF"
        private const val TAG_PART = "#EXT-X-PART"
        private const val TAG_I_FRAME_STREAM_INF = "#EXT-X-I-FRAME-STREAM-INF"
        private const val TAG_IFRAME = "#EXT-X-I-FRAMES-ONLY"
        private const val TAG_MEDIA = "#EXT-X-MEDIA"
        private const val TAG_TARGET_DURATION = "#EXT-X-TARGETDURATION"
        private const val TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY"
        private const val TAG_DISCONTINUITY_SEQUENCE = "#EXT-X-DISCONTINUITY-SEQUENCE"
        private const val TAG_PROGRAM_DATE_TIME = "#EXT-X-PROGRAM-DATE-TIME"
        private const val TAG_INIT_SEGMENT = "#EXT-X-MAP"
        private const val TAG_INDEPENDENT_SEGMENTS = "#EXT-X-INDEPENDENT-SEGMENTS"
        private const val TAG_MEDIA_DURATION = "#EXTINF"
        private const val TAG_MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE"
        private const val TAG_START = "#EXT-X-START"
        private const val TAG_ENDLIST = "#EXT-X-ENDLIST"
        private const val TAG_KEY = "#EXT-X-KEY"
        private const val TAG_SESSION_KEY = "#EXT-X-SESSION-KEY"
        private const val TAG_BYTERANGE = "#EXT-X-BYTERANGE"
        private const val TAG_GAP = "#EXT-X-GAP"
        private const val TAG_PREFETCH = "#EXT-X-TWITCH-PREFETCH"
        private const val TAG_SKIP = "#EXT-X-SKIP"
        private const val TAG_PRELOAD_HINT = "#EXT-X-PRELOAD-HINT"
        private const val TAG_RENDITION_REPORT = "#EXT-X-RENDITION-REPORT"

        private const val TYPE_AUDIO = "AUDIO"
        private const val TYPE_VIDEO = "VIDEO"
        private const val TYPE_SUBTITLES = "SUBTITLES"
        private const val TYPE_CLOSED_CAPTIONS = "CLOSED-CAPTIONS"
        private const val TYPE_PART = "PART"
        private const val TYPE_MAP = "MAP"

        private const val METHOD_NONE = "NONE"
        private const val METHOD_AES_128 = "AES-128"
        private const val METHOD_SAMPLE_AES = "SAMPLE-AES"

        // Replaced by METHOD_SAMPLE_AES_CTR. Keep for backward compatibility.
        private const val METHOD_SAMPLE_AES_CENC = "SAMPLE-AES-CENC"
        private const val METHOD_SAMPLE_AES_CTR = "SAMPLE-AES-CTR"
        private const val KEYFORMAT_PLAYREADY = "com.microsoft.playready"
        private const val KEYFORMAT_IDENTITY = "identity"
        private const val KEYFORMAT_WIDEVINE_PSSH_BINARY =
            "urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
        private const val KEYFORMAT_WIDEVINE_PSSH_JSON = "com.widevine"

        private const val BOOLEAN_TRUE = "YES"
        private const val BOOLEAN_FALSE = "NO"

        private const val ATTR_CLOSED_CAPTIONS_NONE = "CLOSED-CAPTIONS=NONE"

        private val REGEX_AVERAGE_BANDWIDTH: Pattern =
            Pattern.compile("AVERAGE-BANDWIDTH=(\\d+)\\b")
        private val REGEX_VIDEO: Pattern = Pattern.compile("VIDEO=\"(.+?)\"")
        private val REGEX_AUDIO: Pattern = Pattern.compile("AUDIO=\"(.+?)\"")
        private val REGEX_SUBTITLES: Pattern = Pattern.compile("SUBTITLES=\"(.+?)\"")
        private val REGEX_CLOSED_CAPTIONS: Pattern = Pattern.compile("CLOSED-CAPTIONS=\"(.+?)\"")
        private val REGEX_BANDWIDTH: Pattern = Pattern.compile("[^-]BANDWIDTH=(\\d+)\\b")
        private val REGEX_CHANNELS: Pattern = Pattern.compile("CHANNELS=\"(.+?)\"")
        private val REGEX_CODECS: Pattern = Pattern.compile("CODECS=\"(.+?)\"")
        private val REGEX_RESOLUTION: Pattern = Pattern.compile("RESOLUTION=(\\d+x\\d+)")
        private val REGEX_FRAME_RATE: Pattern = Pattern.compile("FRAME-RATE=([\\d.]+)\\b")
        private val REGEX_TARGET_DURATION: Pattern =
            Pattern.compile("$TAG_TARGET_DURATION:(\\d+)\\b")
        private val REGEX_ATTR_DURATION: Pattern = Pattern.compile("DURATION=([\\d.]+)\\b")
        private val REGEX_PART_TARGET_DURATION: Pattern =
            Pattern.compile("PART-TARGET=([\\d.]+)\\b")
        private val REGEX_VERSION: Pattern = Pattern.compile("$TAG_VERSION:(\\d+)\\b")
        private val REGEX_PLAYLIST_TYPE: Pattern = Pattern.compile("$TAG_PLAYLIST_TYPE:(.+)\\b")
        private val REGEX_CAN_SKIP_UNTIL: Pattern = Pattern.compile("CAN-SKIP-UNTIL=([\\d.]+)\\b")
        private val REGEX_CAN_SKIP_DATE_RANGES: Pattern =
            compileBooleanAttrPattern("CAN-SKIP-DATERANGES")
        private val REGEX_SKIPPED_SEGMENTS: Pattern = Pattern.compile("SKIPPED-SEGMENTS=(\\d+)\\b")
        private val REGEX_HOLD_BACK: Pattern = Pattern.compile("[:|,]HOLD-BACK=([\\d.]+)\\b")
        private val REGEX_PART_HOLD_BACK: Pattern = Pattern.compile("PART-HOLD-BACK=([\\d.]+)\\b")
        private val REGEX_CAN_BLOCK_RELOAD: Pattern = compileBooleanAttrPattern("CAN-BLOCK-RELOAD")
        private val REGEX_MEDIA_SEQUENCE: Pattern =
            Pattern.compile("$TAG_MEDIA_SEQUENCE:(\\d+)\\b")
        private val REGEX_MEDIA_DURATION: Pattern =
            Pattern.compile("$TAG_MEDIA_DURATION:([\\d.]+)\\b")
        private val REGEX_MEDIA_TITLE: Pattern =
            Pattern.compile("$TAG_MEDIA_DURATION:[\\d.]+\\b,(.+)")
        private val REGEX_LAST_MSN: Pattern = Pattern.compile("LAST-MSN" + "=(\\d+)\\b")
        private val REGEX_LAST_PART: Pattern = Pattern.compile("LAST-PART" + "=(\\d+)\\b")
        private val REGEX_TIME_OFFSET: Pattern = Pattern.compile("TIME-OFFSET=(-?[\\d.]+)\\b")
        private val REGEX_BYTERANGE: Pattern =
            Pattern.compile("$TAG_BYTERANGE:(\\d+(?:@\\d+)?)\\b")
        private val REGEX_ATTR_BYTERANGE: Pattern =
            Pattern.compile("BYTERANGE=\"(\\d+(?:@\\d+)?)\\b\"")
        private val REGEX_BYTERANGE_START: Pattern = Pattern.compile("BYTERANGE-START=(\\d+)\\b")
        private val REGEX_BYTERANGE_LENGTH: Pattern = Pattern.compile("BYTERANGE-LENGTH=(\\d+)\\b")
        private val REGEX_METHOD: Pattern = Pattern.compile(
            ("METHOD=("
                    + METHOD_NONE
                    + "|"
                    + METHOD_AES_128
                    + "|"
                    + METHOD_SAMPLE_AES
                    + "|"
                    + METHOD_SAMPLE_AES_CENC
                    + "|"
                    + METHOD_SAMPLE_AES_CTR
                    + ")"
                    + "\\s*(?:,|$)")
        )
        private val REGEX_KEYFORMAT: Pattern = Pattern.compile("KEYFORMAT=\"(.+?)\"")
        private val REGEX_KEYFORMATVERSIONS: Pattern =
            Pattern.compile("KEYFORMATVERSIONS=\"(.+?)\"")
        private val REGEX_URI: Pattern = Pattern.compile("URI=\"(.+?)\"")
        private val REGEX_IV: Pattern = Pattern.compile("IV=([^,.*]+)")
        private val REGEX_TYPE: Pattern = Pattern.compile(
            ("TYPE=("
                    + TYPE_AUDIO
                    + "|"
                    + TYPE_VIDEO
                    + "|"
                    + TYPE_SUBTITLES
                    + "|"
                    + TYPE_CLOSED_CAPTIONS
                    + ")")
        )
        private val REGEX_PRELOAD_HINT_TYPE: Pattern =
            Pattern.compile("TYPE=($TYPE_PART|$TYPE_MAP)")
        private val REGEX_LANGUAGE: Pattern = Pattern.compile("LANGUAGE=\"(.+?)\"")
        private val REGEX_NAME: Pattern = Pattern.compile("NAME=\"(.+?)\"")
        private val REGEX_GROUP_ID: Pattern = Pattern.compile("GROUP-ID=\"(.+?)\"")
        private val REGEX_CHARACTERISTICS: Pattern = Pattern.compile("CHARACTERISTICS=\"(.+?)\"")
        private val REGEX_INSTREAM_ID: Pattern =
            Pattern.compile("INSTREAM-ID=\"((?:CC|SERVICE)\\d+)\"")
        private val REGEX_AUTOSELECT: Pattern = compileBooleanAttrPattern("AUTOSELECT")
        private val REGEX_DEFAULT: Pattern = compileBooleanAttrPattern("DEFAULT")
        private val REGEX_FORCED: Pattern = compileBooleanAttrPattern("FORCED")
        private val REGEX_INDEPENDENT: Pattern = compileBooleanAttrPattern("INDEPENDENT")
        private val REGEX_GAP: Pattern = compileBooleanAttrPattern("GAP")
        private val REGEX_PRECISE: Pattern = compileBooleanAttrPattern("PRECISE")
        private val REGEX_VALUE: Pattern = Pattern.compile("VALUE=\"(.+?)\"")
        private val REGEX_IMPORT: Pattern = Pattern.compile("IMPORT=\"(.+?)\"")
        private val REGEX_VARIABLE_REFERENCE: Pattern =
            Pattern.compile("\\{\\$([a-zA-Z0-9\\-_]+)\\}")

        @Throws(IOException::class)
        private fun checkPlaylistHeader(reader: BufferedReader): Boolean {
            var last = reader.read()
            if (last == 0xEF) {
                if (reader.read() != 0xBB || reader.read() != 0xBF) {
                    return false
                }
                // The playlist contains a Byte Order Mark, which gets discarded.
                last = reader.read()
            }
            last = skipIgnorableWhitespace(reader, true, last)
            val playlistHeaderLength: Int = PLAYLIST_HEADER.length
            for (i in 0..<playlistHeaderLength) {
                if (last != PLAYLIST_HEADER[i].code) {
                    return false
                }
                last = reader.read()
            }
            last = skipIgnorableWhitespace(reader, false, last)
            return Util.isLinebreak(last)
        }

        @Throws(IOException::class)
        private fun skipIgnorableWhitespace(
            reader: BufferedReader,
            skipLinebreaks: Boolean,
            c: Int
        ): Int {
            var c = c
            while (c != -1 && Character.isWhitespace(c) && (skipLinebreaks || !Util.isLinebreak(c))) {
                c = reader.read()
            }
            return c
        }

        @Throws(IOException::class)
        private fun parseMultivariantPlaylist(
            iterator: LineIterator, baseUri: String
        ): HlsMultivariantPlaylist {
            val urlToVariantInfos = HashMap<Uri, ArrayList<VariantInfo>>()
            val variableDefinitions = HashMap<String, String>()
            val variants = ArrayList<HlsMultivariantPlaylist.Variant>()
            val videos = ArrayList<Rendition>()
            val audios = ArrayList<Rendition>()
            val subtitles = ArrayList<Rendition>()
            val closedCaptions = ArrayList<Rendition>()
            val mediaTags = ArrayList<String>()
            val sessionKeyDrmInitData = ArrayList<DrmInitData>()
            val tags = ArrayList<String>()
            var muxedAudioFormat: Format? = null
            var muxedCaptionFormats: MutableList<Format>? = null
            var noClosedCaptions = false
            var hasIndependentSegmentsTag = false

            var line: String
            while (iterator.hasNext()) {
                line = iterator.next()

                if (line.startsWith(TAG_PREFIX)) {
                    // We expose all tags through the playlist.
                    tags.add(line)
                }
                val isIFrameOnlyVariant: Boolean = line.startsWith(TAG_I_FRAME_STREAM_INF)

                if (line.startsWith(TAG_DEFINE)) {
                    variableDefinitions.put( /* key= */
                        parseStringAttr(line, REGEX_NAME, variableDefinitions),  /* value= */
                        parseStringAttr(line, REGEX_VALUE, variableDefinitions)
                    )
                } else if (line == TAG_INDEPENDENT_SEGMENTS) {
                    hasIndependentSegmentsTag = true
                } else if (line.startsWith(TAG_MEDIA)) {
                    // Media tags are parsed at the end to include codec information from #EXT-X-STREAM-INF
                    // tags.
                    mediaTags.add(line)
                } else if (line.startsWith(TAG_SESSION_KEY)) {
                    val keyFormat: String? =
                        parseOptionalStringAttr(
                            line,
                            REGEX_KEYFORMAT,
                            KEYFORMAT_IDENTITY,
                            variableDefinitions
                        )
                    val schemeData: SchemeData? =
                        parseDrmSchemeData(line, keyFormat, variableDefinitions)
                    if (schemeData != null) {
                        val method: String =
                            parseStringAttr(line, REGEX_METHOD, variableDefinitions)
                        val scheme: String = parseEncryptionScheme(method)
                        sessionKeyDrmInitData.add(DrmInitData(scheme, schemeData))
                    }
                } else if (line.startsWith(TAG_STREAM_INF) || isIFrameOnlyVariant) {
                    noClosedCaptions = noClosedCaptions or line.contains(ATTR_CLOSED_CAPTIONS_NONE)
                    val roleFlags = if (isIFrameOnlyVariant) C.ROLE_FLAG_TRICK_PLAY else 0
                    val peakBitrate: Int = parseIntAttr(line, REGEX_BANDWIDTH)
                    val averageBitrate: Int =
                        parseOptionalIntAttr(line, REGEX_AVERAGE_BANDWIDTH, -1)
                    val codecs: String? =
                        parseOptionalStringAttr(line, REGEX_CODECS, variableDefinitions)
                    val resolutionString: String? =
                        parseOptionalStringAttr(line, REGEX_RESOLUTION, variableDefinitions)
                    var width: Int
                    var height: Int
                    if (resolutionString != null) {
                        val widthAndHeight: Array<String> = Util.split(resolutionString, "x")
                        width = widthAndHeight[0].toInt()
                        height = widthAndHeight[1].toInt()
                        if (width <= 0 || height <= 0) {
                            // Resolution string is invalid.
                            width = Format.NO_VALUE
                            height = Format.NO_VALUE
                        }
                    } else {
                        width = Format.NO_VALUE
                        height = Format.NO_VALUE
                    }
                    var frameRate = Format.NO_VALUE.toFloat()
                    val frameRateString: String? =
                        parseOptionalStringAttr(line, REGEX_FRAME_RATE, variableDefinitions)
                    if (frameRateString != null) {
                        frameRate = frameRateString.toFloat()
                    }
                    val videoGroupId: String? =
                        parseOptionalStringAttr(line, REGEX_VIDEO, variableDefinitions)
                    val audioGroupId: String? =
                        parseOptionalStringAttr(line, REGEX_AUDIO, variableDefinitions)
                    val subtitlesGroupId: String? =
                        parseOptionalStringAttr(line, REGEX_SUBTITLES, variableDefinitions)
                    val closedCaptionsGroupId: String? =
                        parseOptionalStringAttr(line, REGEX_CLOSED_CAPTIONS, variableDefinitions)
                    val uri: Uri?
                    if (isIFrameOnlyVariant) {
                        uri =
                            UriUtil.resolveToUri(
                                baseUri,
                                parseStringAttr(line, REGEX_URI, variableDefinitions)
                            )
                    } else if (!iterator.hasNext()) {
                        throw ParserException.createForMalformedManifest(
                            "#EXT-X-STREAM-INF must be followed by another line",  /* cause= */null
                        )
                    } else {
                        // The following line contains #EXT-X-STREAM-INF's URI.
                        line = replaceVariableReferences(iterator.next(), variableDefinitions)
                        uri = UriUtil.resolveToUri(baseUri, line)
                    }

                    val format =
                        Format.Builder()
                            .setId(variants.size)
                            .setContainerMimeType(MimeTypes.APPLICATION_M3U8)
                            .setCodecs(codecs)
                            .setAverageBitrate(averageBitrate)
                            .setPeakBitrate(peakBitrate)
                            .setWidth(width)
                            .setHeight(height)
                            .setFrameRate(frameRate)
                            .setRoleFlags(roleFlags)
                            .build()
                    val variant =
                        HlsMultivariantPlaylist.Variant(
                            uri,
                            format,
                            videoGroupId,
                            audioGroupId,
                            subtitlesGroupId,
                            closedCaptionsGroupId
                        )
                    variants.add(variant)
                    var variantInfosForUrl = urlToVariantInfos.get(uri)
                    if (variantInfosForUrl == null) {
                        variantInfosForUrl = ArrayList()
                        urlToVariantInfos.put(uri, variantInfosForUrl)
                    }
                    variantInfosForUrl.add(
                        VariantInfo(
                            averageBitrate,
                            peakBitrate,
                            videoGroupId,
                            audioGroupId,
                            subtitlesGroupId,
                            closedCaptionsGroupId
                        )
                    )
                }
            }

            // TODO: Don't deduplicate variants by URL.
            val deduplicatedVariants = ArrayList<HlsMultivariantPlaylist.Variant>()
            val urlsInDeduplicatedVariants = HashSet<Uri?>()
            for (i in variants.indices) {
                val variant = variants[i]
                if (urlsInDeduplicatedVariants.add(variant.url)) {
                    Assertions.checkState(variant.format.metadata == null)
                    val hlsMetadataEntry =
                        HlsTrackMetadataEntry( /* groupId= */
                            null,  /* name= */
                            null,
                            Assertions.checkNotNull<ArrayList<VariantInfo>>(
                                urlToVariantInfos.get(
                                    variant.url
                                )
                            )
                        )
                    val metadata = Metadata(hlsMetadataEntry)
                    val format = variant.format.buildUpon().setMetadata(metadata).build()
                    deduplicatedVariants.add(variant.copyWithFormat(format))
                }
            }

            for (i in mediaTags.indices) {
                line = mediaTags[i]
                val groupId: String = parseStringAttr(line, REGEX_GROUP_ID, variableDefinitions)
                val name: String = parseStringAttr(line, REGEX_NAME, variableDefinitions)
                val formatBuilder =
                    Format.Builder()
                        .setId("$groupId:$name")
                        .setLabel(name)
                        .setContainerMimeType(MimeTypes.APPLICATION_M3U8)
                        .setSelectionFlags(parseSelectionFlags(line))
                        .setRoleFlags(parseRoleFlags(line, variableDefinitions))
                        .setLanguage(
                            parseOptionalStringAttr(
                                line,
                                REGEX_LANGUAGE,
                                variableDefinitions
                            )
                        )

                val referenceUri: String? =
                    parseOptionalStringAttr(line, REGEX_URI, variableDefinitions)
                val uri =
                    if (referenceUri == null) null else UriUtil.resolveToUri(baseUri, referenceUri)
                val metadata =
                    Metadata(HlsTrackMetadataEntry(groupId, name, mutableListOf()))
                when (parseStringAttr(line, REGEX_TYPE, variableDefinitions)) {
                    TYPE_VIDEO -> {
                        val variant: HlsMultivariantPlaylist.Variant? =
                            getVariantWithVideoGroup(variants, groupId)
                        if (variant != null) {
                            val variantFormat = variant.format
                            val codecs =
                                Util.getCodecsOfType(variantFormat.codecs, C.TRACK_TYPE_VIDEO)
                            formatBuilder
                                .setCodecs(codecs)
                                .setSampleMimeType(MimeTypes.getMediaMimeType(codecs))
                                .setWidth(variantFormat.width)
                                .setHeight(variantFormat.height)
                                .setFrameRate(variantFormat.frameRate)
                        }
                        if (uri == null) {
                            // TODO: Remove this case and add a Rendition with a null uri to videos.
                        } else {
                            formatBuilder.setMetadata(metadata)
                            videos.add(Rendition(uri, formatBuilder.build(), groupId, name))
                        }
                    }

                    TYPE_AUDIO -> {
                        var sampleMimeType: String? = null
                        val variant = getVariantWithAudioGroup(variants, groupId)
                        if (variant != null) {
                            val codecs =
                                Util.getCodecsOfType(variant.format.codecs, C.TRACK_TYPE_AUDIO)
                            formatBuilder.setCodecs(codecs)
                            sampleMimeType = MimeTypes.getMediaMimeType(codecs)
                        }
                        val channelsString: String? =
                            parseOptionalStringAttr(line, REGEX_CHANNELS, variableDefinitions)
                        if (channelsString != null) {
                            val channelCount = Util.splitAtFirst(channelsString, "/")[0].toInt()
                            formatBuilder.setChannelCount(channelCount)
                            if (MimeTypes.AUDIO_E_AC3 == sampleMimeType && channelsString.endsWith("/JOC")) {
                                sampleMimeType = MimeTypes.AUDIO_E_AC3_JOC
                                formatBuilder.setCodecs(MimeTypes.CODEC_E_AC3_JOC)
                            }
                        }
                        formatBuilder.setSampleMimeType(sampleMimeType)
                        if (uri != null) {
                            formatBuilder.setMetadata(metadata)
                            audios.add(Rendition(uri, formatBuilder.build(), groupId, name))
                        } else if (variant != null) {
                            // TODO: Remove muxedAudioFormat and add a Rendition with a null uri to audios.
                            muxedAudioFormat = formatBuilder.build()
                        }
                    }

                    TYPE_SUBTITLES -> {
                        var sampleMimeType: String? = null
                        val variant = getVariantWithSubtitleGroup(variants, groupId)
                        if (variant != null) {
                            val codecs =
                                Util.getCodecsOfType(variant.format.codecs, C.TRACK_TYPE_TEXT)
                            formatBuilder.setCodecs(codecs)
                            sampleMimeType = MimeTypes.getMediaMimeType(codecs)
                        }
                        if (sampleMimeType == null) {
                            sampleMimeType = MimeTypes.TEXT_VTT
                        }
                        formatBuilder.setSampleMimeType(sampleMimeType).setMetadata(metadata)
                        if (uri != null) {
                            subtitles.add(Rendition(uri, formatBuilder.build(), groupId, name))
                        } else {
                            Log.w(
                                LOG_TAG,
                                "EXT-X-MEDIA tag with missing mandatory URI attribute: skipping"
                            )
                        }
                    }

                    TYPE_CLOSED_CAPTIONS -> {
                        val instreamId: String =
                            parseStringAttr(line, REGEX_INSTREAM_ID, variableDefinitions)
                        val accessibilityChannel: Int
                        var sampleMimeType: String?
                        if (instreamId.startsWith("CC")) {
                            sampleMimeType = MimeTypes.APPLICATION_CEA608
                            accessibilityChannel = instreamId.substring(2).toInt()
                        } else  /* starts with SERVICE */ {
                            sampleMimeType = MimeTypes.APPLICATION_CEA708
                            accessibilityChannel = instreamId.substring(7).toInt()
                        }
                        if (muxedCaptionFormats == null) {
                            muxedCaptionFormats = ArrayList()
                        }
                        formatBuilder
                            .setSampleMimeType(sampleMimeType)
                            .setAccessibilityChannel(accessibilityChannel)
                        muxedCaptionFormats.add(formatBuilder.build())
                    }

                    else -> {}
                }
            }

            if (noClosedCaptions) {
                muxedCaptionFormats = mutableListOf()
            }

            return HlsMultivariantPlaylist(
                baseUri,
                tags,
                deduplicatedVariants,
                videos,
                audios,
                subtitles,
                closedCaptions,
                muxedAudioFormat,
                muxedCaptionFormats,
                hasIndependentSegmentsTag,
                variableDefinitions,
                sessionKeyDrmInitData
            )
        }

        private fun getVariantWithAudioGroup(
            variants: ArrayList<HlsMultivariantPlaylist.Variant>,
            groupId: String
        ): HlsMultivariantPlaylist.Variant? {
            for (i in variants.indices) {
                val variant = variants[i]
                if (groupId == variant.audioGroupId) {
                    return variant
                }
            }
            return null
        }

        private fun getVariantWithVideoGroup(
            variants: ArrayList<HlsMultivariantPlaylist.Variant>,
            groupId: String
        ): HlsMultivariantPlaylist.Variant? {
            for (i in variants.indices) {
                val variant = variants[i]
                if (groupId == variant.videoGroupId) {
                    return variant
                }
            }
            return null
        }

        private fun getVariantWithSubtitleGroup(
            variants: ArrayList<HlsMultivariantPlaylist.Variant>,
            groupId: String
        ): HlsMultivariantPlaylist.Variant? {
            for (i in variants.indices) {
                val variant = variants[i]
                if (groupId == variant.subtitleGroupId) {
                    return variant
                }
            }
            return null
        }

        @OptIn(ExperimentalStdlibApi::class)
        @Throws(IOException::class)
        private fun parseMediaPlaylist(
            multivariantPlaylist: HlsMultivariantPlaylist,
            previousMediaPlaylist: HlsMediaPlaylist?,
            iterator: LineIterator,
            baseUri: String
        ): HlsMediaPlaylist {
            var playlistType: @PlaylistType Int = HlsMediaPlaylist.PLAYLIST_TYPE_UNKNOWN
            var startOffsetUs = C.TIME_UNSET
            var mediaSequence = 0L
            var version = 1 // Default version == 1.
            var targetDurationUs = C.TIME_UNSET
            var partTargetDurationUs = C.TIME_UNSET
            var hasIndependentSegmentsTag = multivariantPlaylist.hasIndependentSegments
            var hasEndTag = false
            var initializationSegment: HlsMediaPlaylist.Segment? = null
            val variableDefinitions = HashMap<String, String>()
            val urlToInferredInitSegment = HashMap<String?, HlsMediaPlaylist.Segment?>()
            val segments: MutableList<HlsMediaPlaylist.Segment> =
                ArrayList()
            var trailingParts: MutableList<HlsMediaPlaylist.Part> =
                ArrayList()
            var preloadPart: HlsMediaPlaylist.Part? = null
            val renditionReports: MutableList<RenditionReport> = ArrayList()
            val tags: MutableList<String> = ArrayList()

            var segmentDurationUs = 0L
            var segmentTitle: String? = ""
            var hasDiscontinuitySequence = false
            var playlistDiscontinuitySequence = 0
            var relativeDiscontinuitySequence = 0
            var playlistStartTimeUs = 0L
            var segmentStartTimeUs = 0L
            var preciseStart = false
            var segmentByteRangeOffset = 0L
            var segmentByteRangeLength = C.LENGTH_UNSET.toLong()
            var partStartTimeUs = 0L
            var partByteRangeOffset = 0L
            var isIFrameOnly = false
            var segmentMediaSequence = 0L
            var hasGapTag = false
            var serverControl =
                ServerControl( /* skipUntilUs= */
                    C.TIME_UNSET,  /* canSkipDateRanges= */
                    false,  /* holdBackUs= */
                    C.TIME_UNSET,  /* partHoldBackUs= */
                    C.TIME_UNSET,  /* canBlockReload= */
                    false
                )

            var playlistProtectionSchemes: DrmInitData? = null
            var fullSegmentEncryptionKeyUri: String? = null
            var fullSegmentEncryptionIV: String? = null
            val currentSchemeDatas = TreeMap<String?, SchemeData>()
            var encryptionScheme: String? = null
            var cachedDrmInitData: DrmInitData? = null

            var line: String
            while (iterator.hasNext()) {
                line = iterator.next()

                if (line.startsWith(TAG_PREFIX)) {
                    // We expose all tags through the playlist.
                    tags.add(line)
                }

                if (line.startsWith(TAG_PLAYLIST_TYPE)) {
                    val playlistTypeString: String =
                        parseStringAttr(line, REGEX_PLAYLIST_TYPE, variableDefinitions)
                    if ("VOD" == playlistTypeString) {
                        playlistType = HlsMediaPlaylist.PLAYLIST_TYPE_VOD
                    } else if ("EVENT" == playlistTypeString) {
                        playlistType = HlsMediaPlaylist.PLAYLIST_TYPE_EVENT
                    }
                } else if (line == TAG_IFRAME) {
                    isIFrameOnly = true
                } else if (line.startsWith(TAG_START)) {
                    startOffsetUs =
                        (parseDoubleAttr(line, REGEX_TIME_OFFSET) * C.MICROS_PER_SECOND).toLong()
                    preciseStart =
                        parseOptionalBooleanAttribute(
                            line,
                            REGEX_PRECISE,  /* defaultValue= */
                            false
                        )
                } else if (line.startsWith(TAG_SERVER_CONTROL)) {
                    serverControl = parseServerControl(line)
                } else if (line.startsWith(TAG_PART_INF)) {
                    val partTargetDurationSeconds: Double =
                        parseDoubleAttr(line, REGEX_PART_TARGET_DURATION)
                    partTargetDurationUs =
                        (partTargetDurationSeconds * C.MICROS_PER_SECOND).toLong()
                } else if (line.startsWith(TAG_INIT_SEGMENT)) {
                    val uri: String = parseStringAttr(line, REGEX_URI, variableDefinitions)
                    val byteRange: String? =
                        parseOptionalStringAttr(line, REGEX_ATTR_BYTERANGE, variableDefinitions)
                    if (byteRange != null) {
                        val splitByteRange: Array<String> = Util.split(byteRange, "@")
                        segmentByteRangeLength = splitByteRange[0].toLong()
                        if (splitByteRange.size > 1) {
                            segmentByteRangeOffset = splitByteRange[1].toLong()
                        }
                    }
                    if (segmentByteRangeLength == C.LENGTH_UNSET.toLong()) {
                        // The segment has no byte range defined.
                        segmentByteRangeOffset = 0
                    }
                    if (fullSegmentEncryptionKeyUri != null && fullSegmentEncryptionIV == null) {
                        // See RFC 8216, Section 4.3.2.5.
                        throw ParserException.createForMalformedManifest(
                            "The encryption IV attribute must be present when an initialization segment is"
                                    + " encrypted with METHOD=AES-128.",  /* cause= */
                            null
                        )
                    }
                    initializationSegment =
                        HlsMediaPlaylist.Segment(
                            uri,
                            segmentByteRangeOffset,
                            segmentByteRangeLength,
                            fullSegmentEncryptionKeyUri,
                            fullSegmentEncryptionIV
                        )
                    if (segmentByteRangeLength != C.LENGTH_UNSET.toLong()) {
                        segmentByteRangeOffset += segmentByteRangeLength
                    }
                    segmentByteRangeLength = C.LENGTH_UNSET.toLong()
                } else if (line.startsWith(TAG_TARGET_DURATION)) {
                    // The correct target duration is 2 seconds in low latency.
                    targetDurationUs = 2 * C.MICROS_PER_SECOND
                    //targetDurationUs = parseIntAttr(line, REGEX_TARGET_DURATION) * C.MICROS_PER_SECOND;
                } else if (line.startsWith(TAG_MEDIA_SEQUENCE)) {
                    mediaSequence = parseLongAttr(line, REGEX_MEDIA_SEQUENCE)
                    segmentMediaSequence = mediaSequence
                } else if (line.startsWith(TAG_VERSION)) {
                    version = parseIntAttr(line, REGEX_VERSION)
                } else if (line.startsWith(TAG_DEFINE)) {
                    val importName: String? =
                        parseOptionalStringAttr(line, REGEX_IMPORT, variableDefinitions)
                    if (importName != null) {
                        val value = multivariantPlaylist.variableDefinitions[importName]
                        if (value != null) {
                            variableDefinitions.put(importName, value)
                        } else {
                            // The multivariant playlist does not declare the imported variable. Ignore.
                        }
                    } else {
                        variableDefinitions.put(
                            parseStringAttr(line, REGEX_NAME, variableDefinitions),
                            parseStringAttr(line, REGEX_VALUE, variableDefinitions)
                        )
                    }
                } else if (line.startsWith(TAG_MEDIA_DURATION)) {
                    segmentDurationUs = parseTimeSecondsToUs(line, REGEX_MEDIA_DURATION)
                    segmentTitle =
                        parseOptionalStringAttr(line, REGEX_MEDIA_TITLE, "", variableDefinitions)
                } else if (line.startsWith(TAG_SKIP)) {
                    val skippedSegmentCount: Int = parseIntAttr(line, REGEX_SKIPPED_SEGMENTS)
                    Assertions.checkState(previousMediaPlaylist != null && segments.isEmpty())
                    val startIndex =
                        (mediaSequence - Util.castNonNull<HlsMediaPlaylist>(previousMediaPlaylist).mediaSequence).toInt()
                    val endIndex = startIndex + skippedSegmentCount
                    if (startIndex < 0 || endIndex > previousMediaPlaylist!!.segments.size) {
                        // Throw to force a reload if not all segments are available in the previous playlist.
                        throw DeltaUpdateException()
                    }
                    for (i in startIndex..<endIndex) {
                        var segment = previousMediaPlaylist.segments[i]
                        if (mediaSequence != previousMediaPlaylist.mediaSequence) {
                            // If the media sequences of the playlists are not the same, we need to recreate the
                            // object with the updated relative start time and the relative discontinuity
                            // sequence. With identical playlist media sequences these values do not change.
                            val newRelativeDiscontinuitySequence =
                                (previousMediaPlaylist.discontinuitySequence
                                        - playlistDiscontinuitySequence
                                        + segment.relativeDiscontinuitySequence)
                            segment = segment.copyWith(
                                segmentStartTimeUs,
                                newRelativeDiscontinuitySequence
                            )
                        }
                        segments.add(segment)
                        segmentStartTimeUs += segment.durationUs
                        partStartTimeUs = segmentStartTimeUs
                        if (segment.byteRangeLength != C.LENGTH_UNSET.toLong()) {
                            segmentByteRangeOffset =
                                segment.byteRangeOffset + segment.byteRangeLength
                        }
                        relativeDiscontinuitySequence = segment.relativeDiscontinuitySequence
                        initializationSegment = segment.initializationSegment
                        cachedDrmInitData = segment.drmInitData
                        fullSegmentEncryptionKeyUri = segment.fullSegmentEncryptionKeyUri
                        if (segment.encryptionIV == null
                            || segment.encryptionIV != segmentMediaSequence.toHexString()
                        ) {
                            fullSegmentEncryptionIV = segment.encryptionIV
                        }
                        segmentMediaSequence++
                    }
                } else if (line.startsWith(TAG_KEY)) {
                    val method: String = parseStringAttr(line, REGEX_METHOD, variableDefinitions)
                    val keyFormat: String? =
                        parseOptionalStringAttr(
                            line,
                            REGEX_KEYFORMAT,
                            KEYFORMAT_IDENTITY,
                            variableDefinitions
                        )
                    fullSegmentEncryptionKeyUri = null
                    fullSegmentEncryptionIV = null
                    if (METHOD_NONE == method) {
                        currentSchemeDatas.clear()
                        cachedDrmInitData = null
                    } else  /* !METHOD_NONE.equals(method) */ {
                        fullSegmentEncryptionIV =
                            parseOptionalStringAttr(line, REGEX_IV, variableDefinitions)
                        if (KEYFORMAT_IDENTITY == keyFormat) {
                            if (METHOD_AES_128 == method) {
                                // The segment is fully encrypted using an identity key.
                                fullSegmentEncryptionKeyUri =
                                    parseStringAttr(line, REGEX_URI, variableDefinitions)
                            } else {
                                // Do nothing. Samples are encrypted using an identity key, but this is not supported.
                                // Hopefully, a traditional DRM alternative is also provided.
                            }
                        } else {
                            if (encryptionScheme == null) {
                                encryptionScheme = parseEncryptionScheme(method)
                            }
                            val schemeData: SchemeData? =
                                parseDrmSchemeData(line, keyFormat, variableDefinitions)
                            if (schemeData != null) {
                                cachedDrmInitData = null
                                currentSchemeDatas.put(keyFormat, schemeData)
                            }
                        }
                    }
                } else if (line.startsWith(TAG_BYTERANGE)) {
                    val byteRange: String =
                        parseStringAttr(line, REGEX_BYTERANGE, variableDefinitions)
                    val splitByteRange: Array<String> = Util.split(byteRange, "@")
                    segmentByteRangeLength = splitByteRange[0].toLong()
                    if (splitByteRange.size > 1) {
                        segmentByteRangeOffset = splitByteRange[1].toLong()
                    }
                } else if (line.startsWith(TAG_DISCONTINUITY_SEQUENCE)) {
                    hasDiscontinuitySequence = true
                    playlistDiscontinuitySequence =
                        line.substring(line.indexOf(':'.code.toChar()) + 1).toInt()
                } else if (line == TAG_DISCONTINUITY) {
                    relativeDiscontinuitySequence++
                } else if (line.startsWith(TAG_PROGRAM_DATE_TIME)) {
                    if (playlistStartTimeUs == 0L) {
                        val programDatetimeUs =
                            Util.msToUs(Util.parseXsDateTime(line.substring(line.indexOf(':') + 1)))
                        playlistStartTimeUs = programDatetimeUs - segmentStartTimeUs
                    }
                } else if (line == TAG_GAP) {
                    hasGapTag = true
                } else if (line == TAG_INDEPENDENT_SEGMENTS) {
                    hasIndependentSegmentsTag = true
                } else if (line == TAG_ENDLIST) {
                    hasEndTag = true
                } else if (line.startsWith(TAG_RENDITION_REPORT)) {
                    val lastMediaSequence: Long =
                        parseOptionalLongAttr(line, REGEX_LAST_MSN, C.INDEX_UNSET.toLong())
                    val lastPartIndex: Int =
                        parseOptionalIntAttr(line, REGEX_LAST_PART, C.INDEX_UNSET)
                    val uri: String = parseStringAttr(line, REGEX_URI, variableDefinitions)
                    val playlistUri = UriUtil.resolve(baseUri, uri).toUri()
                    renditionReports.add(
                        RenditionReport(
                            playlistUri,
                            lastMediaSequence,
                            lastPartIndex
                        )
                    )
                } else if (line.startsWith(TAG_PRELOAD_HINT)) {
                    if (preloadPart != null) {
                        continue
                    }
                    val type: String =
                        parseStringAttr(line, REGEX_PRELOAD_HINT_TYPE, variableDefinitions)
                    if (TYPE_PART != type) {
                        continue
                    }
                    val url: String = parseStringAttr(line, REGEX_URI, variableDefinitions)
                    val byteRangeStart: Long =
                        parseOptionalLongAttr(
                            line,
                            REGEX_BYTERANGE_START,  /* defaultValue= */
                            C.LENGTH_UNSET.toLong()
                        )
                    val byteRangeLength: Long =
                        parseOptionalLongAttr(
                            line,
                            REGEX_BYTERANGE_LENGTH,  /* defaultValue= */
                            C.LENGTH_UNSET.toLong()
                        )
                    val segmentEncryptionIV: String? =
                        getSegmentEncryptionIV(
                            segmentMediaSequence,
                            fullSegmentEncryptionKeyUri,
                            fullSegmentEncryptionIV
                        )
                    if (cachedDrmInitData == null && !currentSchemeDatas.isEmpty()) {
                        val schemeDatas = currentSchemeDatas.values.toTypedArray()
                        cachedDrmInitData = DrmInitData(encryptionScheme, *schemeDatas)
                        if (playlistProtectionSchemes == null) {
                            playlistProtectionSchemes =
                                getPlaylistProtectionSchemes(encryptionScheme, schemeDatas)
                        }
                    }
                    if (byteRangeStart == C.LENGTH_UNSET.toLong() || byteRangeLength != C.LENGTH_UNSET.toLong()) {
                        // Skip preload part if it is an unbounded range request.
                        preloadPart =
                            HlsMediaPlaylist.Part(
                                url,
                                initializationSegment,  /* durationUs= */
                                0,
                                relativeDiscontinuitySequence,
                                partStartTimeUs,
                                cachedDrmInitData,
                                fullSegmentEncryptionKeyUri,
                                segmentEncryptionIV,
                                if (byteRangeStart != C.LENGTH_UNSET.toLong()) byteRangeStart else 0,
                                byteRangeLength,  /* hasGapTag= */
                                false,  /* isIndependent= */
                                false,  /* isPreload= */
                                true
                            )
                    }
                } else if (line.startsWith(TAG_PART)) {
                    val segmentEncryptionIV: String? =
                        getSegmentEncryptionIV(
                            segmentMediaSequence,
                            fullSegmentEncryptionKeyUri,
                            fullSegmentEncryptionIV
                        )
                    val url: String = parseStringAttr(line, REGEX_URI, variableDefinitions)
                    val partDurationUs =
                        (parseDoubleAttr(line, REGEX_ATTR_DURATION) * C.MICROS_PER_SECOND).toLong()
                    var isIndependent: Boolean =
                        parseOptionalBooleanAttribute(
                            line,
                            REGEX_INDEPENDENT,  /* defaultValue= */
                            false
                        )
                    // The first part of a segment is always independent if the segments are independent.
                    isIndependent =
                        isIndependent or (hasIndependentSegmentsTag && trailingParts.isEmpty())
                    val isGap: Boolean =
                        parseOptionalBooleanAttribute(line, REGEX_GAP,  /* defaultValue= */false)
                    val byteRange: String? =
                        parseOptionalStringAttr(line, REGEX_ATTR_BYTERANGE, variableDefinitions)
                    var partByteRangeLength = C.LENGTH_UNSET.toLong()
                    if (byteRange != null) {
                        val splitByteRange: Array<String> = Util.split(byteRange, "@")
                        partByteRangeLength = splitByteRange[0].toLong()
                        if (splitByteRange.size > 1) {
                            partByteRangeOffset = splitByteRange[1].toLong()
                        }
                    }
                    if (partByteRangeLength == C.LENGTH_UNSET.toLong()) {
                        partByteRangeOffset = 0
                    }
                    if (cachedDrmInitData == null && !currentSchemeDatas.isEmpty()) {
                        val schemeDatas = currentSchemeDatas.values.toTypedArray()
                        cachedDrmInitData = DrmInitData(encryptionScheme, *schemeDatas)
                        if (playlistProtectionSchemes == null) {
                            playlistProtectionSchemes =
                                getPlaylistProtectionSchemes(encryptionScheme, schemeDatas)
                        }
                    }
                    trailingParts.add(
                        HlsMediaPlaylist.Part(
                            url,
                            initializationSegment,
                            partDurationUs,
                            relativeDiscontinuitySequence,
                            partStartTimeUs,
                            cachedDrmInitData,
                            fullSegmentEncryptionKeyUri,
                            segmentEncryptionIV,
                            partByteRangeOffset,
                            partByteRangeLength,
                            isGap,
                            isIndependent,  /* isPreload= */
                            false
                        )
                    )
                    partStartTimeUs += partDurationUs
                    if (partByteRangeLength != C.LENGTH_UNSET.toLong()) {
                        partByteRangeOffset += partByteRangeLength
                    }
                } else if (!line.startsWith("#") || line.startsWith(TAG_PREFETCH)) {
                    val segmentEncryptionIV: String? =
                        getSegmentEncryptionIV(
                            segmentMediaSequence,
                            fullSegmentEncryptionKeyUri,
                            fullSegmentEncryptionIV
                        )
                    segmentMediaSequence++

                    if (line.startsWith(TAG_PREFETCH)) {
                        segmentDurationUs = targetDurationUs
                        line = line.substring(line.indexOf(':') + 1)
                    }

                    val segmentUri: String = replaceVariableReferences(line, variableDefinitions)
                    var inferredInitSegment = urlToInferredInitSegment.get(segmentUri)
                    if (segmentByteRangeLength == C.LENGTH_UNSET.toLong()) {
                        // The segment has no byte range defined.
                        segmentByteRangeOffset = 0
                    } else if (isIFrameOnly && initializationSegment == null && inferredInitSegment == null) {
                        // The segment is a resource byte range without an initialization segment.
                        // As per RFC 8216, Section 4.3.3.6, we assume the initialization section exists in the
                        // bytes preceding the first segment in this segment's URL.
                        // We assume the implicit initialization segment is unencrypted, since there's no way for
                        // the playlist to provide an initialization vector for it.
                        inferredInitSegment =
                            HlsMediaPlaylist.Segment(
                                segmentUri,  /* byteRangeOffset= */
                                0,
                                segmentByteRangeOffset,  /* fullSegmentEncryptionKeyUri= */
                                null,  /* encryptionIV= */
                                null
                            )
                        urlToInferredInitSegment.put(segmentUri, inferredInitSegment)
                    }

                    if (cachedDrmInitData == null && !currentSchemeDatas.isEmpty()) {
                        val schemeDatas = currentSchemeDatas.values.toTypedArray()
                        cachedDrmInitData = DrmInitData(encryptionScheme, *schemeDatas)
                        if (playlistProtectionSchemes == null) {
                            playlistProtectionSchemes =
                                getPlaylistProtectionSchemes(encryptionScheme, schemeDatas)
                        }
                    }

                    segments.add(
                        HlsMediaPlaylist.Segment(
                            segmentUri,
                            initializationSegment ?: inferredInitSegment,
                            segmentTitle!!,
                            segmentDurationUs,
                            relativeDiscontinuitySequence,
                            segmentStartTimeUs,
                            cachedDrmInitData,
                            fullSegmentEncryptionKeyUri,
                            segmentEncryptionIV,
                            segmentByteRangeOffset,
                            segmentByteRangeLength,
                            hasGapTag,
                            trailingParts
                        )
                    )
                    segmentStartTimeUs += segmentDurationUs
                    partStartTimeUs = segmentStartTimeUs
                    segmentDurationUs = 0
                    segmentTitle = ""
                    trailingParts = ArrayList()
                    if (segmentByteRangeLength != C.LENGTH_UNSET.toLong()) {
                        segmentByteRangeOffset += segmentByteRangeLength
                    }
                    segmentByteRangeLength = C.LENGTH_UNSET.toLong()
                    hasGapTag = false
                }
            }

            val renditionReportMap: MutableMap<Uri, RenditionReport> =
                HashMap()
            for (i in renditionReports.indices) {
                val renditionReport = renditionReports[i]
                var lastMediaSequence = renditionReport.lastMediaSequence
                if (lastMediaSequence == C.INDEX_UNSET.toLong()) {
                    lastMediaSequence =
                        mediaSequence + segments.size - (if (trailingParts.isEmpty()) 1 else 0)
                }
                var lastPartIndex = renditionReport.lastPartIndex
                if (lastPartIndex == C.INDEX_UNSET && partTargetDurationUs != C.TIME_UNSET) {
                    val lastParts: MutableList<HlsMediaPlaylist.Part> =
                        if (trailingParts.isEmpty()) Iterables.getLast(
                            segments
                        )!!.parts else trailingParts
                    lastPartIndex = lastParts.size - 1
                }
                renditionReportMap.put(
                    renditionReport.playlistUri,
                    RenditionReport(renditionReport.playlistUri, lastMediaSequence, lastPartIndex)
                )
            }

            if (preloadPart != null) {
                trailingParts.add(preloadPart)
            }

            return HlsMediaPlaylist(
                playlistType,
                baseUri,
                tags,
                startOffsetUs,
                preciseStart,
                playlistStartTimeUs,
                hasDiscontinuitySequence,
                playlistDiscontinuitySequence,
                mediaSequence,
                version,
                targetDurationUs,
                partTargetDurationUs,
                hasIndependentSegmentsTag,
                hasEndTag,  /* hasProgramDateTime= */
                playlistStartTimeUs != 0L,
                playlistProtectionSchemes,
                segments,
                trailingParts,
                serverControl,
                renditionReportMap
            )
        }

        private fun getPlaylistProtectionSchemes(
            encryptionScheme: String?, schemeDatas: Array<SchemeData>
        ): DrmInitData {
            val playlistSchemeDatas = arrayOfNulls<SchemeData>(schemeDatas.size)
            for (i in schemeDatas.indices) {
                playlistSchemeDatas[i] = schemeDatas[i].copyWithData(null)
            }
            return DrmInitData(
                encryptionScheme,
                *playlistSchemeDatas.mapNotNull { it }.toTypedArray()
            )
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun getSegmentEncryptionIV(
            segmentMediaSequence: Long,
            fullSegmentEncryptionKeyUri: String?,
            fullSegmentEncryptionIV: String?
        ): String? {
            if (fullSegmentEncryptionKeyUri == null) {
                return null
            } else if (fullSegmentEncryptionIV != null) {
                return fullSegmentEncryptionIV
            }
            return segmentMediaSequence.toHexString()
        }

        private fun parseSelectionFlags(line: String): @SelectionFlags Int {
            var flags = 0
            if (parseOptionalBooleanAttribute(line, REGEX_DEFAULT, false)) {
                flags = flags or C.SELECTION_FLAG_DEFAULT
            }
            if (parseOptionalBooleanAttribute(line, REGEX_FORCED, false)) {
                flags = flags or C.SELECTION_FLAG_FORCED
            }
            if (parseOptionalBooleanAttribute(line, REGEX_AUTOSELECT, false)) {
                flags = flags or C.SELECTION_FLAG_AUTOSELECT
            }
            return flags
        }

        private fun parseRoleFlags(
            line: String, variableDefinitions: MutableMap<String, String>
        ): @RoleFlags Int {
            val concatenatedCharacteristics: String? =
                parseOptionalStringAttr(line, REGEX_CHARACTERISTICS, variableDefinitions)
            if (TextUtils.isEmpty(concatenatedCharacteristics)) {
                return 0
            }
            val characteristics: Array<String> = Util.split(concatenatedCharacteristics!!, ",")
            var roleFlags: @RoleFlags Int = 0
            if (Util.contains(characteristics, "public.accessibility.describes-video")) {
                roleFlags = roleFlags or C.ROLE_FLAG_DESCRIBES_VIDEO
            }
            if (Util.contains(characteristics, "public.accessibility.transcribes-spoken-dialog")) {
                roleFlags = roleFlags or C.ROLE_FLAG_TRANSCRIBES_DIALOG
            }
            if (Util.contains(characteristics, "public.accessibility.describes-music-and-sound")) {
                roleFlags = roleFlags or C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND
            }
            if (Util.contains(characteristics, "public.easy-to-read")) {
                roleFlags = roleFlags or C.ROLE_FLAG_EASY_TO_READ
            }
            return roleFlags
        }

        @Throws(ParserException::class)
        private fun parseDrmSchemeData(
            line: String, keyFormat: String?, variableDefinitions: MutableMap<String, String>
        ): SchemeData? {
            val keyFormatVersions: String? =
                parseOptionalStringAttr(line, REGEX_KEYFORMATVERSIONS, "1", variableDefinitions)
            if (KEYFORMAT_WIDEVINE_PSSH_BINARY == keyFormat) {
                val uriString: String = parseStringAttr(line, REGEX_URI, variableDefinitions)
                return SchemeData(
                    C.WIDEVINE_UUID,
                    MimeTypes.VIDEO_MP4,
                    Base64.decode(uriString.substring(uriString.indexOf(',')), Base64.DEFAULT)
                )
            } else if (KEYFORMAT_WIDEVINE_PSSH_JSON == keyFormat) {
                return SchemeData(C.WIDEVINE_UUID, "hls", Util.getUtf8Bytes(line))
            } else if (KEYFORMAT_PLAYREADY == keyFormat && "1" == keyFormatVersions) {
                val uriString: String = parseStringAttr(line, REGEX_URI, variableDefinitions)
                val data =
                    Base64.decode(uriString.substring(uriString.indexOf(',')), Base64.DEFAULT)
                val psshData = PsshAtomUtil.buildPsshAtom(C.PLAYREADY_UUID, data)
                return SchemeData(C.PLAYREADY_UUID, MimeTypes.VIDEO_MP4, psshData)
            }
            return null
        }

        private fun parseServerControl(line: String): ServerControl {
            val skipUntilSeconds: Double =
                parseOptionalDoubleAttr(
                    line,
                    REGEX_CAN_SKIP_UNTIL,  /* defaultValue= */
                    C.TIME_UNSET.toDouble()
                )
            val skipUntilUs =
                if (skipUntilSeconds == C.TIME_UNSET.toDouble())
                    C.TIME_UNSET
                else (skipUntilSeconds * C.MICROS_PER_SECOND).toLong()
            val canSkipDateRanges: Boolean =
                parseOptionalBooleanAttribute(
                    line,
                    REGEX_CAN_SKIP_DATE_RANGES,  /* defaultValue= */
                    false
                )
            val holdBackSeconds: Double =
                parseOptionalDoubleAttr(
                    line,
                    REGEX_HOLD_BACK,  /* defaultValue= */
                    C.TIME_UNSET.toDouble()
                )
            val holdBackUs =
                if (holdBackSeconds == C.TIME_UNSET.toDouble())
                    C.TIME_UNSET
                else (holdBackSeconds * C.MICROS_PER_SECOND).toLong()
            val partHoldBackSeconds: Double =
                parseOptionalDoubleAttr(line, REGEX_PART_HOLD_BACK, C.TIME_UNSET.toDouble())
            val partHoldBackUs =
                if (partHoldBackSeconds == C.TIME_UNSET.toDouble())
                    C.TIME_UNSET
                else (partHoldBackSeconds * C.MICROS_PER_SECOND).toLong()
            val canBlockReload: Boolean =
                parseOptionalBooleanAttribute(
                    line,
                    REGEX_CAN_BLOCK_RELOAD,  /* defaultValue= */
                    false
                )

            return ServerControl(
                skipUntilUs, canSkipDateRanges, holdBackUs, partHoldBackUs, canBlockReload
            )
        }

        private fun parseEncryptionScheme(method: String?): String {
            return if (METHOD_SAMPLE_AES_CENC == method || METHOD_SAMPLE_AES_CTR == method)
                C.CENC_TYPE_cenc
            else
                C.CENC_TYPE_cbcs
        }

        @Throws(ParserException::class)
        private fun parseIntAttr(line: String, pattern: Pattern): Int {
            return parseStringAttr(line, pattern, mutableMapOf()).toInt()
        }

        private fun parseOptionalIntAttr(line: String, pattern: Pattern, defaultValue: Int): Int {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                return Assertions.checkNotNull<String>(matcher.group(1)).toInt()
            }
            return defaultValue
        }

        @Throws(ParserException::class)
        private fun parseLongAttr(line: String, pattern: Pattern): Long {
            return parseStringAttr(line, pattern, mutableMapOf()).toLong()
        }

        private fun parseOptionalLongAttr(
            line: String,
            pattern: Pattern,
            defaultValue: Long
        ): Long {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                return Assertions.checkNotNull<String>(matcher.group(1)).toLong()
            }
            return defaultValue
        }

        @Throws(ParserException::class)
        private fun parseTimeSecondsToUs(line: String, pattern: Pattern): Long {
            val timeValueSeconds: String =
                parseStringAttr(line, pattern, mutableMapOf())
            val timeValue = BigDecimal(timeValueSeconds)
            return timeValue.multiply(BigDecimal(C.MICROS_PER_SECOND)).toLong()
        }

        @Throws(ParserException::class)
        private fun parseDoubleAttr(line: String, pattern: Pattern): Double {
            return parseStringAttr(line, pattern, mutableMapOf()).toDouble()
        }

        @Throws(ParserException::class)
        private fun parseStringAttr(
            line: String, pattern: Pattern, variableDefinitions: MutableMap<String, String>
        ): String {
            val value: String? = parseOptionalStringAttr(line, pattern, variableDefinitions)
            if (value != null) {
                return value
            } else {
                throw ParserException.createForMalformedManifest(
                    "Couldn't match " + pattern.pattern() + " in " + line,  /* cause= */null
                )
            }
        }

        private fun parseOptionalStringAttr(
            line: String, pattern: Pattern, variableDefinitions: MutableMap<String, String>
        ): String? {
            return parseOptionalStringAttr(line, pattern, null, variableDefinitions)
        }

        private fun parseOptionalStringAttr(
            line: String,
            pattern: Pattern,
            defaultValue: @PolyNull String?,
            variableDefinitions: MutableMap<String, String>
        ): @PolyNull String? {
            val matcher = pattern.matcher(line)
            val value: @PolyNull String? =
                if (matcher.find()) Assertions.checkNotNull<String?>(matcher.group(1)) else defaultValue
            return (if (variableDefinitions.isEmpty() || value == null)
                value
            else
                replaceVariableReferences(
                    value,
                    variableDefinitions
                ))
        }

        private fun parseOptionalDoubleAttr(
            line: String,
            pattern: Pattern,
            defaultValue: Double
        ): Double {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                return Assertions.checkNotNull<String>(matcher.group(1)).toDouble()
            }
            return defaultValue
        }

        private fun replaceVariableReferences(
            string: String, variableDefinitions: MutableMap<String, String>
        ): String {
            val matcher: Matcher = REGEX_VARIABLE_REFERENCE.matcher(string)
            // TODO: Replace StringBuffer with StringBuilder once Java 9 is available.
            val stringWithReplacements = StringBuffer()
            while (matcher.find()) {
                val groupName = matcher.group(1)!!
                if (variableDefinitions.containsKey(groupName)) {
                    matcher.appendReplacement(
                        stringWithReplacements,
                        Matcher.quoteReplacement(variableDefinitions[groupName]!!)
                    )
                } else {
                    // The variable is not defined. The value is ignored.
                }
            }
            matcher.appendTail(stringWithReplacements)
            return stringWithReplacements.toString()
        }

        private fun parseOptionalBooleanAttribute(
            line: String, pattern: Pattern, defaultValue: Boolean
        ): Boolean {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                return BOOLEAN_TRUE == matcher.group(1)
            }
            return defaultValue
        }

        private fun compileBooleanAttrPattern(attribute: String?): Pattern {
            return Pattern.compile("$attribute=($BOOLEAN_FALSE|$BOOLEAN_TRUE)")
        }
    }
}
