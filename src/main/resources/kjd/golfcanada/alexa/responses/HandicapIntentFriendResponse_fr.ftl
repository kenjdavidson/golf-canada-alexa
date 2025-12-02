{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if name??>L'index de handicap actuel de ${name} est<#else>L'index de handicap actuel du joueur est</#if> ${handicap!"non disponible"}.<#if lowValue??> Son index de handicap bas est ${lowValue?string["0.0"]}.</#if>"
    },
    "shouldEndSession": false
}
