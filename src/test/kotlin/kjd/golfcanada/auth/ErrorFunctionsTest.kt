package kjd.golfcanada.auth

import io.kotest.core.spec.style.DescribeSpec

class ErrorFunctionsTest: DescribeSpec({
    describe("invalidClientIdResponse") {
        it("should return 400 status and text") {
            val response = invalidAuthenticationRequest(ErrorCode.CLIENT_ERROR)

            response.statusCode = 400
            response.body = "Invalid Client Id: 'clientId'"
        }
    }

    describe("codeOrStateNotProvided") {
        it("should return 400 status and text") {
            val response = invalidAuthenticationRequest(ErrorCode.INVALID_CODE)

            response.statusCode = 400
            response.body = "Invalid code 'code' or state 'state'"
        }
    }
})