package com.devidea.chevy.storage

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

    @Query("SELECT * FROM documents")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentByIdFlow(id: String): Flow<DocumentEntity?>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("SELECT * FROM documents WHERE isFavorite = 1")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Transaction
    suspend fun setTagForDocument(id: String, tag: DocumentTag?) {
        tag?.let { clearTag(it) }
        updateDocumentTag(id, tag)
    }

    @Query("UPDATE documents SET tag = :tag WHERE id = :id")
    suspend fun updateDocumentTag(id: String, tag: DocumentTag?)

    @Query("UPDATE documents SET tag = NULL WHERE tag = :tag")
    suspend fun clearTag(tag: DocumentTag)

    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM documents WHERE id = :id AND isFavorite = 1")
    fun isFavoriteCount(id: String): Flow<Int>
}