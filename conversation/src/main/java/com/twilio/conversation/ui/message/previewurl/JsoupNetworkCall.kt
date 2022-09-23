package com.twilio.conversation.ui.message.previewurl

import com.twilio.conversation.data.model.previewurl.OpenGraphResult
import com.twilio.conversation.utils.JsoupUtils.AGENT
import com.twilio.conversation.utils.JsoupUtils.DOC_SELECT_QUERY
import com.twilio.conversation.utils.JsoupUtils.OG_DESCRIPTION
import com.twilio.conversation.utils.JsoupUtils.OG_IMAGE
import com.twilio.conversation.utils.JsoupUtils.OG_SITE_NAME
import com.twilio.conversation.utils.JsoupUtils.OG_TITLE
import com.twilio.conversation.utils.JsoupUtils.OG_TYPE
import com.twilio.conversation.utils.JsoupUtils.OG_URL
import com.twilio.conversation.utils.JsoupUtils.OPEN_GRAPH_KEY
import com.twilio.conversation.utils.JsoupUtils.PROPERTY
import com.twilio.conversation.utils.JsoupUtils.REFERRER
import com.twilio.conversation.utils.JsoupUtils.TIMEOUT
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL

/**
 * Created by ThanhTran on 7/18/2022.
 */

class JsoupNetworkCall {

    private var openGraphResult: OpenGraphResult? = null

    fun callUrl(url: String): OpenGraphResult? {
        openGraphResult = OpenGraphResult()
        try {
            val response = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(AGENT)
                .referrer(REFERRER)
                .timeout(TIMEOUT)
                .followRedirects(true)
                .execute()

            val doc = response.parse()
            val ogTags = doc.select(DOC_SELECT_QUERY)
            when {
                ogTags.size > 0 ->
                    ogTags.forEachIndexed { index, _ ->
                        val tag = ogTags[index]
                        val text = tag.attr(PROPERTY)

                        when (text) {
                            OG_IMAGE -> {
                                openGraphResult?.image = (tag.attr(OPEN_GRAPH_KEY))
                            }
                            OG_DESCRIPTION -> {
                                openGraphResult?.description = (tag.attr(OPEN_GRAPH_KEY))
                            }
                            OG_URL -> {
                                openGraphResult?.url = (tag.attr(OPEN_GRAPH_KEY))
                            }
                            OG_TITLE -> {
                                openGraphResult?.title = (tag.attr(OPEN_GRAPH_KEY))
                            }
                            OG_SITE_NAME -> {
                                openGraphResult?.siteName = (tag.attr(OPEN_GRAPH_KEY))
                            }
                            OG_TYPE -> {
                                openGraphResult?.type = (tag.attr(OPEN_GRAPH_KEY))
                            }
                        }
                    }
            }

            if (openGraphResult?.title.isNullOrEmpty()){
                openGraphResult?.title = doc.title()
            }
            if (openGraphResult?.description.isNullOrEmpty())
                openGraphResult?.description = if (doc.select("meta[name=description]").size != 0) doc.select("meta[name=description]")
                    .first().attr("content") else ""
            if (openGraphResult?.url.isNullOrEmpty())
                openGraphResult?.url = getBaseUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return openGraphResult
    }

    private fun getBaseUrl(urlString: String): String {
        val url: URL = URI.create(urlString).toURL()
        return url.protocol.toString() + "://" + url.authority + "/"
    }
}