package rest_api.controller

import com.example.api.repository.model.Employee
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import rest_api.service.TransactionService
import rest_api.repository.model.Transaction
import rest_api.repository.model.TransactionsLedger
import rest_api.repository.model.UTxOs

/**
 * Controller for REST API endpoints
 */
@RestController
class TransactionController (private val transactionService: TransactionService) {

    @PostMapping("/submit_transaction")
    fun createTransaction(@RequestBody payload: Transaction):
            Boolean = transactionService.createTransaction(payload)

    @GetMapping("/transfer/{sender_address}/{receiver_address}/{amount}")
    fun transferCoins(@PathVariable("sender_address") sender_address: String,
                      @PathVariable("receiver_address") receiver_address: String,
                      @PathVariable("amount") amount: Long):
            Boolean = transactionService.transferCoins(sender_address,receiver_address, amount)

    // TODO: add controller for atomic transaction list

    @GetMapping("/unspent/{address}")
    fun getUnspentTransactions(@PathVariable("address") address: String):
            Map<Long,Unit> = transactionService.getUnspentTransactions(address)

    // --------- for debugging -------------------------------------
    @GetMapping("/coins/{address}")
    fun getCoins(@PathVariable("address") address: String):
            Long = transactionService.getCoins(address)
    // --------------------------------------------------------
    @GetMapping("/history/{address}")
    fun getTransactionHistoryForAddress(@PathVariable("address") address: String):
            TransactionsLedger = transactionService.getTransactionHistory(address)

    @GetMapping("/history")
    fun getTransactionHistoryForAll():
            TransactionsLedger = transactionService.getTransactionHistoryForAll()

    @GetMapping("/keys")
    fun getPublicKeys():
            MutableMap<Int,String> = transactionService.getPublicKeys()

}