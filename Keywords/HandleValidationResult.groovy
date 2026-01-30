/**
 * Handle validation results - check success and throw exception with details if failed
 * @param result - Map with 'success' boolean and 'mismatches' list
 * @param validationName - Name of validation for error messages
 * @param throwOnFailure - Whether to throw exception on failure (default: true)
 * @return boolean - true if validation passed, false if failed
 */
class HandleValidationResult {
    static boolean checkAndThrow(Map<String, Object> result, String validationName, boolean throwOnFailure = true) {
        if (!result['success']) {
            List mismatches = result['mismatches'] ?: []
            println("\n❌ ${validationName} validation failed: ${mismatches.size()} mismatch(es)")
            
            // Log all mismatches
            mismatches.each { mismatch ->
                println("   - Row ${mismatch['row']}, Column '${mismatch['column']}': Expected '${mismatch['expected']}', Actual '${mismatch['actual']}'" + 
                       (mismatch['matchType'] ? " (matchType: ${mismatch['matchType']})" : ""))
            }
            
            if (throwOnFailure) {
                throw new Exception("${validationName} validation failed: ${mismatches.size()} mismatches")
            }
            
            return false
        } else {
            println("✅ ${validationName} validation passed")
            return true
        }
    }
    
    /**
     * Check validation result but don't throw - just log warnings
     * @param result - Map with 'success' boolean and 'mismatches' list
     * @param validationName - Name of validation for log messages
     * @return boolean - true if validation passed, false if failed
     */
    static boolean checkAndLog(Map<String, Object> result, String validationName) {
        return checkAndThrow(result, validationName, false)
    }
    
    /**
     * Handle Data View validation result with node-specific details
     * @param result - Map with 'success', 'nodeResults', and 'mismatches'
     * @param validationName - Name of validation for error messages
     * @param throwOnFailure - Whether to throw exception on failure (default: true)
     * @return boolean - true if validation passed, false if failed
     */
    static boolean checkDataViewResult(Map<String, Object> result, String validationName, boolean throwOnFailure = true) {
        if (!result['success']) {
            List failedNodes = result['nodeResults'].findAll { !it['success'] }.collect { it['nodeType'] }
            List allMismatches = result['mismatches'] ?: []
            println("❌ ${validationName} validation failed for node type(s): ${failedNodes.join(', ')}")
            println("   Total mismatches: ${allMismatches.size()}")
            
            // Log first few mismatches
            allMismatches.take(5).each { mismatch ->
                println("   - Row ${mismatch['row']}, Column ${mismatch['column']}: Expected '${mismatch['expected']}', Actual '${mismatch['actual']}'")
            }
            
            if (throwOnFailure) {
                throw new Exception("${validationName} validation failed for node type(s): ${failedNodes.join(', ')}")
            }
            
            return false
        } else {
            println("✅ ${validationName} validation passed for all node types")
            return true
        }
    }
}
