package rest_api.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import rest_api.repository.model.UTxOs


interface ClientUTxORepository: JpaRepository<UTxOs, String> {

    /*@Modifying
    @Query("update UTxOs utxo set utxo.lst = :lst where utxo.id = :id")
    fun setUtxoList(lst: List<Pair<String?,String>>, id: String)
    : Int*/


    @Modifying
    @Transactional
    @Query("update UTxOs client set client.lst = ?1 where client.id = ?2")
    fun setUtxoList(lst: List<Pair<String?,String>>, id: String): Int
}