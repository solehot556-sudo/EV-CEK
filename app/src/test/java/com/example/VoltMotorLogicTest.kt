package com.example

import com.example.data.local.SeedCatalogData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoltMotorLogicTest {

    @Test
    fun seedCatalog_containsUniqueIdsAndValidSpecs() {
        val motors = SeedCatalogData.getInitialMotors(1_000L)
        assertTrue("Catalog should have at least 50 models", motors.size >= 50)
        assertTrue("Catalog should have at least 18 brands", motors.map { it.brand }.distinct().size >= 18)
        assertEquals("All IDs must be unique", motors.size, motors.map { it.id }.distinct().size)

        motors.forEach { motor ->
            assertTrue("${motor.id} price must be > 0", motor.priceIdr > 0L)
            assertTrue("${motor.id} range must be > 0", motor.rangeKm > 0)
            assertTrue("${motor.id} power must be > 0", motor.motorPowerWatt > 0)
            assertTrue("${motor.id} warrantyYears must be >= 1", motor.warrantyYears >= 1)
            assertTrue("${motor.id} whPerKm must be reasonable", motor.whPerKm in 5.0..100.0)
        }
    }

    @Test
    fun comparisonDuel_identifiesEfficiencyCostClimbAndWarrantyWinners() {
        val motors = SeedCatalogData.getInitialMotors(1_000L)
        val avand = motors.first { it.id == "avand-sc121" }
        val cervo = motors.first { it.id == "alva-cervo" }

        // Avand lebih murah dan lebih irit Wh/km; ALVA Cervo jauh lebih kuat tanjakan & garansi lebih lama
        val cheaper = listOf(avand, cervo).minByOrNull { it.priceIdr }
        val strongerClimb = listOf(avand, cervo).maxByOrNull { it.hillClimbDegrees }
        val longerWarranty = listOf(avand, cervo).maxByOrNull { it.warrantyYears }

        assertEquals("avand-sc121", cheaper?.id)
        assertEquals("alva-cervo", strongerClimb?.id)
        assertEquals("alva-cervo", longerWarranty?.id)
    }
}
