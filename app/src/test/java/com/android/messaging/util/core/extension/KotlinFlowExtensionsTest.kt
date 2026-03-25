package com.android.messaging.util.core.extension

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KotlinFlowExtensionsTest {

    @Test
    fun typedFlow_emitsReturnedValue() {
        runTest {
            typedFlow {
                emit("before")
                return@typedFlow "after"
            }.test {
                org.junit.Assert.assertEquals("before", awaitItem())
                org.junit.Assert.assertEquals("after", awaitItem())
                awaitComplete()
            }
        }
    }

    @Test
    fun unitFlow_runsBlockThenEmitsUnit() {
        runTest {
            val collectedValues = mutableListOf<String>()

            unitFlow {
                collectedValues += "executed"
            }.test {
                org.junit.Assert.assertEquals(listOf("executed"), collectedValues)
                org.junit.Assert.assertEquals(Unit, awaitItem())
                awaitComplete()
            }
        }
    }
}
