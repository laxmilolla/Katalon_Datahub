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
import internal.GlobalVariable as GlobalVariable

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
        
        // Step 3: Extract node types from filenames
        // Pattern: Extract node type between underscores: something_nodetype_something
        // After first underscore, node type ends with next underscore
        // Examples: "GC_sample_v11.0.3" -> "sample", "GC_Data_Loading_Template_consent_group_v9.0.0" -> "consent_group"
        List<String> nodeTypes = []
        for (File tsvFile : tsvFiles) {
            String basename = tsvFile.name.replace('.tsv', '').toLowerCase()
            
            // Pattern: Extract node type between underscores
            // Find first underscore, then extract until next underscore (or end if no more underscores)
            // For versioned files: extract part before version pattern
            def versionMatch = basename =~ /^(.+?)_v\d+\.\d+\.\d+$/
            if (versionMatch) {
                // Has version pattern: extract everything after last _ before version
                // e.g., "gc_data_loading_template_consent_group_v9.0.0" -> "consent_group"
                String beforeVersion = versionMatch[0][1]
                int lastUnderscore = beforeVersion.lastIndexOf('_')
                if (lastUnderscore >= 0) {
                    String nodeType = beforeVersion.substring(lastUnderscore + 1)
                    nodeTypes.add(nodeType)
                } else {
                    nodeTypes.add(beforeVersion)
                }
            } else {
                // No version pattern: extract between first _ and next _
                // Pattern: anything_nodetype_anything
                // After first _, extract until next _
                def match = basename =~ /^[^_]+_([^_]+)_/
                if (match) {
                    // Found pattern: something_nodetype_something
                    nodeTypes.add(match[0][1])
                } else {
                    // Try: something_nodetype (no trailing underscore)
                    def matchNoTrailing = basename =~ /^[^_]+_([^_]+)$/
                    if (matchNoTrailing) {
                        nodeTypes.add(matchNoTrailing[0][1])
                    } else {
                        // Fallback: extract after first underscore until last underscore
                        int firstUnderscore = basename.indexOf('_')
                        int lastUnderscore = basename.lastIndexOf('_')
                        if (firstUnderscore >= 0 && lastUnderscore > firstUnderscore) {
                            String nodeType = basename.substring(firstUnderscore + 1, lastUnderscore)
                            nodeTypes.add(nodeType)
                        } else if (firstUnderscore >= 0) {
                            // Only one underscore: extract everything after it
                            nodeTypes.add(basename.substring(firstUnderscore + 1))
                        } else {
                            // No underscores: use whole filename
                            nodeTypes.add(basename)
                        }
                    }
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
                
                // Read table data based on validation method
                Map<String, Object> uiTable
                String validationMethod = GlobalVariable.validationMethod ?: 'ui'
                
                if (validationMethod == 'download') {
                    // Download and read from file
                    println("📥 Using download method for validation")
                    String downloadButtonXPath = '//button[@data-testid="export-node-data-button"]'
                    Map<String, Object> downloadResult = DownloadTableData.downloadAndRead(downloadButtonXPath, 30)
                    
                    if (!downloadResult['success']) {
                        throw new Exception("Download failed: ${downloadResult['error']}")
                    }
                    
                    // Read downloaded TSV file
                    String downloadedFilePath = downloadResult['filePath']
                    File downloadedFile = new File(downloadedFilePath)
                    uiTable = DownloadTableData.readTSVFile(downloadedFilePath)
                    println("✅ Read downloaded file: ${downloadedFile.name} - ${uiTable['headers'].size()} columns, ${uiTable['rows'].size()} rows")
                } else {
                    // Read from UI table (default)
                    println("🖥️  Using UI table method for validation")
                    uiTable = readUITable(tableXPath)
                    println("✅ Read UI table: ${uiTable['headers'].size()} columns, ${uiTable['rows'].size()} rows")
                }
                
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
                
                if (sortColumnForNode) {
                    println("🔍 Using sort column '${sortColumnForNode}' - sorting both files before comparison")
                    
                    // Sort both files by the same column to ensure same order
                    tsvData = sortTable(tsvData, sortColumnForNode)
                    uiTable = sortTable(uiTable, sortColumnForNode)
                    
                    println("✅ Both files sorted by '${sortColumnForNode}' - using position-based comparison")
                } else {
                    println("⚠️  No sort column detected - will use position-based row matching (files not sorted)")
                }
                
                // Compare data - returns both mismatches and all results (pass/fail)
                // Files are sorted by same column, so position-based comparison works correctly
                Map<String, Object> comparisonResult = compareTables(uiTable, tsvData, nodeType, validationMethod, sortColumnForNode)
                List<Map<String, Object>> mismatches = comparisonResult['mismatches'] ?: []
                List<Map<String, Object>> allResults = comparisonResult['allResults'] ?: []
                
                if (mismatches.size() > 0) {
                    println("❌ Node type '${nodeType}' validation failed: ${mismatches.size()} mismatch(es) out of ${allResults.size()} comparisons")
                    nodeResults.add([
                        nodeType: nodeType,
                        success: false,
                        mismatches: mismatches,
                        allResults: allResults  // Include all results (pass + fail)
                    ])
                    allMismatches.addAll(mismatches)
                } else {
                    println("✅ Node type '${nodeType}' validation passed: ${allResults.size()} comparison(s)")
                    nodeResults.add([
                        nodeType: nodeType,
                        success: true,
                        allResults: allResults  // Include all results (all pass)
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
                                     String nodeType,
                                     String validationMethod = 'ui',
                                     String sortColumn = null) {
        def mismatches = []
        def allResults = []  // Track all comparisons (both pass and fail)
        def uiHeaders = uiTable['headers']
        def tsvHeaders = tsvData['headers']
        def uiRows = uiTable['rows']
        def tsvRows = tsvData['rows']
        
        // Auto-detect Expected_* columns in TSV (e.g., Expected_status, Expected_Severity)
        // These will be mapped to corresponding columns in downloaded files for comparison
        Map<String, Integer> expectedColumnMap = [:]  // TSV Expected_* column → TSV column index
        Map<String, String> expectedToDownloadedMap = [:]  // TSV Expected_* column → Downloaded column name
        
        for (int i = 0; i < tsvHeaders.size(); i++) {
            String tsvHeader = tsvHeaders[i]
            String tsvHeaderLower = tsvHeader.toLowerCase().trim()
            
            // Check if column starts with "Expected_" (case-insensitive)
            if (tsvHeaderLower.startsWith('expected_')) {
                // Extract the suffix (e.g., "Expected_status" → "status", "Expected_Severity" → "Severity")
                // "Expected_" is 9 characters, so substring(9) removes it
                String expectedSuffix = tsvHeader.substring(9)  // Remove "Expected_" prefix (9 chars)
                expectedColumnMap[tsvHeader] = i
                
                // Find corresponding column in downloaded file (case-insensitive)
                // IMPORTANT: Exclude Expected_* columns from search - we want the actual column, not Expected_* version
                int downloadedColIndex = -1
                String expectedSuffixLower = expectedSuffix.toLowerCase()
                
                // First try exact match (excluding Expected_* columns)
                for (int j = 0; j < uiHeaders.size(); j++) {
                    String uiHeaderLower = uiHeaders[j].toLowerCase().trim()
                    // Skip Expected_* columns - we want the actual column
                    if (uiHeaderLower.startsWith('expected_')) {
                        continue
                    }
                    if (uiHeaderLower == expectedSuffixLower) {
                        downloadedColIndex = j
                        break
                    }
                }
                
                // If exact match not found, try partial match (still excluding Expected_* columns)
                if (downloadedColIndex < 0) {
                    for (int j = 0; j < uiHeaders.size(); j++) {
                        String uiHeaderLower = uiHeaders[j].toLowerCase().trim()
                        // Skip Expected_* columns
                        if (uiHeaderLower.startsWith('expected_')) {
                            continue
                        }
                        if (uiHeaderLower.contains(expectedSuffixLower)) {
                            downloadedColIndex = j
                            break
                        }
                    }
                }
                
                if (downloadedColIndex >= 0) {
                    expectedToDownloadedMap[tsvHeader] = uiHeaders[downloadedColIndex]
                    println("🔍 Auto-detected mapping: TSV '${tsvHeader}' → Downloaded '${uiHeaders[downloadedColIndex]}'")
                } else {
                    println("⚠️  Found TSV column '${tsvHeader}' but no matching column '${expectedSuffix}' in downloaded file (excluding Expected_* columns)")
                    println("      Available downloaded headers: ${uiHeaders.join(', ')}")
                }
            }
        }
        
        if (expectedColumnMap.size() > 0) {
            println("✅ Auto-detected ${expectedColumnMap.size()} Expected_* column(s) for comparison")
        }
        
        // For downloaded files, we keep the status/Severity columns (they will be compared via Expected_* mapping)
        // But we need to exclude Expected_* columns from regular header count comparison
        // since they are compared separately
        
        // Compare headers (case-insensitive)
        // Exclude 'type' and Expected_* columns from count (they are handled separately)
        int tsvRegularColumns = tsvHeaders.count { 
            String h = it.toLowerCase().trim()
            return h != 'type' && !h.startsWith('expected_')
        }
        
        // For downloaded files, exclude Expected_* columns and 'type' from count (same as TSV)
        int uiRegularColumns = uiHeaders.count {
            String h = it.toLowerCase().trim()
            return h != 'type' && !h.startsWith('expected_')
        }
        
        // For downloaded files, account for Expected_* columns that map to downloaded columns
        // If Expected_status maps to status, both count as one column for header count
        if (validationMethod == 'download' && expectedColumnMap.size() > 0) {
            // Downloaded file should have: regular columns (excluding Expected_*) + mapped Expected_* columns
            // But Expected_* columns in TSV don't count as separate columns
            // So: downloaded_count (excluding Expected_*) should equal tsv_regular_count + mapped_expected_count
            int mappedExpectedCount = expectedToDownloadedMap.size()
            int expectedDownloadedCount = tsvRegularColumns + mappedExpectedCount
            
            if (uiRegularColumns != expectedDownloadedCount) {
                mismatches.add([
                    row: 0,
                    column: 'Header Count',
                    expected: String.valueOf(expectedDownloadedCount) + " (regular: ${tsvRegularColumns} + mapped Expected_*: ${mappedExpectedCount})",
                    actual: String.valueOf(uiRegularColumns) + " (excluding Expected_* columns in downloaded file)",
                    matchType: 'exact',
                    nodeType: nodeType
                ])
            }
        } else {
            // Normal comparison (no Expected_* columns or UI method)
            if (uiRegularColumns != tsvRegularColumns) {
                mismatches.add([
                    row: 0,
                    column: 'Header Count',
                    expected: String.valueOf(tsvRegularColumns),
                    actual: String.valueOf(uiRegularColumns),
                    matchType: 'exact',
                    nodeType: nodeType
                ])
            }
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
        
        // Compare data rows
        // Even if header counts don't match, we still want to compare Expected_* columns row-by-row
        int expectedRowCount = tsvRows.size()
        boolean headerCountsOk = false
        
        if (validationMethod == 'download' && expectedColumnMap.size() > 0) {
            // For downloaded files with Expected_* columns: downloaded should have regular + mapped columns
            headerCountsOk = (uiRegularColumns == (tsvRegularColumns + expectedToDownloadedMap.size()))
        } else if (validationMethod == 'ui') {
            // For UI method: regular column counts should match
            headerCountsOk = (uiRegularColumns == tsvRegularColumns)
        } else {
            // For downloaded without Expected_*: regular counts should match
            headerCountsOk = (uiRegularColumns == tsvRegularColumns)
        }
        
        // Always compare Expected_* columns row-by-row, even if header counts don't match
        // For regular columns, only compare if header counts match (to avoid false positives)
        if (uiRows.size() == expectedRowCount) {
            
            // Map TSV headers to UI/downloaded table headers (case-insensitive)
            // Skip 'type' and Expected_* columns (handled separately)
            List<Integer> headerMap = []
            println("\n🔍 DEBUG: Column Mapping (TSV → UI/Downloaded):")
            for (String tsvHeader : tsvHeaders) {
                String tsvHeaderLower = tsvHeader.toLowerCase().trim()
                
                // Skip 'type' column - it's metadata in TSV but not displayed in UI table
                if (tsvHeaderLower == 'type') {
                    println("   ⏭️  TSV '${tsvHeader}' → SKIPPED (metadata column, not in UI)")
                    headerMap.add(-1)  // Mark as skip
                    continue
                }
                
                // Skip Expected_* columns - they are compared separately via mapping
                if (tsvHeaderLower.startsWith('expected_')) {
                    println("   ⏭️  TSV '${tsvHeader}' → SKIPPED (Expected_* column, compared separately)")
                    headerMap.add(-2)  // Mark as Expected_* column (different handling)
                    continue
                }
                
                // Special mapping: Automation_status → Status
                String mappedHeader = tsvHeader
                if (tsvHeaderLower == 'automation_status') {
                    mappedHeader = 'Status'
                }
                
                int uiIndex = findColumnIndex(uiHeaders, mappedHeader)
                headerMap.add(uiIndex)
                
                if (uiIndex >= 0) {
                    println("   ✅ TSV '${tsvHeader}' → UI/Downloaded '${uiHeaders[uiIndex]}' (index ${uiIndex})")
                } else {
                    println("   ❌ TSV '${tsvHeader}' → NOT FOUND in UI/Downloaded headers")
                    println("      Available headers: ${uiHeaders.join(', ')}")
                }
            }
            
            // Since both files are sorted by the same column, use position-based comparison
            // Row 1 vs Row 1, Row 2 vs Row 2, etc.
            int maxRows = Math.min(tsvRows.size(), uiRows.size())
            
            for (int rowIdx = 0; rowIdx < maxRows; rowIdx++) {
                def tsvRow = tsvRows[rowIdx]
                def uiRow = uiRows[rowIdx]
                
                if (sortColumn) {
                    // Verify rows match by sort column (sanity check after sorting)
                    int tsvSortColIdx = findColumnIndex(tsvHeaders, sortColumn)
                    int uiSortColIdx = findColumnIndex(uiHeaders, sortColumn)
                    if (tsvSortColIdx >= 0 && uiSortColIdx >= 0) {
                        String tsvSortValue = (tsvRow[tsvSortColIdx] ?: '').trim().toLowerCase()
                        String uiSortValue = (uiRow[uiSortColIdx] ?: '').trim().toLowerCase()
                        if (tsvSortValue != uiSortValue) {
                            println("   ⚠️  Row ${rowIdx + 1} sort column mismatch: TSV='${tsvRow[tsvSortColIdx]}' vs Downloaded='${uiRow[uiSortColIdx]}' (files may not be sorted correctly)")
                        }
                    }
                }
                
                // First: Compare regular columns (non-Expected_* columns) - only if header counts match
                if (headerCountsOk) {
                    for (int colIdx = 0; colIdx < tsvHeaders.size(); colIdx++) {
                        int uiColIdx = headerMap[colIdx]
                        String tsvHeaderLower = tsvHeaders[colIdx].toLowerCase().trim()
                        
                        // Skip 'type' column - it's metadata, not in UI
                        if (tsvHeaderLower == 'type') {
                            continue
                        }
                        
                        // Skip Expected_* columns - handled separately below
                        if (tsvHeaderLower.startsWith('expected_')) {
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
                            
                            // Track result (both pass and fail)
                            boolean isMatch = (expected == actual)
                            Map<String, Object> result = [
                                row: rowIdx + 1,
                                column: tsvHeaders[colIdx],
                                expected: expected,
                                actual: actual,
                                matchType: 'exact',
                                nodeType: nodeType,
                                result: isMatch ? 'PASS' : 'FAIL'
                            ]
                            allResults.add(result)
                            
                            if (!isMatch) {
                                // DEBUG: Log first few mismatches with details
                                if (mismatches.size() < 3) {
                                    println("   🔍 Mismatch Row ${rowIdx + 1}, Column '${tsvHeaders[colIdx]}':")
                                    println("      Expected (TSV): '${expected}' (length: ${expected.length()})")
                                    println("      Actual (UI/Downloaded):   '${actual}' (length: ${actual.length()})")
                                    if (actual.contains('...')) {
                                        println("      ⚠️  UI value appears truncated!")
                                    }
                                }
                                
                                mismatches.add(result)
                            }
                        } else if (uiColIdx < 0) {
                            // Column not found in UI
                            if (mismatches.size() < 3) {
                                println("   ❌ Column '${tsvHeaders[colIdx]}' not found in UI/Downloaded table for Row ${rowIdx + 1}")
                            }
                        }
                    }
                } else {
                    println("   ⚠️  Skipping regular column comparison for Row ${rowIdx + 1} - header counts don't match")
                }
                
                // Second: Compare Expected_* columns with their mapped downloaded columns
                // Always do this, even if header counts don't match
                expectedColumnMap.each { expectedTsvCol, expectedTsvColIdx ->
                    String downloadedColName = expectedToDownloadedMap[expectedTsvCol]
                    if (downloadedColName) {
                        // Find downloaded column index
                        int downloadedColIdx = findColumnIndex(uiHeaders, downloadedColName)
                        
                        if (downloadedColIdx >= 0 && downloadedColIdx < uiRow.size()) {
                            String expectedValue = (tsvRow[expectedTsvColIdx] ?: '').trim()
                            String actualValue = (uiRow[downloadedColIdx] ?: '').trim()
                            
                            // Track result (both pass and fail)
                            boolean isMatch = (expectedValue == actualValue)
                            Map<String, Object> result = [
                                row: rowIdx + 1,
                                column: expectedTsvCol,
                                expected: expectedValue,
                                actual: actualValue,
                                matchType: 'exact',
                                nodeType: nodeType,
                                note: "Mapped from downloaded column '${downloadedColName}'",
                                result: isMatch ? 'PASS' : 'FAIL'
                            ]
                            allResults.add(result)
                            
                            if (!isMatch) {
                                println("   🔍 Expected_* Mismatch Row ${rowIdx + 1}:")
                                println("      TSV '${expectedTsvCol}' (expected): '${expectedValue}'")
                                println("      Downloaded '${downloadedColName}' (actual): '${actualValue}'")
                                mismatches.add(result)
                            } else {
                                // Log successful match for first few rows
                                if (rowIdx < 3) {
                                    println("   ✅ Expected_* Match Row ${rowIdx + 1}: TSV '${expectedTsvCol}'='${expectedValue}' == Downloaded '${downloadedColName}'='${actualValue}'")
                                }
                            }
                        } else {
                            println("   ⚠️  Expected_* column '${expectedTsvCol}' mapped to '${downloadedColName}' but column not found in downloaded file")
                        }
                    }
                }
            }
            
            // Handle rows that exist in one file but not the other
            if (tsvRows.size() > uiRows.size()) {
                println("   ⚠️  TSV has ${tsvRows.size()} rows but downloaded file has only ${uiRows.size()} rows")
                for (int extraRowIdx = uiRows.size(); extraRowIdx < tsvRows.size(); extraRowIdx++) {
                    Map<String, Object> result = [
                        row: extraRowIdx + 1,
                        column: 'Row Count',
                        expected: 'Row exists in TSV',
                        actual: 'Row missing in downloaded file',
                        matchType: 'exact',
                        nodeType: nodeType,
                        result: 'FAIL'
                    ]
                    mismatches.add(result)
                    allResults.add(result)
                }
            } else if (uiRows.size() > tsvRows.size()) {
                println("   ⚠️  Downloaded file has ${uiRows.size()} rows but TSV has only ${tsvRows.size()} rows")
                for (int extraRowIdx = tsvRows.size(); extraRowIdx < uiRows.size(); extraRowIdx++) {
                    Map<String, Object> result = [
                        row: extraRowIdx + 1,
                        column: 'Row Count',
                        expected: 'Row exists in TSV',
                        actual: 'Extra row in downloaded file',
                        matchType: 'exact',
                        nodeType: nodeType,
                        result: 'FAIL'
                    ]
                    mismatches.add(result)
                    allResults.add(result)
                }
            }
        }
        
        return [
            mismatches: mismatches,
            allResults: allResults
        ]
    }
}
