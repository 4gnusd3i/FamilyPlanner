package io.github.by4gnusd3i.familyplanner.domain.setup

data class SetupRequest(
    val familyName: String,
    val firstMemberName: String,
)

object SetupValidator {
    fun validate(familyName: String, firstMemberName: String): SetupRequest {
        val normalizedFamilyName = familyName.trim()
        val normalizedFirstMemberName = firstMemberName.trim()
        require(normalizedFamilyName.isNotEmpty()) { "Family name is required." }
        require(normalizedFirstMemberName.isNotEmpty()) { "First member name is required." }
        return SetupRequest(
            familyName = normalizedFamilyName,
            firstMemberName = normalizedFirstMemberName,
        )
    }
}
