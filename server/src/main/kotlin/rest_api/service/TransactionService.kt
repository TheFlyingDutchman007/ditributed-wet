package rest_api.service

import com.example.api.repository.model.Employee
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import rest_api.repository.model.Transaction

/**
 * Service for interactions with employee domain object
 */
@Service
class TransactionService {

    /**
     * Create transaction.
     *
     * @param tx the transaction
     * @return status (TRUEEEEEEEE)
     */
    fun createTransaction(tx: Transaction): Boolean {
        // TODO: do!!
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

    fun getTransactionHistoryForAll(): Int{
        return 5
    }
}