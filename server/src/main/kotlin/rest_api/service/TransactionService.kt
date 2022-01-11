package rest_api.service

import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import rest_api.repository.TransactionRepository
import rest_api.repository.model.Transaction

/**
 * Service for interactions with employee domain object
 */
@Service
class TransactionService (private val transactionRepository: TransactionRepository) {

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
        transactionRepository.save(tx)
        return true
    }


    fun transferCoins(sender_address: String, receiver_address: String, amount: Long): Boolean {
        // TODO: do!!
        val txs = transactionRepository.findAll()
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