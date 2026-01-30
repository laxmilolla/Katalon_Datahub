import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.apache.poi.ss.usermodel.Workbook

/**
 * Validate table data against expected results
 * @param tabName - Tab name to switch to (optional)
 * @param expectedResultsSheet - Sheet name with expected results
 * @param tableXPath - XPath to table (optional, defaults to first visible table)
 * @param workbook - Excel Workbook object
 * @return Map with success (boolean) and mismatches (List)
 */
class ValidateTable {
    static Map<String, Object> validate(String tabName, String expectedResultsSheet, 
                                         String tableXPath, Workbook workbook) {
        // Get expected results
        def expectedResults = GetExpectedResults.getExpectedResults(workbook)
        def expected = expectedResults[expectedResultsSheet]
        
        if (!expected) {
            throw new Exception("Expected results not found for sheet: ${expectedResultsSheet}")
        }
        
        // Switch to tab if needed
        if (tabName) {
            switchToTab(tabName)
        }
        
        // Read table from UI
        def tableData = readTableFromUI(tableXPath)
        
        // Compare and find mismatches
        List<Map<String, Object>> mismatches = []
        List<Map<String, Object>> matches = []
        
        for (def expectedRow : expected) {
            int rowNum = expectedRow['row_number'] ?: 0
            String colName = expectedRow['column_name'] ?: ''
            String expectedValue = expectedRow['expected_value'] ?: ''
            String matchType = expectedRow['match_type'] ?: 'exact'
            
            if (rowNum > 0 && colName && expectedValue) {
                // Find column index
                int colIndex = findColumnIndex(tableData['headers'], colName)
                if (colIndex < 0) {
                    mismatches.add([
                        row: rowNum,
                        column: colName,
                        expected: expectedValue,
                        actual: 'Column not found',
                        matchType: matchType
                    ])
                    continue
                }
                
                // Get actual value
                if (rowNum <= tableData['rows'].size()) {
                    def row = tableData['rows'][rowNum - 1]
                    String actualValue = row[colIndex] ?: ''
                    
                    // Match based on match type
                    boolean matched = matchValue(actualValue, expectedValue, matchType)
                    
                    if (matched) {
                        matches.add([
                            row: rowNum,
                            column: colName,
                            expected: expectedValue,
                            actual: actualValue,
                            matchType: matchType
                        ])
                    } else {
                        mismatches.add([
                            row: rowNum,
                            column: colName,
                            expected: expectedValue,
                            actual: actualValue,
                            matchType: matchType
                        ])
                    }
                } else {
                    mismatches.add([
                        row: rowNum,
                        column: colName,
                        expected: expectedValue,
                        actual: 'Row not found',
                        matchType: matchType
                    ])
                }
            }
        }
        
        return [
            success: mismatches.isEmpty(),
            mismatches: mismatches,
            matches: matches
        ]
    }
    
    private static void switchToTab(String tabName) {
        try {
            // Find tab by text (case-insensitive)
            String xpath = "//*[@role='tab' and contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${tabName.toLowerCase()}')]"
            WebUI.click(findTestObject('Object Repository/Dynamic', ['xpath': xpath]))
            WebUI.delay(1)
        } catch (Exception e) {
            // Try button fallback
            String xpath = "//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${tabName.toLowerCase()}')]"
            WebUI.click(findTestObject('Object Repository/Dynamic', ['xpath': xpath]))
            WebUI.delay(1)
        }
    }
    
    private static Map<String, Object> readTableFromUI(String tableXPath) {
        def driver = DriverFactory.getWebDriver()
        WebElement table
        
        if (tableXPath) {
            table = driver.findElement(By.xpath(tableXPath))
        } else {
            // Find first visible table
            table = driver.findElement(By.xpath("//table[.//tbody/tr]"))
        }
        
        // Read headers
        List<String> headers = []
        def headerElements = table.findElements(By.xpath(".//thead//th | .//thead//td"))
        for (def header : headerElements) {
            String text = header.getText()?.trim()
            if (text) headers.add(text)
        }
        
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
        
        return [
            headers: headers,
            rows: rows
        ]
    }
    
    private static int findColumnIndex(List<String> headers, String columnName) {
        String colLower = columnName.toLowerCase().trim()
        
        // Try exact match first
        for (int i = 0; i < headers.size(); i++) {
            if (headers[i].toLowerCase().trim() == colLower) {
                return i
            }
        }
        
        // Try partial match
        for (int i = 0; i < headers.size(); i++) {
            if (headers[i].toLowerCase().contains(colLower)) {
                return i
            }
        }
        
        return -1
    }
    
    private static boolean matchValue(String actual, String expected, String matchType) {
        String actualTrimmed = (actual ?: '').trim()
        String expectedTrimmed = (expected ?: '').trim()
        
        // Normalize numeric values: remove trailing .0 from numbers (e.g., '6.0' -> '6', '1.0' -> '1')
        actualTrimmed = normalizeNumericString(actualTrimmed)
        expectedTrimmed = normalizeNumericString(expectedTrimmed)
        
        // Convert to lowercase for comparison (except for numeric comparisons)
        String actualLower = actualTrimmed.toLowerCase()
        String expectedLower = expectedTrimmed.toLowerCase()
        
        if (matchType == 'empty_check') {
            return !actualLower || actualLower == '' || actualLower.contains('0 errors')
        } else if (matchType == 'exact') {
            return actualLower == expectedLower
        } else if (matchType == 'contains') {
            return actualLower.contains(expectedLower)
        }
        
        return actualLower == expectedLower
    }
    
    /**
     * Normalize numeric strings by removing trailing .0 (e.g., '6.0' -> '6', '1.0' -> '1')
     * Also handles negative numbers and decimals (e.g., '6.5' stays '6.5')
     */
    private static String normalizeNumericString(String value) {
        if (!value) return value
        
        // Check if it's a number (integer or decimal)
        // Pattern: optional minus, digits, optional .0 or .digits
        def numericPattern = /^-?\d+\.0+$/
        if (value.matches(numericPattern)) {
            // Remove .0 trailing zeros (e.g., '6.0' -> '6', '1.00' -> '1')
            return value.replaceAll(/\.0+$/, '')
        }
        
        // Check for integer with trailing .0 (more flexible pattern)
        if (value.matches(/^-?\d+\.0$/)) {
            return value.replaceAll(/\.0$/, '')
        }
        
        return value
    }
    
}
