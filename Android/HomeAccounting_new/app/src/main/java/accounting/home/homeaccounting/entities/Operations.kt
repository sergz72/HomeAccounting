package accounting.home.homeaccounting.entities

data class Operations(val operations: List<FinanceOperation>,
                      val totals: List<FinanceTotal>)

