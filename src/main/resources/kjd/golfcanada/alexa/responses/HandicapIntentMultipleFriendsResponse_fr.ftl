{
    "outputSpeech": {
        "type": "PlainText",
        "text": "J'ai trouvé ${count} amis avec le nom '${query}'. <#list friends as friend><#if friend.name??>${friend.name}<#else>Un ami</#if> a un indice de handicap de ${friend.handicap!"non disponible"}. </#list>"
    },
    "shouldEndSession": false
}
