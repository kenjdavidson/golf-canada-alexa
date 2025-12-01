{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if firstName?? && lastName??>Hello ${firstName} ${lastName}. </#if><#if membershipLevel??>Your membership level is ${membershipLevel}. </#if><#if expirationDate??>Your membership expires on ${expirationDate}. </#if><#if golfCanadaCardId??>Your Golf Canada card ID is ${golfCanadaCardId}. </#if><#if facilityName??>Your default scoring is set to ${facilityName}. </#if><#if postHoleByHole??>Hole by hole scoring is <#if postHoleByHole>required<#else>not required</#if>.</#if>"
    },
    "shouldEndSession": "false"
}
