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
import com.kms.katalon.core.webui.driver.DriverFactory
import org.apache.poi.ss.usermodel.*
import internal.GlobalVariable as GlobalVariable
import java.io.File
import java.util.HashMap
import java.util.Map

/**
 * Simple Submission Test Case
 * Uses Katalon built-in keywords + custom Keywords for reusable flows
 */

// ============================================================================
// VALIDATION METHOD CONFIGURATION
// ============================================================================
// Set to 'ui' to read from UI table, or 'download' to download and read from file
// Note: Upload Activities tab has no download button, so it will always use UI
String validationMethod = 'download'  // Change to 'ui' to use UI table reading, 'download' for file download
GlobalVariable.validationMethod = validationMethod
println("🔧 Validation method set to: ${validationMethod}")

// ============================================================================
// BROWSER SETUP
// ============================================================================
// Step 0: Configure Chrome download preferences BEFORE opening browser
String projectDir = RunConfiguration.getProjectDir()
String downloadFolderPath = GlobalVariable.downloadFolder ?: 'Downloads/'
File downloadFolder = downloadFolderPath.startsWith('/') ? new File(downloadFolderPath) : new File(projectDir, downloadFolderPath)

// Create download folder if it doesn't exist
if (!downloadFolder.exists()) {
    downloadFolder.mkdirs()
    println("📁 Created download folder: ${downloadFolder.absolutePath}")
}

// Get absolute path with forward slashes (required for Chrome preferences)
String downloadPath = downloadFolder.absolutePath.replace('\\', '/')
println("📥 Configuring Chrome to download to: ${downloadPath}")

// Configure Chrome preferences as Map (W3C compliant format)
Map<String, Object> chromePrefs = new HashMap<String, Object>()
chromePrefs.put("download.default_directory", downloadPath)
chromePrefs.put("download.prompt_for_download", false)
chromePrefs.put("download.directory_upgrade", true)
chromePrefs.put("safebrowsing.enabled", false)

// Set preferences using Katalon's WebDriver preferences API
RunConfiguration.setWebDriverPreferencesProperty("goog:chromeOptions", [
    "prefs": chromePrefs
])
println("✅ Chrome download preferences configured")

// Now open browser with configured preferences
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
// Note: Upload Activities tab has no download button, so it will always use UI method
Workbook uploadActivitiesWorkbook = ReadExcel.readExcelFile(excelPath)

// DEBUG: Log table comparison
DebugTableData.logTableComparison('//table[@data-testid="generic-table"]', uploadActivitiesWorkbook, 'Expected_Upload_Activities')

Map<String, Object> uploadActivitiesResult = ValidateTable.validate(
    null,                                              // Tab name (null = current tab)
    'Expected_Upload_Activities',                       // Expected results sheet
    '//table[@data-testid="generic-table"]',          // Table XPath
    uploadActivitiesWorkbook,                          // Excel workbook
    null                                               // Download button XPath (null = no download, uses UI)
)
HandleValidationResult.checkAndThrow(uploadActivitiesResult, 'Upload Activities')

// Step 27: Validate Data View (all node types automatically)
// Uses download button: //button[@data-testid="export-node-data-button"]
String dataViewFolderPath = GlobalVariable.uploadFileFolder ?: 'TestFiles/cds/'
String dataViewProjectDir = RunConfiguration.getProjectDir()
String dataViewFullFolderPath = dataViewFolderPath.startsWith('/') ? dataViewFolderPath : "${dataViewProjectDir}/${dataViewFolderPath}"

println("📊 Starting Data View validation using method: ${validationMethod}")
if (validationMethod == 'download') {
    println("   Download button XPath: //button[@data-testid=\"export-node-data-button\"]")
    println("   Downloads will be saved to: ${downloadFolder.absolutePath}")
}

Map<String, Object> dataViewResult = ValidateDataView.validate(
    dataViewFullFolderPath,                            // Folder with TSV files
    '//div[@id="mui-component-select-nodeType"]',     // Dropdown XPath
    '//table[@data-testid="generic-table"]',          // Table XPath
    '{"participant":"study_participant_id","consent_group":"consent_group_id","pdx":"pdx_id","study":"study_id","program":"program_acronym","sample":"sample_id"}',  // Sort column JSON mapping
    3000                                               // Wait time for status updates (ms)
)

HandleValidationResult.checkDataViewResult(dataViewResult, 'Data View')

println("✅ Test completed successfully")

// Close browser
WebUI.closeBrowser()
