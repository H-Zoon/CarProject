package com.devidea.chevy.storage.room.document

// DocumentRepository.kt
import com.devidea.chevy.network.reomote.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DocumentRepository @Inject constructor(
    private val dao: DocumentDao
) {
    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = dao.getFavoriteDocuments()

    suspend fun insert(document: DocumentEntity) = dao.insertDocument(document)
    suspend fun deleteById(id: String) = dao.deleteDocumentById(id)
    suspend fun setTag(id: String, tag: DocumentTag?) = dao.setTagForDocument(id, tag)
    suspend fun toggleFavorite(id: String, isFav: Boolean) {
        dao.insertDocument(
            dao.getDocumentByIdFlow(id).firstOrNull()?.copy(isFavorite = isFav)
                ?: return
        )
    }

    /** 특정 태그의 문서를 Flow로 가져오기 */
    fun getDocumentByTag(tag: DocumentTag): Flow<DocumentEntity?> =
        dao.getDocumentByTag(tag)

    // 즐겨찾기 조회: 네트워크 Document id 기반
    fun isFavorite(id: String): Flow<Boolean> = dao.isFavoriteCount(id).map { it > 0 }

    // 태그 조회: 네트워크 Document id 기반
    fun getTag(id: String): Flow<DocumentTag?> = dao.getDocumentByIdFlow(id).map { it?.tag }

    // 네트워크 Document 객체를 기반으로 즐겨찾기 업데이트
    suspend fun updateFavoriteFromNetwork(doc: Document, isFav: Boolean) {
        insert(
            DocumentEntity(
                id = doc.id,
                place_name = doc.place_name,
                category_name = doc.category_name,
                category_group_code = doc.category_group_code,
                category_group_name = doc.category_group_name,
                phone = doc.phone,
                address_name = doc.address_name,
                road_address_name = doc.road_address_name,
                x = doc.x,
                y = doc.y,
                place_url = doc.place_url,
                distance = doc.distance,
                tag = dao.getDocumentByIdFlow(doc.id).firstOrNull()?.tag,
                isFavorite = isFav
            )
        )
    }

    // 네트워크 Document 객체를 기반으로 태그 업데이트
    suspend fun updateTagFromNetwork(doc: Document, tag: DocumentTag?) {
        insert(
            DocumentEntity(
                id = doc.id,
                place_name = doc.place_name,
                category_name = doc.category_name,
                category_group_code = doc.category_group_code,
                category_group_name = doc.category_group_name,
                phone = doc.phone,
                address_name = doc.address_name,
                road_address_name = doc.road_address_name,
                x = doc.x,
                y = doc.y,
                place_url = doc.place_url,
                distance = doc.distance,
                tag = tag,
                isFavorite = dao.isFavoriteCount(doc.id).firstOrNull()?.let { it > 0 } ?: false
            )
        )
    }
}