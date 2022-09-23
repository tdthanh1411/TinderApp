package com.twilio.conversation.ui.message.previewurl

import com.twilio.conversation.data.model.previewurl.OpenGraphResult

interface OpenGraphCallback {
    fun onPostResponse(openGraphResult: OpenGraphResult)
    fun onError(error: String)
}