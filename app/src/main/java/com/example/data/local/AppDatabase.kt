package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.domain.Article
import com.example.domain.ArticleRecord
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.VerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity representing a saved / bookmarked Article on the local device
 * (Build Specification v3, Sections 2.1, 3.1, 3.5 & 7.1).
 *
 * Includes all required fields of [Article] (`id`, `title`, `language`, `category`,
 * `scope`, `cityId`, `publisherName`, `sourceUrl`, `publishedAt`, `fetchedAt`,
 * `verificationStatus`, and `rightsStatus`) plus bookmark metadata (`savedAtEpochMs`,
 * `isBookmarked`, `stateCode`, `permittedSummary`).
 */
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val language: String,
    val category: String,
    val scope: String,
    val cityId: String?,
    val publisherName: String,
    val sourceUrl: String,
    val publishedAt: String?,
    val fetchedAt: Long,
    val verificationStatus: String,
    val rightsStatus: String,
    val stateCode: String? = null,
    val permittedSummary: String? = null,
    val savedAtEpochMs: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = true
) {
    /**
     * Converts this Room [ArticleEntity] back into a domain [Article],
     * enforcing LINK_ONLY / BLOCKED summary stripping rules.
     */
    fun toDomain(): Article {
        val parsedVerification = VerificationStatus.fromWire(verificationStatus)
        val parsedRights = RightsStatus.fromWire(rightsStatus)
        val safeSummary = if (
            parsedRights == RightsStatus.LINK_ONLY ||
            parsedRights == RightsStatus.BLOCKED ||
            parsedVerification == VerificationStatus.BLOCKED
        ) {
            null
        } else {
            permittedSummary
        }
        return Article(
            id = id,
            title = title,
            language = language,
            category = category,
            scope = NewsScope.fromWire(scope) ?: NewsScope.NATIONAL,
            cityId = cityId,
            publisherName = publisherName,
            sourceUrl = sourceUrl,
            publishedAt = publishedAt,
            fetchedAt = fetchedAt,
            verificationStatus = parsedVerification,
            rightsStatus = parsedRights,
            stateCode = stateCode,
            permittedSummary = safeSummary
        )
    }

    /**
     * Converts this [ArticleEntity] into a [BookmarkEntity] for UI compatibility.
     */
    fun toBookmarkEntity(): BookmarkEntity {
        val domain = toDomain()
        return BookmarkEntity(
            articleId = domain.id,
            title = domain.title,
            language = domain.language,
            category = domain.category,
            scopeWire = domain.scope.wireKey,
            cityId = domain.cityId,
            stateCode = domain.stateCode,
            publisherName = domain.publisherName,
            canonicalSourceUrl = domain.sourceUrl,
            publishedAtIso = domain.publishedAt,
            savedAtEpochMs = savedAtEpochMs,
            verificationStatusWire = domain.verificationStatus.wireKey,
            rightsStatusWire = domain.rightsStatus.wireKey,
            permittedSummary = domain.safePermittedSummary
        )
    }

    companion object {
        /**
         * Creates an [ArticleEntity] from a domain [Article], stripping unlicensed or blocked summaries
         * before persisting to SQLite.
         */
        fun fromDomain(
            article: Article,
            savedAtEpochMs: Long = System.currentTimeMillis(),
            isBookmarked: Boolean = true
        ): ArticleEntity = ArticleEntity(
            id = article.id,
            title = article.title,
            language = article.language,
            category = article.category,
            scope = article.scope.wireKey,
            cityId = article.cityId,
            publisherName = article.publisherName,
            sourceUrl = article.sourceUrl,
            publishedAt = article.publishedAt,
            fetchedAt = article.fetchedAt,
            verificationStatus = article.verificationStatus.wireKey,
            rightsStatus = article.rightsStatus.wireKey,
            stateCode = article.stateCode,
            permittedSummary = article.safePermittedSummary,
            savedAtEpochMs = savedAtEpochMs,
            isBookmarked = isBookmarked
        )

        /**
         * Creates an [ArticleEntity] from an [ArticleRecord].
         */
        fun fromArticleRecord(
            record: ArticleRecord,
            savedAtEpochMs: Long = System.currentTimeMillis(),
            isBookmarked: Boolean = true
        ): ArticleEntity = fromDomain(
            article = Article.fromArticleRecord(record),
            savedAtEpochMs = savedAtEpochMs,
            isBookmarked = isBookmarked
        )
    }
}

/**
 * Room Data Access Object (`ArticleDao`) supporting full reactive & suspend CRUD operations
 * for saved bookmarks (`ArticleEntity` / `Article`).
 */
@Dao
interface ArticleDao {

    // -------------------------------------------------------------------------
    // READ Operations (Reactive Flow & One-shot Suspend)
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY savedAtEpochMs DESC")
    fun getAllBookmarks(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY savedAtEpochMs DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY savedAtEpochMs DESC")
    suspend fun getAllBookmarksOnce(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    fun observeArticleById(id: String): Flow<ArticleEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE id = :id AND isBookmarked = 1)")
    fun isBookmarked(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE id = :id AND isBookmarked = 1)")
    suspend fun isBookmarkedOnce(id: String): Boolean

    // -------------------------------------------------------------------------
    // CREATE & UPDATE Operations
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(articles: List<ArticleEntity>)

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Update
    suspend fun updateBookmark(article: ArticleEntity)

    // -------------------------------------------------------------------------
    // DELETE Operations
    // -------------------------------------------------------------------------

    @Delete
    suspend fun deleteArticle(article: ArticleEntity)

    @Delete
    suspend fun deleteBookmark(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("DELETE FROM articles WHERE isBookmarked = 1")
    suspend fun clearAllBookmarks()

    // -------------------------------------------------------------------------
    // Domain CRUD Helper Methods for Saved Bookmarks
    // -------------------------------------------------------------------------

    @Transaction
    suspend fun saveBookmark(article: Article, savedAtEpochMs: Long = System.currentTimeMillis()) {
        insertBookmark(
            ArticleEntity.fromDomain(
                article = article,
                savedAtEpochMs = savedAtEpochMs,
                isBookmarked = true
            )
        )
    }

    @Transaction
    suspend fun updateSavedBookmark(article: Article, savedAtEpochMs: Long = System.currentTimeMillis()) {
        updateBookmark(
            ArticleEntity.fromDomain(
                article = article,
                savedAtEpochMs = savedAtEpochMs,
                isBookmarked = true
            )
        )
    }

    @Transaction
    suspend fun removeBookmark(articleId: String) {
        deleteBookmarkById(articleId)
    }

    /**
     * Toggles the saved bookmark state for an [Article]:
     * - If currently bookmarked, deletes it and returns `false`.
     * - If not bookmarked, inserts it and returns `true`.
     */
    @Transaction
    suspend fun toggleBookmark(
        article: Article,
        savedAtEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        return if (isBookmarkedOnce(article.id)) {
            deleteBookmarkById(article.id)
            false
        } else {
            insertBookmark(
                ArticleEntity.fromDomain(
                    article = article,
                    savedAtEpochMs = savedAtEpochMs,
                    isBookmarked = true
                )
            )
            true
        }
    }
}

/**
 * Primary Room Database (`AppDatabase`) exposing both [ArticleDao] (for `ArticleEntity` bookmark CRUD)
 * and [NetraDao] (for cached feeds, local feedback drafts, and bounded health logs).
 */
@Database(
    entities = [
        ArticleEntity::class,
        CachedArticleEntity::class,
        BookmarkEntity::class,
        FeedbackDraftEntity::class,
        HealthLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun netraDao(): NetraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netra_news_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
