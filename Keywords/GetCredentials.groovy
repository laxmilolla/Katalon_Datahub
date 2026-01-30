
import org.apache.poi.ss.usermodel.*
import java.util.Map
import java.util.HashMap

/**
 * Get credentials from Excel Credentials sheet
 * @param workbook - Excel Workbook object
 * @param sheetName - Sheet name (default: "Credentials")
 * @return Map with email and totp_secret
 */
class GetCredentials {
    static Map<String, String> getCredentials(Workbook workbook, String sheetName = "Credentials") {
        Sheet sheet = workbook.getSheet(sheetName)
        if (sheet == null) {
            throw new Exception("Credentials sheet not found: ${sheetName}")
        }
        
        Map<String, String> creds = [:]
        Row headerRow = sheet.getRow(0)
        if (headerRow == null) return creds
        
        // Get column indices
        int emailCol = -1
        int totpCol = -1
        
        headerRow.each { cell ->
            String colName = cell.getStringCellValue()?.trim()?.toLowerCase()
            if (colName == 'email') {
                emailCol = cell.getColumnIndex()
            } else if (colName == 'totp_secret' || colName == 'totp secret') {
                totpCol = cell.getColumnIndex()
            }
        }
        
        // Read first data row
        if (sheet.getLastRowNum() >= 1) {
            Row row = sheet.getRow(1)
            if (row != null) {
                if (emailCol >= 0) {
                    creds['email'] = getCellValue(row, emailCol)
                }
                if (totpCol >= 0) {
                    creds['totp_secret'] = getCellValue(row, totpCol)
                }
            }
        }
        
        return creds
    }
    
    private static String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex)
        if (cell == null) return null
        
        switch (cell.getCellType()) {
            case CellType.STRING:
                return cell.getStringCellValue()?.trim()
            case CellType.NUMERIC:
                return String.valueOf(cell.getNumericCellValue())
            default:
                return null
        }
    }
}
