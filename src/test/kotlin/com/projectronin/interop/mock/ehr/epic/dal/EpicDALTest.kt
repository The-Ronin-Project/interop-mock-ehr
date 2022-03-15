package com.projectronin.interop.mock.ehr.epic.dal

import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class EpicDALTest {

    @Test
    fun `code coverage test`() {
        EpicDAL(mockk(), mockk(), mockk())
    }
}
