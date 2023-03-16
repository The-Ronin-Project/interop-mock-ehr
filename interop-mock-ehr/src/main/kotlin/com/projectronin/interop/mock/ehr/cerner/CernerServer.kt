package com.projectronin.interop.mock.ehr.cerner

import com.projectronin.interop.ehr.cerner.auth.CernerAuthentication
import com.projectronin.interop.mock.ehr.cerner.dal.CernerDAL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/cerner")
class CernerServer(private var dal: CernerDAL) {

    @Operation(summary = "Returns Mock Cerner Authentication Token", description = "Returns token if successful")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CernerAuthentication::class)
                    )
                ]
            )
        ]
    )
    @PostMapping("/oauth2/token")
    fun getAuthToken(): CernerAuthentication {
        return CernerAuthentication(
            accessToken = UUID.randomUUID().toString(),
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search",
            refreshToken = null
        )
    }
}
