import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.File

/**
 * Read Excel file and return Workbook object
 * @param excelPath - Path to Excel file
 * @return Workbook object
 */
class ReadExcel {
    static Workbook readExcelFile(String excelPath) {
        FileInputStream fis = new FileInputStream(new File(excelPath))
        return new XSSFWorkbook(fis)
    }
    
    static void closeWorkbook(Workbook workbook) {
        if (workbook != null) {
            workbook.close()
        }
    }
}
