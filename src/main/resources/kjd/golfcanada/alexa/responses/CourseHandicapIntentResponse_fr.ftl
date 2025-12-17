{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if tees?size == 1 && tees[0].courseHandicap??>Votre handicap de parcours<#if facilityName??> à ${facilityName}</#if><#if tees[0].name??> depuis les départs ${tees[0].name}</#if> est ${tees[0].courseHandicap}.<#if tees[0].targetScore??> Votre score attendu est ${tees[0].targetScore}.</#if><#else>Votre score attendu<#if facilityName??> à ${facilityName}</#if> de chaque départ sont: <#list tees as tee><#if tee.targetScore??>départs ${tee.name} ${tee.targetScore}<#if tee?has_next>, </#if></#if></#list>.</#if>"
    },
    "shouldEndSession": false
}
