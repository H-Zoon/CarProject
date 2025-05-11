package com.devidea.aicar.storage.room.document

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("SELECT * FROM document_entity")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document_entity WHERE id = :id")
    fun getDocumentByIdFlow(id: String): Flow<DocumentEntity?>

    @Query("DELETE FROM document_entity WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("SELECT * FROM document_entity WHERE isFavorite = 1")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    /** 태그에 해당하는 DocumentEntity 하나를 Flow로 반환 */
    @Query("SELECT * FROM document_entity WHERE tag = :tag LIMIT 1")
    fun getDocumentByTag(tag: DocumentTag): Flow<DocumentEntity?>

    @Transaction
    suspend fun setTagForDocument(id: String, tag: DocumentTag?) {
        tag?.let { clearTag(it) }
        updateDocumentTag(id, tag)
    }

    @Query("UPDATE document_entity SET tag = :tag WHERE id = :id")
    suspend fun updateDocumentTag(id: String, tag: DocumentTag?)

    @Query("UPDATE document_entity SET tag = NULL WHERE tag = :tag")
    suspend fun clearTag(tag: DocumentTag)

    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM document_entity WHERE id = :id AND isFavorite = 1")
    fun isFavoriteCount(id: String): Flow<Int>
}