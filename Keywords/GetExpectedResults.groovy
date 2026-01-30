
import org.apache.poi.ss.usermodel.*
import java.util.List
import java.util.ArrayList
import java.util.Map
import java.util.HashMap

/**
 * Get expected results from Excel Expected_* sheets
 * @param workbook - Excel Workbook object
 * @return Map of sheet name -> List of expected result rows
 */
class GetExpectedResults {
    static Map<String, List<Map<String, Object>>> getExpectedResults(Workbook workbook) {
        Map<String, List<Map<String, Object>>> results = [:]
        
        workbook.each { sheet ->
            String sheetName = sheet.getSheetName()
            if (sheetName.toLowerCase().startsWith('expected')) {
                results[sheetName] = parseExpectedSheet(sheet)
            }
        }
        
        return results
    }
    
    private static List<Map<String, Object>> parseExpectedSheet(Sheet sheet) {
        List<Map<String, Object>> rows = new ArrayList<>()
        Row headerRow = sheet.getRow(0)
        if (headerRow == null) return rows
        
        // Get column indices (normalize names)
        Map<String, Integer> colMap = [:]
        headerRow.each { cell ->
            String colName = cell.getStringCellValue()?.trim()?.toLowerCase()?.replace(' ', '_')
            if (colName) {
                // Map variations to standard names
                if (colName in ['row', 'row_num', 'row_number']) {
                    colMap['row_number'] = cell.getColumnIndex()
                } else if (colName in ['column', 'col', 'column_name']) {
                    colMap['column_name'] = cell.getColumnIndex()
                } else if (colName in ['expected', 'value', 'expected_value']) {
                    colMap['expected_value'] = cell.getColumnIndex()
                } else if (colName in ['match', 'type', 'match_type']) {
                    colMap['match_type'] = cell.getColumnIndex()
                } else if (colName in ['action', 'error_action', 'action_on_error']) {
                    colMap['action_on_error'] = cell.getColumnIndex()
                }
            }
        }
        
        // Read data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i)
            if (row == null) continue
            
            Map<String, Object> result = [:]
            result['row_number'] = getCellValueAsInt(row, colMap['row_number'], null)
            result['column_name'] = getCellValue(row, colMap['column_name'], null)
            result['expected_value'] = getCellValue(row, colMap['expected_value'], null)
            result['match_type'] = getCellValue(row, colMap['match_type'], 'exact')?.toLowerCase()
            result['action_on_error'] = getCellValue(row, colMap['action_on_error'], 'fail')?.toLowerCase()
            
            rows.add(result)
        }
        
        return rows
    }
    
    private static String getCellValue(Row row, Integer colIndex, String defaultValue) {
        if (colIndex == null) return defaultValue
        Cell cell = row.getCell(colIndex)
        if (cell == null) return defaultValue
        
        switch (cell.getCellType()) {
            case CellType.STRING:
                return cell.getStringCellValue()?.trim() ?: defaultValue
            case CellType.NUMERIC:
                return String.valueOf(cell.getNumericCellValue())
            default:
                return defaultValue
        }
    }
    
    private static Integer getCellValueAsInt(Row row, Integer colIndex, Integer defaultValue) {
        if (colIndex == null) return defaultValue
        Cell cell = row.getCell(colIndex)
        if (cell == null) return defaultValue
        
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int)cell.getNumericCellValue()
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue()?.trim())
            } catch (Exception e) {
                return defaultValue
            }
        }
        return defaultValue
    }
}
