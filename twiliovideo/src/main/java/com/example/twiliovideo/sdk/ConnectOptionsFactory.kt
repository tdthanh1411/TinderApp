package com.example.twiliovideo.sdk

import android.content.Context
import android.content.SharedPreferences
import com.example.twiliovideo.data.Preferences.AUDIO_ACOUSTIC_ECHO_CANCELER
import com.example.twiliovideo.data.Preferences.AUDIO_ACOUSTIC_ECHO_CANCELER_DEFAULT
import com.example.twiliovideo.data.Preferences.AUDIO_ACOUSTIC_NOISE_SUPRESSOR
import com.example.twiliovideo.data.Preferences.AUDIO_ACOUSTIC_NOISE_SUPRESSOR_DEFAULT
import com.example.twiliovideo.data.Preferences.AUDIO_AUTOMATIC_GAIN_CONTROL
import com.example.twiliovideo.data.Preferences.AUDIO_AUTOMATIC_GAIN_CONTROL_DEFAULT
import com.example.twiliovideo.data.Preferences.AUDIO_CODEC
import com.example.twiliovideo.data.Preferences.AUDIO_CODEC_DEFAULT
import com.example.twiliovideo.data.Preferences.AUDIO_OPEN_SLES_USAGE
import com.example.twiliovideo.data.Preferences.AUDIO_OPEN_SLES_USAGE_DEFAULT
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_DOMINANT_SPEAKER_PRIORITY
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_DOMINANT_SPEAKER_PRIORITY_DEFAULT
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_MAX_SUBSCRIPTION_BITRATE
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_MAX_SUBSCRIPTION_BITRATE_DEFAULT
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_MODE
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_MODE_DEFAULT
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_CONTROL
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_MODE
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_MODE_DEFAULT
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_VIDEO_CONTENT_PREFERENCES_MODE
import com.example.twiliovideo.data.Preferences.BANDWIDTH_PROFILE_VIDEO_CONTENT_PREFERENCES_MODE_DEFAULT
import com.example.twiliovideo.data.Preferences.ENABLE_AUTOMATIC_TRACK_SUBSCRIPTION
import com.example.twiliovideo.data.Preferences.ENABLE_AUTOMATIC_TRACK_SUBSCRIPTION_DEFAULT
import com.example.twiliovideo.data.Preferences.ENABLE_DOMINANT_SPEAKER
import com.example.twiliovideo.data.Preferences.ENABLE_DOMINANT_SPEAKER_DEFAULT
import com.example.twiliovideo.data.Preferences.ENABLE_INSIGHTS
import com.example.twiliovideo.data.Preferences.ENABLE_INSIGHTS_DEFAULT
import com.example.twiliovideo.data.Preferences.ENABLE_NETWORK_QUALITY_LEVEL
import com.example.twiliovideo.data.Preferences.ENABLE_NETWORK_QUALITY_LEVEL_DEFAULT
import com.example.twiliovideo.data.Preferences.ENVIRONMENT
import com.example.twiliovideo.data.Preferences.ENVIRONMENT_DEFAULT
import com.example.twiliovideo.data.Preferences.MAX_AUDIO_BITRATE
import com.example.twiliovideo.data.Preferences.MAX_AUDIO_BITRATE_DEFAULT
import com.example.twiliovideo.data.Preferences.MAX_VIDEO_BITRATE
import com.example.twiliovideo.data.Preferences.MAX_VIDEO_BITRATE_DEFAULT
import com.example.twiliovideo.data.Preferences.VIDEO_CODEC
import com.example.twiliovideo.data.Preferences.VIDEO_CODEC_DEFAULT
import com.example.twiliovideo.data.Preferences.VP8_SIMULCAST
import com.example.twiliovideo.data.Preferences.VP8_SIMULCAST_DEFAULT
import com.example.twiliovideo.util.EnvUtil
import com.example.twiliovideo.util.get
import com.twilio.androidenv.Env
import com.twilio.video.AudioCodec
import com.twilio.video.BandwidthProfileMode
import com.twilio.video.ClientTrackSwitchOffControl
import com.twilio.video.ConnectOptions
import com.twilio.video.EncodingParameters
import com.twilio.video.G722Codec
import com.twilio.video.H264Codec
import com.twilio.video.IsacCodec
import com.twilio.video.NetworkQualityConfiguration
import com.twilio.video.NetworkQualityVerbosity
import com.twilio.video.OpusCodec
import com.twilio.video.PcmaCodec
import com.twilio.video.PcmuCodec
import com.twilio.video.TrackPriority
import com.twilio.video.TrackSwitchOffMode
import com.twilio.video.VideoCodec
import com.twilio.video.VideoContentPreferencesMode
import com.twilio.video.Vp8Codec
import com.twilio.video.Vp9Codec
import com.twilio.video.ktx.createBandwidthProfileOptions
import com.twilio.video.ktx.createConnectOptions
import tvi.webrtc.voiceengine.WebRtcAudioManager
import tvi.webrtc.voiceengine.WebRtcAudioUtils

class ConnectOptionsFactory(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
//    private val tokenService: TokenService
) {

    suspend fun newInstance(token: String, roomName: String): ConnectOptions {
        setSdkEnvironment(sharedPreferences)
        val enableInsights = sharedPreferences.getBoolean(
                ENABLE_INSIGHTS,
                ENABLE_INSIGHTS_DEFAULT)

        val enableAutomaticTrackSubscription = sharedPreferences.get(
                ENABLE_AUTOMATIC_TRACK_SUBSCRIPTION,
                ENABLE_AUTOMATIC_TRACK_SUBSCRIPTION_DEFAULT)

        val enableDominantSpeaker = sharedPreferences.get(
                ENABLE_DOMINANT_SPEAKER,
                ENABLE_DOMINANT_SPEAKER_DEFAULT)

        val preferedVideoCodec: VideoCodec = getVideoCodecPreference(VIDEO_CODEC)

        val preferredAudioCodec: AudioCodec = getAudioCodecPreference()

        val configuration = NetworkQualityConfiguration(
                NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL,
                NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL)

        val mode = sharedPreferences.get(BANDWIDTH_PROFILE_MODE,
                BANDWIDTH_PROFILE_MODE_DEFAULT).let {
            getBandwidthProfileMode(it)
        }
        val maxSubscriptionBitrate = sharedPreferences.get(BANDWIDTH_PROFILE_MAX_SUBSCRIPTION_BITRATE,
                BANDWIDTH_PROFILE_MAX_SUBSCRIPTION_BITRATE_DEFAULT).toLong()

        val dominantSpeakerPriority = sharedPreferences.get(BANDWIDTH_PROFILE_DOMINANT_SPEAKER_PRIORITY,
                BANDWIDTH_PROFILE_DOMINANT_SPEAKER_PRIORITY_DEFAULT).let {
            getDominantSpeakerPriority(it)
        }
        val trackSwitchOffMode = sharedPreferences.get(BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_MODE,
                BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_MODE_DEFAULT).let {
            getTrackSwitchOffMode(it)
        }
        val clientTrackSwitchOffControl = sharedPreferences.get(BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_CONTROL,
            BANDWIDTH_PROFILE_TRACK_SWITCH_OFF_MODE_DEFAULT).let {
                getClientTrackSwitchOffControl(it)
        }
        val videoContentPreferencesMode = sharedPreferences.get(BANDWIDTH_PROFILE_VIDEO_CONTENT_PREFERENCES_MODE,
                BANDWIDTH_PROFILE_VIDEO_CONTENT_PREFERENCES_MODE_DEFAULT).let {
            getVideoContentPreferencesMode(it)
        }
        val bandwidthProfileOptions = createBandwidthProfileOptions {
            mode(mode)
            maxSubscriptionBitrate(maxSubscriptionBitrate)
            dominantSpeakerPriority(dominantSpeakerPriority)
            trackSwitchOffMode(trackSwitchOffMode)
            clientTrackSwitchOffControl?.let { clientTrackSwitchOffControl(it) }
            videoContentPreferencesMode?.let { videoContentPreferencesMode(it) }
        }

        val acousticEchoCanceler = sharedPreferences.getBoolean(
                AUDIO_ACOUSTIC_ECHO_CANCELER,
                AUDIO_ACOUSTIC_ECHO_CANCELER_DEFAULT)
        val noiseSuppressor = sharedPreferences.getBoolean(
                AUDIO_ACOUSTIC_NOISE_SUPRESSOR,
                AUDIO_ACOUSTIC_NOISE_SUPRESSOR_DEFAULT)
        val automaticGainControl = sharedPreferences.getBoolean(
                AUDIO_AUTOMATIC_GAIN_CONTROL,
                AUDIO_AUTOMATIC_GAIN_CONTROL_DEFAULT)
        val openSLESUsage = sharedPreferences.getBoolean(
                AUDIO_OPEN_SLES_USAGE,
                AUDIO_OPEN_SLES_USAGE_DEFAULT)
        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(!acousticEchoCanceler)
        WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(!noiseSuppressor)
        WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(!automaticGainControl)
        WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(!openSLESUsage)

        val isNetworkQualityEnabled = sharedPreferences.get(
                ENABLE_NETWORK_QUALITY_LEVEL,
                ENABLE_NETWORK_QUALITY_LEVEL_DEFAULT)

        val maxVideoBitrate = sharedPreferences.get(
                MAX_VIDEO_BITRATE,
                MAX_VIDEO_BITRATE_DEFAULT)

        val maxAudioBitrate = sharedPreferences.get(
                MAX_AUDIO_BITRATE,
                MAX_AUDIO_BITRATE_DEFAULT)

        return createConnectOptions(token) {
            roomName(roomName)
            enableInsights(enableInsights)
            enableAutomaticSubscription(enableAutomaticTrackSubscription)
            enableDominantSpeaker(enableDominantSpeaker)
            enableNetworkQuality(isNetworkQualityEnabled)
            networkQualityConfiguration(configuration)
            bandwidthProfile(bandwidthProfileOptions)
            encodingParameters(EncodingParameters(maxAudioBitrate, maxVideoBitrate))
            preferVideoCodecs(listOf(preferedVideoCodec))
            preferAudioCodecs(listOf(preferredAudioCodec))
        }
    }

    private fun getTrackSwitchOffMode(trackSwitchOffModeString: String) =
            when (trackSwitchOffModeString) {
                TrackSwitchOffMode.PREDICTED.name -> TrackSwitchOffMode.PREDICTED
                TrackSwitchOffMode.DETECTED.name -> TrackSwitchOffMode.DETECTED
                TrackSwitchOffMode.DISABLED.name -> TrackSwitchOffMode.DISABLED
                else -> null
            }

    private fun getClientTrackSwitchOffControl(controlString: String) =
            when (controlString.uppercase()) {
                ClientTrackSwitchOffControl.MANUAL.name -> ClientTrackSwitchOffControl.MANUAL
                ClientTrackSwitchOffControl.AUTO.name -> ClientTrackSwitchOffControl.AUTO
                else -> null
            }

    private fun getVideoContentPreferencesMode(modeString: String) =
            when (modeString.uppercase()) {
                VideoContentPreferencesMode.MANUAL.name -> VideoContentPreferencesMode.MANUAL
                VideoContentPreferencesMode.AUTO.name -> VideoContentPreferencesMode.AUTO
                else -> null
            }

    private fun getDominantSpeakerPriority(dominantSpeakerPriorityString: String) =
            when (dominantSpeakerPriorityString) {
                TrackPriority.LOW.name -> TrackPriority.LOW
                TrackPriority.STANDARD.name -> TrackPriority.STANDARD
                TrackPriority.HIGH.name -> TrackPriority.HIGH
                else -> null
            }

    private fun getBandwidthProfileMode(modeString: String): BandwidthProfileMode? {
        return when (modeString) {
            BandwidthProfileMode.COLLABORATION.name -> BandwidthProfileMode.COLLABORATION
            BandwidthProfileMode.GRID.name -> BandwidthProfileMode.GRID
            BandwidthProfileMode.PRESENTATION.name -> BandwidthProfileMode.PRESENTATION
            else -> null
        }
    }

    private fun getVideoCodecPreference(key: String): VideoCodec {
        return sharedPreferences.getString(key, VIDEO_CODEC_DEFAULT)?.let { videoCodecName ->
            when (videoCodecName) {
                Vp8Codec.NAME -> {
                    val simulcast = sharedPreferences.getBoolean(
                            VP8_SIMULCAST, VP8_SIMULCAST_DEFAULT)
                    Vp8Codec(simulcast)
                }
                H264Codec.NAME -> H264Codec()
                Vp9Codec.NAME -> Vp9Codec()
                else -> Vp8Codec()
            }
        } ?: Vp8Codec()
    }

    private fun getAudioCodecPreference(): AudioCodec {
        return sharedPreferences.getString(
                AUDIO_CODEC, AUDIO_CODEC_DEFAULT)?.let { audioCodecName ->

            when (audioCodecName) {
                IsacCodec.NAME -> IsacCodec()
                PcmaCodec.NAME -> PcmaCodec()
                PcmuCodec.NAME -> PcmuCodec()
                G722Codec.NAME -> G722Codec()
                else -> OpusCodec()
            }
        } ?: OpusCodec()
    }

    private fun setSdkEnvironment(sharedPreferences: SharedPreferences) {
        val env = sharedPreferences.getString(
                ENVIRONMENT, ENVIRONMENT_DEFAULT)
        val nativeEnvironmentVariableValue = EnvUtil.getNativeEnvironmentVariableValue(env)
        Env.set(
                context,
                EnvUtil.TWILIO_ENV_KEY,
                nativeEnvironmentVariableValue,
                true)
    }
}
