// ============================================================================
// CREATE ALL KATALON OBJECTS FROM JSON REGISTRY
// ============================================================================
// Run this in Katalon Studio: Tools > Groovy Script Console
// This will create all 40 objects with XPaths in Object Repository

import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.testobject.TestObjectProperty as TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType as ConditionType
import com.kms.katalon.core.testobject.ObjectRepository as ObjectRepository

println("="*60)
println("Creating Katalon Test Objects from JSON Registry")
println("="*60)

// All objects with their paths, IDs, and XPaths
def objects = [
    // auth.nih.gov/LoginMFA
    [path: "Object Repository/auth.nih.gov/LoginMFA/button_4", id: "ID_c20d27bc", xpath: "(//*[normalize-space(.)='Login.gov'])[1]", desc: "button from LoginMFA"],
    
    // hub-stage.datacommons.cancer.gov/data-submissions
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/text", id: "ID_d4e1aae2", xpath: "(//*)[1]", desc: "Discovered by AI"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/organization_filter", id: "ID_org_filter_001", xpath: "//input[@id=\"organization-filter\"]", desc: "organization filter input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/submission_name_input", id: "ID_sub_name_001", xpath: "//input[@data-testid=\"submission-name-input\"]", desc: "submission name input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/generic_table", id: "ID_generic_table_001", xpath: "//table[@data-testid=\"generic-table\"]", desc: "generic table"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_11", id: "ID_1bcedbdb", xpath: "//div[@id='navbar-dropdown-data-submissions' and @role='button']", desc: "data submissions dropdown"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_12", id: "ID_9bc56a38", xpath: "//button[text()='Create a Data Submission']", desc: "create submission button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/dropdown_13", id: "ID_988a2a2e", xpath: "//*[@id=\"mui-component-select-dataCommons\" and text()=\"GC\"]", desc: "data commons dropdown"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/option_14", id: "ID_7824b1a8", xpath: "//li[text()=\"GC\"]", desc: "GC option"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/dropdown_15", id: "ID_7011a92f", xpath: "//div[@id=\"mui-component-select-studyID\"]", desc: "study ID dropdown"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/option_16", id: "ID_383e84d1", xpath: "//ul[@role=\"listbox\"]//li[@role=\"option\" and normalize-space(.)=\"1CASofia - 1CA_Sofia\"]", desc: "study option"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/input_17", id: "ID_3c797626", xpath: "(//*[@data-testid=\"create-submission-dialog\"])//input[@name='name']", desc: "submission name input in dialog"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_18", id: "ID_fd60dbc8", xpath: "(//*[@data-testid=\"create-submission-dialog\"])//button[@data-testid='create-data-submission-dialog-create-button']", desc: "create button in dialog"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_2", id: "ID_c1bf258c", xpath: "//div[@data-testid='system-use-warning-dialog']//button[contains(., 'Continue')]", desc: "continue button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/link_3", id: "ID_f0a2425a", xpath: "(//a[@id='header-navbar-login-button'])[1]", desc: "login link"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_4", id: "ID_c20d27bc", xpath: "(//*[normalize-space(.)='Login.gov'])[1]", desc: "login.gov button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/input_5", id: "ID_b92fade3", xpath: "//input[@type='email']", desc: "email input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/input_6", id: "ID_096dd456", xpath: "//input[@type='password']", desc: "password input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_7", id: "ID_33b5218d", xpath: "(//*[normalize-space(.)='Submit'])[1]", desc: "submit button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/input_8", id: "ID_7a01fb49", xpath: "//input[@class='one-time-code-input__input']", desc: "TOTP input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_9", id: "ID_fcabe4f4", xpath: "(//*[normalize-space(.)='Submit'])[1]", desc: "submit button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_grant", id: "ID_grant_button_001", xpath: "//input[@name='action' and @value='Grant']", desc: "grant button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/option_17_newtestspn", id: "ID_e2eb18da", xpath: "//ul[@role=\"listbox\"]//li[@role=\"option\" and normalize-space(.)=\"NewTestSpn_laxmi\"]", desc: "study option"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/input_18_submission_name", id: "ID_2b78d9b9", xpath: "//*[@data-testid=\"create-data-submission-dialog-submission-name-input\"]//input", desc: "submission name input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_19_create_dialog", id: "ID_c2c0e820", xpath: "//button[@data-testid='create-data-submission-dialog-create-button' and @form='create-submission-dialog-form']", desc: "create dialog button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/input_20_submission_search", id: "ID_f4be329a", xpath: "//input[@data-testid=\"submission-name-input\" and @aria-labelledby=\"submission-name-filter\"]", desc: "submission search input"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/link_21_submission_name_table", id: "ID_732cc0e8", xpath: "//a[.//span[@aria-label=\"{text_value}\"]]", desc: "submission name link"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_22_choose_files", id: "ID_0a6489a1", xpath: "//button[@data-testid=\"metadata-upload-file-select-button\"]", desc: "choose files button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_22_5_upload_wait", id: "ID_c0a5d87a", xpath: "//button[@data-testid=\"metadata-upload-file-upload-button\"]", desc: "upload button wait"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_23_upload", id: "ID_2312863d", xpath: "//button[@data-testid=\"metadata-upload-file-upload-button\"]", desc: "upload button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/table_24_upload_activities", id: "ID_88ce6780", xpath: "//table[@data-testid=\"generic-table\"]", desc: "upload activities table"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/button_25_validate", id: "ID_abe42b26", xpath: "//button[@data-testid=\"validate-controls-validate-button\"]", desc: "validate button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/data-submissions/dropdown_26_node_type", id: "ID_node_type_dropdown_001", xpath: "//div[@id=\"mui-component-select-nodeType\"]", desc: "node type dropdown"],
    
    // hub-stage.datacommons.cancer.gov/home
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/home/button_2", id: "ID_c1bf258c", xpath: "//div[@data-testid='system-use-warning-dialog']//button[contains(., 'Continue')]", desc: "continue button"],
    [path: "Object Repository/hub-stage.datacommons.cancer.gov/home/link_3", id: "ID_f0a2425a", xpath: "(//a[@id='header-navbar-login-button'])[1]", desc: "login link"],
    
    // secure.login.gov/home
    [path: "Object Repository/secure.login.gov/home/input_5", id: "ID_b92fade3", xpath: "//input[@type='email']", desc: "email input"],
    [path: "Object Repository/secure.login.gov/home/input_6", id: "ID_096dd456", xpath: "//input[@type='password']", desc: "password input"],
    [path: "Object Repository/secure.login.gov/home/button_7", id: "ID_33b5218d", xpath: "(//*[normalize-space(.)='Submit'])[1]", desc: "submit button"],
    [path: "Object Repository/secure.login.gov/home/input_8", id: "ID_7a01fb49", xpath: "//input[@class='one-time-code-input__input']", desc: "TOTP input"],
    
    // secure.login.gov/sms
    [path: "Object Repository/secure.login.gov/sms/button_9", id: "ID_fcabe4f4", xpath: "(//*[normalize-space(.)='Submit'])[1]", desc: "submit button"]
]

int created = 0
int skipped = 0
int failed = 0

objects.each { obj ->
    try {
        // Check if object already exists
        try {
            def existing = findTestObject(obj.path)
            println("  ⚠️  Skipping (already exists): ${obj.path}")
            skipped++
            return
        } catch (Exception e) {
            // Object doesn't exist, create it
        }
        
        // Create new TestObject
        TestObject testObj = new TestObject()
        testObj.setObjectId(obj.id)
        
        // Add XPath property
        TestObjectProperty xpathProp = TestObjectProperty.createProperty("xpath", ConditionType.EQUALS, obj.xpath)
        testObj.addProperty(xpathProp)
        
        // Add to Object Repository
        ObjectRepository.addTestObject(obj.path, testObj)
        
        println("  ✓ Created: ${obj.path}")
        println("      XPath: ${obj.xpath}")
        created++
    } catch (Exception e) {
        println("  ❌ Failed to create ${obj.path}: ${e.message}")
        e.printStackTrace()
        failed++
    }
}

println("\n" + "="*60)
println("Migration Complete!")
println("="*60)
println("Created: ${created} objects")
println("Skipped: ${skipped} objects (already exist)")
println("Failed: ${failed} objects")
println("\nObjects are now available in Object Repository")
println("You can use them with: findTestObject('Object Repository/...')")
