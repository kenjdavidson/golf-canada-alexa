{
    "outputSpeech": {
        "type": "PlainText",
        "text": "Votre score prévu<#if courseName??> à ${courseName}</#if> pour chaque té sont: <#list tees as tee>tés ${tee.name} ${tee.score}<#if tee?has_next>, </#if></#list>."
    },
    "shouldEndSession": false
}
