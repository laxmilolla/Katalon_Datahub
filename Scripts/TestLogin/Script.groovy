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
import internal.GlobalVariable as GlobalVariable
import Keywords.FindObjectByXPath

/**
 * Simple test script to navigate to DataHub and click Login button
 */

// Step 1: Open browser
WebUI.openBrowser('')
println("✅ Browser opened")

// Step 2: Navigate to DataHub
String url = GlobalVariable.baseUrl ?: 'https://hub-stage.datacommons.cancer.gov/'
WebUI.navigateToUrl(url)
println("✅ Navigated to: ${url}")

// Step 3: Wait for page to load
WebUI.delay(3)
println("⏱️  Waited for page to load")

// Step 4: Click Login button
TestObject btnLogin = FindObjectByXPath.findObject('(//a[@id=\'header-navbar-login-button\'])[1]')
WebUI.click(btnLogin, FailureHandling.STOP_ON_FAILURE)
println("✅ Clicked Login button")

// Step 5: Wait a moment to see the result
WebUI.delay(2)
println("✅ Test completed - Login button clicked")

// Close browser
WebUI.closeBrowser()
