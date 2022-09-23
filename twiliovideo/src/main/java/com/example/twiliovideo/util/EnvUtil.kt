package com.example.twiliovideo.util

import com.example.twiliovideo.data.api.TWILIO_API_DEV_ENV
import com.example.twiliovideo.data.api.TWILIO_API_STAGE_ENV

object EnvUtil {
    private const val TWILIO_DEV_ENV = "Development"
    private const val TWILIO_STAGE_ENV = "Staging"
    private const val TWILIO_PROD_ENV = "Production"
    const val TWILIO_ENV_KEY = "TWILIO_ENVIRONMENT"
    fun getNativeEnvironmentVariableValue(environment: String?): String {
        if (environment != null) {
            if (environment == TWILIO_API_DEV_ENV) {
                return TWILIO_DEV_ENV
            } else if (environment == TWILIO_API_STAGE_ENV) {
                return TWILIO_STAGE_ENV
            }
        }
        return TWILIO_PROD_ENV
    }
}