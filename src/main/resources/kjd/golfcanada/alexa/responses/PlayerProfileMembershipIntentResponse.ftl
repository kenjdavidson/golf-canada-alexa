{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if firstName?? && lastName??>Hello ${firstName} ${lastName}. </#if><#if membershipLevel??>Your membership level is ${membershipLevel}. </#if><#if golfCanadaCardId??>Your Golf Canada card ID is ${golfCanadaCardId}. </#if><#if handicap??>Your handicap is ${handicap}.</#if>"
    },
    "shouldEndSession": "false"
}
