package com.example

import com.example.location.CityLocationEngine
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.RightsStatus
import org.junit.Assert.*
import org.junit.Test

class PrivacyLocationTest {
    @Test fun rejectsStaleFutureAndUnmeasuredLocations() {
        fun usable(age: Long, accuracy: Float = 100f) = CityLocationEngine.isUsableApproximateFix(age, accuracy, 26.8, 80.9)
        assertTrue(usable(0))
        assertTrue(usable(900000))
        assertFalse(usable(900001))
        assertFalse(usable(-1))
        assertFalse(usable(1, 0f))
        assertFalse(usable(1, Float.NaN))
        assertFalse(CityLocationEngine.isUsableApproximateFix(1, 10f, 91.0, 80.0))
    }
    @Test fun publisherRegistryDoesNotInventPermissionReview() {
        assertTrue(ValidationAndRightsEngine.ALLOWLISTED_SOURCES.all { !it.termsReviewed && it.defaultRightsStatus == RightsStatus.LINK_ONLY })
        assertTrue(ValidationAndRightsEngine.isVerificationCheckStale("2099-01-01T00:00:00Z", 1000))
    }
}
