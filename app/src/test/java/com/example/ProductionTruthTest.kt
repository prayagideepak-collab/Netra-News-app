package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.NetraRepository
import com.example.data.local.AppDatabase
import com.example.domain.BulletinDataStatus
import com.example.location.LocationPreference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductionTruthTest {
    @Test fun disconnectedAdaptersNeverInventNewsOrAllClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = NetraRepository(context, db.netraDao(), db.articleDao())
            val location = LocationPreference()
            val now = System.currentTimeMillis()
            val envelope = repo.buildVerifiedStage1Envelope(location, now)
            assertTrue(envelope.items.isEmpty())
            assertTrue(envelope.sourceHealth.isEmpty())
            assertFalse(envelope.generatedAtUtcIso.startsWith("2026-11-06T00:00"))
            for (offline in listOf(false, true)) {
                val weather = repo.getCivicWeatherBulletins(location, offline)
                val earthquakes = repo.getCivicEarthquakeBulletins(location, offline)
                assertTrue(weather.all { it.status == BulletinDataStatus.UNAVAILABLE_OFFLINE && it.issuedAtIso == null && it.temperatureCelsius == null })
                assertTrue(earthquakes.all { it.status == BulletinDataStatus.UNAVAILABLE_OFFLINE && it.issuedAtIso == null && it.magnitudeMw == null })
            }
        } finally { db.close() }
    }
}
