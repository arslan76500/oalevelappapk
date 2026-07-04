package com.oalevel.resources.data.repository

import com.oalevel.resources.data.local.*
import com.oalevel.resources.data.remote.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepository @Inject constructor(
    private val api: ApiService,
    private val resourceDao: ResourceDao,
    private val recentViewedDao: RecentViewedDao
) {
    /** Get top-level education levels */
    suspend fun getLevels(): Result<List<ResourceNode>> = runCatching { api.getLevels() }

    /** Get children of a node */
    suspend fun getNodeChildren(nodeId: String): Result<NodeChildrenResponse> =
        runCatching { api.getNodeChildren(nodeId) }

    /** Get a specific node */
    suspend fun getNode(nodeId: String): Result<ResourceNode> =
        runCatching { api.getNode(nodeId) }

    /** Get breadcrumb path */
    suspend fun getBreadcrumb(nodeId: String): Result<List<BreadcrumbItem>> =
        runCatching { api.getNodeBreadcrumb(nodeId) }

    /** Get PDF streaming URL */
    suspend fun getPdfUrl(nodeId: String): Result<PdfUrlResponse> =
        runCatching { api.getPdfUrl(nodeId) }

    /** Get resource statistics */
    suspend fun getStats(): Result<ResourceStats> = runCatching { api.getResourceStats() }

    /** Get recently added resources */
    suspend fun getRecentResources(limit: Int = 20): Result<List<ResourceItem>> =
        runCatching { api.getRecentResources(limit) }

    /** Search resources */
    suspend fun search(query: String, type: String = "all", page: Int = 1): Result<SearchPage> =
        runCatching { api.search(query, type, page) }

    /** Get public config */
    suspend fun getPublicConfig(): Result<PublicConfig> =
        runCatching { api.getPublicConfig() }

    /** Get active announcements */
    suspend fun getAnnouncements(): Result<List<Announcement>> =
        runCatching { api.getAnnouncements(active = true) }

    /** Get sync status */
    suspend fun getSyncStatus(): Result<SyncStatus> =
        runCatching { api.getSyncStatus() }

    /** Track recently viewed */
    suspend fun trackView(resource: ResourceNode, parentPath: String = "") {
        recentViewedDao.insert(
            RecentViewed(
                resourceId = resource.id,
                driveId = resource.driveId,
                name = resource.name,
                type = resource.type,
                parentPath = parentPath,
                viewedAt = System.currentTimeMillis()
            )
        )
    }

    fun getRecentViewed(): Flow<List<RecentViewed>> = recentViewedDao.getRecent()
}

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {
    fun getAllDownloads(): Flow<List<Download>> = downloadDao.getAllDownloads()
    suspend fun getByResourceId(resourceId: String): Download? = downloadDao.getByResourceId(resourceId)
    suspend fun insert(download: Download) = downloadDao.insert(download)
    suspend fun update(download: Download) = downloadDao.update(download)
    suspend fun updateProgress(id: String, status: String, progress: Int, downloaded: Long) =
        downloadDao.updateProgress(id, status, progress, downloaded)
    suspend fun delete(id: String) = downloadDao.delete(id)
}

@Singleton
class FavouriteRepository @Inject constructor(
    private val favouriteDao: FavouriteDao
) {
    fun getAllFavourites(): Flow<List<Favourite>> = favouriteDao.getAllFavourites()

    suspend fun isFavourite(resourceId: String): Boolean =
        favouriteDao.isFavourite(resourceId) > 0

    suspend fun toggleFavourite(resource: ResourceNode, parentPath: String = ""): Boolean {
        val existing = favouriteDao.getByResourceId(resource.id)
        return if (existing != null) {
            favouriteDao.delete(resource.id)
            false
        } else {
            favouriteDao.insert(
                Favourite(
                    id = java.util.UUID.randomUUID().toString(),
                    resourceId = resource.id,
                    driveId = resource.driveId,
                    name = resource.name,
                    type = resource.type,
                    parentPath = parentPath
                )
            )
            true
        }
    }
}

@Singleton
class ReadingProgressRepository @Inject constructor(
    private val readingProgressDao: ReadingProgressDao
) {
    fun getAllProgress(): Flow<List<ReadingProgress>> = readingProgressDao.getAllProgress()

    suspend fun getProgress(resourceId: String): ReadingProgress? =
        readingProgressDao.getProgress(resourceId)

    suspend fun saveProgress(
        resourceId: String,
        driveId: String,
        name: String,
        currentPage: Int,
        totalPages: Int,
        filePath: String? = null
    ) {
        readingProgressDao.saveProgress(
            ReadingProgress(
                resourceId = resourceId,
                driveId = driveId,
                name = name,
                currentPage = currentPage,
                totalPages = totalPages,
                filePath = filePath,
                lastReadAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getRecentlyRead(limit: Int = 10): List<ReadingProgress> =
        readingProgressDao.getRecent(limit)
}

@Singleton
class AiRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun sendMessage(
        message: String,
        sessionId: String,
        model: String? = null,
        imageBase64: String? = null,
        pdfText: String? = null
    ): Result<AiReply> = runCatching {
        api.aiChat(AiChatRequest(message, sessionId, model, imageBase64, pdfText))
    }

    suspend fun getMessages(sessionId: String): Result<List<AiMessage>> =
        runCatching { api.getAiMessages(sessionId) }

    suspend fun clearSession(sessionId: String): Result<Unit> =
        runCatching { api.clearAiSession(sessionId); Unit }
}
