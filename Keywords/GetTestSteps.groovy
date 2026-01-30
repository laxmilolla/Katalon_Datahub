
import org.apache.poi.ss.usermodel.*
import java.util.List
import java.util.ArrayList

/**
 * Get test steps from Excel main sheet
 * @param workbook - Excel Workbook object
 * @param sheetName - Sheet name (default: first sheet)
 * @return List of Maps with step data
 */
class GetTestSteps {
    static List<Map<String, Object>> getSteps(Workbook workbook, String sheetName = null) {
        Sheet sheet = sheetName ? workbook.getSheet(sheetName) : workbook.getSheetAt(0)
        if (sheet == null) {
            throw new Exception("Sheet not found: ${sheetName ?: 'first sheet'}")
        }
        
        List<Map<String, Object>> steps = new ArrayList<>()
        Row headerRow = sheet.getRow(0)
        if (headerRow == null) return steps
        
        // Get column indices (normalize column names)
        Map<String, Integer> colMap = [:]
        headerRow.each { cell ->
            String colName = cell.getStringCellValue()?.trim()?.toLowerCase()?.replace(' ', '_')
            if (colName) {
                colMap[colName] = cell.getColumnIndex()
            }
        }
        
        // Read data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i)
            if (row == null) continue
            
            Map<String, Object> step = [:]
            step['step'] = getCellValue(row, colMap['step'], i)
            step['url'] = getCellValue(row, colMap['url'], null)
            step['xpath'] = getCellValue(row, colMap['xpath'], null)
            step['object_type'] = getCellValue(row, colMap['object_type'], null)
            step['action'] = getCellValue(row, colMap['action'], 'click')?.toLowerCase()
            step['functions'] = getCellValue(row, colMap['functions'], null)
            step['text_value'] = getCellValue(row, colMap['text_value'], null)
            step['wait_time'] = getCellValueAsNumber(row, colMap['wait_time'], null)
            step['optional'] = getCellValue(row, colMap['optional'], 'false')?.toLowerCase() in ['true', 'yes', '1', 'y']
            step['sort_by_column'] = getCellValue(row, colMap['sort_by_column'], null)
            
            steps.add(step)
        }
        
        return steps
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
    
    private static Double getCellValueAsNumber(Row row, Integer colIndex, Double defaultValue) {
        if (colIndex == null) return defaultValue
        Cell cell = row.getCell(colIndex)
        if (cell == null) return defaultValue
        
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue()
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue()?.trim())
            } catch (Exception e) {
                return defaultValue
            }
        }
        return defaultValue
    }
}
