package rest_api.service

import org.springframework.stereotype.Service
import rest_api.repository.ClientUTxORepository
import rest_api.repository.TransactionRepository
import rest_api.repository.model.Transaction
import rest_api.repository.model.UTxOs
import java.util.*
import kotlin.reflect.typeOf

/**
 * Service for interactions with employee domain object
 */
@Service
class TransactionService (private val transactionRepository: TransactionRepository,
                          private val clientUTxOsRepository: ClientUTxORepository) {

    var utxos : List<Pair<String,String>> = listOf(Pair("",""))

    /**
     * Create transaction.
     *
     * @param tx the transaction
     * @return status (TRUEEEEEEEE)
     */
    fun createTransaction(tx: Transaction): Boolean {
        // TODO: do!!
        /*println(tx.inputs_tx_id)
        println(tx.inputs_address)
        println(tx.outputs_address)
        println(tx.outputs_coins)*/
        //println(tx)

        val tx_entity = transactionRepository.save(tx)

        // add to utxo list of client(s)
        val output_addresses = tx.outputs_address
        val tx_id = tx_entity.tx_id
        //clientUTxOsRepository.save(UTxOs(output_addresses[0], listOf(Pair("a","b"))))
        for(adr in output_addresses){
            val client = clientUTxOsRepository.findById(adr)
            //println("--------")
            //println(client)
            //println("--------")
            if (client.isEmpty){
                println("---add client---")
                clientUTxOsRepository.save(UTxOs(adr, listOf(Pair(tx_id,adr))))
                println("---add client---")
            }
            else{
                var lst = client.get().lst
                println("1111111")
                println(lst)
                println("1111111")
                lst += Pair(tx_id,adr)
                println("222222")
                println(lst)
                println("222222")
                try {
                    clientUTxOsRepository.setUtxoList(lst,client.get().id)
                }catch (e : Exception){
                    println("-----Exception-----")
                    println(e)
                    println("----Exception-----")
                }
                println("333333")
                println(client.get().lst)
                println("333333")
            }
        }
        return true
    }


    fun transferCoins(sender_address: String, receiver_address: String, amount: Long): Boolean {
        // TODO: do!!
        val txs = transactionRepository.findAll()
        val utxo = clientUTxOsRepository.findById(sender_address)
        println(utxo)
        var count: Long = 0
        for (t in txs){
            for ((i, o) in t.outputs_address.withIndex()){
                if (o == sender_address){
                    count += t.outputs_coins[i]
                }
            }
        }
        if (count >= amount)
            return true
        return false
    }

    fun getUnspentTransactions(address: Long): Int{
        return 3
    }

    fun getTransactionHistory(address: Long): Int{
        return 4
    }

    fun getTransactionHistoryForAll(): List<Transaction> = transactionRepository.findAll()
}