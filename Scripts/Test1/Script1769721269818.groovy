import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import org.apache.poi.ss.usermodel.*
import internal.GlobalVariable as GlobalVariable

/**
 * Simple Submission Test Case
 * Uses Katalon built-in keywords + custom Keywords for reusable flows
 */

// Step 0: Open browser (required before navigation)
WebUI.openBrowser('')

// Step 1: Navigate to the application
WebUI.navigateToUrl(GlobalVariable.baseUrl)
WebUI.delay(2)

// Step 2: Wait for page to load
WebUI.delay(3)

// Step 3-11: Complete login flow
LoginFlow.login()

// Step 12-21: Create submission and navigate to it
CreateSubmission.create()

// Step 22-23: Upload files
Map<String, Object> uploadResult = UploadFiles.uploadFilesFromFolder(
    null,  // Use GlobalVariable.uploadFileFolder
    '//input[@type="file"]',
    '//button[@data-testid="metadata-upload-file-upload-button"]'
)

if (!uploadResult['success']) {
    println("⚠️  File upload failed: ${uploadResult.get('error', 'Unknown error')}")
}

// Step 24: Verify table is present
TestObject tableGeneric = FindObjectByXPath.findObject('//table[@data-testid="generic-table"]')
WebUI.verifyElementPresent(tableGeneric, 10, FailureHandling.OPTIONAL)

// Step 24.5: Validate upload activities table (immediately after upload)
String excelPath = GlobalVariable.excelFilePath ?: 
                   RunConfiguration.getProjectDir() + '/TestExcel/submission-full-flow.xlsx'
Workbook uploadWorkbook = ReadExcel.readExcelFile(excelPath)
Map<String, Object> uploadResultValidation = ValidateTable.validate(null, 'Expected_Upload_Activities', '//table[@data-testid="generic-table"]', uploadWorkbook)
HandleValidationResult.checkAndLog(uploadResultValidation, 'Upload Activities (after upload)')

// Step 25: Click Validate button
TestObject btnValidate = FindObjectByXPath.findObject('//button[@data-testid="validate-controls-validate-button"]')
WebUI.click(btnValidate, FailureHandling.OPTIONAL)
WebUI.delay(3)

// Step 26: Validate Upload Activities table (after Validate button click)
Workbook uploadActivitiesWorkbook = ReadExcel.readExcelFile(excelPath)

// DEBUG: Log table comparison
DebugTableData.logTableComparison('//table[@data-testid="generic-table"]', uploadActivitiesWorkbook, 'Expected_Upload_Activities')

Map<String, Object> uploadActivitiesResult = ValidateTable.validate(null, 'Expected_Upload_Activities', '//table[@data-testid="generic-table"]', uploadActivitiesWorkbook)
HandleValidationResult.checkAndThrow(uploadActivitiesResult, 'Upload Activities')

// Step 27: Validate Data View (all node types automatically)
String dataViewFolderPath = GlobalVariable.uploadFileFolder ?: 'TestFiles/cds/'
String dataViewProjectDir = RunConfiguration.getProjectDir()
String dataViewFullFolderPath = dataViewFolderPath.startsWith('/') ? dataViewFolderPath : "${dataViewProjectDir}/${dataViewFolderPath}"

Map<String, Object> dataViewResult = ValidateDataView.validate(
    dataViewFullFolderPath,                            // Folder with TSV files
    '//div[@id="mui-component-select-nodeType"]',     // Dropdown XPath
    '//table[@data-testid="generic-table"]',          // Table XPath
    null,                                              // Sort column (null = auto-detect)
    3000                                               // Wait time for status updates (ms)
)

HandleValidationResult.checkDataViewResult(dataViewResult, 'Data View')

println("✅ Test completed successfully")

// Close browser
WebUI.closeBrowser()
