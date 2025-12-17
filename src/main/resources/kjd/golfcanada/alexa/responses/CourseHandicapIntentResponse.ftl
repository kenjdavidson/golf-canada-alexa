{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if tees?size == 1 && tees[0].courseHandicap??>Your course handicap<#if facilityName??> at ${facilityName}</#if><#if tees[0].name??> from the ${tees[0].name} tees</#if> is ${tees[0].courseHandicap}.<#if tees[0].targetScore??> Your expected score is ${tees[0].targetScore}.</#if><#else>Your expected score<#if facilityName??> at ${facilityName}</#if> from each tee are: <#list tees as tee><#if tee.targetScore??>${tee.name} tees ${tee.targetScore}<#if tee?has_next>, </#if></#if></#list>.</#if>"
    },
    "shouldEndSession": false
}
