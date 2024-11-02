package kjd.golfcanada.auth

import io.kotest.core.spec.style.DescribeSpec

class ErrorFunctionsTest: DescribeSpec({
    describe("invalidClientIdResponse") {
        it("should return 400 status and text") {
            val response = invalidClientIdResponse("clientId")

            response.statusCode = 400
            response.body = "Invalid Client Id: 'clientId'"
        }
    }

    describe("codeOrStateNotProvided") {
        it("should return 400 status and text") {
            val response = codeOrStateNotProvided("code", "state")

            response.statusCode = 400
            response.body = "Invalid code 'code' or state 'state'"
        }
    }
})