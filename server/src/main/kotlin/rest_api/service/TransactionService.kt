package rest_api.service

import com.example.api.repository.model.Employee
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
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
        val id = tx.id
        val transfer = tx.transfer
        val UTxO = tx.UTxO
        transactionRepository.save(tx)
        return true
    }


    fun transferCoins(address: Long, amount: Long): Int {
        // TODO: do!!
        return 2
    }

    fun getUnspentTransactions(address: Long): Int{
        return 3
    }

    fun getTransactionHistory(address: Long): Int{
        return 4
    }

    fun getTransactionHistoryForAll(): List<Transaction> = transactionRepository.findAll()
}