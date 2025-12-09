{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if count == 0>You don't have any favorite courses in your list.<#else>You have ${count} favorite course<#if count != 1>s</#if>. <#list courses as course>${course.name}<#if course.city??> in ${course.city}</#if><#if course.region??>, ${course.region}</#if>. </#list></#if>"
    },
    "shouldEndSession": false
}
