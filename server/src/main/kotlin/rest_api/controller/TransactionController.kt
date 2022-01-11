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

/**
 * Controller for REST API endpoints
 */
@RestController
class TransactionController (private val transactionService: TransactionService) {

    @PostMapping("/submit_transaction>")
    fun createTransaction(@RequestBody payload: Transaction):
            Boolean = transactionService.createTransaction(payload)

    @PutMapping("/transfer/{address}/{amount}")
    fun transferCoins(@PathVariable("address") address: Long, @PathVariable("amount") amount: Long ):
            Int = transactionService.transferCoins(address, amount)

    // TODO: add controller for atomic transaction list

    @GetMapping("/unspent/{address}")
    fun getUnspentTransactions(@PathVariable("address") address: Long):
            Int = transactionService.getUnspentTransactions(address)

    @GetMapping("/history/{address}")
    fun getTransactionHistoryForAddress(@PathVariable("address") address: Long):
            Int = transactionService.getTransactionHistory(address)

    @GetMapping("/history")
    fun getTransactionHistoryForAll():
            Int = transactionService.getTransactionHistoryForAll()

}