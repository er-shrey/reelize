package com.shrey.reelize

import com.google.common.truth.Truth.assertThat
import com.shrey.reelize.ui.keepRunningTips
import org.junit.Test

class KeepRunningTipsTest {
    @Test
    fun `sub-brands share their parent's steps`() {
        assertThat(keepRunningTips("Redmi").brand).isEqualTo("Xiaomi")
        assertThat(keepRunningTips("POCO").brand).isEqualTo("Xiaomi")
        assertThat(keepRunningTips("iQOO").brand).isEqualTo("Vivo")
    }

    @Test
    fun `brands that clear Recents are told to lock the app there`() {
        listOf("Xiaomi", "OPPO", "realme", "OnePlus", "vivo", "HUAWEI", "HONOR").forEach {
            assertThat(keepRunningTips(it).steps.first()).contains("Lock Reelize in Recents")
        }
    }

    @Test
    fun `only Xiaomi offers its Autostart screen`() {
        assertThat(keepRunningTips("Xiaomi").extraScreen?.label).isEqualTo("Autostart")
        assertThat(keepRunningTips("samsung").extraScreen).isNull()
        assertThat(keepRunningTips("Google").extraScreen).isNull()
    }

    @Test
    fun `unknown brands get the generic battery steps`() {
        val tips = keepRunningTips("Nothing")
        assertThat(tips.brand).isEqualTo("Nothing")
        assertThat(tips.steps.first()).contains("Unrestricted")
    }
}
