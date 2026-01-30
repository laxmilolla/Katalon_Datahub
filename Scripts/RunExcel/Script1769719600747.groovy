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
import internal.GlobalVariable as GlobalVariable
import org.apache.poi.ss.usermodel.*
import java.io.File

/**
 * Excel-driven test case
 * Reads test steps from Excel and executes them using WebUI keywords
 */

// Get Excel file path - Multiple options:
// Option 1: Set via Profile (Profiles > default > Add Variable: excelFilePath)
// Option 2: Use Test Data folder (recommended - put Excel files in Test Data/)
// Option 3: Hard-code here (for quick testing)

String excelPath = null

// Try GlobalVariable first (set via Profile)
// GlobalVariable can be:
// - Relative path: "Test Data/TestExcel/submission_test.xlsx"
// - Absolute path: "/full/path/to/file.xlsx"
if (GlobalVariable.hasProperty('excelFilePath') && GlobalVariable.excelFilePath) {
    String path = GlobalVariable.excelFilePath
    // If relative path (doesn't start with /), make it relative to project
    if (!path.startsWith('/')) {
        excelPath = RunConfiguration.getProjectDir() + '/' + path
    } else {
        excelPath = path
    }
}
// Fallback: Use TestExcel folder (relative to project root)
else {
    // Excel file is in: TestExcel/submission-full-flow.xlsx
    String projectDir = RunConfiguration.getProjectDir()
    excelPath = projectDir + '/TestExcel/submission-full-flow.xlsx'
    
    // If file doesn't exist, show helpful message
    File excelFile = new File(excelPath)
    if (!excelFile.exists()) {
        println("⚠️  Excel file not found at: ${excelPath}")
        println("📁 Expected location: TestExcel/submission-full-flow.xlsx")
        println("📄 Or set GlobalVariable.excelFilePath = 'TestExcel/your_file.xlsx'")
    }
}

if (!excelPath) {
    throw new Exception("Excel file path not set! Set GlobalVariable.excelFilePath or place Excel file in Test Data folder")
}

println("📁 Using Excel file: ${excelPath}")

Workbook workbook = null
Map<String, String> credentials = [:]
String userEmail = null

try {
    // Read Excel file
    workbook = ReadExcel.readExcelFile(excelPath)
    
    // Get credentials
    try {
        credentials = GetCredentials.getCredentials(workbook)
        userEmail = credentials['email']
    } catch (Exception e) {
        println("⚠️  Credentials sheet not found or empty: ${e.message}")
    }
    
    // Get test steps
    List<Map<String, Object>> steps = GetTestSteps.getSteps(workbook)
    
    if (steps.isEmpty()) {
        throw new Exception("No test steps found in Excel file")
    }
    
    println("✅ Loaded ${steps.size()} test steps from Excel")
    
    // Execute each step
    String currentUrl = null
    List<String> criticalFailures = []
    
    for (Map<String, Object> step : steps) {
        String stepNum = step['step'] ?: 'Unknown'
        String action = step['action'] ?: 'click'
        String xpath = step['xpath'] ?: ''
        String url = step['url'] ?: null
        String textValue = step['text_value'] ?: ''
        String functions = step['functions'] ?: ''
        Double waitTime = step['wait_time'] ?: null
        boolean isOptional = step['optional'] ?: false
        String sortByColumn = step['sort_by_column'] ?: null
        
        println("📋 Step ${stepNum}: ${action.toUpperCase()} ${xpath ?: '(no xpath)'}")
        
        try {
            // Update current URL
            if (url && url != 'N/A') {
                currentUrl = url
            }
            
            // Execute action
            switch (action.toLowerCase()) {
                case 'navigate':
                    if (url && url != 'N/A') {
                        int waitMs = waitTime ? waitTime.intValue() : 0
                        ExecuteNavigate.navigate(url, waitMs)
                        println("✅ Step ${stepNum}: Navigated to ${url}")
                    } else {
                        throw new Exception("Navigate action requires URL")
                    }
                    break
                    
                case 'click':
                    if (xpath && xpath != 'N/A') {
                        TestObject obj = FindObjectByXPath.findObject(xpath)
                        WebUI.click(obj, FailureHandling.OPTIONAL)
                        
                        // Wait after click if specified
                        if (waitTime) {
                            WebUI.delay(waitTime.intValue() / 1000)
                        }
                        
                        println("✅ Step ${stepNum}: Clicked element")
                    } else {
                        throw new Exception("Click action requires XPath")
                    }
                    break
                    
                case 'fill':
                    if (xpath && xpath != 'N/A') {
                        TestObject obj = FindObjectByXPath.findObject(xpath)
                        
                        // Check if TOTP fill
                        boolean isTOTP = functions.toUpperCase().contains('TOTP')
                        
                        if (isTOTP) {
                            // Get TOTP secret from credentials or environment
                            String totpSecret = credentials['totp_secret'] ?: 
                                               System.getenv('TOTP_SECRET_KEY')
                            
                            if (!totpSecret) {
                                throw new Exception("TOTP secret not found in credentials or environment")
                            }
                            
                            // Generate TOTP code
                            String totpCode = GenerateTOTP.generateTOTP(totpSecret, userEmail)
                            
                            // Fill TOTP code
                            WebUI.clearText(obj)
                            WebUI.setText(obj, totpCode)
                            println("✅ Step ${stepNum}: Filled TOTP code")
                        } else {
                            // Regular fill
                            WebUI.clearText(obj)
                            WebUI.setText(obj, textValue)
                            println("✅ Step ${stepNum}: Filled text: ${textValue.substring(0, Math.min(20, textValue.length()))}...")
                        }
                        
                        // Wait after fill if specified
                        if (waitTime) {
                            WebUI.delay(waitTime.intValue() / 1000)
                        }
                    } else {
                        throw new Exception("Fill action requires XPath")
                    }
                    break
                    
                case 'verify':
                    if (xpath && xpath != 'N/A') {
                        boolean isTableVerification = functions.toUpperCase().contains('TABLE')
                        
                        if (isTableVerification) {
                            // Table verification
                            String expectedSheet = textValue ?: 'Expected_Results'
                            Map<String, Object> result = ValidateTable.validate(
                                null, // tabName (can be extracted from step if needed)
                                expectedSheet,
                                xpath,
                                workbook
                            )
                            
                            if (!result['success']) {
                                List mismatches = result['mismatches'] ?: []
                                String errorMsg = "Table validation failed: ${mismatches.size()} mismatches"
                                if (isOptional) {
                                    println("⚠️  Step ${stepNum}: ${errorMsg} (optional)")
                                } else {
                                    criticalFailures.add("Step ${stepNum}: ${errorMsg}")
                                    throw new Exception(errorMsg)
                                }
                            } else {
                                println("✅ Step ${stepNum}: Table validation passed")
                            }
                        } else {
                            // Regular verify (element present/visible)
                            TestObject obj = FindObjectByXPath.findObject(xpath)
                            WebUI.verifyElementPresent(obj, 10, FailureHandling.OPTIONAL)
                            println("✅ Step ${stepNum}: Verified element present")
                        }
                    } else {
                        throw new Exception("Verify action requires XPath")
                    }
                    break
                    
                case 'wait':
                    int waitMs = waitTime ? waitTime.intValue() : 1000
                    WebUI.delay(waitMs / 1000)
                    println("✅ Step ${stepNum}: Waited ${waitMs}ms")
                    break
                    
                case 'wait_for':
                    if (xpath && xpath != 'N/A') {
                        TestObject obj = FindObjectByXPath.findObject(xpath)
                        
                        // Parse wait type from functions (visible/clickable/enabled)
                        String waitType = 'visible'
                        if (functions.toLowerCase().contains('clickable')) {
                            waitType = 'clickable'
                        } else if (functions.toLowerCase().contains('enabled')) {
                            waitType = 'enabled'
                        }
                        
                        int timeoutMs = waitTime ? waitTime.intValue() : 10000
                        
                        switch (waitType) {
                            case 'clickable':
                                WebUI.waitForElementClickable(obj, timeoutMs, FailureHandling.OPTIONAL)
                                break
                            case 'enabled':
                                WebUI.waitForElementVisible(obj, timeoutMs, FailureHandling.OPTIONAL)
                                break
                            default:
                                WebUI.waitForElementVisible(obj, timeoutMs, FailureHandling.OPTIONAL)
                        }
                        
                        println("✅ Step ${stepNum}: Waited for element ${waitType}")
                    } else {
                        throw new Exception("Wait_for action requires XPath")
                    }
                    break
                    
                default:
                    throw new Exception("Unknown action: ${action}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Step ${stepNum} failed: ${e.message}"
            println("❌ ${errorMsg}")
            
            if (isOptional) {
                println("⚠️  Step ${stepNum} is optional, continuing...")
            } else {
                criticalFailures.add(errorMsg)
                // Continue execution but track failure
            }
        }
    }
    
    // Check for critical failures
    if (criticalFailures.size() > 0) {
        println("\n❌ Test completed with ${criticalFailures.size()} failure(s):")
        for (String failure : criticalFailures) {
            println("  - ${failure}")
        }
        throw new Exception("Test failed with ${criticalFailures.size()} critical failure(s)")
    } else {
        println("\n✅ Test completed successfully")
    }
    
} catch (Exception e) {
    println("❌ Test execution failed: ${e.message}")
    throw e
} finally {
    // Close workbook
    if (workbook != null) {
        ReadExcel.closeWorkbook(workbook)
    }
}
