import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import internal.GlobalVariable as GlobalVariable

/**
 * Create a new data submission
 * @param dccName - Data Commons name (optional, uses GlobalVariable.DCCname if not provided)
 * @param studySearch - Study to search for (optional, uses GlobalVariable.Study_search if not provided)
 * @param submissionName - Submission name to create (optional, uses GlobalVariable.Study_create if not provided)
 */
class CreateSubmission {
    static void create(String dccName = null, String studySearch = null, String submissionName = null) {
        String dcc = dccName ?: GlobalVariable.DCCname
        String study = studySearch ?: GlobalVariable.Study_search
        String submission = submissionName ?: GlobalVariable.Study_create
        
        // Step 1: Click Data Submissions dropdown
        TestObject dropdownSubmissions = FindObjectByXPath.findObject('//div[@id=\'navbar-dropdown-data-submissions\' and @role=\'button\']')
        WebUI.click(dropdownSubmissions, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 2: Click Create a Data Submission
        TestObject btnCreateSubmission = FindObjectByXPath.findObject('//button[text()=\'Create a Data Submission\']')
        WebUI.click(btnCreateSubmission, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 3: Click Data Commons dropdown
        TestObject dropdownDataCommons = FindObjectByXPath.findObject("//*[@id=\"mui-component-select-dataCommons\" and text()=\"${dcc}\"]")
        WebUI.click(dropdownDataCommons, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 4: Click Data Commons option
        TestObject optionDCC = FindObjectByXPath.findObject("//li[text()=\"${dcc}\"]")
        WebUI.click(optionDCC, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 5: Click Study ID dropdown
        TestObject dropdownStudyID = FindObjectByXPath.findObject('//div[@id="mui-component-select-studyID"]')
        WebUI.click(dropdownStudyID, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 6: Select Study
        TestObject optionStudy = FindObjectByXPath.findObject("//ul[@role=\"listbox\"]//li[@role=\"option\" and normalize-space(.)=\"${study}\"]")
        WebUI.click(optionStudy, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 7: Fill submission name in dialog
        TestObject inputSubmissionName = FindObjectByXPath.findObject('//*[@data-testid="create-data-submission-dialog-submission-name-input"]//input')
        WebUI.clearText(inputSubmissionName)
        WebUI.setText(inputSubmissionName, submission)
        WebUI.delay(3)
        
        // Step 8: Click Create button
        TestObject btnCreate = FindObjectByXPath.findObject('//button[@data-testid=\'create-data-submission-dialog-create-button\' and @form=\'create-submission-dialog-form\']')
        WebUI.click(btnCreate, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 9: Fill submission name filter
        TestObject inputSubmissionFilter = FindObjectByXPath.findObject('//input[@data-testid="submission-name-input" and @aria-labelledby="submission-name-filter"]')
        WebUI.clearText(inputSubmissionFilter)
        WebUI.setText(inputSubmissionFilter, submission)
        WebUI.delay(3)
        
        // Step 10: Click submission link
        TestObject linkSubmission = FindObjectByXPath.findObject("//a[.//span[@aria-label=\"${submission}\"]]")
        WebUI.click(linkSubmission, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        println("✅ Submission '${submission}' created and opened successfully")
    }
}
