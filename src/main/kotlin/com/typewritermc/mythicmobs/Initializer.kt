package com.typewritermc.mythicmobs

import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.logger

@Singleton
object Initializer : Initializable {
    override suspend fun initialize() {
        logger.info("MythicMobsExtension initialized.")
        MythicMobsExtension.init()
    }

    override suspend fun shutdown() {
        // cleanup
    }
}
