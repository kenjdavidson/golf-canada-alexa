{
    "outputSpeech": {
        "type": "PlainText",
        "text": "Your expected score<#if courseName??> at ${courseName}</#if> from each tee are: <#list tees as tee>${tee.name} tees ${tee.score}<#if tee?has_next>, </#if></#list>."
    },
    "shouldEndSession": false
}
