package accounting.home.homeaccounting.entities

data class Operations(val operations: List<FinanceOperation>,
                      val totals: List<FinanceTotal>,
                      val properties: Map<String, List<FinOpProperty>>)

