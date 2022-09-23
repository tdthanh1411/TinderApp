package com.twilio.conversation.ui.message.previewurl

import com.twilio.conversation.data.model.previewurl.OpenGraphResult
import com.twilio.conversation.utils.checkNullParserResult
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class OpenGraphParser(
    private val listener: OpenGraphCallback,
    private var showNullOnEmpty: Boolean = false
) {

    private var url: String = ""

    private val jsoupNetworkCall = JsoupNetworkCall()

    private var openGraphResult: OpenGraphResult? = null

    fun parse(url: String) {
        this.url = url
        parseLink().parse()
    }

    inner class parseLink : CoroutineScope {

        private val job: Job = Job()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + job

        fun parse() = launch {
            val result = fetchContent()
            result?.let {
                listener.onPostResponse(it)
            }
        }
    }

    private suspend fun fetchContent() = withContext(Dispatchers.IO) {
        if (!url.contains("http")) {
            url = "http://$url"
        }
            openGraphResult = jsoupNetworkCall.callUrl(url)
            val isResultNull = checkNullParserResult(openGraphResult)
            if (!isResultNull) {
                return@withContext openGraphResult
            }

        if (checkNullParserResult(openGraphResult) && showNullOnEmpty) {
            launch(Dispatchers.Main) {
                listener.onError("Null or empty response from the server")
            }
            return@withContext null
        }
        return@withContext openGraphResult
    }
}