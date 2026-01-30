import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.configuration.RunConfiguration
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import java.io.File
import java.io.FileFilter
import groovy.json.JsonSlurper

/**
 * Validate data view table against TSV files (matches Playwright Validate_data_view)
 * @param folderPath - Path to folder containing TSV files (relative to project root or absolute)
 * @param dropdownXPath - XPath to node type dropdown
 * @param tableXPath - XPath to data view table (optional)
 * @param sortByColumn - Column name to sort by, or JSON mapping per node type (optional)
 * @param waitTime - Wait time in ms for status updates (default: 3000)
 * @return Map with success (boolean), nodeResults (List), and mismatches (List)
 */
class ValidateDataView {
    static Map<String, Object> validate(String folderPath, String dropdownXPath, 
                                         String tableXPath = null, String sortByColumn = null, 
                                         int waitTime = 3000) {
        println("🔍 ValidateDataView: Validating data view for folder '${folderPath}'")
        
        // Step 1: Resolve folder path
        String projectDir = RunConfiguration.getProjectDir()
        File folder
        if (folderPath.startsWith('/')) {
            folder = new File(folderPath)
        } else {
            folder = new File(projectDir, folderPath)
        }
        
        if (!folder.exists() || !folder.isDirectory()) {
            throw new Exception("Folder not found: ${folder.absolutePath}")
        }
        
        // Step 2: Find all TSV files
        File[] tsvFiles = folder.listFiles({ File f -> f.name.toLowerCase().endsWith('.tsv') } as FileFilter)
        if (!tsvFiles || tsvFiles.length == 0) {
            throw new Exception("No TSV files found in: ${folder.absolutePath}")
        }
        
        println("📁 Found ${tsvFiles.length} TSV file(s): ${tsvFiles.collect { it.name }.join(', ')}")
        
        // Step 3: Extract node types from filenames (same pattern as Playwright)
        List<String> nodeTypes = []
        for (File tsvFile : tsvFiles) {
            String basename = tsvFile.name.replace('.tsv', '').toLowerCase()
            
            // Pattern: "GC_Data_Loading_Template_consent_group_v9.0.0.tsv" -> "consent_group"
            def templateMatch = basename =~ /gc_data_loading_template_(.+?)_v\d+\.\d+\.\d+/
            if (templateMatch) {
                nodeTypes.add(templateMatch[0][1])
            } else {
                // Try simple pattern: "template_nodeType"
                def simpleMatch = basename =~ /template_(.+)/
                if (simpleMatch) {
                    nodeTypes.add(simpleMatch[0][1])
                } else {
                    // Fallback: use whole filename without extension
                    nodeTypes.add(basename)
                }
            }
        }
        
        // Step 4: Switch to "Data View" tab
        try {
            switchToTab('Data View')
            println("✅ Switched to Data View tab")
        } catch (Exception tabError) {
            println("⚠️  Could not switch to Data View tab (may already be active): ${tabError.message}")
        }
        WebUI.delay(1)
        
        // Step 5: Click dropdown to open it (if not already open)
        println("🖱️  Clicking dropdown: ${dropdownXPath}")
        TestObject dropdownObj = FindObjectByXPath.findObject(dropdownXPath)
        WebUI.waitForElementPresent(dropdownObj, 10)
        
        // Check if dropdown is already open
        def driver = DriverFactory.getWebDriver()
        WebElement dropdownElement = driver.findElement(By.xpath(dropdownXPath))
        String isExpanded = dropdownElement.getAttribute('aria-expanded')
        if (isExpanded != 'true') {
            WebUI.click(dropdownObj)
            WebUI.delay(0.5)
        } else {
            println("✅ Dropdown is already open")
        }
        
        // Step 6: For each node type, select it and validate
        List<Map<String, Object>> nodeResults = []
        List<Map<String, Object>> allMismatches = []
        
        for (int i = 0; i < nodeTypes.size(); i++) {
            String nodeType = nodeTypes[i]
            File tsvFile = tsvFiles[i]
            
            println("\n📊 Validating node type: ${nodeType} (file: ${tsvFile.name})")
            
            try {
                // Select the node type option from dropdown
                // Material-UI Select: options are in a listbox, find by text content
                String optionXPath = "//li[contains(@role, 'option') and contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${nodeType.toLowerCase()}')]"
                TestObject optionObj = FindObjectByXPath.findObject(optionXPath)
                WebUI.waitForElementPresent(optionObj, 5)
                WebUI.click(optionObj)
                WebUI.delay(1) // Wait for selection to apply
                
                // Wait for table to update after dropdown selection
                waitForTableStable(tableXPath)
                
                // Wait for status values to update (from "New" to final status)
                WebUI.delay(waitTime / 1000)
                println("⏱️  Waited ${waitTime}ms for status values to update")
                
                // Read table from UI
                Map<String, Object> uiTable = readUITable(tableXPath)
                println("✅ Read UI table: ${uiTable['headers'].size()} columns, ${uiTable['rows'].size()} rows")
                
                // DEBUG: Show UI table headers and sample data
                println("🔍 DEBUG UI Table Headers: ${uiTable['headers'].join(' | ')}")
                if (uiTable['rows'].size() > 0) {
                    println("🔍 DEBUG UI Table Row 1: ${uiTable['rows'][0].join(' | ')}")
                    // Show full cell values (check for truncation)
                    uiTable['rows'][0].eachWithIndex { cellValue, idx ->
                        if (cellValue.contains('...')) {
                            println("   ⚠️  Column ${idx} (${uiTable['headers'][idx]}): Value appears truncated: '${cellValue}'")
                        }
                    }
                }
                
                // Read TSV file
                Map<String, Object> tsvData = readTSV(tsvFile)
                println("✅ Read TSV file: ${tsvData['headers'].size()} columns, ${tsvData['rows'].size()} rows")
                
                // DEBUG: Show TSV headers and sample data
                println("🔍 DEBUG TSV Headers: ${tsvData['headers'].join(' | ')}")
                if (tsvData['rows'].size() > 0) {
                    println("🔍 DEBUG TSV Row 1: ${tsvData['rows'][0].join(' | ')}")
                }
                
                // Determine sort column for this node type
                String sortColumnForNode = getSortColumn(sortByColumn, nodeType, tsvData['headers'] as List<String>)
                
                // Sort TSV rows by sortColumnForNode if determined
                if (sortColumnForNode) {
                    tsvData = sortTable(tsvData, sortColumnForNode)
                    println("✅ Sorted TSV by column: ${sortColumnForNode}")
                }
                
                // Sort UI rows by sortColumnForNode if determined
                if (sortColumnForNode) {
                    uiTable = sortTable(uiTable, sortColumnForNode)
                    println("✅ Sorted UI table by column: ${sortColumnForNode}")
                }
                
                // Compare data
                List<Map<String, Object>> mismatches = compareTables(uiTable, tsvData, nodeType)
                
                if (mismatches.size() > 0) {
                    println("❌ Node type '${nodeType}' validation failed: ${mismatches.size()} mismatch(es)")
                    nodeResults.add([
                        nodeType: nodeType,
                        success: false,
                        mismatches: mismatches
                    ])
                    allMismatches.addAll(mismatches)
                } else {
                    println("✅ Node type '${nodeType}' validation passed")
                    nodeResults.add([
                        nodeType: nodeType,
                        success: true
                    ])
                }
                
            } catch (Exception error) {
                println("❌ Error validating node type '${nodeType}': ${error.message}")
                nodeResults.add([
                    nodeType: nodeType,
                    success: false,
                    error: error.message
                ])
            }
            
            // Re-open dropdown for next iteration (if not last)
            if (i < nodeTypes.size() - 1) {
                WebUI.click(dropdownObj)
                WebUI.delay(0.5)
            }
        }
        
        // Step 7: Determine overall success
        boolean allSuccess = nodeResults.every { it['success'] == true }
        int successCount = nodeResults.count { it['success'] == true }
        
        if (allSuccess) {
            println("✅ ValidateDataView passed: All ${nodeResults.size()} node type(s) validated successfully")
        } else {
            println("❌ ValidateDataView failed: ${successCount}/${nodeResults.size()} node type(s) passed")
        }
        
        return [
            success: allSuccess,
            nodeResults: nodeResults,
            mismatches: allMismatches
        ]
    }
    
    private static void switchToTab(String tabName) {
        try {
            // Find tab by text (case-insensitive) - same as ValidateTable
            String xpath = "//*[@role='tab' and contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${tabName.toLowerCase()}')]"
            TestObject tabObj = FindObjectByXPath.findObject(xpath)
            WebUI.click(tabObj)
            WebUI.delay(1)
        } catch (Exception e) {
            // Try button fallback
            String xpath = "//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${tabName.toLowerCase()}')]"
            TestObject tabObj = FindObjectByXPath.findObject(xpath)
            WebUI.click(tabObj)
            WebUI.delay(1)
        }
    }
    
    private static void waitForTableStable(String tableXPath) {
        // Wait for table to be stable (no changes for 500ms)
        String xpath = tableXPath ?: "//table[@data-testid='generic-table']"
        TestObject tableObj = FindObjectByXPath.findObject(xpath)
        WebUI.waitForElementPresent(tableObj, 10)
        WebUI.delay(0.5) // Additional wait for table to stabilize
    }
    
    private static Map<String, Object> readUITable(String tableXPath) {
        def driver = DriverFactory.getWebDriver()
        WebElement table
        
        if (tableXPath) {
            table = driver.findElement(By.xpath(tableXPath))
        } else {
            // Find first visible table
            table = driver.findElement(By.xpath("//table[@data-testid='generic-table']"))
        }
        
        // Read headers
        List<String> headers = []
        def headerElements = table.findElements(By.xpath(".//thead//th | .//thead//td"))
        for (def header : headerElements) {
            String text = header.getText()?.trim()
            if (text) headers.add(text)
        }
        
        // Read rows
        def rows = []
        def rowElements = table.findElements(By.xpath(".//tbody//tr"))
        for (def row : rowElements) {
            def rowData = []
            def cells = row.findElements(By.xpath(".//td"))
            for (def cell : cells) {
                String cellText = cell.getText()?.trim() ?: ''
                
                // Try to get full text if truncated - check multiple sources
                if (cellText.contains('...') || (cellText.length() > 0 && cellText.length() < 15)) {
                    // Try title attribute first (most common for tooltips)
                    String titleAttr = cell.getAttribute('title') ?: ''
                    if (titleAttr && titleAttr.length() > cellText.length() && !titleAttr.contains('...')) {
                        cellText = titleAttr.trim()
                    } else {
                        // Try data-value attribute
                        String dataValue = cell.getAttribute('data-value') ?: ''
                        if (dataValue && dataValue.length() > cellText.length() && !dataValue.contains('...')) {
                            cellText = dataValue.trim()
                        } else {
                            // Try aria-label
                            String ariaLabel = cell.getAttribute('aria-label') ?: ''
                            if (ariaLabel && ariaLabel.length() > cellText.length() && !ariaLabel.contains('...')) {
                                cellText = ariaLabel.trim()
                            } else {
                                // Try innerHTML/textContent via JavaScript
                                try {
                                    // Reuse driver variable declared at method level
                                    // Get textContent which may have full text even if displayed text is truncated
                                    String fullText = driver.executeScript('return arguments[0].textContent || arguments[0].innerText || \'\';', cell)
                                    if (fullText && fullText.length() > cellText.length() && !fullText.contains('...')) {
                                        cellText = fullText.trim()
                                    }
                                } catch (Exception e) {
                                    // Keep original cellText if JavaScript fails
                                }
                            }
                        }
                    }
                }
                
                rowData.add(cellText)
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
    
    private static Map<String, Object> readTSV(File tsvFile) {
        def headers = []
        def rows = []
        
        tsvFile.eachLine { line, lineNum ->
            String[] parts = line.split('\t')
            if (lineNum == 1) {
                headers = parts.collect { it.trim() }
            } else {
                rows.add(parts.collect { it.trim() })
            }
        }
        
        return [
            headers: headers,
            rows: rows
        ]
    }
    
    private static String getSortColumn(String sortByColumn, String nodeType, List<String> tsvHeaders) {
        if (!sortByColumn) {
            // Auto-detect: look for column ending in _id or ID
            for (String header : tsvHeaders) {
                String lower = header.toLowerCase()
                if (lower.endsWith('_id') || lower.endsWith('id') || lower == 'id') {
                    return header
                }
            }
            return null
        }
        
        // Try to parse as JSON object (node type mapping)
        try {
            JsonSlurper jsonSlurper = new JsonSlurper()
            def sortMapping = jsonSlurper.parseText(sortByColumn)
            if (sortMapping instanceof Map) {
                // Use nodeType-specific column if available
                String mapped = sortMapping[nodeType] ?: sortMapping[nodeType.toLowerCase()] ?: sortMapping[nodeType.replace('_', '')]
                if (mapped) {
                    println("📋 Using node-specific sort column for '${nodeType}': ${mapped}")
                    return mapped
                }
            }
        } catch (Exception e) {
            // Not JSON, treat as single column name (use for all node types)
        }
        
        return sortByColumn
    }
    
    private static Map<String, Object> sortTable(Map<String, Object> table, String sortColumn) {
        List<String> headers = table['headers'] as List<String>
        int sortColIdx = findColumnIndex(headers, sortColumn)
        
        if (sortColIdx < 0) {
            println("⚠️  Sort column '${sortColumn}' not found in headers: ${headers.join(', ')}")
            return table
        }
        
        def sortedRows = new ArrayList<>(table['rows'])
        sortedRows.sort { row1, row2 ->
            String val1 = (row1[sortColIdx] ?: '').trim().toLowerCase()
            String val2 = (row2[sortColIdx] ?: '').trim().toLowerCase()
            return val1.compareTo(val2)
        }
        
        return [
            headers: headers,
            rows: sortedRows
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
    
    private static def compareTables(Map<String, Object> uiTable, 
                                     Map<String, Object> tsvData,
                                     String nodeType) {
        def mismatches = []
        def uiHeaders = uiTable['headers']
        def tsvHeaders = tsvData['headers']
        def uiRows = uiTable['rows']
        def tsvRows = tsvData['rows']
        
        // Compare headers (case-insensitive)
        if (uiHeaders.size() != tsvHeaders.size()) {
            mismatches.add([
                row: 0,
                column: 'Header Count',
                expected: String.valueOf(tsvHeaders.size()),
                actual: String.valueOf(uiHeaders.size()),
                matchType: 'exact',
                nodeType: nodeType
            ])
        }
        
        // Compare row count
        if (uiRows.size() != tsvRows.size()) {
            mismatches.add([
                row: 0,
                column: 'Row Count',
                expected: String.valueOf(tsvRows.size()),
                actual: String.valueOf(uiRows.size()),
                matchType: 'exact',
                nodeType: nodeType
            ])
        }
        
        // Compare data rows (only if headers and row counts match)
        if (uiHeaders.size() == tsvHeaders.size() && uiRows.size() == tsvRows.size()) {
            // Map TSV headers to UI table headers (case-insensitive)
            // Special mapping: Automation_status → Status
            List<Integer> headerMap = []
            println("\n🔍 DEBUG: Column Mapping (TSV → UI):")
            for (String tsvHeader : tsvHeaders) {
                // Skip 'type' column - it's metadata in TSV but not displayed in UI table
                if (tsvHeader.toLowerCase() == 'type') {
                    println("   ⏭️  TSV '${tsvHeader}' → SKIPPED (metadata column, not in UI)")
                    headerMap.add(-1)  // Mark as skip
                    continue
                }
                
                // Special mapping: Automation_status → Status
                String mappedHeader = tsvHeader
                if (tsvHeader.toLowerCase() == 'automation_status') {
                    mappedHeader = 'Status'
                }
                
                int uiIndex = findColumnIndex(uiHeaders, mappedHeader)
                headerMap.add(uiIndex)
                
                if (uiIndex >= 0) {
                    println("   ✅ TSV '${tsvHeader}' → UI '${uiHeaders[uiIndex]}' (index ${uiIndex})")
                } else {
                    println("   ❌ TSV '${tsvHeader}' → NOT FOUND in UI headers")
                    println("      Available UI headers: ${uiHeaders.join(', ')}")
                }
            }
            
            // Compare each row
            for (int rowIdx = 0; rowIdx < Math.min(uiRows.size(), tsvRows.size()); rowIdx++) {
                def uiRow = uiRows[rowIdx]
                def tsvRow = tsvRows[rowIdx]
                
                for (int colIdx = 0; colIdx < tsvHeaders.size(); colIdx++) {
                    int uiColIdx = headerMap[colIdx]
                    
                    // Skip 'type' column - it's metadata, not in UI
                    if (tsvHeaders[colIdx].toLowerCase() == 'type') {
                        continue
                    }
                    
                    if (uiColIdx >= 0 && uiColIdx < uiRow.size()) {
                        String expected = (tsvRow[colIdx] ?: '').trim()
                        String actual = (uiRow[uiColIdx] ?: '').trim()
                        
                        // Normalize truncated text for comparison (remove trailing ...)
                        if (actual.endsWith('...')) {
                            // Try to match partial text
                            String actualWithoutEllipsis = actual.replaceAll(/\\.\\.\\.$/, '').trim()
                            if (expected.startsWith(actualWithoutEllipsis)) {
                                // If expected starts with the visible part, consider it a match
                                // (UI truncates but shows beginning)
                                continue
                            }
                        }
                        
                        if (expected != actual) {
                            // DEBUG: Log first few mismatches with details
                            if (mismatches.size() < 3) {
                                println("   🔍 Mismatch Row ${rowIdx + 1}, Column '${tsvHeaders[colIdx]}':")
                                println("      Expected (TSV): '${expected}' (length: ${expected.length()})")
                                println("      Actual (UI):   '${actual}' (length: ${actual.length()})")
                                if (actual.contains('...')) {
                                    println("      ⚠️  UI value appears truncated!")
                                }
                            }
                            
                            mismatches.add([
                                row: rowIdx + 1,
                                column: tsvHeaders[colIdx],
                                expected: expected,
                                actual: actual,
                                matchType: 'exact',
                                nodeType: nodeType
                            ])
                        }
                    } else if (uiColIdx < 0) {
                        // Column not found in UI
                        if (mismatches.size() < 3) {
                            println("   ❌ Column '${tsvHeaders[colIdx]}' not found in UI table")
                        }
                    }
                }
            }
        }
        
        return mismatches
    }
}
