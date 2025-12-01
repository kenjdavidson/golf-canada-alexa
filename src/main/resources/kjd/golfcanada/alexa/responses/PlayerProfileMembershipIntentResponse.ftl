{
    "outputSpeech": {
        "type": "PlainText",
        "text": "Hello ${firstName} ${lastName}. <#if membershipLevel?has_content>Your membership level is ${membershipLevel}. </#if><#if golfCanadaCardId?has_content>Your Golf Canada card ID is ${golfCanadaCardId}. </#if><#if handicap?has_content>Your handicap is ${handicap}.</#if>"
    },
    "shouldEndSession": "false"
}
