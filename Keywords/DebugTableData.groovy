import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.apache.poi.ss.usermodel.Workbook

/**
 * Debug utility to log table data from UI and compare with Excel expected values
 * @param tableXPath - XPath to table element
 * @param workbook - Excel Workbook object
 * @param expectedSheetName - Sheet name with expected results
 */
class DebugTableData {
    static void logTableComparison(String tableXPath, Workbook workbook, String expectedSheetName) {
        println("\n🔍 DEBUG: Reading table from UI...")
        def driver = DriverFactory.getWebDriver()
        WebElement table = driver.findElement(By.xpath(tableXPath))
        
        // Read headers
        List<String> headers = []
        def headerElements = table.findElements(By.xpath(".//thead//th | .//thead//td"))
        for (def header : headerElements) {
            String text = header.getText()?.trim()
            if (text) headers.add(text)
        }
        println("📋 Table Headers (${headers.size()}): ${headers.join(' | ')}")
        
        // Read rows
        List<List<String>> rows = []
        def rowElements = table.findElements(By.xpath(".//tbody//tr"))
        for (def row : rowElements) {
            List<String> rowData = []
            def cells = row.findElements(By.xpath(".//td"))
            for (def cell : cells) {
                rowData.add(cell.getText()?.trim() ?: '')
            }
            if (rowData.size() > 0) {
                rows.add(rowData)
            }
        }
        println("📊 Table Rows (${rows.size()}):")
        rows.eachWithIndex { row, idx ->
            println("   Row ${idx + 1}: ${row.join(' | ')}")
        }
        
        // Show expected values from Excel
        def expectedResults = GetExpectedResults.getExpectedResults(workbook)
        def expected = expectedResults[expectedSheetName]
        if (expected) {
            println("\n📝 Expected Values from Excel (${expected.size()} checks):")
            expected.each { exp ->
                println("   Row ${exp['row_number']}, Column '${exp['column_name']}': Expected '${exp['expected_value']}' (match: ${exp['match_type']})")
            }
        } else {
            println("\n⚠️  No expected results found for sheet: ${expectedSheetName}")
        }
    }
}
