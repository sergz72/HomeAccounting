package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.*
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface

import com.google.gson.Gson

object SharedResources {
    var db: DB? = null
        private set

    var operations: List<FinanceTotalAndOperations> = listOf()

    fun getOperation(operationId: Int): FinanceOperation {
        return FinanceTotalAndOperations.getOperation(operations, operationId)
    }

    fun buildDB(dicts: Dicts) {
        db = DB(dicts)
    }

    fun setupServer(serverName: String) {
        HomeAccountingService.setupServer(serverName, 50008)
    }

    fun confirm(activity: Activity, messageId: Int, listener: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(messageId)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.confirmation)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null)
        val dialog = builder.create()
        dialog.show()
    }

    fun alert(activity: Activity, message: String?) {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(message)
                .setTitle(R.string.error)
                .setIcon(android.R.drawable.ic_dialog_alert)
        val dialog = builder.create()
        dialog.show()
    }

    fun alert(activity: Activity, messageId: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(messageId)
                .setTitle(R.string.error)
        val dialog = builder.create()
        dialog.show()
    }

    fun buildReportsService(date1: String, date2: String, groupBy: String, category: String, subcategory: String, account: String): HomeAccountingService<List<ReportItem>> {
        return HomeAccountingService<List<ReportItem>>()
                .setRequest("GET /reports?date1=${date1}&date2=${date2}&groupBy=${groupBy}&category=${category}&subcategory=${subcategory}&account=${account}")
    }

    fun buildGasReportService(date1: String, date2: String): HomeAccountingService<List<GasReportItem>> {
        return HomeAccountingService<List<GasReportItem>>()
                .setRequest("GET /reports?type=gas&date1=${date1}&date2=${date2}")
    }

    fun buildOperationsService(date: String): HomeAccountingService<List<FinanceTotalAndOperations>> {
        return HomeAccountingService<List<FinanceTotalAndOperations>>()
                .setRequest("GET /operations/${date}")
    }

    fun buildDeleteOperationService(operation: OperationDelete): HomeAccountingService<String> {
        return HomeAccountingService<String>()
                .setRequest("DELETE /operations " + Gson().toJson(operation))
    }

    fun buildModifyOperationService(operation: OperationModify): HomeAccountingService<String> {
        return HomeAccountingService<String>()
                .setRequest("PUT /operations " + Gson().toJson(operation))
    }

    fun buildAddOperationService(operation: OperationAdd): HomeAccountingService<String> {
        return HomeAccountingService<String>()
                .setRequest("POST /operations " + Gson().toJson(operation))
    }

    fun buildDictsService(): HomeAccountingService<Dicts> {
        return HomeAccountingService<Dicts>()
                .setRequest("GET /dicts")
    }
}
