package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/epic")
class EpicServer {

    @GetMapping("/oauth2/token")
    fun getAuthToken(): EpicAuthentication {
        return EpicAuthentication(
            accessToken = UUID.randomUUID().toString(),
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search"
        )
    }
}
