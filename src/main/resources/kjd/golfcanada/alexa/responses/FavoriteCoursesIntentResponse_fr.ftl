{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if count == 0>Vous n'avez pas de cours favoris dans votre liste.<#else>Vous avez ${count} cours favori<#if count != 1>s</#if>. <#list courses as course>${course.name}<#if course.city??> à ${course.city}</#if><#if course.region??>, ${course.region}</#if>. </#list></#if>"
    },
    "shouldEndSession": false
}
